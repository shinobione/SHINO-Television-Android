package fr.shinobiwan.tvhome.core

import org.json.JSONObject
import java.time.Instant

data class DomainMeta(val capturedAt: Instant?, val availability: String, val freshness: String,
    val maxAgeSeconds: Int, val sourceKind: String, val resets: List<Instant> = emptyList()) {
    fun isCurrent(now: Instant): Boolean = availability != "UNAVAILABLE" && freshness == "CURRENT" &&
        capturedAt != null && capturedAt <= now && now < capturedAt.plusSeconds(maxAgeSeconds.toLong()) && resets.none { it <= now }
    fun label(now: Instant, online: Boolean): String = when {
        availability == "UNAVAILABLE" -> "Indisponible"
        !online -> "En cache · ${ageLabel(capturedAt, now)}"
        capturedAt == null || capturedAt > now -> "Horodatage non qualifié"
        !isCurrent(now) -> "Périmé · ${ageLabel(capturedAt, now)}"
        else -> "${if (availability == "PARTIAL") "Partiel" else "À jour"} · ${ageLabel(capturedAt, now)}"
    }
}
data class ArtworkReference(val id: String, val mediaType: String, val bytes: Int, val width: Int, val height: Int)

private fun JSONObject.text(key: String): String = (get(key) as? String) ?: throw IllegalArgumentException("Invalid text")
private fun JSONObject.choice(key: String, choices: Set<String>) = text(key).also { require(it in choices) }
private fun JSONObject.flag(key: String): Boolean = (get(key) as? Boolean) ?: throw IllegalArgumentException("Invalid flag")
private fun JSONObject.number(key: String, min: Double, max: Double): Double {
    val n=get(key); require(n is Number && n.toDouble().isFinite() && n.toDouble() in min..max); return n.toDouble()
}
private fun JSONObject.integer(key: String, min: Int, max: Int): Int {
    val n=get(key); require(n is Int || n is Long);return number(key,min.toDouble(),max.toDouble()).toInt()
}
private fun JSONObject.stamp(key: String): Instant? = if (get(key) == JSONObject.NULL) null else Instant.parse(text(key))
private fun JSONObject.optionalText(key: String, limit: Int): String {
    if (get(key)==JSONObject.NULL) return ""
    return text(key).also { require(it.isNotEmpty() && it.length <= limit && it.none { c -> Character.isISOControl(c) }) }
}

/** Contract copied verbatim from RELAY; domains deliberately remain independent. */
fun parseHubSnapshot(raw: String): Snapshot {
    require(raw.toByteArray(Charsets.UTF_8).size <= 262144)
    val root=JSONObject(raw)
    require(root.integer("schemaVersion",2,2)==2)
    val generated=Instant.parse(root.text("generatedAt"))
    val sample=root.choice("sampleKind",setOf("TEST","DEMO","LIVE"))
    require(listOf("telemetry","media","codex").any { root.has(it) })
    fun domain(name: String, ttl: Int): Pair<JSONObject,DomainMeta>? {
        if (!root.has(name)) return null
        val b=root.getJSONObject(name);require(b.text("domain")==name)
        val source=b.getJSONObject("source")
        source.choice("component",setOf("LINK","PERISCOPE","TV_HOME","LAB"))
        val kind=source.choice("kind",setOf("DIRECT","HISTORY","DEMO"))
        require(sample != "LIVE" || kind != "DEMO"); require(sample != "DEMO" || kind == "DEMO")
        val availability=b.choice("availability",setOf("AVAILABLE","PARTIAL","UNAVAILABLE"))
        val freshness=b.choice("freshness",setOf("CURRENT","STALE","UNKNOWN"))
        val captured=b.stamp("capturedAt");require(captured == null || captured <= generated)
        require(availability == "UNAVAILABLE" || captured != null)
        require(b.integer("maxAgeSeconds",ttl,ttl)==ttl)
        if (b.get("error") != JSONObject.NULL) b.getJSONObject("error").choice("code",setOf("NO_DATA","SOURCE_UNAVAILABLE","SENSOR_UNAVAILABLE","UNVERIFIED"))
        require(availability != "AVAILABLE" || b.isNull("error"))
        require(availability != "UNAVAILABLE" || captured == null && !b.isNull("error"))
        return b to DomainMeta(captured,availability,freshness,ttl,kind)
    }
    val t=domain("telemetry",6);val m=domain("media",10);val c=domain("codex",900)
    fun sensor(b: JSONObject, key: String, unit: String, min: Double, max: Double): Double? {
        val v=b.getJSONObject(key);require(v.text("unit")==unit)
        return if (v.flag("available")) v.number("value",min,max) else {require(v.get("value")==JSONObject.NULL);null}
    }
    var telemetry: Telemetry?=null
    if(t!=null) {
        val b=t.first
        val value=Telemetry(sensor(b,"cpuPercent","percent",0.0,100.0),sensor(b,"gpuPercent","percent",0.0,100.0),
            sensor(b,"ramUsedGiB","GiB",0.0,65536.0),sensor(b,"gpuTemperatureC","celsius",-40.0,150.0))
        val count=listOf(value.cpu,value.gpu,value.ramGb,value.gpuC).count { it!=null }
        require(t.second.availability == when(count){0->"UNAVAILABLE";4->"AVAILABLE";else->"PARTIAL"})
        telemetry=value.takeIf { count>0 }
    }
    var media=Media();var artwork: ArtworkReference?=null
    if(m!=null) {
        val b=m.first
        val state=b.choice("state",setOf("PLAYING","PAUSED","STOPPED","NO_SESSION","UNAVAILABLE"))
        val title=b.optionalText("title",256);val artist=b.optionalText("artist",256)
        val player=b.optionalText("player",128);val album=b.optionalText("album",256)
        require(b.text("timelineUnit")=="milliseconds")
        fun timeline(key: String): Long? = if(b.get(key)==JSONObject.NULL)null else b.integer(key,0,604800000).toLong()
        val position=timeline("positionMs");val duration=timeline("durationMs")
        require(position==null || duration==null || position<=duration)
        val art=b.getJSONObject("artwork");val hasArt=art.flag("available")
        if(hasArt) {
            val r=art.getJSONObject("reference"); require(r.text("kind")=="CONTENT_ID")
            val id=r.text("id");require(id.matches(Regex("[0-9a-f]{64}")))
            artwork=ArtworkReference(id,r.choice("mediaType",setOf("image/jpeg","image/png")),r.integer("bytes",1,40960),r.integer("width",1,512),r.integer("height",1,512))
        } else require(art.get("reference")==JSONObject.NULL)
        if(state in setOf("NO_SESSION","UNAVAILABLE")) {
            require(title.isEmpty() && artist.isEmpty() && player.isEmpty() && album.isEmpty() && position==null && duration==null && !hasArt)
        } else media=Media("AVAILABLE","PC",title,artist,player,state=="PLAYING",position,duration,playbackState=state)
        require(m.second.availability == when {state=="UNAVAILABLE"->"UNAVAILABLE";state=="NO_SESSION"->"AVAILABLE";title.isNotEmpty() && artist.isNotEmpty()->"AVAILABLE";else->"PARTIAL"})
    }
    var five: Quota?=null;var week: Quota?=null;var reliability="UNVERIFIED"
    if(c!=null) {
        fun quota(key: String): Quota? {
            val b=c.first;if(b.get(key)==JSONObject.NULL)return null
            val q=b.getJSONObject(key);require(q.text("unit")=="percent_remaining")
            return Quota(q.number("remainingPercent",0.0,100.0),q.stamp("resetsAt"))
        }
        five=quota("fiveHour");week=quota("weekly")
        val count=listOfNotNull(five,week).size
        require(c.second.availability == when(count){0->"UNAVAILABLE";2->"AVAILABLE";else->"PARTIAL"})
        reliability=c.first.choice("reliability",setOf("DIRECT","HISTORY","UNVERIFIED"))
        require(reliability == if(count==0 || c.second.sourceKind=="DEMO")"UNVERIFIED" else c.second.sourceKind)
    }
    return Snapshot(generated,c?.second?.capturedAt,c?.second?.availability ?: "UNAVAILABLE",five,week,
        media=media,telemetry=telemetry,sampleKind=sample,telemetryMeta=t?.second,mediaMeta=m?.second,
        codexMeta=c?.second?.copy(resets=listOfNotNull(five?.resetsAt,week?.resetsAt)),reliability=reliability,artworkReference=artwork)
}
