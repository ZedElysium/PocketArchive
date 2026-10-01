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
    fun emptyLibrary_showsAvailableDocumentPickerState() {
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
        composeRule.onNodeWithText("导入资料").assertIsEnabled()
        composeRule.onNodeWithText("当前可选择 PDF 或图片，文件暂不会保存").assertIsDisplayed()
    }

    @Test
    fun enabledImportButton_dispatchesImportAction() {
        var receivedAction: LibraryAction? = null
        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    state = LibraryUiState(),
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

    @Test
    fun openingDocumentPicker_disablesImportButton() {
        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    state = LibraryUiState(isImportEnabled = false),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("导入资料").assertIsNotEnabled()
        composeRule.onNodeWithText("正在打开系统文件选择器").assertIsDisplayed()
    }

    @Test
    fun selectedDocument_showsPendingSaveState() {
        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    state = LibraryUiState(hasSelectedDocument = true),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("导入资料").assertIsEnabled()
        composeRule.onNodeWithText("已选择资料，保存功能将在下一里程碑接入")
            .assertIsDisplayed()
    }
}
