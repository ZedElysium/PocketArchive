package dev.elysium.pocketarchive.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.elysium.pocketarchive.core.model.ArchiveItem
import dev.elysium.pocketarchive.core.model.ArchiveItemType

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

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.items.isNotEmpty() -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, key = { it.id.value }) { item ->
                            ArchiveItemCard(item)
                        }
                    }

                    state.loadStatus == LibraryLoadStatus.Ready -> EmptyLibraryContent()
                    state.loadStatus == LibraryLoadStatus.Loading -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("正在加载资料")
                    }
                }
            }

            if (state.loadStatus == LibraryLoadStatus.Failed) {
                Text("资料读取失败，请重试", color = MaterialTheme.colorScheme.error)
                Button(onClick = { onAction(LibraryAction.RetryLoading) }) {
                    Text("重新加载")
                }
            }

            ImportControls(
                importEnabled = state.isImportEnabled,
                importStatus = state.importStatus,
                onImportClick = { onAction(LibraryAction.ImportClicked) },
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
            )

        }
    }
}

@Composable
private fun EmptyLibraryContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("还没有资料", style = MaterialTheme.typography.headlineSmall)
        Text(
            "导入 PDF 或图片，保存在你的资料库中。",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ArchiveItemCard(item: ArchiveItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(item.displayName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${if (item.type == ArchiveItemType.PDF) "PDF" else "图片"} · ${item.sizeBytes} 字节",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ImportControls(
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

                LibraryImportStatus.Importing -> "正在保存资料，请稍候"
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
            state = LibraryUiState(loadStatus = LibraryLoadStatus.Ready),
            onAction = {},
        )
    }
}
