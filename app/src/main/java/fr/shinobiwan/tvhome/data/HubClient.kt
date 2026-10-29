package fr.shinobiwan.tvhome.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import fr.shinobiwan.tvhome.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Instant
import javax.net.ssl.*

fun validReaderToken(value: String) = value.matches(Regex("[A-Za-z0-9_-]{16,128}"))
fun pairingIdentity(s: Settings): String = sha256("${s.protocol}|${s.host}|${s.port}|${s.caPem}|${s.pin}|${s.encryptedToken}".toByteArray())
internal fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

class HubFailure(val kind: String) : Exception(kind) {
    val pairingRequired get() = kind == "UNAUTHORIZED" || kind == "READ_FORBIDDEN"
    val messageForUser get() = when(kind) {
        "UNAUTHORIZED" -> "HUB : jeton TV_HOME refusé (401). Vérifiez l'appairage."
        "READ_FORBIDDEN" -> "HUB : identité sans droit de lecture (403)."
        "NO_DATA" -> "HUB connecté · aucune donnée publiée"
        "CLOCK_QUALIFICATION_REQUIRED" -> "HUB : horloges à qualifier"
        "CONFIGURATION" -> "HUB : renseignez CA publique, empreinte et jeton TV_HOME"
        "TLS" -> "HUB : certificat, CA, nom ou empreinte non valide"
        else -> "HUB indisponible · dernières données en cache"
    }
}

/** Never follows redirects or exposes server bodies/credentials in an error. */
fun readHubBytes(connection: HttpURLConnection, limit: Int): ByteArray {
    connection.connectTimeout=5000;connection.readTimeout=5000;connection.instanceFollowRedirects=false
    connection.useCaches=false;connection.requestMethod="GET"
    try {
        when(connection.responseCode) {
            200 -> Unit
            401 -> throw HubFailure("UNAUTHORIZED")
            403 -> throw HubFailure("READ_FORBIDDEN")
            503 -> {
                val code=try {
                    val bytes=connection.errorStream?.use { it.readBytesBounded(4096) }
                    if(bytes==null) "UNAVAILABLE" else JSONObject(String(bytes,Charsets.UTF_8)).getJSONObject("error").getString("code")
                } catch (_:Exception) { "UNAVAILABLE" }
                throw HubFailure(if(code in setOf("NO_DATA","CLOCK_QUALIFICATION_REQUIRED"))code else "UNAVAILABLE")
            }
            else -> throw HubFailure("UNAVAILABLE")
        }
        require(connection.contentLengthLong <= limit)
        return connection.inputStream.use { it.readBytesBounded(limit) }
    } finally { connection.disconnect() }
}
private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
    val out=ByteArrayOutputStream();val buffer=ByteArray(4096)
    while(true) { val n=read(buffer);if(n<0)break;require(out.size()+n<=limit);out.write(buffer,0,n) }
    return out.toByteArray()
}

/** Explicit app-only CA anchors + normal hostname verification + additional leaf pin. */
fun hubTrustManager(caPem: String, pin: String): X509TrustManager {
    require(caPem.length in 1..16384 && !caPem.contains("PRIVATE KEY"))
    require(pin.matches(Regex("[0-9a-f]{64}")))
    val certificates=CertificateFactory.getInstance("X.509").generateCertificates(caPem.byteInputStream())
    require(certificates.size in 1..4)
    val store=KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null) }
    certificates.forEachIndexed { index, cert ->
        require(cert is X509Certificate && cert.basicConstraints>=0);cert.checkValidity();store.setCertificateEntry("hub-ca-$index",cert)
    }
    val factory=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(store) }
    val delegate=factory.trustManagers.filterIsInstance<X509TrustManager>().single()
    val expected=pin.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    return object:X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) { throw java.security.cert.CertificateException("Reader only") }
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            delegate.checkServerTrusted(chain,authType)
            if(chain.isEmpty() || !MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(chain[0].encoded),expected))
                throw java.security.cert.CertificateException("Pin mismatch")
        }
    }
}

class HubClient(private val context: Context) {
    private val cache get()=File(context.filesDir,"snapshot-v2.json")
    private var artKey: String?=null
    private var artBitmap: Bitmap?=null
    fun cached(s: Settings): Snapshot? = try {
        if(cache.length()>262144) null else {
            val data=JSONObject(cache.readText())
            if(data.getString("pairing")!=pairingIdentity(s))null else parseHubSnapshot(data.getJSONObject("snapshot").toString())
        }
    } catch (_:Exception) { null }
    fun clearCache() { cache.delete();artKey=null;artBitmap=null }
    suspend fun fetch(s: Settings, secret: String): Snapshot = withContext(Dispatchers.IO) {
        if(!isLanIpv4(s.host) || s.port.toIntOrNull() !in 1024..65535 || !validReaderToken(secret) || s.caPem.isBlank() || !s.pin.matches(Regex("[0-9a-f]{64}")))
            throw HubFailure("CONFIGURATION")
        val ssl=try { SSLContext.getInstance("TLSv1.3").apply { init(null,arrayOf(hubTrustManager(s.caPem,s.pin)),null) } }
            catch (_:Exception) { throw HubFailure("TLS") }
        fun request(path: String): HttpsURLConnection {
            val c=URL("https://${s.host}:${s.port}$path").openConnection(Proxy.NO_PROXY) as HttpsURLConnection
            c.sslSocketFactory=ssl.socketFactory
            // HttpsURLConnection's standard hostname verifier remains enabled (including IP SAN).
            c.setRequestProperty("Authorization","Bearer $secret")
            return c
        }
        val raw=try { String(readHubBytes(request("/hub/v1/snapshot"),262144),Charsets.UTF_8) }
            catch (_:SSLException) { throw HubFailure("TLS") }
        var snapshot=parseHubSnapshot(raw)
        require(snapshot.generatedAt <= Instant.now().plusSeconds(2))
        val tmp=File(context.filesDir,"snapshot-v2.tmp")
        tmp.writeText(JSONObject().put("pairing",pairingIdentity(s)).put("snapshot",JSONObject(raw)).toString())
        check(tmp.renameTo(cache))
        val ref=snapshot.artworkReference
        if(ref==null || ref.mediaType!="image/jpeg" || snapshot.media.status!="AVAILABLE") {artKey=null;artBitmap=null}
        else if(snapshot.mediaMeta?.isCurrent(Instant.now())==true) {
            val key="${pairingIdentity(s)}|${ref.id}"
            if(artKey!=key) {
                artKey=null;artBitmap=null
                try {
                    val bytes=readHubBytes(request("/hub/v1/artwork/${ref.id}"),40960)
                    require(bytes.size==ref.bytes && sha256(bytes)==ref.id)
                    val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                    BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                    require(bounds.outMimeType=="image/jpeg" && bounds.outWidth==ref.width && bounds.outHeight==ref.height)
                    artBitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size)
                    if(artBitmap!=null)artKey=key
                } catch(e:CancellationException){throw e} catch (_:Exception){ /* Keep text and telemetry if only artwork fails. */ }
            }
            if(artKey==key)snapshot=snapshot.copy(media=snapshot.media.copy(artwork=artBitmap))
        }
        snapshot
    }
}
