package fr.shinobiwan.tvhome

import fr.shinobiwan.tvhome.data.parseHubPairingConfig
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HubPairingImportTest {
    private val pem get() = javaClass.getResource("/tvhome-test-ca.pem")!!.readText()
    private fun config(): JSONObject = JSONObject()
        .put("format", "SHINO_TV_HOME_HUB_PAIRING")
        .put("version", 1)
        .put("host", "192.168.1.195")
        .put("port", 8767)
        .put("caPem", pem)
        .put("certificateSha256", "a".repeat(64))
        .put("readerToken", "test_reader_tvhome_123456789")

    @Test fun validFileYieldsCompletePairing() {
        val parsed = parseHubPairingConfig(config().toString())
        assertEquals("192.168.1.195", parsed.host)
        assertEquals("8767", parsed.port)
        assertEquals(pem.trim(), parsed.caPem)
        assertEquals("a".repeat(64), parsed.certificateSha256)
        assertEquals("test_reader_tvhome_123456789", parsed.readerToken)
    }

    @Test fun wrongFormatOrVersionRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            parseHubPairingConfig(config().put("version", 2).toString())
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseHubPairingConfig(config().put("format", "OTHER").toString())
        }
    }

    @Test fun rejectsBadPortAndNonLanHost() {
        assertThrows(IllegalArgumentException::class.java) {
            parseHubPairingConfig(config().put("port", 0).toString())
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseHubPairingConfig(config().put("host", "8.8.8.8").toString())
        }
    }

    @Test fun rejectsMissingTokenAndBrokenCa() {
        assertThrows(IllegalArgumentException::class.java) {
            parseHubPairingConfig(config().put("readerToken", "").toString())
        }
        assertThrows(Exception::class.java) {
            parseHubPairingConfig(config().put("caPem", "not a certificate").toString())
        }
    }

    @Test fun rejectsOversizedFile() {
        assertThrows(IllegalArgumentException::class.java) {
            parseHubPairingConfig(config().put("unused", "x".repeat(25000)).toString())
        }
    }

    @Test fun acceptsUtf8BomAndNormalizesPublicPin() {
        val pin = "AB:".repeat(31) + "AB"
        val parsed = parseHubPairingConfig("\uFEFF" + config().put("certificateSha256", pin).toString())
        assertEquals("ab".repeat(32), parsed.certificateSha256)
    }

    @Test fun rejectsTrailingDocumentOrGarbage() {
        for (suffix in listOf("{}", "trailing garbage")) {
            assertThrows(IllegalArgumentException::class.java) {
                parseHubPairingConfig(config().toString() + suffix)
            }
        }
    }

    @Test fun rejectsWrongFieldTypesMissingFieldsAndMalformedPin() {
        for (document in listOf(config().put("version", "1"), config().put("port", "8767"),
            config().put("host", 123), config().put("certificateSha256", "bad"),
            config().apply { remove("readerToken") })) {
            assertThrows(Exception::class.java) { parseHubPairingConfig(document.toString()) }
        }
    }
}
