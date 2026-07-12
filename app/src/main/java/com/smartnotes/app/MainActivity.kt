package com.smartnotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.room.Room
import dagger.hilt.android.AndroidEntryPoint
import com.smartnotes.app.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartNotesRoot(applicationContext)
                }
            }
        }
    }
}

@Composable
private fun SmartNotesRoot(context: android.content.Context) {
    LaunchedEffect(Unit) {
        // Force SQLite to create the underlying DB file on first launch.
        withContext(Dispatchers.IO) {
            val db = Room.databaseBuilder(context, AppDatabase::class.java, "smartnotes.db").build()
            db.openHelper.writableDatabase
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("SmartNotes")
    }
}
