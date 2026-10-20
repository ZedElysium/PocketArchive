package dev.elysium.pocketarchive.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                text = "资料库",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.weight(1f))

            EmptyLibraryContent(
                importEnabled = state.isImportEnabled,
                importStatus = state.importStatus,
                onImportClick = { onAction(LibraryAction.ImportClicked) },
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EmptyLibraryContent(
    importEnabled: Boolean,
    importStatus: LibraryImportStatus,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (importStatus is LibraryImportStatus.Succeeded) {
                "资料已保存"
            } else {
                "还没有资料"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = if (importStatus is LibraryImportStatus.Succeeded) {
                "文件已复制到应用私有存储，资料列表将在后续里程碑接入。"
            } else {
                "可以把 PDF 和图片导入 PocketArchive 的私有存储。"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onImportClick,
            enabled = importEnabled,
        ) {
            Text(
                if (importStatus == LibraryImportStatus.Importing) {
                    "正在导入"
                } else {
                    "导入资料"
                },
            )
        }
        Text(
            text = when (importStatus) {
                LibraryImportStatus.Idle -> if (importEnabled) {
                    "当前支持 PDF 或图片"
                } else {
                    "正在打开系统文件选择器"
                }

                LibraryImportStatus.Importing -> "正在复制文件，请稍候"
                is LibraryImportStatus.Succeeded ->
                    "已导入：${importStatus.displayName}"

                is LibraryImportStatus.Failed -> when (importStatus.error) {
                    LibraryImportError.UNSUPPORTED_DOCUMENT -> "不支持这种文件类型"
                    LibraryImportError.ACCESS_DENIED -> "无法读取该文件，请重新选择"
                    LibraryImportError.IMPORT_FAILED -> "导入失败，请重试"
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Library empty - light", showBackground = true)
@Preview(name = "Library empty - dark", showBackground = true, uiMode = 0x20)
@Composable
private fun LibraryScreenPreview() {
    MaterialTheme {
        LibraryScreen(
            state = LibraryUiState(),
            onAction = {},
        )
    }
}
