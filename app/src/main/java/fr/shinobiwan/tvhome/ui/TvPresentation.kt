package fr.shinobiwan.tvhome.ui

const val APPS_PER_PAGE = 5
fun boundedAppIndex(index: Int, count: Int) = index.coerceIn(0, (count - 1).coerceAtLeast(0))
fun appPage(index: Int, count: Int) = boundedAppIndex(index, count) / APPS_PER_PAGE
fun moveApp(index: Int, direction: Int, count: Int) = boundedAppIndex(index + direction, count)
fun pageRange(index: Int, count: Int): IntRange {
    if (count == 0) return IntRange.EMPTY
    val start = appPage(index, count) * APPS_PER_PAGE
    return start..minOf(start + APPS_PER_PAGE - 1, count - 1)
}
fun homeWidgetIds(stored: List<String>) = stored.filter { it in setOf("codex", "media", "telemetry") }.distinct().take(3)

data class WidgetDefinition(val title: String)
object WidgetRegistry {
    val definitions = linkedMapOf(
        "codex" to WidgetDefinition("CODEX PERISCOPE"),
        "media" to WidgetDefinition("NOW PLAYING"),
        "telemetry" to WidgetDefinition("SHINO // LINK")
    )
}
