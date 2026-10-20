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
        composeRule.onNodeWithText("当前支持 PDF 或图片").assertIsDisplayed()
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
    fun importingDocument_disablesImportButtonAndShowsProgress() {
        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    state = LibraryUiState(
                        isImportEnabled = false,
                        importStatus = LibraryImportStatus.Importing,
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("正在导入").assertIsNotEnabled()
        composeRule.onNodeWithText("正在复制文件，请稍候").assertIsDisplayed()
    }

    @Test
    fun successfulImport_showsSavedDocumentName() {
        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    state = LibraryUiState(
                        importStatus = LibraryImportStatus.Succeeded("guide.pdf"),
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("资料已保存").assertIsDisplayed()
        composeRule.onNodeWithText("已导入：guide.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("导入资料").assertIsEnabled()
    }

    @Test
    fun failedImport_showsActionableMessage() {
        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    state = LibraryUiState(
                        importStatus = LibraryImportStatus.Failed(
                            LibraryImportError.ACCESS_DENIED,
                        ),
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("无法读取该文件，请重新选择").assertIsDisplayed()
        composeRule.onNodeWithText("导入资料").assertIsEnabled()
    }
}
