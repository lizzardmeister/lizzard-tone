package com.lizzardtone.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.IBinder

/**
 * Mantém uma nota soando até ser desligada. Serviço em primeiro plano porque a nota
 * não tem fim definido e o processo precisa continuar vivo com a tela inicial à frente.
 *
 * [ACTION_TOGGLE] com [EXTRA_WRITTEN_MIDI]: liga a nota, ou desliga se ela já está
 * soando. Outra nota substitui a atual. [ACTION_STOP] silencia. [ACTION_RETUNE]
 * reaplica tom e Lá de referência à nota em curso.
 */
class TonePlaybackService : Service() {
    private val synth = ToneSynth()
    private var focusRequest: AudioFocusRequest? = null

    /** Só esta instância sabe se há som de verdade; o estado gravado pode estar órfão. */
    private var sounding = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val state = TuningSettings.get(this)
        startForeground(NOTIFICATION_ID, notification(state))

        when (intent?.action) {
            ACTION_TOGGLE -> {
                val written = intent.getIntExtra(EXTRA_WRITTEN_MIDI, -1)
                when {
                    written < 0 -> stopPlayback()
                    sounding && state.activeWrittenMidi == written -> stopPlayback()
                    else -> startNote(written, state)
                }
            }
            ACTION_RETUNE -> {
                val written = state.activeWrittenMidi
                if (written == null) stopPlayback() else startNote(written, state)
            }
            else -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        synth.release()
        abandonFocus()
        super.onDestroy()
    }

    private fun startNote(writtenMidi: Int, state: TuningState) {
        val sounding = state.soundingMidiFor(writtenMidi)
        if (!requestFocus()) {
            stopPlayback()
            return
        }
        synth.play(Notes.frequency(sounding, state.a4Hz))
        this.sounding = true
        val updated = TuningSettings.update(this) {
            it.copy(activeWrittenMidi = writtenMidi, activeSoundingMidi = sounding)
        }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(updated))
    }

    private fun stopPlayback() {
        synth.stop()
        sounding = false
        abandonFocus()
        TuningSettings.update(this) { it.copy(activeWrittenMidi = null, activeSoundingMidi = null) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun requestFocus(): Boolean {
        val manager = getSystemService(AudioManager::class.java)
        val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    stopPlayback()
                }
            }
            .build()
            .also { focusRequest = it }
        return manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Nota de referência", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun notification(state: TuningState): Notification {
        val written = state.activeWrittenMidi
        val sounding = state.activeSoundingMidi
        val text = when {
            written == null || sounding == null -> "Silêncio"
            written == sounding -> "${Notes.name(sounding)} · ${Notes.formatHz(Notes.frequency(sounding, state.a4Hz))}"
            else -> "${Notes.name(written)} escrito · soa ${Notes.name(sounding)} · " +
                Notes.formatHz(Notes.frequency(sounding, state.a4Hz))
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Lizzard Tone")
            .setContentText(text)
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Parar", stop).build())
            .build()
    }

    companion object {
        const val ACTION_TOGGLE = "com.lizzardtone.app.TOGGLE_NOTE"
        const val ACTION_STOP = "com.lizzardtone.app.STOP"

        /** Recalcula a nota que está soando com o tom e o Lá de referência atuais. */
        const val ACTION_RETUNE = "com.lizzardtone.app.RETUNE"
        const val EXTRA_WRITTEN_MIDI = "com.lizzardtone.app.WRITTEN_MIDI"
        private const val CHANNEL_ID = "reference_note_playback"
        private const val NOTIFICATION_ID = 1001

        /** Intent distinto por tecla, para cada botão do widget ter seu próprio PendingIntent. */
        fun toggleIntent(context: Context, writtenMidi: Int): Intent =
            Intent(context, TonePlaybackService::class.java)
                .setAction(ACTION_TOGGLE)
                .setData(Uri.parse("lizzardtone://toggle/$writtenMidi"))
                .putExtra(EXTRA_WRITTEN_MIDI, writtenMidi)

        fun stopIntent(context: Context): Intent =
            Intent(context, TonePlaybackService::class.java).setAction(ACTION_STOP)

        fun retuneIntent(context: Context): Intent =
            Intent(context, TonePlaybackService::class.java).setAction(ACTION_RETUNE)
    }
}
