package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Invisible, zero-asset, high-energy Audio Synthesizer for Chess Sound Effects.
 * Plays at 100% full volume to keep players alert, energized, and engaged!
 */
object ChessAudioPlayer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    var isMuted: Boolean = false
    var volumeScale: Float = 0.85f

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun playMoveSound() {
        scope.launch {
            val durationMs = 75
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val freq = 420.0 - (progress * 300.0)
                val decay = Math.exp(-progress * 7.5)
                val sample = sin(2.0 * PI * freq * t) * decay * 32767.0
                buffer[i] = sample.coerceIn(-32767.0, 32767.0).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playCaptureSound() {
        scope.launch {
            val durationMs = 130
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            val random = java.util.Random(1337)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val freq = 240.0 - (progress * 150.0)
                val decay = Math.exp(-progress * 5.5)
                val tone = sin(2.0 * PI * freq * t) * 0.65
                val noise = (random.nextDouble() * 2.0 - 1.0) * 0.35 * Math.exp(-progress * 12.0)
                val sample = (tone + noise) * decay * 32767.0
                buffer[i] = sample.coerceIn(-32767.0, 32767.0).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playCheckSound() {
        scope.launch {
            val durationMs = 300
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val decay = Math.exp(-progress * 4.5)
                val tone1 = sin(2.0 * PI * 880.0 * t) * 0.6
                val tone2 = sin(2.0 * PI * 1320.0 * t) * 0.4
                val sample = (tone1 + tone2) * decay * 32767.0
                buffer[i] = sample.coerceIn(-32767.0, 32767.0).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playWinSound() {
        scope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50)
            val noteDurationMs = 120
            val totalSamples = (SAMPLE_RATE * noteDurationMs * notes.size) / 1000
            val buffer = ShortArray(totalSamples)

            var sampleIdx = 0
            for (freq in notes) {
                val noteSamples = (SAMPLE_RATE * noteDurationMs) / 1000
                for (i in 0 until noteSamples) {
                    if (sampleIdx >= totalSamples) break
                    val t = i.toDouble() / SAMPLE_RATE
                    val progress = i.toDouble() / noteSamples
                    val decay = Math.exp(-progress * 2.8)
                    val tone = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 2.0 * t)
                    val sample = (tone / 1.3) * decay * 32767.0
                    buffer[sampleIdx++] = sample.coerceIn(-32767.0, 32767.0).toInt().toShort()
                }
            }
            playPcm(buffer)
        }
    }

    fun playDefeatSound() {
        scope.launch {
            val durationMs = 280
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val freq = if (progress < 0.5) 210.0 else 145.0
                val decay = 1.0 - progress * 0.4
                val tone = if (sin(2.0 * PI * freq * t) > 0) 0.85 else -0.85
                val sample = tone * decay * 30000.0
                buffer[i] = sample.coerceIn(-32767.0, 32767.0).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    fun playStartSound() {
        scope.launch {
            val durationMs = 380
            val numSamples = (SAMPLE_RATE * durationMs) / 1000
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val decay = Math.exp(-progress * 3.5)
                val tone = sin(2.0 * PI * 587.33 * t) * 0.65 + sin(2.0 * PI * 880.0 * t) * 0.35
                val sample = tone * decay * 32767.0
                buffer[i] = sample.coerceIn(-32767.0, 32767.0).toInt().toShort()
            }
            playPcm(buffer)
        }
    }

    private fun playPcm(pcmData: ShortArray) {
        if (isMuted || volumeScale <= 0f) return
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.setVolume(volumeScale.coerceIn(0.0f, 1.0f))
            audioTrack.play()
            Thread.sleep((pcmData.size * 1000L / SAMPLE_RATE) + 40)
            audioTrack.release()
        } catch (_: Exception) {}
    }
}
