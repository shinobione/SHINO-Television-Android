@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package fr.shinobiwan.tvhome.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.tv.material3.*
import fr.shinobiwan.tvhome.data.TvApp

@Composable fun AppRail(apps: List<TvApp>, selected: Int, focus: FocusRequester, settingsFocus: FocusRequester,
    widgetFocus: FocusRequester?, onSelected: (Int) -> Unit, onLaunch: (TvApp) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Applications", fontSize = 25.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            if (apps.isNotEmpty()) Text("${selected + 1} / ${apps.size}   ·   ${appPage(selected, apps.size) + 1} / ${(apps.size + 4) / 5}",
                color = Muted, fontSize = 17.sp, modifier = Modifier.testTag("app-position"))
        }
        Spacer(Modifier.height(12.dp))
        if (apps.isEmpty()) {
            Action("Choisir vos applications dans Paramètres", { settingsFocus.requestFocus() }, Modifier.fillMaxWidth().testTag("empty-apps"))
        } else Row(Modifier.fillMaxWidth().height(124.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val range = pageRange(selected, apps.size)
            for (slot in 0 until APPS_PER_PAGE) {
                val index = range.first + slot
                if (index !in range) { Spacer(Modifier.weight(1f)); continue }
                val app = apps[index]
                var focused by remember(app.packageName) { mutableStateOf(false) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { onLaunch(app) }, modifier = Modifier.fillMaxWidth().height(88.dp)
                        .testTag("app-${app.packageName}")
                        .then(if (index == selected) Modifier.focusRequester(focus) else Modifier)
                        .focusProperties { up = settingsFocus; down = widgetFocus ?: settingsFocus }
                        .onFocusChanged { focused = it.isFocused; if (it.isFocused && index != selected) onSelected(index) }
                        .then(if (focused) Modifier.border(2.dp, Paper, RoundedCornerShape(14.dp)) else Modifier)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) false
                            else when (event.key) {
                                Key.DirectionRight -> { onSelected(moveApp(index, 1, apps.size)); true }
                                Key.DirectionLeft -> { onSelected(moveApp(index, -1, apps.size)); true }
                                else -> false
                            }
                        },
                        colors = ButtonDefaults.colors(containerColor = Color(0xFF22202A), contentColor = Paper,
                            focusedContainerColor = Color(0xFF49315E), focusedContentColor = Paper),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(14.dp)),
                        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Paper), shape = RoundedCornerShape(14.dp))),
                        scale = ButtonDefaults.scale(focusedScale = 1.035f), contentPadding = PaddingValues(0.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            when {
                                app.banner != null -> Image(app.banner.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                                app.icon != null -> Image(app.icon.asImageBitmap(), null, Modifier.size(54.dp), contentScale = ContentScale.Fit)
                                else -> Text(app.label.take(2).uppercase(), color = Violet, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(app.label, modifier = if (index == selected) Modifier.basicMarquee() else Modifier,
                        fontSize = 18.sp, color = if (index == selected) Paper else Muted,
                        fontWeight = if (index == selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
