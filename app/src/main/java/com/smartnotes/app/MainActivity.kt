package com.smartnotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartnotes.app.ui.noteeditor.NoteEditorScreen
import com.smartnotes.app.ui.notelist.NoteListScreen
import dagger.hilt.android.AndroidEntryPoint

private const val ROUTE_NOTE_LIST = "note_list"
private const val ROUTE_NOTE_EDITOR = "note_editor"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = ROUTE_NOTE_LIST) {
                        composable(ROUTE_NOTE_LIST) {
                            NoteListScreen(onAddNote = { navController.navigate(ROUTE_NOTE_EDITOR) })
                        }
                        composable(ROUTE_NOTE_EDITOR) {
                            NoteEditorScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
