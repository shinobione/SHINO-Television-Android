package fr.shinobiwan.tvhome.data

import android.content.Context
import fr.shinobiwan.tvhome.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.Proxy
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import javax.net.ssl.*

private const val MAX_BYTES = 262144
fun readLimited(connection: java.net.HttpURLConnection): String {
    connection.connectTimeout=5000; connection.readTimeout=5000; connection.instanceFollowRedirects=false
    try {
        require(connection.responseCode == 200) { "HTTP unavailable" }
        return connection.inputStream.use { stream ->
            val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(4096)
            while (true) { val n=stream.read(buffer); if(n<0) break; require(out.size()+n<=MAX_BYTES); out.write(buffer,0,n) }
            out.toString("UTF-8")
        }
    } finally { connection.disconnect() }
}
class BridgeClient(private val context: Context) {
    private val hub=HubClient(context)
    private val cache get()=File(context.filesDir,"snapshot-v1.json")
    private fun identity(s: Settings): String = MessageDigest.getInstance("SHA-256").digest("${s.host}|${s.port}|${s.pin}|${s.encryptedToken}".toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
    fun cached(s: Settings): Snapshot? = try {
        if(s.protocol=="HUB_V2") hub.cached(s) else if(cache.length()>MAX_BYTES) null else {
            val stored=JSONObject(cache.readText())
            if(stored.getString("pairing")!=identity(s)) null else parseSnapshot(stored.getJSONObject("snapshot").toString())
        }
    } catch (_: Exception) { null }
    suspend fun fetch(s: Settings, secret: String): Snapshot = withContext(Dispatchers.IO) {
        if(s.protocol=="HUB_V2") return@withContext hub.fetch(s,secret)
        require(isLanIpv4(s.host)); require(s.port.toIntOrNull() in 1024..65535)
        val pin=s.pin.lowercase().replace(":","").replace(" ","")
        require(pin.matches(Regex("[0-9a-f]{64}")))
        require(secret.length in 32..256 && secret.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' })
        val manager=object:X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain:Array<X509Certificate>, authType:String) { throw java.security.cert.CertificateException("client not supported") }
            override fun checkServerTrusted(chain:Array<X509Certificate>, authType:String) {
                if(chain.isEmpty()) throw java.security.cert.CertificateException("empty chain")
                chain[0].checkValidity()
                val actual=MessageDigest.getInstance("SHA-256").digest(chain[0].encoded)
                val expected=pin.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                if(!MessageDigest.isEqual(actual,expected)) throw java.security.cert.CertificateException("pin mismatch")
            }
        }
        val ssl=SSLContext.getInstance("TLS").apply { init(null,arrayOf(manager),SecureRandom()) }
        val connection=URL("https://${s.host}:${s.port}/v1/snapshot").openConnection(Proxy.NO_PROXY) as HttpsURLConnection
        connection.sslSocketFactory=ssl.socketFactory
        // Identity is the exact out-of-band certificate fingerprint, even for IP-only self-signed certificates.
        connection.hostnameVerifier=HostnameVerifier { _,session ->
            val cert=session.peerCertificates.firstOrNull() as? X509Certificate
            cert != null && MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString("") { "%02x".format(it.toInt() and 255) } == pin
        }
        connection.setRequestProperty("Authorization","Bearer $secret")
        val raw=readLimited(connection)
        val result=parseSnapshot(raw)
        require(result.generatedAt <= Instant.now().plusSeconds(120))
        val tmp=File(context.filesDir,"snapshot-v1.tmp")
        tmp.writeText(JSONObject().put("pairing",identity(s)).put("snapshot",JSONObject(raw)).toString()); check(tmp.renameTo(cache))
        result
    }
    fun clearCache() { cache.delete();hub.clearCache() }
}
class WeatherClient(private val context: Context) {
    private val cache get()=File(context.filesDir,"weather-v1.json")
    private fun decode(raw:String): Weather {
        val j=JSONObject(raw); return Weather(j.getString("city"),j.getDouble("celsius"),j.getInt("code"),Instant.parse(j.getString("observedAt")),Instant.parse(j.getString("fetchedAt")))
    }
    fun cached():Weather?=try { if(cache.length()<=MAX_BYTES) decode(cache.readText()) else null } catch (_:Exception){null}
    suspend fun fetch(city:String):Weather = withContext(Dispatchers.IO) {
        val previous=cached()
        if(previous?.city == city && previous.fetchedAt > Instant.now().minusSeconds(1800) && previous.fetchedAt <= Instant.now()) return@withContext previous
        val query=java.net.URLEncoder.encode(city,"UTF-8")
        val geo=JSONObject(readLimited(URL("https://geocoding-api.open-meteo.com/v1/search?name=$query&count=1&language=fr&format=json").openConnection(Proxy.NO_PROXY) as HttpsURLConnection))
            .getJSONArray("results").getJSONObject(0)
        val lat=geo.getDouble("latitude"); val lon=geo.getDouble("longitude")
        require(lat.isFinite() && lat in -90.0..90.0 && lon.isFinite() && lon in -180.0..180.0)
        val current=JSONObject(readLimited(URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&timeformat=unixtime&timezone=Europe%2FParis").openConnection(Proxy.NO_PROXY) as HttpsURLConnection)).getJSONObject("current")
        val temp=current.getDouble("temperature_2m"); require(temp.isFinite() && temp in -100.0..80.0)
        val now=Instant.now(); val observed=Instant.ofEpochSecond(current.getLong("time"))
        require(observed<=now.plusSeconds(120))
        val raw=JSONObject().put("city",city).put("celsius",temp).put("code",current.getInt("weather_code")).put("observedAt",observed.toString()).put("fetchedAt",now.toString()).toString()
        cache.writeText(raw); decode(raw)
    }
}
