package fr.shinobiwan.tvhome.core

import org.json.JSONObject
import java.time.Instant

enum class QuotaBand { VIOLET, TURQUOISE, GOLD, ORANGE, RED }
fun quotaBand(remaining: Double) = when {
    remaining >= 76 -> QuotaBand.VIOLET
    remaining >= 51 -> QuotaBand.TURQUOISE
    remaining >= 26 -> QuotaBand.GOLD
    remaining >= 11 -> QuotaBand.ORANGE
    else -> QuotaBand.RED
}
data class Quota(val remaining: Double, val resetsAt: Instant?) {
    init { require(remaining.isFinite() && remaining in 0.0..100.0) }
    fun isCurrent(captured: Instant?, now: Instant): Boolean = captured != null &&
        captured <= now.plusSeconds(120) && captured >= now.minusSeconds(900) && (resetsAt == null || resetsAt > now)
}
data class Media(val status: String = "UNAVAILABLE", val source: String = "PC", val title: String = "",
    val artist: String = "", val app: String = "", val playing: Boolean = false,
    val positionMs: Long? = null, val durationMs: Long? = null, val artwork: android.graphics.Bitmap? = null,
    val canPlayPause: Boolean = false, val canNext: Boolean = false,
    val playbackState: String = if (playing) "PLAYING" else "PAUSED", val quality: String = "")
data class Telemetry(val cpu: Double?, val gpu: Double?, val ramGb: Double?, val gpuC: Double?)
data class Snapshot(val generatedAt: Instant, val capturedAt: Instant?, val status: String,
    val five: Quota?, val week: Quota?, val activity: String = "UNVERIFIED",
    val media: Media = Media(), val telemetry: Telemetry? = null,
    val sampleKind: String = "LIVE", val telemetryMeta: DomainMeta? = null,
    val mediaMeta: DomainMeta? = null, val codexMeta: DomainMeta? = null,
    val reliability: String = "UNVERIFIED", val artworkReference: ArtworkReference? = null)
data class Weather(val city: String, val celsius: Double, val code: Int, val observedAt: Instant, val fetchedAt: Instant)

fun isLanIpv4(host: String): Boolean {
    val parts = host.split('.')
    if (parts.size != 4 || parts.any { it.isEmpty() || it.length > 3 || it.any { c -> !c.isDigit() } || (it.length > 1 && it[0] == '0') }) return false
    val n = parts.map { it.toIntOrNull() ?: return false }
    if (n.any { it !in 0..255 }) return false
    return n[0] == 10 || n[0] == 192 && n[1] == 168 || n[0] == 172 && n[1] in 16..31
}
fun connectionLabel(online: Boolean, generated: Instant?, now: Instant): String = when {
    !online -> "PC hors ligne"
    generated == null || generated < now.minusSeconds(120) || generated > now.plusSeconds(120) -> "Passerelle : données périmées"
    else -> "PC connecté"
}
fun ageLabel(stamp: Instant?, now: Instant): String {
    if (stamp == null) return "aucune donnée"
    val seconds = java.time.Duration.between(stamp, now).seconds
    if (seconds < -120) return "horodatage futur"
    val minutes = seconds.coerceAtLeast(0) / 60
    return if (minutes < 1) "à l'instant" else if (minutes < 60) "il y a $minutes min" else "il y a ${minutes / 60} h"
}
fun parseSnapshot(raw: String): Snapshot {
    val root = JSONObject(raw)
    require(root.getInt("schemaVersion") == 1) { "Unsupported schema" }
    val codex = root.getJSONObject("codex")
    fun nullableInstant(obj: JSONObject, name: String): Instant? = if (obj.isNull(name)) null else Instant.parse(obj.getString(name))
    fun quota(name: String): Quota? {
        val obj = codex.optJSONObject(name) ?: return null
        val value = obj.get("remainingPercent")
        require(value is Number)
        return Quota(value.toDouble(), nullableInstant(obj, "resetsAt"))
    }
    val media = root.optJSONObject("media")
    val telemetry = root.optJSONObject("telemetry")
    fun metric(name: String, range: ClosedFloatingPointRange<Double>): Double? {
        if (telemetry == null || telemetry.isNull(name)) return null
        val value = telemetry.get(name)
        require(value is Number && value.toDouble().isFinite() && value.toDouble() in range)
        return value.toDouble()
    }
    return Snapshot(Instant.parse(root.getString("generatedAt")), nullableInstant(codex, "capturedAt"), codex.getString("status"), quota("fiveHour"), quota("weekly"),
        media = if (media?.optString("status") == "AVAILABLE") Media("AVAILABLE", "PC", media.optString("title").take(256), media.optString("artist").take(256),
            media.optString("app").take(128), media.optBoolean("playing"), media.optLong("positionMs").coerceAtLeast(0), media.optLong("durationMs").coerceAtLeast(0)) else Media(),
        telemetry = if (telemetry?.optString("status") == "AVAILABLE") Telemetry(metric("cpuPercent", 0.0..100.0), metric("gpuPercent", 0.0..100.0), metric("ramUsedGb", 0.0..65536.0), metric("gpuTemperatureC", -40.0..150.0)) else null)
}
