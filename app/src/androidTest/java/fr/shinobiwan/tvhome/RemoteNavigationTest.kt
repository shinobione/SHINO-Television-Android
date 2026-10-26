@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package fr.shinobiwan.tvhome

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** Execute only on an owner-authorized emulator/TV. No device package changes in the build. */
class RemoteNavigationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun settingsCanBeReachedAndClosedWithRemote(){
        compose.onNodeWithText("Réglages").assertExists().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        compose.onNodeWithText("Réglages").performKeyInput{pressKey(Key.DirectionCenter)}
        compose.onNodeWithText("Votre TV, à votre façon.").assertIsDisplayed()
        compose.onNodeWithText("Retour",useUnmergedTree=true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        compose.onNodeWithText("Retour",useUnmergedTree=true).performKeyInput{pressKey(Key.DirectionCenter)}
        compose.onNodeWithText("Réglages").assertIsDisplayed()
    }
    @Test fun eachDirectionKeepsAnAccessibleSettingsEscape(){
        compose.onNodeWithText("Réglages").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        compose.onNodeWithText("Réglages").performKeyInput{
            pressKey(Key.DirectionDown);pressKey(Key.DirectionLeft);pressKey(Key.DirectionRight);pressKey(Key.DirectionUp)
        }
        compose.onNodeWithText("Réglages").assertExists()
    }
}
