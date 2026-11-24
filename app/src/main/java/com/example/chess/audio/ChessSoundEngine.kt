package com.example.chess.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class ChessSound {
    MOVE,
    CAPTURE,
    CASTLE,
    CHECK,
    PROMOTION,
    GAME_OVER,
    PUZZLE_CORRECT,
    PUZZLE_WRONG
}

/**
 * Pure Kotlin PCM sound synthesizer using Android AudioTrack.
 * Generates tactile wooden board clicks, captures, checks, promotions, and fanfares
 * completely offline with zero external audio files.
 */
class ChessSoundEngine {
    private val sampleRate = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var enabled: Boolean = true

    private val buffers: Map<ChessSound, ShortArray> by lazy {
        mapOf(
            ChessSound.MOVE to synthMoveThock(baseFreq = 190.0, durationMs = 55, crispness = 0.25),
            ChessSound.CAPTURE to synthCaptureSnap(),
            ChessSound.CASTLE to synthDoubleThock(),
            ChessSound.CHECK to synthCheckAlert(),
            ChessSound.PROMOTION to synthPromotionChime(),
            ChessSound.GAME_OVER to synthGameOverChord(),
            ChessSound.PUZZLE_CORRECT to synthPuzzleCorrect(),
            ChessSound.PUZZLE_WRONG to synthPuzzleWrong()
        )
    }

    fun play(sound: ChessSound) {
        if (!enabled) return
        val pcm = buffers[sound] ?: return
        scope.launch {
            playPcm(pcm)
        }
    }

    private fun playPcm(pcm: ShortArray) {
        var track: AudioTrack? = null
        try {
            val byteLen = pcm.size * 2
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBuf, byteLen)
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
            val durationMs = (pcm.size * 1000L / sampleRate) + 40L
            Thread.sleep(durationMs)
        } catch (_: Throwable) {
            // Ignore audio hardware failures on headless/muted devices
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Throwable) {
                // Safe cleanup
            }
        }
    }

    private fun synthMoveThock(baseFreq: Double, durationMs: Int, crispness: Double): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        val rng = Random(42)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 68.0)
            val pitchDrop = baseFreq * (1.0 + 0.55 * exp(-t * 140.0))
            val body = sin(2.0 * PI * pitchDrop * t) * 0.72
            val overtone = sin(2.0 * PI * (pitchDrop * 2.4) * t) * 0.18
            val noise = (rng.nextDouble() * 2.0 - 1.0) * crispness * exp(-t * 210.0)
            val sample = ((body + overtone + noise) * env * 22000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sample.toShort()
        }
        return out
    }

    private fun synthCaptureSnap(): ShortArray {
        val count = (sampleRate * 85) / 1000
        val out = ShortArray(count)
        val rng = Random(99)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 48.0)
            val punch = sin(2.0 * PI * (145.0 * (1.0 + 0.9 * exp(-t * 110.0))) * t) * 0.78
            val click = (rng.nextDouble() * 2.0 - 1.0) * 0.55 * exp(-t * 160.0)
            val sample = ((punch + click) * env * 26000.0).toInt().coerceIn(-32767, 32767)
            out[i] = sample.toShort()
        }
        return out
    }

    private fun synthDoubleThock(): ShortArray {
        val first = synthMoveThock(185.0, 52, 0.22)
        val second = synthMoveThock(225.0, 58, 0.28)
        val gap = (sampleRate * 38) / 1000
        val out = ShortArray(first.size + gap + second.size)
        first.copyInto(out, 0)
        second.copyInto(out, first.size + gap)
        return out
    }

    private fun synthCheckAlert(): ShortArray {
        val count = (sampleRate * 165) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 20.0) * (1.0 - exp(-t * 250.0))
            val tone = sin(2.0 * PI * 680.0 * t) * 0.55 + sin(2.0 * PI * 1020.0 * t) * 0.35
            out[i] = (tone * env * 21000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthPromotionChime(): ShortArray {
        val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteLen = (sampleRate * 65) / 1000
        val out = ShortArray(noteLen * freqs.size)
        for (n in freqs.indices) {
            val freq = freqs[n]
            for (i in 0 until noteLen) {
                val t = i.toDouble() / sampleRate
                val env = exp(-t * 22.0) * (1.0 - exp(-t * 320.0))
                val s = sin(2.0 * PI * freq * t) * 0.7 + sin(2.0 * PI * freq * 2.0 * t) * 0.2
                out[n * noteLen + i] = (s * env * 19000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }

    private fun synthGameOverChord(): ShortArray {
        val count = (sampleRate * 360) / 1000
        val out = ShortArray(count)
        val chord = doubleArrayOf(261.63, 329.63, 392.00, 523.25)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 6.5) * (1.0 - exp(-t * 140.0))
            var mix = 0.0
            for (f in chord) {
                mix += sin(2.0 * PI * f * t) / chord.size
            }
            out[i] = (mix * env * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthPuzzleCorrect(): ShortArray {
        val freqs = doubleArrayOf(587.33, 880.0)
        val noteLen = (sampleRate * 75) / 1000
        val out = ShortArray(noteLen * 2)
        for (n in freqs.indices) {
            for (i in 0 until noteLen) {
                val t = i.toDouble() / sampleRate
                val env = exp(-t * 19.0) * (1.0 - exp(-t * 300.0))
                val s = sin(2.0 * PI * freqs[n] * t)
                out[n * noteLen + i] = (s * env * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
            }
        }
        return out
    }

    private fun synthPuzzleWrong(): ShortArray {
        val count = (sampleRate * 180) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 14.0)
            val s = sin(2.0 * PI * 165.0 * t) * 0.65 + sin(2.0 * PI * 155.0 * t) * 0.35
            out[i] = (s * env * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }
}
