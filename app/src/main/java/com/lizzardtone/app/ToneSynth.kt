package com.lizzardtone.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tom contínuo sintetizado na hora por um [AudioTrack] em streaming.
 *
 * [play] troca a frequência com um fade curto (sem estalo) e [stop] faz fade-out.
 * A thread de áudio nasce no primeiro [play] e morre sozinha pouco depois do silêncio.
 */
class ToneSynth {
    @Volatile private var targetHz = 0.0
    private var thread: Thread? = null
    private val lock = Any()

    fun play(frequencyHz: Double) {
        targetHz = frequencyHz
        synchronized(lock) {
            if (thread?.isAlive != true) {
                thread = Thread(::render, "ToneSynth").also { it.start() }
            }
        }
    }

    fun stop() {
        targetHz = 0.0
    }

    /** Para e espera a thread terminar. */
    fun release() {
        targetHz = 0.0
        synchronized(lock) { thread }?.join(500)
    }

    private fun render() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuffer, FRAMES_PER_BLOCK * 2 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val block = ShortArray(FRAMES_PER_BLOCK)
        var currentHz = 0.0
        var phase = 0.0
        var amp = 0.0
        var silentFrames = 0
        val ampStep = 1.0 / (FADE_SECONDS * SAMPLE_RATE)

        try {
            track.play()
            while (true) {
                for (i in block.indices) {
                    val wanted = targetHz
                    // Só troca de frequência com o volume em zero, para não estalar.
                    if (amp <= 0.0 && wanted != currentHz) {
                        currentHz = wanted
                        phase = 0.0
                    }
                    val goal = if (wanted > 0.0 && wanted == currentHz) 1.0 else 0.0
                    amp = if (amp < goal) minOf(goal, amp + ampStep) else maxOf(goal, amp - ampStep)

                    val sample = if (amp > 0.0 && currentHz > 0.0) {
                        phase += 2.0 * PI * currentHz / SAMPLE_RATE
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                        amp * timbre(phase)
                    } else {
                        0.0
                    }
                    block[i] = (sample * MASTER_GAIN * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(block, 0, block.size)

                silentFrames = if (amp <= 0.0 && targetHz <= 0.0) silentFrames + block.size else 0
                if (silentFrames >= SAMPLE_RATE * IDLE_SECONDS_BEFORE_EXIT) break
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    /** Fundamental com alguns harmônicos, normalizado para pico 1.0. */
    private fun timbre(phase: Double): Double =
        (sin(phase) + 0.35 * sin(2 * phase) + 0.16 * sin(3 * phase) + 0.08 * sin(4 * phase)) / 1.59

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val FRAMES_PER_BLOCK = 480
        const val FADE_SECONDS = 0.02
        const val MASTER_GAIN = 0.45
        const val IDLE_SECONDS_BEFORE_EXIT = 0.5
    }
}
