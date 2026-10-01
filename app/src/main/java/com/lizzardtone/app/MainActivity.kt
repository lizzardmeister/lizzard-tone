package com.lizzardtone.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PrototypeScreen { note ->
                        startForegroundService(
                            Intent(this, NotePlaybackService::class.java)
                                .setAction(NotePlaybackService.ACTION_PLAY)
                                .putExtra(NotePlaybackService.EXTRA_NOTE, note.name),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrototypeScreen(play: (ReferenceNote) -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Lizzard Tone", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Referência rápida para afinação coral")
        Spacer(Modifier.height(24.dp))
        ReferenceNote.entries.chunked(2).forEach { rowNotes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowNotes.forEach { note ->
                    Button(
                        onClick = { play(note) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(note.label)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Text("Lá4 = 440 Hz · Adicione o widget à tela inicial para tocar sem abrir o app.")
    }
}
