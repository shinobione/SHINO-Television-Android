@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package fr.shinobiwan.tvhome

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import fr.shinobiwan.tvhome.core.*
import fr.shinobiwan.tvhome.data.Settings
import fr.shinobiwan.tvhome.data.TvApp
import fr.shinobiwan.tvhome.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], qualifiers = "w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeHomeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val now = Instant.parse("2026-10-26T19:42:00Z")
    private fun apps(count: Int = 8) = (0 until count).map { TvApp("test.app$it", listOf("Cinéma", "Musique", "Vidéos", "Bibliothèque", "Direct", "Studio", "Concerts", "Réglages TV")[it], null) }
    private fun state(demo: Boolean = false) = HomeState(settings = Settings(demo = demo, order = apps().map { it.packageName }), apps = apps(), now = now)
    private fun show(s: HomeState = state(), media: Media = Media(), onLaunch: (TvApp) -> Unit = {}) {
        compose.runOnUiThread { compose.activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION }
        compose.setContent { TvViewport { HomeContent(s, media, onLaunch = onLaunch) } }
        compose.waitForIdle()
    }
    private fun frame(name: String) {
        compose.mainClock.advanceTimeBy(350)
        compose.waitForIdle()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            val file = File(System.getProperty("tvhome.screenshots"), name)
            file.parentFile?.mkdirs()
            file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertEquals(1920, image.width)
            assertEquals(1080, image.height)
        }
    }
    @Test fun emptyStateNativePreviewAndClockNotRepeated() {
        show(state(demo = false))
        compose.onNodeWithTag("main-clock").assertTextEquals("20:42")
        compose.onAllNodesWithText("20:42").assertCountEquals(1)
        compose.onNodeWithText("Indisponible").assertIsDisplayed()
        frame("native-api29-offline-test-apps.png")
    }
    @Test fun demoNativePreview() {
        show(state(demo = true))
        compose.onNodeWithText("DEMO · données fictives").assertIsDisplayed()
        frame("native-api29-demo.png")
    }
    @Test @Config(qualifiers = "w1920dp-h1080dp-land-mdpi") fun fullHdWithDifferentOemDensity() {
        show(state(demo = true))
        compose.onNodeWithTag("main-clock").assertIsDisplayed()
        compose.onNodeWithTag("widget-codex").assertIsDisplayed()
        compose.onNodeWithText("DEMO · données fictives").assertIsDisplayed()
        frame("native-api29-demo-mdpi.png")
    }
    @Test fun threeWidgetsRemainOnScreen() {
        show(state(demo = true).copy(settings = state(demo = true).settings.copy(widgets = listOf("codex", "media", "telemetry"))))
        for (id in listOf("codex", "media", "telemetry")) compose.onNodeWithTag("widget-$id").assertIsDisplayed()
        compose.onNodeWithText("DEMO · données fictives").assertIsDisplayed()
        frame("native-api29-three-widgets.png")
    }
    @Test fun hubTelemetryMusicAndSyntheticSourceAreVisible() {
        val at=Instant.parse("2026-10-26T12:00:00Z")
        val transcript=org.json.JSONObject(javaClass.getResource("/hub-scenario-test.json")!!.readText()).getJSONArray("steps")
        val body=(0 until transcript.length()).map { transcript.getJSONObject(it).getJSONObject("body") }.first { it.optInt("schemaVersion")==2 }
        val snapshot=parseHubSnapshot(body.toString())
        val testState=state().copy(settings=state().settings.copy(widgets=listOf("telemetry","media","codex")),snapshot=snapshot,online=true,now=at)
        val art=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.MAGENTA) }
        show(testState,media=snapshot.media.copy(artwork=art))
        compose.onNodeWithText("TEST · données synthétiques du HUB").assertIsDisplayed()
        compose.onNodeWithText("GPU 45 °C").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pochette").assertIsDisplayed()
        assertTextFits("${snapshot.reliability} · À jour · à l'instant")
        frame("native-api29-hub-v2-test.png")
    }
    @Test fun telemetryOnlyStillFitsItsCard() {
        val meta=DomainMeta(now,"AVAILABLE","CURRENT",6,"DIRECT")
        val snapshot=Snapshot(now,null,"UNAVAILABLE",null,null,telemetry=Telemetry(20.0,10.0,8.0,45.0),telemetryMeta=meta)
        val testState=state().copy(settings=state().settings.copy(widgets=listOf("telemetry")),snapshot=snapshot,online=true)
        show(testState)
        val card=compose.onNodeWithTag("widget-telemetry").getUnclippedBoundsInRoot()
        val temp=compose.onNodeWithText("GPU 45 °C").getUnclippedBoundsInRoot()
        assertTrue("Temperature must fit inside telemetry card",temp.bottom<=card.bottom)
        assertTextFits("GPU 45 °C")
        assertTextFits("À jour · à l'instant")
        frame("native-api29-hub-v2-telemetry-only.png")
    }
    private fun assertTextFits(text: String) {
        val layouts=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText(text,useUnmergedTree=true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue("Expected text layout for $text",layouts.isNotEmpty())
        assertFalse("Text must not be clipped: $text",layouts.any { it.hasVisualOverflow })
    }
    @Test fun completeNextPageAndLaunchAreReachedByDpad() {
        var launched: String? = null
        show(onLaunch = { launched = it.packageName })
        compose.onNodeWithTag("app-test.app0").assertIsFocused()
        val screen = compose.onNodeWithTag("tv-home").getUnclippedBoundsInRoot()
        for (index in 0..4) {
            val tile = compose.onNodeWithTag("app-test.app$index").getUnclippedBoundsInRoot()
            assertTrue(tile.left >= screen.left && tile.right <= screen.right)
        }
        repeat(5) { index -> compose.onNodeWithTag("app-test.app$index").performKeyInput { pressKey(Key.DirectionRight) } }
        compose.onNodeWithTag("app-test.app5").assertIsFocused().assertIsDisplayed()
        compose.onNodeWithTag("app-test.app4").assertDoesNotExist()
        compose.onNodeWithTag("app-test.app5").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals("test.app5", launched) }
        frame("native-api29-page2.png")
    }
    @Test fun zonesAndDetailsHaveAnEscape() {
        show()
        compose.onNodeWithTag("app-test.app0").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("settings").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("app-test.app0").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("widget-codex").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("close-details").assertIsFocused()
        compose.runOnUiThread { ShadowDialog.getLatestDialog().onBackPressed() }
        compose.onNodeWithTag("close-details").assertDoesNotExist()
        compose.onNodeWithTag("widget-codex").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("close-details").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("close-details").assertDoesNotExist()
        compose.onNodeWithTag("widget-codex").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("widget-media").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithText("Source : PC · Indisponible").assertIsDisplayed()
        compose.onNodeWithTag("close-details").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("widget-media").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("app-test.app0").assertIsFocused()
    }
    @Test fun returnRestoresAnAppOnTheLastPage() {
        show(state().copy(lastApp = "test.app7"))
        compose.onNodeWithTag("app-test.app7").assertIsFocused().assertIsDisplayed()
        compose.onNodeWithTag("app-test.app7").performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("app-test.app7").assertIsFocused()
    }
    @Test fun mediaArrivalDoesNotMoveFocusedWidgets() {
        var media by mutableStateOf(Media())
        compose.setContent { TvViewport { HomeContent(state(), media) } }
        compose.onNodeWithTag("app-test.app0").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("widget-codex").assertIsFocused()
        val before = compose.onNodeWithTag("widget-codex").getUnclippedBoundsInRoot()
        compose.runOnIdle { media = Media("AVAILABLE", "TV", "Titre de test", "Artiste de test") }
        compose.onNodeWithTag("widget-codex").assertIsFocused()
        assertEquals(before, compose.onNodeWithTag("widget-codex").getUnclippedBoundsInRoot())
    }
}
