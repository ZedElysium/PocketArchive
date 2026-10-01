package dev.elysium.pocketarchive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.elysium.pocketarchive.feature.library.LibraryAction
import dev.elysium.pocketarchive.feature.library.LibraryEffect
import dev.elysium.pocketarchive.feature.library.LibraryScreen
import dev.elysium.pocketarchive.feature.library.LibraryViewModel
import dev.elysium.pocketarchive.ui.theme.PocketArchiveTheme
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { documentUri ->
        libraryViewModel.onAction(
            LibraryAction.DocumentPickerFinished(documentUri?.toString()),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        collectLibraryEffects()
        setContent {
            val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()

            PocketArchiveTheme {
                LibraryScreen(
                    state = uiState,
                    onAction = libraryViewModel::onAction,
                )
            }
        }
    }

    private fun collectLibraryEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                libraryViewModel.effects.collect { effect ->
                    when (effect) {
                        LibraryEffect.OpenDocumentPicker -> {
                            openDocumentLauncher.launch(SUPPORTED_DOCUMENT_MIME_TYPES)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val SUPPORTED_DOCUMENT_MIME_TYPES = arrayOf(
            "application/pdf",
            "image/*",
        )
    }
}
