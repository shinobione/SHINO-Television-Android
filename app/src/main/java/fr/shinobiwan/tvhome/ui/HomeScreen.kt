
@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package fr.shinobiwan.tvhome.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Text
import fr.shinobiwan.tvhome.*
import fr.shinobiwan.tvhome.R
import fr.shinobiwan.tvhome.core.*
import fr.shinobiwan.tvhome.data.TvApp
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Paris = ZoneId.of("Europe/Paris")
private val clockFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(Paris)
private val dateFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH).withZone(Paris)

@Composable fun HomeScreen(state: HomeState, model: HomeViewModel) {
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var selectedApp by rememberSaveable { mutableStateOf<String?>(null) }
    var resumeEpoch by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumeEpoch++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    TvViewport {
        if (settingsOpen) SettingsScreen(state, model) { settingsOpen = false; resumeEpoch++ }
        else HomeContent(state.copy(lastApp = selectedApp ?: state.lastApp), model.preferredMedia(state), resumeEpoch,
            onSettings = { settingsOpen = true }, onLaunch = model::launch,
            onToggleMedia = model::toggleMedia, onNextMedia = model::nextMedia, onAppFocused = { selectedApp = it })
    }
}

/** Same native renderer used by the Activity and host-side Android graphics tests. */
@Composable fun HomeContent(state: HomeState, media: Media, restoreSignal: Int = 0,
    onSettings: () -> Unit = {}, onLaunch: (TvApp) -> Unit = {},
    onToggleMedia: () -> Unit = {}, onNextMedia: () -> Unit = {}, onAppFocused: (String) -> Unit = {}) {
    val apps = remember(state.apps, state.settings.order, state.settings.hidden) {
        state.apps.filter { it.packageName !in state.settings.hidden }.sortedWith(compareBy<TvApp> {
            state.settings.order.indexOf(it.packageName).let { index -> if (index < 0) Int.MAX_VALUE else index }
        }.thenBy { it.label })
    }
    var selectedPackage by rememberSaveable { mutableStateOf(state.lastApp) }
    val selected = boundedAppIndex(apps.indexOfFirst { it.packageName == selectedPackage }.coerceAtLeast(0), apps.size)
    val appFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    val ids = homeWidgetIds(state.settings.widgets)
    val widgetFocusers = remember(ids) { ids.associateWith { FocusRequester() } }
    val fallbackFocus = remember { FocusRequester() }
    val widgetFocus = widgetFocusers.values.firstOrNull() ?: fallbackFocus
    var widgetHasFocus by remember { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf<String?>(null) }
    val hasQuotas = state.snapshot?.five != null || state.snapshot?.week != null
    var expandedMedia by remember { mutableStateOf(media.status == "AVAILABLE" || state.settings.demo || hasQuotas) }
    LaunchedEffect(media.status, state.settings.demo, hasQuotas, widgetHasFocus) {
        if (!widgetHasFocus) expandedMedia = media.status == "AVAILABLE" || state.settings.demo || hasQuotas
    }
    LaunchedEffect(restoreSignal, apps.map { it.packageName }) {
        if (state.lastApp != null && apps.any { it.packageName == state.lastApp }) selectedPackage = state.lastApp
        if (details == null) { if (apps.isEmpty()) settingsFocus.requestFocus() else appFocus.requestFocus() }
    }
    LaunchedEffect(selectedPackage) {
        if (selectedPackage != null && apps.isNotEmpty() && details == null) {
            appFocus.requestFocus()
        }
    }
    Box(Modifier.fillMaxSize().testTag("tv-home").background(Brush.radialGradient(
        colors = listOf(Color(0xFF261332), Ink), center = androidx.compose.ui.geometry.Offset(240f, 80f), radius = 1400f))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 24.dp)) {
            Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Image(painterResource(R.drawable.brand), "Logo officiel SHINOBIWAN", Modifier.width(284.dp).height(94.dp), contentScale = ContentScale.Fit)
                    Text("TV HOME", color = Violet, fontSize = 16.sp, letterSpacing = 4.sp, modifier = Modifier.padding(start = 20.dp))
                }
                WeatherHeader(state, Modifier.width(180.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(clockFormat.format(state.now), fontSize = 68.sp, lineHeight = 72.sp,
                        color = Paper, modifier = Modifier.testTag("main-clock"))
                    Text(dateFormat.format(state.now).replaceFirstChar { it.titlecase(Locale.FRENCH) }, fontSize = 18.sp, color = Muted)
                }
                Action("Réglages", onSettings, Modifier.testTag("settings").focusRequester(settingsFocus)
                    .focusProperties { down = if (apps.isEmpty()) widgetFocus.takeIf { ids.isNotEmpty() } ?: FocusRequester.Default else appFocus })
            }
            Spacer(Modifier.height(12.dp))
            AppRail(apps, selected, appFocus, settingsFocus, widgetFocus.takeIf { ids.isNotEmpty() },
                onSelected = { index -> apps.getOrNull(index)?.packageName?.let { selectedPackage = it; onAppFocused(it) } }, onLaunch = onLaunch)
            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.weight(1f))
            WidgetStrip(state, media, ids, expandedMedia, widgetFocusers, appFocus.takeIf { apps.isNotEmpty() } ?: settingsFocus,
                onFocus = { widgetHasFocus = it }, onOpen = { details = it })
            if (state.settings.demo) Text("DEMO · données fictives", fontSize = 16.sp, color = Color(0xFFFFD277), modifier = Modifier.padding(top = 6.dp))
        }
        details?.let { id ->
            WidgetDetails(id, state, media, onToggleMedia, onNextMedia) {
                details = null
                widgetFocusers[id]?.requestFocus()
            }
        }
    }
}

@Composable private fun WeatherHeader(state: HomeState, modifier: Modifier) {
    val weather = state.weather?.takeIf { it.city == state.settings.city }
    Column(modifier, horizontalAlignment = Alignment.End) {
        if (weather == null) Text("Météo indisponible", fontSize = 18.sp, color = Muted)
        else {
            Text("${weather.celsius.toInt()}°", fontSize = 34.sp, color = Paper)
            Text(weatherCondition(weather.code), fontSize = 18.sp, color = Muted, maxLines = 2)
            if (!state.weatherOnline || weather.observedAt < state.now.minusSeconds(3600))
                Text("En cache", fontSize = 15.sp, color = Color(0xFFFFD277))
        }
        Text(state.settings.city, fontSize = 17.sp, color = Muted, maxLines = 2)
    }
}

fun weatherCondition(code: Int) = when (code) {
    0 -> "Ciel clair"; 1, 2, 3 -> "Nuageux"; 45, 48 -> "Brume"
    in 51..67 -> "Pluie"; in 71..77 -> "Neige"; in 80..82 -> "Averses"
    in 85..86 -> "Neige"; in 95..99 -> "Orage"; else -> "Conditions inconnues"
}
