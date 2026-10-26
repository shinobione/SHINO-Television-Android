@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package fr.shinobiwan.tvhome.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.tv.material3.*

val Ink = Color(0xFF09090D)
val Panel = Color(0xFF17151E)
val Muted = Color(0xFFB9B4C7)
val Violet = Color(0xFFB38AFF)
val Mint = Color(0xFF66DDBB)
val Paper = Color(0xFFF6F2EB)

/** TV composition units are fitted to the actual pixel viewport, independent of OEM density.
 * User fontScale is retained. This does not change any device display setting. */
@Composable fun TvViewport(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
        val platform = LocalDensity.current
        val fit = minOf(maxWidth.value / 960f, maxHeight.value / 540f).coerceAtLeast(0.5f)
        CompositionLocalProvider(LocalDensity provides Density(platform.density * fit, platform.fontScale)) {
            MaterialTheme(colorScheme = darkColorScheme(primary = Violet, onPrimary = Ink, background = Ink,
                surface = Panel, onSurface = Paper)) {
                CompositionLocalProvider(LocalContentColor provides Paper) { content() }
            }
        }
    }
}

@Composable fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = Muted, fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
}

@Composable fun Action(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 42.dp),
        colors = ButtonDefaults.colors(containerColor = Color(0xFF282332), contentColor = Paper,
            focusedContainerColor = Violet, focusedContentColor = Ink),
        border = ButtonDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Paper), shape = RoundedCornerShape(12.dp))),
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
        scale = ButtonDefaults.scale(focusedScale = 1.025f),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable fun Meter(value: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(5.dp).background(Color(0xFF35303F), RoundedCornerShape(4.dp))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(value.coerceIn(0f, 1f)).background(color, RoundedCornerShape(4.dp)))
    }
}
