package com.example.media

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.MusicTrackPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * 100% Offline-capable procedural royalty-free background music synthesizer.
 * Generates 16-bit mono 44.1kHz PCM for both live studio AudioTrack preview
 * and real AAC audio mixing during MP4 export.
 */
class ProceduralMusicEngine {
    companion object {
        const val SAMPLE_RATE = 44100
    }

    private var previewJob: Job? = null
    private var audioTrack: AudioTrack? = null

    fun startPreview(
        scope: CoroutineScope,
        preset: MusicTrackPreset,
        volume: Float,
        onStopped: () -> Unit = {}
    ) {
        stopPreview()
        if (preset == MusicTrackPreset.NONE || preset == MusicTrackPreset.CUSTOM_AUDIO) {
            onStopped()
            return
        }

        previewJob = scope.launch(Dispatchers.IO) {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            try {
                track.play()
                val loopSamples = synthesizePcmChunk(preset, durationSec = 8.0f, volume = volume)
                var offset = 0
                val step = 2048
                while (isActive) {
                    val count = minOf(step, loopSamples.size - offset)
                    val written = track.write(loopSamples, offset, count)
                    if (written < 0) break
                    offset += count
                    if (offset >= loopSamples.size) offset = 0
                }
            } catch (_: Exception) {
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
                if (audioTrack === track) audioTrack = null
                onStopped()
            }
        }
    }

    fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        }
        audioTrack = null
    }

    fun synthesizePcmChunk(
        preset: MusicTrackPreset,
        durationSec: Float,
        volume: Float
    ): ShortArray {
        val totalSamples = (SAMPLE_RATE * durationSec).toInt().coerceAtLeast(SAMPLE_RATE)
        val out = ShortArray(totalSamples)
        if (preset == MusicTrackPreset.NONE || volume <= 0.01f) return out

        val bpm = preset.bpm.coerceAtLeast(80)
        val beatSec = 60.0 / bpm
        val gain = volume.coerceIn(0f, 1f) * 0.42

        val chords: Array<DoubleArray> = when (preset) {
            MusicTrackPreset.PHONK_DRIVE -> arrayOf(
                doubleArrayOf(110.0, 130.81, 164.81),
                doubleArrayOf(98.0, 123.47, 146.83),
                doubleArrayOf(87.31, 110.0, 130.81),
                doubleArrayOf(82.41, 103.83, 123.47)
            )
            MusicTrackPreset.LOFI_CHILL -> arrayOf(
                doubleArrayOf(220.0, 261.63, 329.63, 392.0),
                doubleArrayOf(174.61, 220.0, 261.63, 329.63),
                doubleArrayOf(196.0, 246.94, 293.66, 349.23),
                doubleArrayOf(164.81, 196.0, 246.94, 293.66)
            )
            MusicTrackPreset.CINEMATIC_PULSE -> arrayOf(
                doubleArrayOf(73.42, 146.83, 220.0),
                doubleArrayOf(65.41, 130.81, 196.0),
                doubleArrayOf(58.27, 116.54, 174.61),
                doubleArrayOf(65.41, 130.81, 220.0)
            )
            MusicTrackPreset.INDIE_UPBEAT -> arrayOf(
                doubleArrayOf(261.63, 329.63, 392.0),
                doubleArrayOf(196.0, 246.94, 293.66),
                doubleArrayOf(220.0, 261.63, 329.63),
                doubleArrayOf(174.61, 220.0, 261.63)
            )
            else -> arrayOf(
                doubleArrayOf(146.83, 220.0, 293.66),
                doubleArrayOf(116.54, 174.61, 233.08),
                doubleArrayOf(130.81, 196.0, 261.63),
                doubleArrayOf(110.0, 164.81, 220.0)
            )
        }

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val beatPos = t / beatSec
            val beatIdx = beatPos.toInt()
            val beatFrac = beatPos - beatIdx
            val chord = chords[(beatIdx / 4) % chords.size]

            // Harmonic pad / lead
            var synth = 0.0
            for (freq in chord) {
                synth += sin(2.0 * PI * freq * t) * 0.24
                if (preset == MusicTrackPreset.CYBER_SYNTH || preset == MusicTrackPreset.PHONK_DRIVE) {
                    synth += sin(4.0 * PI * freq * t) * 0.08
                }
            }

            // Punchy kick drum on beats 0 & 2
            val kickEnv = if (beatIdx % 2 == 0) exp(-beatFrac * 18.0) else 0.0
            val kickFreq = 48.0 + 95.0 * exp(-beatFrac * 24.0)
            val kick = sin(2.0 * PI * kickFreq * beatFrac * beatSec) * kickEnv * 0.55

            // Crisp rhythmic pulse / hi-hat on 8th notes
            val eighthFrac = (beatPos * 2.0) - (beatPos * 2.0).toInt()
            val hatEnv = exp(-eighthFrac * 35.0)
            val hat = sin(2.0 * PI * 4200.0 * t) * sin(2.0 * PI * 1730.0 * t) * hatEnv * 0.12

            // Subtle bass pulse
            val bassEnv = exp(-beatFrac * 4.5)
            val bass = sin(2.0 * PI * (chord[0] * 0.5) * t) * bassEnv * 0.34

            val sample = ((synth + kick + hat + bass) * gain).coerceIn(-0.98, 0.98)
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }
}
