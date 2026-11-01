package dev.elysium.pocketarchive.feature.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.elysium.pocketarchive.core.model.ArchiveItem
import dev.elysium.pocketarchive.core.model.ArchiveItemId
import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.time.Instant
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
                    state = LibraryUiState(loadStatus = LibraryLoadStatus.Ready),
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
        composeRule.onNodeWithText("正在保存资料，请稍候").assertIsDisplayed()
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

    @Test
    fun loading_doesNotShowEmptyLibrary() {
        composeRule.setContent {
            MaterialTheme { LibraryScreen(LibraryUiState(), onAction = {}) }
        }
        composeRule.onNodeWithText("正在加载资料").assertIsDisplayed()
        composeRule.onNodeWithText("还没有资料").assertDoesNotExist()
    }

    @Test
    fun repositoryStateUpdate_replacesEmptyLibraryWithRealItems() {
        val state = mutableStateOf(LibraryUiState(loadStatus = LibraryLoadStatus.Ready))
        composeRule.setContent {
            MaterialTheme { LibraryScreen(state.value, onAction = {}) }
        }
        composeRule.onNodeWithText("还没有资料").assertIsDisplayed()

        composeRule.runOnIdle {
            state.value = state.value.copy(items = listOf(
                ArchiveItem(
                    id = ArchiveItemId("item-1"),
                    displayName = "guide.pdf",
                    type = ArchiveItemType.PDF,
                    mimeType = "application/pdf",
                    sizeBytes = 321L,
                    importedAt = Instant.parse("2026-11-01T00:00:00Z"),
                ),
            ))
        }

        composeRule.onNodeWithText("guide.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("PDF · 321 字节").assertIsDisplayed()
        composeRule.onNodeWithText("还没有资料").assertDoesNotExist()
        composeRule.onNodeWithText("导入资料").assertIsEnabled()
    }

    @Test
    fun failedLoading_offersRetryAction() {
        var action: LibraryAction? = null
        composeRule.setContent {
            MaterialTheme {
                LibraryScreen(
                    LibraryUiState(loadStatus = LibraryLoadStatus.Failed),
                    onAction = { action = it },
                )
            }
        }
        composeRule.onNodeWithText("资料读取失败，请重试").assertIsDisplayed()
        composeRule.onNodeWithText("还没有资料").assertDoesNotExist()
        composeRule.onNodeWithText("重新加载").performClick()
        composeRule.runOnIdle { assertEquals(LibraryAction.RetryLoading, action) }
    }
}
