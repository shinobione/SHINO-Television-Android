@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package fr.shinobiwan.tvhome.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.*
import fr.shinobiwan.tvhome.HomeState
import fr.shinobiwan.tvhome.core.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable fun WidgetStrip(state: HomeState, media: Media, ids: List<String>, expanded: Boolean,
    focusers: Map<String, FocusRequester>, appsFocus: FocusRequester, onFocus: (Boolean) -> Unit, onOpen: (String) -> Unit) {
    val height = if (expanded) 160.dp else 116.dp
    Row(Modifier.fillMaxWidth().height(height).onFocusChanged { onFocus(it.hasFocus) }, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        ids.forEachIndexed { index, id ->
            val weight = when (id) { "codex" -> 1.4f; "media" -> if (expanded) 1.35f else 1f; else -> 0.9f }
            val base = when (id) { "codex" -> Color(0xFF22182E); "media" -> Color(0xFF17191F); else -> Color(0xFF14171C) }
            Button(onClick = { onOpen(id) }, modifier = Modifier.weight(weight).fillMaxHeight().testTag("widget-$id")
                .focusRequester(focusers.getValue(id))
                .focusProperties { up = appsFocus },
                colors = ButtonDefaults.colors(containerColor = base, focusedContainerColor = base, contentColor = Paper, focusedContentColor = Paper),
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(18.dp)),
                scale = ButtonDefaults.scale(focusedScale = 1.02f),
                border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Violet), shape = RoundedCornerShape(18.dp))),
                contentPadding = PaddingValues(16.dp)) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text(WidgetRegistry.definitions.getValue(id).title, color = if (id == "codex") Violet else Muted, fontSize = 16.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    when (id) {
                        "codex" -> CodexCard(state, expanded)
                        "media" -> MediaCard(if (state.settings.demo) demoMedia() else media, expanded)
                        "telemetry" -> LinkCard(state)
                    }
                }
            }
        }
    }
}

private fun demoMedia() = Media("AVAILABLE", "DEMO", "Morceau de démonstration", "Artiste fictif", playing = true, positionMs = 42000, durationMs = 180000)
private fun bandColor(quota: Quota) = when (quotaBand(quota.remaining)) {
    QuotaBand.VIOLET -> Violet; QuotaBand.TURQUOISE -> Mint; QuotaBand.GOLD -> Color(0xFFE5B64D)
    QuotaBand.ORANGE -> Color(0xFFF28C38); QuotaBand.RED -> Color(0xFFEE4B55)
}
private val shortReset = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.FRENCH).withZone(ZoneId.of("Europe/Paris"))

@Composable private fun CodexCard(s: HomeState, expanded: Boolean) {
    val five = if (s.settings.demo) Quota(83.0, null) else s.snapshot?.five
    val week = if (s.settings.demo) Quota(62.0, null) else s.snapshot?.week
    if (five == null && week == null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Indisponible", fontSize = 27.sp, fontWeight = FontWeight.Light)
            Text(if (s.online) "Données absentes" else "PC hors ligne", fontSize = 16.sp, color = Muted)
        }
    } else if (!expanded) {
        Text("5 h ${five?.remaining?.toInt()?.toString() ?: "—"} %  ·  Sem. ${week?.remaining?.toInt()?.toString() ?: "—"} %", fontSize = 24.sp)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            QuotaValue("5 heures", five, s, Modifier.weight(1f))
            QuotaValue("Semaine", week, s, Modifier.weight(1f))
        }
    }
}
@Composable private fun QuotaValue(label: String, quota: Quota?, s: HomeState, modifier: Modifier) {
    val current = s.settings.demo || s.online && s.snapshot?.status == "AVAILABLE" && quota?.isCurrent(s.snapshot.capturedAt, s.now) == true
    val color = if (current && quota != null) bandColor(quota) else Muted
    Column(modifier) {
        Text(label, fontSize = 16.sp, color = Muted)
        Text(quota?.let { "${it.remaining.toInt()} %" } ?: "—", fontSize = 43.sp, lineHeight = 48.sp, color = color, fontWeight = FontWeight.Medium)
        Meter((quota?.remaining ?: 0.0).toFloat() / 100, color)
        Spacer(Modifier.height(7.dp))
        Text(when {
            s.settings.demo -> "DEMO"
            quota == null -> "Indisponible"
            !current -> "En cache"
            quota.resetsAt != null -> "Reset ${shortReset.format(quota.resetsAt)}"
            else -> "Connecté · restant"
        }, fontSize = 14.sp, color = Muted, maxLines = 1)
    }
}
@Composable private fun MediaCard(media: Media, expanded: Boolean) {
    if (media.status != "AVAILABLE") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("♫", color = Violet, fontSize = 32.sp)
            Column { Text("À votre rythme.", fontSize = 25.sp, fontWeight = FontWeight.Light); Text("Aucune lecture détectée", fontSize = 16.sp, color = Muted) }
        }
    } else if (!expanded) {
        Text(media.title.ifBlank { "Titre indisponible" }, fontSize = 23.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(media.artist, fontSize = 17.sp, color = Muted, maxLines = 1)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (media.artwork != null) Image(media.artwork.asImageBitmap(), "Pochette", Modifier.size(76.dp), contentScale = ContentScale.Fit)
            else Box(Modifier.size(64.dp).background(Color(0xFF30253E), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("♫", fontSize = 36.sp, color = Violet) }
            Column(Modifier.weight(1f)) {
                Text(media.title.ifBlank { "Titre indisponible" }, fontSize = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(media.artist.ifBlank { "Artiste indisponible" }, fontSize = 17.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${media.source} · ${if (media.playing) "Lecture" else "Pause"}", fontSize = 15.sp, color = Mint)
            }
        }
        if (media.durationMs > 0) { Spacer(Modifier.height(12.dp)); Meter(media.positionMs.toFloat() / media.durationMs, Violet) }
    }
}
@Composable private fun LinkCard(s: HomeState) {
    val data = s.snapshot?.telemetry
    if (data == null) { Text("PC hors ligne", fontSize = 23.sp); Text("Télémétrie indisponible", fontSize = 16.sp, color = Muted) }
    else {
        Text("RAM ${data.ramGb?.let { String.format(Locale.FRANCE, "%.1f", it) } ?: "—"} Go", fontSize = 26.sp)
        Text("CPU ${data.cpu?.toInt()?.toString() ?: "—"} %  ·  GPU ${data.gpu?.toInt()?.toString() ?: "—"} %", fontSize = 17.sp, color = Muted)
        Text(if (s.online) "Mesures LINK" else "En cache", fontSize = 15.sp, color = Muted)
    }
}

@Composable fun WidgetDetails(id: String, s: HomeState, media: Media, onToggle: () -> Unit, onNext: () -> Unit, onClose: () -> Unit) {
    val close = remember { FocusRequester() }
    val shownMedia = if (s.settings.demo) demoMedia() else media
    Dialog(onDismissRequest = onClose) {
        BackHandler(onBack = onClose)
        LaunchedEffect(Unit) { close.requestFocus() }
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().background(Panel, RoundedCornerShape(24.dp)).border(1.dp, Violet, RoundedCornerShape(24.dp)).padding(30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(WidgetRegistry.definitions.getValue(id).title, fontSize = 26.sp, color = Paper)
            when (id) {
                "codex" -> {
                    Text(connectionLabel(s.online, s.snapshot?.generatedAt, s.now), fontSize = 20.sp)
                    Text("Dernière mesure : ${ageLabel(s.snapshot?.capturedAt, s.now)}", fontSize = 18.sp, color = Muted)
                    val full = DateTimeFormatter.ofPattern("EEEE d MMMM · HH:mm", Locale.FRENCH).withZone(ZoneId.of("Europe/Paris"))
                    Text("Reset 5 h : ${s.snapshot?.five?.resetsAt?.let(full::format) ?: "Indisponible"}", fontSize = 18.sp)
                    Text("Reset semaine : ${s.snapshot?.week?.resetsAt?.let(full::format) ?: "Indisponible"}", fontSize = 18.sp)
                    Text("Activité : UNVERIFIED", fontSize = 17.sp, color = Muted)
                    if (s.settings.demo) Text("DEMO · quotas fictifs", fontSize = 20.sp, color = Color(0xFFFFD277))
                }
                "media" -> {
                    Text(shownMedia.title.ifBlank { "Aucune lecture détectée" }, fontSize = 26.sp)
                    Text(shownMedia.artist, fontSize = 20.sp, color = Muted)
                    Text("Source : ${shownMedia.source} · ${if (shownMedia.status != "AVAILABLE") "Indisponible" else if (shownMedia.playing) "Lecture" else "Pause"}", fontSize = 18.sp)
                    Text(if (shownMedia.durationMs > 0) "Position observée : ${shownMedia.positionMs / 1000} / ${shownMedia.durationMs / 1000} s" else "Progression indisponible", fontSize = 18.sp, color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (!s.settings.demo && media.source == "TV" && media.canPlayPause) Action(if (media.playing) "Pause" else "Lecture", onToggle)
                        if (!s.settings.demo && media.source == "TV" && media.canNext) Action("Suivant", onNext)
                    }
                }
                "telemetry" -> Text("Mesures de la source LINK existante. Âge des capteurs inconnu ; en cas de déconnexion, valeurs en cache.", fontSize = 20.sp)
            }
            Text("Météo : Open-Meteo · CC BY 4.0", fontSize = 15.sp, color = Muted)
            Action("Fermer", onClose, Modifier.focusRequester(close).testTag("close-details"))
        }
    }
}
