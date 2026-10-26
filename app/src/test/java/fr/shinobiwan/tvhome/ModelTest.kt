package fr.shinobiwan.tvhome

import fr.shinobiwan.tvhome.core.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ModelTest {
    @Test fun quotaBoundaries() {
        assertEquals(QuotaBand.VIOLET, quotaBand(76.0))
        assertEquals(QuotaBand.TURQUOISE, quotaBand(75.9))
        assertEquals(QuotaBand.GOLD, quotaBand(50.0))
        assertEquals(QuotaBand.ORANGE, quotaBand(25.0))
        assertEquals(QuotaBand.RED, quotaBand(10.0))
    }
    @Test fun expiredAndFutureSamplesAreNotLive() {
        val now = Instant.parse("2026-10-26T12:00:00Z")
        assertFalse(Quota(50.0, now.minusSeconds(1)).isCurrent(now, now))
        assertFalse(Quota(50.0, null).isCurrent(now.plusSeconds(200), now))
        assertFalse(Quota(50.0, null).isCurrent(now.minusSeconds(901), now))
        assertTrue(Quota(50.0, now.plusSeconds(500)).isCurrent(now.minusSeconds(60), now))
    }
    @Test fun publicOrMalformedAddressesRejected() {
        for (host in listOf("8.8.8.8", "0.0.0.0", "localhost", "192.168.1.4.evil.com", "169.254.1.2", "192.168.1.256")) assertFalse(isLanIpv4(host))
        assertTrue(isLanIpv4("192.168.1.4"))
        assertTrue(isLanIpv4("10.0.0.2"))
    }
    @Test fun contractPreservesRemainingAndReliability() {
        val json = """{"schemaVersion":1,"generatedAt":"2026-10-26T12:00:00Z","codex":{"status":"AVAILABLE","activity":"RUNNING","capturedAt":"2026-10-26T11:59:00Z","fiveHour":{"remainingPercent":76.5,"resetsAt":null},"weekly":null},"media":{"status":"UNAVAILABLE"},"telemetry":{"status":"UNAVAILABLE"}}"""
        val parsed = parseSnapshot(json)
        assertEquals(76.5, parsed.five!!.remaining, 0.001)
        assertEquals("UNVERIFIED", parsed.activity)
        assertNull(parsed.week)
    }
    @Test(expected = IllegalArgumentException::class) fun unsupportedContractRejected() { parseSnapshot("""{"schemaVersion":2}""") }
    @Test(expected = IllegalArgumentException::class) fun invalidQuotaRejected() { Quota(101.0, null) }
    @Test fun cacheAgingIncludesDisconnect() {
        val now = Instant.parse("2026-10-26T12:00:00Z")
        assertEquals("PC hors ligne", connectionLabel(false, now.minusSeconds(10), now))
        assertEquals("PC connecté", connectionLabel(true, now.minusSeconds(10), now))
        assertEquals("Passerelle : données périmées", connectionLabel(true, now.minusSeconds(121), now))
    }
}
