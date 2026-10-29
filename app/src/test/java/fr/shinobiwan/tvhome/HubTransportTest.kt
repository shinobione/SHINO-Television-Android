package fr.shinobiwan.tvhome

import fr.shinobiwan.tvhome.data.*
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.net.InetAddress
import java.net.Proxy
import java.net.URL
import java.nio.file.Files
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.*

/** Local TLS only; certificates are generated per run. No user credentials or LAN access. */
class HubTransportTest {
    companion object {
        private lateinit var dir: File
        private const val password="test-only-password"
        @BeforeClass @JvmStatic fun certificates() {
            dir=Files.createTempDirectory("tvhome-tls-test-").toFile()
            fun keytool(vararg args: String) {
                val command=listOf(File(System.getProperty("java.home"),"bin/keytool.exe").absolutePath)+args
                val p=ProcessBuilder(command).redirectErrorStream(true).start()
                val log=p.inputStream.bufferedReader().readText()
                check(p.waitFor()==0) { "Test certificate creation failed: $log" }
            }
            fun generate(alias: String, ca: Boolean=false) {
                keytool("-genkeypair","-alias",alias,"-keystore",File(dir,"$alias.p12").path,"-storetype","PKCS12","-storepass",password,"-keypass",password,"-keyalg","RSA","-keysize","2048","-validity","2","-dname","CN=TVHOME TEST $alias","-ext",if(ca)"bc=ca:true" else "bc=ca:false")
            }
            generate("ca",true);generate("other",true)
            keytool("-exportcert","-rfc","-alias","ca","-keystore",File(dir,"ca.p12").path,"-storepass",password,"-file",File(dir,"ca.pem").path)
            for(alias in listOf("server","wrong-san")) {
                generate(alias)
                keytool("-certreq","-alias",alias,"-keystore",File(dir,"$alias.p12").path,"-storepass",password,"-file",File(dir,"$alias.csr").path)
                keytool("-gencert","-alias","ca","-keystore",File(dir,"ca.p12").path,"-storepass",password,"-infile",File(dir,"$alias.csr").path,"-outfile",File(dir,"$alias.crt").path,"-validity","2","-ext","SAN=IP:${if(alias=="server")"127.0.0.1" else "127.0.0.2"}","-ext","EKU=serverAuth")
                keytool("-importcert","-noprompt","-alias","ca","-keystore",File(dir,"$alias.p12").path,"-storepass",password,"-file",File(dir,"ca.pem").path)
                keytool("-importcert","-alias",alias,"-keystore",File(dir,"$alias.p12").path,"-storepass",password,"-file",File(dir,"$alias.crt").path)
            }
        }
        private fun store(alias: String) = KeyStore.getInstance("PKCS12").apply { File(dir,"$alias.p12").inputStream().use { load(it,password.toCharArray()) } }
        private fun pin(cert: X509Certificate) = MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    private data class TestServer(val port: Int)
    private fun withServer(alias: String="server", run: (TestServer,X509Certificate)->Unit) {
        val keys=store(alias)
        val km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(keys,password.toCharArray()) }
        val ssl=SSLContext.getInstance("TLSv1.3").apply { init(km.keyManagers,null,null) }
        val listener=ssl.serverSocketFactory.createServerSocket(0,16,InetAddress.getByName("127.0.0.1")) as SSLServerSocket
        listener.enabledProtocols=arrayOf("TLSv1.3")
        val thread=kotlin.concurrent.thread(isDaemon=true,name="tvhome-test-loopback") {
            while(!listener.isClosed) {
                try {
                    (listener.accept() as SSLSocket).use { socket ->
                        socket.soTimeout=5000;socket.startHandshake()
                        val reader=socket.inputStream.bufferedReader();var auth="";var size=0
                        while(true) { val line=reader.readLine() ?: break;size+=line.length;check(size<=8192);if(line.isEmpty())break
                            if(line.startsWith("Authorization:",ignoreCase=true))auth=line.substringAfter(':').trim()
                        }
                        val status=when(auth){"Bearer test-reader-token-1234"->200;"Bearer test-publisher-token-1234"->403;"Bearer test-empty-token-1234"->503;else->401}
                        val body=if(status==200)"{}" else """{"protocolVersion":1,"error":{"code":"${if(status==503)"NO_DATA" else "UNAUTHORIZED"}"}}"""
                        val response="HTTP/1.1 $status Result\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n$body"
                        socket.outputStream.write(response.toByteArray());socket.outputStream.flush()
                    }
                } catch(_:Exception){ /* Expected failed handshakes in negative TLS tests. */ }
            }
        }
        try { run(TestServer(listener.localPort),keys.getCertificate(alias) as X509Certificate) } finally { listener.close();thread.join(2000) }
    }
    private fun connection(server: TestServer, cert: X509Certificate, token: String="test-reader-token-1234", pinned: String=pin(cert)): HttpsURLConnection {
        val trust=hubTrustManager(File(dir,"ca.pem").readText(),pinned)
        val ssl=SSLContext.getInstance("TLSv1.3").apply { init(null,arrayOf(trust),null) }
        return (URL("https://127.0.0.1:${server.port}/hub/v1/snapshot").openConnection(Proxy.NO_PROXY) as HttpsURLConnection).apply {
            sslSocketFactory=ssl.socketFactory;setRequestProperty("Authorization","Bearer $token")
        }
    }
    @Test fun validCaSanPinAndReaderSucceeds() = withServer { server,cert -> assertEquals("{}",String(readHubBytes(connection(server,cert),262144))) }
    @Test fun wrongPinFailsTls() = withServer { server,cert -> assertThrows(SSLException::class.java) { readHubBytes(connection(server,cert,pinned="0".repeat(64)),262144) } }
    @Test fun wrongSanFailsTls() = withServer("wrong-san") { server,cert -> assertThrows(SSLException::class.java) { readHubBytes(connection(server,cert),262144) } }
    @Test fun wrongCaFailsChainEvenWithCorrectPin() = withServer { _,cert ->
        val other=store("other").getCertificate("other") as X509Certificate
        val pem="-----BEGIN CERTIFICATE-----\n${java.util.Base64.getMimeEncoder(64,byteArrayOf(10)).encodeToString(other.encoded)}\n-----END CERTIFICATE-----"
        assertThrows(java.security.cert.CertificateException::class.java) { hubTrustManager(pem,pin(cert)).checkServerTrusted(arrayOf(cert),"RSA") }
    }
    @Test fun roleAuthAndEmptyAreDistinct() = withServer { server,cert ->
        for((token,expected) in listOf("test-publisher-token-1234" to "READ_FORBIDDEN","test-invalid-token-1234" to "UNAUTHORIZED","test-empty-token-1234" to "NO_DATA")) {
            val error=assertThrows(HubFailure::class.java) { readHubBytes(connection(server,cert,token),262144) };assertEquals(expected,error.kind)
        }
    }
    @Test fun bodyBoundIsEnforced() = withServer { server,cert -> assertThrows(IllegalArgumentException::class.java) { readHubBytes(connection(server,cert),1) } }
    @Test fun tokenSyntaxAndPairingIdentity() {
        assertTrue(validReaderToken("reader_token-test-1234"));assertFalse(validReaderToken("short"));assertFalse(validReaderToken("a".repeat(129)))
        assertFalse(validReaderToken("reader\r\ntoken-1234567"))
        val s=Settings();assertNotEquals(pairingIdentity(s),pairingIdentity(s.copy(caPem="different CA")))
        assertNotEquals(pairingIdentity(s),pairingIdentity(s.copy(protocol="V1")))
        assertNotEquals(pairingIdentity(s),pairingIdentity(s.copy(encryptedToken="different reader")))
    }
}
