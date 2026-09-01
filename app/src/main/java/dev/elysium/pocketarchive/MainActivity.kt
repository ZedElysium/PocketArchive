package dev.elysium.pocketarchive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.elysium.pocketarchive.feature.library.LibraryScreen
import dev.elysium.pocketarchive.feature.library.LibraryViewModel
import dev.elysium.pocketarchive.ui.theme.PocketArchiveTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
}
