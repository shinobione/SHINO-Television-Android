"""Read-only TV HOME bridge. No Codex credentials, collection or remote control.

Default loopback; RFC1918 LAN requires TLS and an explicit --allow-lan.
Run manually only after owner authorization. Never changes firewall/autostart.
"""
import argparse
import asyncio
from datetime import datetime, timezone
import hashlib
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import ipaddress
import json
import math
import os
from pathlib import Path
import ssl
import threading
from urllib.request import build_opener, ProxyHandler, HTTPRedirectHandler

UTC = timezone.utc
MAX_BODY = 262144

def instant(value):
    stamp = datetime.fromisoformat(value.replace('Z', '+00:00'))
    if stamp.tzinfo is None:
        raise ValueError('timezone required')
    return stamp.astimezone(UTC)

def number(value, low, high):
    if type(value) not in (int, float) or not math.isfinite(value) or not low <= value <= high:
        raise ValueError('invalid metric')
    return value

def allowed_ip(value):
    try:
        ip = ipaddress.IPv4Address(value)
        return ip == ipaddress.IPv4Address('127.0.0.1') or any(ip in ipaddress.ip_network(net) for net in ('10.0.0.0/8','172.16.0.0/12','192.168.0.0/16'))
    except ValueError:
        return False

def project_history(row, now):
    captured = instant(row['utc'])
    age = (now - captured).total_seconds()
    if age < -120:
        raise ValueError('future sample')
    if row.get('source') not in ('Codex CLI / account/rateLimits/read', 'Codex CLI'):
        raise ValueError('unverified source')
    stale = age > 900
    result = dict(capturedAt=row['utc'], source='PERISCOPE_HISTORY', activity='UNVERIFIED')
    for source, target in [('five', 'fiveHour'), ('week', 'weekly')]:
        val = row.get(source)
        reset = row.get(source + 'ResetUtc')
        if reset is not None and instant(reset) <= now:
            stale = True
        result[target] = None if val is None else dict(remainingPercent=number(val, 0, 100), resetsAt=reset)
    result['status'] = 'STALE' if stale else ('AVAILABLE' if result['fiveHour'] or result['weekly'] else 'UNAVAILABLE')
    return result

def unavailable():
    return dict(status='UNAVAILABLE', capturedAt=None, source='PERISCOPE_HISTORY', activity='UNVERIFIED', fiveHour=None, weekly=None)

def read_history(path, now):
    try:
        with Path(path).open('rb') as stream:
            stream.seek(0, 2)
            size = stream.tell()
            stream.seek(max(0, size - MAX_BODY))
            rows = stream.read(MAX_BODY).decode('utf-8-sig', errors='replace').splitlines()
        for line in reversed(rows):
            try:
                return project_history(json.loads(line), now)
            except (ValueError, KeyError, TypeError, AttributeError):
                continue
    except OSError:
        pass
    return unavailable()

def project_telemetry(row):
    if row.get('ok') is not True:
        raise ValueError('no telemetry')
    gpu = row.get('gpu_available') is True
    return dict(status='AVAILABLE', cpuPercent=number(row['cpu_usage'], 0, 100),
                ramUsedGb=number(row['memory_used_gb'], 0, 65536),
                gpuPercent=number(row['gpu_usage'], 0, 100) if gpu else None,
                gpuTemperatureC=number(row['gpu_temp_c'], -40, 150) if gpu else None)

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None

def read_telemetry(enabled):
    if not enabled:
        return dict(status='UNAVAILABLE')
    try:
        # Existing SHINO LINK local server only; never contacts a SmallTV.
        opener = build_opener(ProxyHandler({}), NoRedirect())
        with opener.open('http://127.0.0.1:8765/metrics', timeout=2) as response:
            raw = response.read(MAX_BODY + 1)
        if len(raw) > MAX_BODY:
            raise ValueError('oversized telemetry')
        return project_telemetry(json.loads(raw))
    except Exception:
        return dict(status='UNAVAILABLE')

async def smtc_snapshot():
    # Optional maintained WinRT projection; no media-control endpoint exposed.
    from winsdk.windows.media.control import GlobalSystemMediaTransportControlsSessionManager as Manager
    manager = await Manager.request_async()
    session = manager.get_current_session()
    if session is None:
        return dict(status='EMPTY', source='PC')
    props = await session.try_get_media_properties_async()
    timeline = session.get_timeline_properties()
    info = session.get_playback_info()
    state = str(info.playback_status).lower()
    return dict(status='AVAILABLE', source='PC', title=str(props.title)[:256], artist=str(props.artist)[:256],
                app=str(session.source_app_user_model_id)[:128], playing='playing' in state or int(info.playback_status) == 4,
                positionMs=max(0, int(timeline.position.total_seconds() * 1000)),
                durationMs=max(0, int(timeline.end_time.total_seconds() * 1000)), artwork=None)

def read_media(enabled):
    if enabled:
        try:
            async def bounded():
                return await asyncio.wait_for(smtc_snapshot(), timeout=3)
            return asyncio.run(bounded())
        except Exception:
            pass
    return dict(status='UNAVAILABLE', source='PC')

def snapshot(history, telemetry=False, smtc=False):
    now = datetime.now(UTC)
    return dict(schemaVersion=1, generatedAt=now.isoformat(), codex=read_history(history, now),
                media=read_media(smtc), telemetry=read_telemetry(telemetry))

class BridgeServer(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = False
    tls_context = None

    def __init__(self, *args, **kwargs):
        self.connection_slots = threading.BoundedSemaphore(16)
        super().__init__(*args, **kwargs)

    def process_request(self, request, address):
        # Bound unauthenticated connections, including TLS handshakes.
        if not self.connection_slots.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try:
            super().process_request(request, address)
        except Exception:
            self.connection_slots.release()
            self.shutdown_request(request)

    def process_request_thread(self, request, address):
        try:
            request.settimeout(5)
            if self.tls_context is not None:
                # Negotiate inside a bounded worker, never in the accept loop.
                request = self.tls_context.wrap_socket(request, server_side=True, do_handshake_on_connect=False)
                request.do_handshake()
            super().process_request_thread(request, address)
        except (ssl.SSLError, OSError):
            self.shutdown_request(request)
        finally:
            self.connection_slots.release()

def handler_for(token, supplier):
    gate = threading.BoundedSemaphore(4)
    class Handler(BaseHTTPRequestHandler):
        server_version = 'TVHOME/0.1'
        sys_version = ''
        def setup(self):
            super().setup()
            self.connection.settimeout(5)

        def do_GET(self):
            if not allowed_ip(self.client_address[0]):
                return self.send_empty(403)
            provided = self.headers.get('Authorization', '')
            if not hmac.compare_digest(provided.encode('utf8'), ('Bearer ' + token).encode('utf8')):
                return self.send_empty(401)
            if self.path != '/v1/snapshot':
                return self.send_empty(404)
            if not gate.acquire(blocking=False):
                return self.send_empty(503)
            try:
                body = json.dumps(supplier(), allow_nan=False, ensure_ascii=False).encode('utf8')
                self.send_response(200)
                self.send_header('Content-Type', 'application/json; charset=utf-8')
                self.send_header('Cache-Control', 'no-store')
                self.send_header('X-Content-Type-Options', 'nosniff')
                self.send_header('Content-Length', str(len(body)))
                self.end_headers()
                self.wfile.write(body)
            except (BrokenPipeError, ConnectionResetError, TimeoutError):
                pass
            finally:
                gate.release()

        def send_empty(self, status):
            self.send_response(status)
            self.send_header('Content-Length', '0')
            self.send_header('Cache-Control', 'no-store')
            self.end_headers()

        def do_POST(self): self.send_empty(405)
        def do_PUT(self): self.send_empty(405)
        def do_DELETE(self): self.send_empty(405)
        def do_PATCH(self): self.send_empty(405)
        def log_message(self, *args): pass  # Never log headers/tokens/client addresses.
    return Handler

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bind', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=8766)
    parser.add_argument('--allow-lan', action='store_true')
    parser.add_argument('--cert')
    parser.add_argument('--key')
    parser.add_argument('--history', type=Path, default=Path(os.environ.get('LOCALAPPDATA', '.')) / 'SHINO-CODEX-MONITOR/history.jsonl')
    parser.add_argument('--link-loopback', action='store_true')
    parser.add_argument('--smtc', action='store_true')
    args = parser.parse_args()
    if not allowed_ip(args.bind) or not 1024 <= args.port <= 65535:
        parser.error('Use a specific RFC1918 IPv4 or 127.0.0.1, port 1024..65535')
    if args.bind != '127.0.0.1' and not (args.allow_lan and args.cert and args.key):
        parser.error('LAN requires --allow-lan, --cert and --key')
    token = os.environ.get('TVHOME_TOKEN', '')
    if len(token) < 32 or len(token) > 256 or not token.isascii() or not token.isalnum():
        parser.error('Set TVHOME_TOKEN to an independently generated 32..256 character alphanumeric local pairing secret')
    server = BridgeServer((args.bind, args.port), handler_for(token, lambda: snapshot(args.history, args.link_loopback, args.smtc)))
    if args.cert and args.key:
        ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        ctx.minimum_version = ssl.TLSVersion.TLSv1_2
        ctx.load_cert_chain(args.cert, args.key)
        server.tls_context = ctx
        der = ssl.PEM_cert_to_DER_cert(Path(args.cert).read_text('ascii'))
        print('Certificate SHA256:', hashlib.sha256(der).hexdigest())
    print('TV HOME read-only bridge ready. No firewall or startup changes.')
    try:
        server.serve_forever(poll_interval=0.5)
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()

if __name__ == '__main__':
    main()
