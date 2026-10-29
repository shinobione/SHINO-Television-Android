@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package fr.shinobiwan.tvhome

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProvider
import fr.shinobiwan.tvhome.data.Settings
import fr.shinobiwan.tvhome.ui.SettingsScreen
import fr.shinobiwan.tvhome.ui.TvViewport
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], qualifiers = "w960dp-h540dp-land-xhdpi")
class HubPairingSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val importLabel = "Importer configuration HUB depuis une clé USB (.json)"

    private fun show(document: String?, alternate: Boolean = false): HomeViewModel {
        val uri = Uri.parse("content://synthetic-usb/SHINO-TV-HUB.json")
        var launched = false
        shadowOf(compose.activity.contentResolver).registerInputStreamSupplier(uri) {
            document.orEmpty().byteInputStream()
        }
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>,
                                         input: I, options: ActivityOptionsCompat?) {
                launched = true
                val intent = contract.createIntent(compose.activity, input)
                assertEquals(if (alternate) Intent.ACTION_GET_CONTENT else Intent.ACTION_OPEN_DOCUMENT, intent.action)
                dispatchResult(requestCode, if (document == null) Activity.RESULT_CANCELED else Activity.RESULT_OK,
                    Intent().setData(uri))
            }
        }
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        val model = ViewModelProvider(compose.activity)[HomeViewModel::class.java]
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                TvViewport { SettingsScreen(HomeState(settings = Settings(protocol = "HUB_V2")), model) {} }
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        val label = if (alternate) "Autre explorateur de fichiers (si USB absent)" else importLabel
        compose.onNodeWithText(label).performScrollTo()
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
        assertTrue("Picker launch callback executed", launched)
        return model
    }

    private fun document() = JSONObject().put("format", "SHINO_TV_HOME_HUB_PAIRING").put("version", 1)
        .put("host", "192.168.1.195").put("port", 8767)
        .put("caPem", javaClass.getResource("/tvhome-test-ca.pem")!!.readText())
        .put("certificateSha256", "a".repeat(64)).put("readerToken", "test_reader_tvhome_123456789").toString()

    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun openDocumentImportsIntoDraftWithoutSaving() {
        val model = show(document())
        awaitText("Configuration HUB importée pour 192.168.1.195:8767. Appuyez sur Enregistrer.")
        assertTrue(model.state.value.settings.encryptedToken.isBlank())
        assertTrue(model.state.value.settings.caPem.isBlank())
    }

    @Test fun getContentAlternativeUsesTheSameImporter() {
        show(document(), alternate = true)
        awaitText("Configuration HUB importée pour 192.168.1.195:8767. Appuyez sur Enregistrer.")
    }

    @Test fun invalidAndOversizedDocumentsShowGenericError() {
        show("x".repeat(24577))
        awaitText("Import impossible : choisissez un fichier SHINO HUB valide (.json).")
    }

    @Test fun cancellingThePickerDoesNotImportOrSave() {
        val model = show(null)
        compose.waitForIdle()
        compose.onAllNodesWithText("Configuration HUB importée", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("Import impossible", substring = true).assertCountEquals(0)
        assertTrue(model.state.value.settings.encryptedToken.isBlank())
    }
}
