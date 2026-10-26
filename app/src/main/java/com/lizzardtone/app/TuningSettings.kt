package com.lizzardtone.app

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TuningState(
    val octave: Int = 4,
    val transposition: Transposition = Transposition.C,
    val a4Hz: Double = 440.0,
    /** Tecla tocada (nota escrita), ou null se nada está soando. */
    val activeWrittenMidi: Int? = null,
    /** Nota real que está soando, ou null. */
    val activeSoundingMidi: Int? = null,
) {
    val isSounding: Boolean get() = activeWrittenMidi != null && activeSoundingMidi != null

    fun soundingMidiFor(writtenMidi: Int): Int = writtenMidi + transposition.semitones
}

/** Escopo do processo para trabalho que não pode morrer junto com quem o pediu. */
internal val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Configuração compartilhada por app, widget e serviço, todos no mesmo processo.
 * Gravada em SharedPreferences e exposta como [StateFlow] para a tela do app.
 * Toda mudança redesenha o widget.
 */
object TuningSettings {
    private const val PREFS = "tuning"
    private val lock = Any()
    private var flow: MutableStateFlow<TuningState>? = null

    val state: StateFlow<TuningState>
        get() = flow ?: error("TuningSettings.get(context) ainda não foi chamado")

    fun get(context: Context): TuningState = ensure(context).value

    fun update(context: Context, transform: (TuningState) -> TuningState): TuningState {
        val stateFlow = ensure(context)
        val (previous, next) = synchronized(lock) {
            val before = stateFlow.value
            val updated = transform(before)
            stateFlow.value = updated
            persist(context, updated)
            before to updated
        }
        appScope.launch { ReferenceWidget().updateAll(context.applicationContext) }

        // Tom ou Lá de referência mudaram com uma nota soando: o serviço já está em
        // primeiro plano, então pedir a reafinação por startService é permitido.
        val tuningChanged = previous.transposition != next.transposition || previous.a4Hz != next.a4Hz
        if (tuningChanged && next.isSounding) {
            context.applicationContext.startService(TonePlaybackService.retuneIntent(context))
        }
        return next
    }

    private fun ensure(context: Context): MutableStateFlow<TuningState> {
        flow?.let { return it }
        synchronized(lock) {
            flow?.let { return it }
            val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            // Processo novo = nada está soando: o serviço vive neste mesmo processo.
            // Uma nota ativa gravada aqui é resto de um processo que morreu.
            val loaded = TuningState(
                octave = prefs.getInt("octave", 4).coerceIn(Notes.MIN_OCTAVE, Notes.MAX_OCTAVE),
                transposition = Transposition.fromName(prefs.getString("transposition", null)),
                a4Hz = prefs.getFloat("a4", 440f).toDouble(),
                activeWrittenMidi = null,
                activeSoundingMidi = null,
            )
            persist(context, loaded)
            return MutableStateFlow(loaded).also { flow = it }
        }
    }

    private fun persist(context: Context, state: TuningState) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("octave", state.octave)
            .putString("transposition", state.transposition.name)
            .putFloat("a4", state.a4Hz.toFloat())
            .putInt("activeWritten", state.activeWrittenMidi ?: -1)
            .putInt("activeSounding", state.activeSoundingMidi ?: -1)
            .apply()
    }
}
