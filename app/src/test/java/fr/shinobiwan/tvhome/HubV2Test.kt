package fr.shinobiwan.tvhome

import fr.shinobiwan.tvhome.core.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class HubV2Test {
    @Test fun sharedLabTranscriptParsesWithoutTranslation() {
        val root=JSONObject(javaClass.getResource("/hub-scenario-test.json")!!.readText())
        val steps=root.getJSONArray("steps");var count=0
        for(i in 0 until steps.length()) {
            val body=steps.getJSONObject(i).optJSONObject("body") ?: continue
            if(body.optInt("schemaVersion")==2) {
                val s=parseHubSnapshot(body.toString());assertEquals("TEST",s.sampleKind);count++
            }
        }
        assertTrue(count>=2)
    }
    private val now = Instant.parse("2026-10-28T20:00:00Z")
    private fun block(name: String, ttl: Int) = JSONObject("""{"domain":"$name","source":{"component":"LINK","kind":"DIRECT"},"capturedAt":"$now","availability":"AVAILABLE","freshness":"CURRENT","maxAgeSeconds":$ttl,"error":null}""")
    private fun sensor(value: Any, unit: String, available: Boolean = true) = JSONObject().put("available", available).put("value", value).put("unit", unit)
    private fun telemetry() = block("telemetry", 6).put("availability","PARTIAL").put("error",JSONObject("""{"code":"SENSOR_UNAVAILABLE"}""")).put("cpuPercent",sensor(24,"percent")).put("gpuPercent",sensor(JSONObject.NULL,"percent",false))
        .put("ramUsedGiB",sensor(12.5,"GiB")).put("gpuTemperatureC",sensor(61,"celsius"))
    private fun media() = block("media",10).put("state","PLAYING").put("player","Spotify").put("title","Test title").put("artist","Test artist").put("album",JSONObject.NULL)
        .put("positionMs",JSONObject.NULL).put("durationMs",JSONObject.NULL).put("timelineUnit","milliseconds").put("artwork",JSONObject("""{"available":false,"reference":null}"""))
    private fun root(domain: String, value: JSONObject) = JSONObject().put("schemaVersion",2).put("generatedAt",now.toString()).put("sampleKind","TEST").put(domain,value)
    @Test fun independentSensorsAndExactUnits() {
        val s=parseHubSnapshot(root("telemetry",telemetry()).toString())
        assertEquals(12.5,s.telemetry!!.ramGb!!,0.001); assertNull(s.telemetry.gpu)
        assertEquals(61.0,s.telemetry.gpuC!!,0.001); assertNull(s.five)
        assertEquals("TEST",s.sampleKind)
        assertTrue(s.telemetryMeta!!.isCurrent(now)); assertFalse(s.telemetryMeta.isCurrent(now.plusSeconds(6)))
    }
    @Test fun nullableTimelineAndNoSessionClearMetadata() {
        val m=media(); val s=parseHubSnapshot(root("media",m).toString())
        assertTrue(s.media.playing); assertNull(s.media.positionMs); assertNull(s.media.durationMs)
        m.put("state","NO_SESSION").put("title",JSONObject.NULL).put("artist",JSONObject.NULL).put("player",JSONObject.NULL)
        assertEquals("UNAVAILABLE",parseHubSnapshot(root("media",m).toString()).media.status)
    }
    @Test fun staleUnknownAndFutureNeverBecomeCurrent() {
        val b=telemetry(); b.put("freshness","STALE")
        assertFalse(parseHubSnapshot(root("telemetry",b).toString()).telemetryMeta!!.isCurrent(now))
        b.put("freshness","CURRENT").put("capturedAt",now.plusSeconds(1).toString())
        assertFalse(parseHubSnapshot(root("telemetry",b).put("generatedAt",now.plusSeconds(1).toString()).toString()).telemetryMeta!!.isCurrent(now))
        b.put("capturedAt",JSONObject.NULL).put("freshness","UNKNOWN").put("availability","UNAVAILABLE")
        for(k in listOf("cpuPercent","gpuPercent","ramUsedGiB","gpuTemperatureC"))b.getJSONObject(k).put("available",false).put("value",JSONObject.NULL)
        assertNull(parseHubSnapshot(root("telemetry",b).toString()).telemetry)
    }
    @Test(expected=IllegalArgumentException::class) fun wrongScalarTypeRejected() { val b=telemetry(); b.getJSONObject("cpuPercent").put("value","24");parseHubSnapshot(root("telemetry",b).toString()) }
    @Test(expected=IllegalArgumentException::class) fun wrongUnitRejected() { val b=telemetry(); b.getJSONObject("ramUsedGiB").put("unit","GB");parseHubSnapshot(root("telemetry",b).toString()) }
    @Test(expected=IllegalArgumentException::class) fun wrongTtlRejected() { val b=telemetry().put("maxAgeSeconds",120);parseHubSnapshot(root("telemetry",b).toString()) }
    @Test fun artworkReferenceIsBoundedAndContentAddressed() {
        val m=media();m.put("artwork",JSONObject("""{"available":true,"reference":{"kind":"CONTENT_ID","id":"${"a".repeat(64)}","mediaType":"image/jpeg","bytes":2048,"width":128,"height":128}}"""))
        val ref=parseHubSnapshot(root("media",m).toString()).artworkReference!!
        assertEquals(2048,ref.bytes);assertEquals("a".repeat(64),ref.id)
    }
    @Test fun resetAndSourceReliabilityRetained() {
        val b=block("codex",900).put("availability","PARTIAL").put("source",JSONObject("""{"component":"PERISCOPE","kind":"HISTORY"}"""))
            .put("fiveHour",JSONObject("""{"remainingPercent":42,"resetsAt":"${now.plusSeconds(2)}","unit":"percent_remaining"}"""))
            .put("weekly",JSONObject.NULL).put("reliability","HISTORY")
        val s=parseHubSnapshot(root("codex",b).toString());assertEquals("HISTORY",s.reliability)
        assertTrue(s.codexMeta!!.isCurrent(now));assertFalse(s.codexMeta.isCurrent(now.plusSeconds(2)))
        assertEquals("UNVERIFIED",s.activity)
    }
}

