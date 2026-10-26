package com.lizzardtone.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TuningSettings.get(this)
        requestNotificationPermission()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TunerScreen()
                }
            }
        }
    }

    /** Sem essa permissão (Android 13+) a notificação do serviço não aparece, mas o som toca. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission), 1)
        }
    }
}

@Composable
private fun TunerScreen() {
    val context = LocalContext.current
    val state by TuningSettings.state.collectAsState()

    fun toggle(writtenMidi: Int) {
        context.startForegroundService(TonePlaybackService.toggleIntent(context, writtenMidi))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Lizzard Tone", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Toque numa nota para ela soar. Toque de novo para parar; outra nota troca.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(20.dp))
        StatusCard(state) { context.startForegroundService(TonePlaybackService.stopIntent(context)) }

        Spacer(Modifier.height(20.dp))
        Keyboard(state, ::toggle)

        Spacer(Modifier.height(24.dp))
        OctaveRow(state) { delta ->
            TuningSettings.update(context) {
                it.copy(octave = (it.octave + delta).coerceIn(Notes.MIN_OCTAVE, Notes.MAX_OCTAVE))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Tom do instrumento", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Transposition.entries.forEach { option ->
                FilterChip(
                    selected = state.transposition == option,
                    onClick = { TuningSettings.update(context) { it.copy(transposition = option) } },
                    label = { Text(option.label) },
                )
            }
        }
        Text(
            state.transposition.instruments,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        Text("Lá de referência", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(440.0, 442.0, 443.0).forEach { hz ->
                FilterChip(
                    selected = state.a4Hz == hz,
                    onClick = { TuningSettings.update(context) { it.copy(a4Hz = hz) } },
                    label = { Text("${hz.toInt()} Hz") },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Adicione o widget Lizzard Tone à tela inicial para tocar sem abrir o app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusCard(state: TuningState, onStop: () -> Unit) {
    val written = state.activeWrittenMidi
    val sounding = state.activeSoundingMidi
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            if (written != null && sounding != null) {
                val hz = Notes.formatHz(Notes.frequency(sounding, state.a4Hz))
                Text("Soando", style = MaterialTheme.typography.labelMedium)
                Text(
                    if (written == sounding) "${Notes.name(sounding)} · $hz"
                    else "${Notes.name(written)} escrito → ${Notes.name(sounding)} · $hz",
                    style = MaterialTheme.typography.titleLarge,
                )
            } else {
                Text("Silêncio", style = MaterialTheme.typography.labelMedium)
                Text("Oitava ${state.octave} · Lá ${state.a4Hz.toInt()} Hz", style = MaterialTheme.typography.titleLarge)
            }
        }
        if (written != null) {
            OutlinedButton(onClick = onStop) { Text("Parar") }
        }
    }
}

@Composable
private fun Keyboard(state: TuningState, onKey: (Int) -> Unit) {
    KeyRow(state, Notes.SHARPS_ROW, sharps = true, onKey = onKey)
    Spacer(Modifier.height(8.dp))
    KeyRow(state, Notes.NATURALS, sharps = false, onKey = onKey)
}

@Composable
private fun KeyRow(state: TuningState, pitchClasses: List<Int?>, sharps: Boolean, onKey: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        pitchClasses.forEach { pitchClass ->
            if (pitchClass == null) {
                Spacer(Modifier.weight(1f))
                return@forEach
            }
            val written = Notes.midi(pitchClass, state.octave)
            val active = state.activeWrittenMidi == written
            val colors = when {
                active -> ButtonDefaults.buttonColors(containerColor = Color(0xFFF2B33D), contentColor = Color(0xFF1C1300))
                sharps -> ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B2B), contentColor = Color(0xFFF5F5F5))
                else -> ButtonDefaults.buttonColors(containerColor = Color(0xFFEDEDED), contentColor = Color(0xFF1A1A1A))
            }
            Button(
                onClick = { onKey(written) },
                modifier = Modifier.weight(1f).height(72.dp),
                colors = colors,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
            ) {
                Text(Notes.NAMES[pitchClass], textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
private fun OctaveRow(state: TuningState, onDelta: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onDelta(-1) }, enabled = state.octave > Notes.MIN_OCTAVE) { Text("−") }
        Spacer(Modifier.width(12.dp))
        Text(
            "Oitava ${state.octave}  (${Notes.name(Notes.midi(0, state.octave))} a ${Notes.name(Notes.midi(11, state.octave))})",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = { onDelta(1) }, enabled = state.octave < Notes.MAX_OCTAVE) { Text("+") }
    }
}
