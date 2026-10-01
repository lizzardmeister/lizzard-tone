package com.lizzardtone.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder

class NotePlaybackService : Service() {
    private var player: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val note = ReferenceNote.fromId(intent?.getStringExtra(EXTRA_NOTE))
        if (intent?.action != ACTION_PLAY || note == null) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification(note))
        player?.release()
        player = null

        val newPlayer = MediaPlayer()
        try {
            newPlayer.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                resources.openRawResourceFd(note.soundRes).use { descriptor ->
                    setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                }
                setOnCompletionListener {
                    it.release()
                    if (player === it) player = null
                    stopSelfResult(startId)
                }
                setOnErrorListener { failedPlayer, _, _ ->
                    failedPlayer.release()
                    if (player === failedPlayer) player = null
                    stopSelfResult(startId)
                    true
                }
                prepare()
            }
            player = newPlayer
            newPlayer.start()
        } catch (_: Exception) {
            newPlayer.release()
            player = null
            stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Notas de referência",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun notification(note: ReferenceNote): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Lizzard Tone")
            .setContentText("Tocando ${note.label}")
            .setContentIntent(openApp)
            .setOngoing(false)
            .build()
    }

    companion object {
        const val ACTION_PLAY = "com.lizzardtone.app.PLAY_NOTE"
        const val EXTRA_NOTE = "com.lizzardtone.app.NOTE"
        private const val CHANNEL_ID = "reference_note_playback"
        private const val NOTIFICATION_ID = 1001
    }
}
