package dev.elysium.pocketarchive.feature.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyLibrary_showsHonestImportState() {
        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    state = LibraryUiState(),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("资料库").assertIsDisplayed()
        composeRule.onNodeWithText("还没有资料").assertIsDisplayed()
        composeRule.onNodeWithText("导入资料").assertIsNotEnabled()
        composeRule.onNodeWithText("导入功能将在下一里程碑接入").assertIsDisplayed()
    }

    @Test
    fun enabledImportButton_dispatchesImportAction() {
        var receivedAction: LibraryAction? = null
        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    state = LibraryUiState(isImportEnabled = true),
                    onAction = { receivedAction = it },
                )
            }
        }

        composeRule.onNodeWithText("导入资料")
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(LibraryAction.ImportClicked, receivedAction)
        }
    }
}
