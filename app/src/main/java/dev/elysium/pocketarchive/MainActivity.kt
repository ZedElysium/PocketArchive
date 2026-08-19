package dev.elysium.pocketarchive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.elysium.pocketarchive.feature.library.LibraryScreen
import dev.elysium.pocketarchive.ui.theme.PocketArchiveTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketArchiveTheme {
                LibraryScreen(
                    importEnabled = false,
                    onImportClick = {},
                )
            }
        }
    }
}
