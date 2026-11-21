package com.example.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.ClipProject
import com.example.model.ClipSourceType
import com.example.model.ExportQuality
import com.example.model.MusicTrackPreset
import com.example.model.TimelineClipItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

private const val TAG = "Mp4VideoExporter"

enum class ExportStage(val title: String, val detail: String) {
    PREPARING("Preparing Media & Voiceover", "Loading clips, filters & TTS narration"),
    RENDERING("Rendering Video Frames", "Burning effects, transitions & subtitles"),
    MUXING("Mixing Audio & Finalizing", "Combining H.264 video with AAC audio"),
    SAVING("Saving to Gallery", "Publishing MP4 to Movies/ClipPulse")
}

data class ExportProgressUpdate(
    val progress: Float,
    val stage: ExportStage,
    val statusMessage: String,
    val currentFrame: Int = 0,
    val totalFrames: Int = 0
)

data class ExportResult(
    val outputUri: Uri,
    val fileName: String,
    val durationSec: Int,
    val resolutionLabel: String,
    val savedToGallery: Boolean
)

class Mp4VideoExporter(
    private val context: Context,
    private val musicEngine: ProceduralMusicEngine,
    private val ttsManager: TtsVoiceoverManager
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun exportProjectToMp4(
        project: ClipProject,
        onProgress: (ExportProgressUpdate) -> Unit
    ): ExportResult = withContext(Dispatchers.IO) {
        val ratio = project.aspectRatio
        val width = if (project.exportQuality == ExportQuality.FULL_HD_1080P) {
            ratio.exportWidth1080
        } else {
            ratio.exportWidth720
        }.let { it - (it % 16) }.coerceAtLeast(320)

        val height = if (project.exportQuality == ExportQuality.FULL_HD_1080P) {
            ratio.exportHeight1080
        } else {
            ratio.exportHeight720
        }.let { it - (it % 16) }.coerceAtLeast(320)

        val fps = 15
        val totalDurationMs = project.totalDurationMs.coerceIn(5_000L, 180_000L)
        val totalFrames = ((totalDurationMs / 1000.0) * fps).toInt().coerceAtLeast(30)

        onProgress(
            ExportProgressUpdate(
                progress = 0.04f,
                stage = ExportStage.PREPARING,
                statusMessage = "Preparing clips & voiceover…",
                currentFrame = 0,
                totalFrames = totalFrames
            )
        )

        // 1. Synthesize TTS WAV offline if enabled
        val ttsWavFile: File? = if (project.ttsSettings.enabled && project.ttsSettings.script.isNotBlank()) {
            onProgress(
                ExportProgressUpdate(
                    progress = 0.07f,
                    stage = ExportStage.PREPARING,
                    statusMessage = "Synthesizing offline TTS voiceover…",
                    currentFrame = 0,
                    totalFrames = totalFrames
                )
            )
            ttsManager.synthesizeToWavFile(
                text = project.ttsSettings.script,
                languageTag = project.ttsSettings.languageTag,
                speechRate = project.ttsSettings.speechRate,
                pitch = project.ttsSettings.pitch
            )
        } else {
            null
        }

        // 2. Prepare clip frames / bitmaps (works both online & offline)
        val clips = project.clips.ifEmpty {
            listOf(
                TimelineClipItem(
                    title = project.title,
                    sourceType = ClipSourceType.YOUTUBE_SEGMENT,
                    thumbnailUrl = project.coverThumbnailUrl,
                    durationMs = totalDurationMs,
                    trimStartMs = 0L,
                    trimEndMs = totalDurationMs
                )
            )
        }

        val clipResources = prepareClipBitmaps(clips, project.coverThumbnailUrl)
        onProgress(
            ExportProgressUpdate(
                progress = 0.12f,
                stage = ExportStage.RENDERING,
                statusMessage = "Encoding ${width}x${height} H.264 video…",
                currentFrame = 0,
                totalFrames = totalFrames
            )
        )

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = project.title
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(28)
            .ifBlank { "ClipPulse" }
        val fileName = "${safeName}_${ratio.label.replace(":", "x")}_${System.currentTimeMillis()}.mp4"
        val tempFile = File(exportDir, fileName)

        try {
            encodeVideoAndAudioToMp4(
                outputFile = tempFile,
                width = width,
                height = height,
                fps = fps,
                bitrate = project.exportQuality.bitrateBps,
                totalDurationMs = totalDurationMs,
                totalFrames = totalFrames,
                project = project,
                clips = clips,
                clipResources = clipResources,
                ttsWavFile = ttsWavFile,
                onProgress = onProgress
            )

            onProgress(
                ExportProgressUpdate(
                    progress = 0.95f,
                    stage = ExportStage.SAVING,
                    statusMessage = "Saving to Gallery (Movies/ClipPulse)…",
                    currentFrame = totalFrames,
                    totalFrames = totalFrames
                )
            )
            val (finalUri, inGallery) = publishToMediaStoreOrFileProvider(
                sourceFile = tempFile,
                fileName = fileName,
                displayTitle = project.title,
                durationMs = totalDurationMs
            )
            onProgress(
                ExportProgressUpdate(
                    progress = 1.0f,
                    stage = ExportStage.SAVING,
                    statusMessage = "Export complete!",
                    currentFrame = totalFrames,
                    totalFrames = totalFrames
                )
            )

            ExportResult(
                outputUri = finalUri,
                fileName = fileName,
                durationSec = (totalDurationMs / 1000L).toInt(),
                resolutionLabel = "${width}x${height} (${ratio.label})",
                savedToGallery = inGallery
            )
        } finally {
            clipResources.forEach { res ->
                res.retriever?.let { runCatching { it.release() } }
                res.staticBitmap?.let { if (!it.isRecycled) it.recycle() }
            }
            ttsWavFile?.let { runCatching { it.delete() } }
        }
    }

    private data class ClipFrameSource(
        val clip: TimelineClipItem,
        val staticBitmap: Bitmap?,
        val retriever: MediaMetadataRetriever?
    )

    private fun prepareClipBitmaps(
        clips: List<TimelineClipItem>,
        fallbackThumbUrl: String
    ): List<ClipFrameSource> {
        val cachedFallback = downloadBitmapOrNull(fallbackThumbUrl)
        return clips.map { clip ->
            when (clip.sourceType) {
                ClipSourceType.LOCAL_VIDEO -> {
                    val retriever = runCatching {
                        MediaMetadataRetriever().apply {
                            setDataSource(context, Uri.parse(clip.sourceUri))
                        }
                    }.getOrNull()
                    val firstFrame = runCatching {
                        retriever?.getFrameAtTime(clip.trimStartMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }.getOrNull()
                    ClipFrameSource(clip, firstFrame ?: cachedFallback?.copy(Bitmap.Config.ARGB_8888, false), retriever)
                }
                ClipSourceType.LOCAL_IMAGE -> {
                    val bmp = runCatching {
                        context.contentResolver.openInputStream(Uri.parse(clip.sourceUri))?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }.getOrNull()
                    ClipFrameSource(clip, bmp ?: cachedFallback?.copy(Bitmap.Config.ARGB_8888, false), null)
                }
                ClipSourceType.YOUTUBE_SEGMENT -> {
                    val bmp = downloadBitmapOrNull(clip.thumbnailUrl.ifBlank { fallbackThumbUrl })
                        ?: cachedFallback?.copy(Bitmap.Config.ARGB_8888, false)
                    ClipFrameSource(clip, bmp, null)
                }
            }
        }
    }

    private fun downloadBitmapOrNull(url: String): Bitmap? {
        if (url.isBlank()) return null
        return runCatching {
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    resp.body?.byteStream()?.use { BitmapFactory.decodeStream(it) }
                } else null
            }
        }.getOrNull()
    }

    private suspend fun encodeVideoAndAudioToMp4(
        outputFile: File,
        width: Int,
        height: Int,
        fps: Int,
        bitrate: Int,
        totalDurationMs: Long,
        totalFrames: Int,
        project: ClipProject,
        clips: List<TimelineClipItem>,
        clipResources: List<ClipFrameSource>,
        ttsWavFile: File?,
        onProgress: (ExportProgressUpdate) -> Unit
    ) {
        val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }

        val includeAudio = (project.musicPreset != MusicTrackPreset.NONE && project.musicVolume > 0.02f) ||
            (ttsWavFile != null && ttsWavFile.exists())

        val sampleRate = ProceduralMusicEngine.SAMPLE_RATE
        val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 96_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val audioEncoder = if (includeAudio) {
            runCatching { MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC) }.getOrNull()
        } else null

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        var videoTrackIndex = -1
        var audioTrackIndex = -1

        try {
            videoEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = videoEncoder.createInputSurface()
            videoEncoder.start()

            audioEncoder?.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            audioEncoder?.start()

            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBufferInfo = MediaCodec.BufferInfo()
            val transitionMs = (project.transitionDurationSec * 1000f).toLong().coerceIn(200L, 2_000L)

            // Precompute clip start offsets on timeline
            val clipStartOffsets = LongArray(clips.size)
            var acc = 0L
            for (i in clips.indices) {
                clipStartOffsets[i] = acc
                acc += clips[i].effectiveDurationMs
            }

            // Cache last retrieved local video frame to keep encoding fast
            var lastClipIndex = -1
            var lastVideoSampleBucket = -1L
            var cachedVideoFrame: Bitmap? = null

            // Helper to start muxer once required track formats are ready
            fun maybeStartMuxer() {
                if (!muxerStarted && videoTrackIndex >= 0 && (audioEncoder == null || audioTrackIndex >= 0)) {
                    muxer.start()
                    muxerStarted = true
                }
            }

            // Prepare audio PCM loop if audio enabled
            val musicLoop = if (project.musicPreset != MusicTrackPreset.NONE && project.musicVolume > 0.02f) {
                musicEngine.synthesizePcmChunk(
                    preset = project.musicPreset,
                    durationSec = 8.0f,
                    volume = project.musicVolume
                )
            } else null

            val ttsSamples = ttsWavFile?.let { readWavMonoSamples(it, project.ttsSettings.volume) }
            val totalAudioSamples = ((totalDurationMs / 1000.0) * sampleRate).toInt()
            var audioSamplesFed = 0
            var audioEosSent = false

            fun feedAudioStep(forceFlush: Boolean) {
                val enc = audioEncoder ?: return
                if (!audioEosSent) {
                    val chunkSamples = 1024
                    var loops = 0
                    while (loops < 8 && !audioEosSent) {
                        loops++
                        val inIdx = enc.dequeueInputBuffer(0)
                        if (inIdx < 0) break
                        val inBuf = enc.getInputBuffer(inIdx) ?: break
                        inBuf.clear()

                        if (audioSamplesFed >= totalAudioSamples) {
                            enc.queueInputBuffer(inIdx, 0, 0, (audioSamplesFed * 1_000_000L) / sampleRate, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            audioEosSent = true
                        } else {
                            val count = minOf(chunkSamples, totalAudioSamples - audioSamplesFed, inBuf.capacity() / 2)
                            inBuf.order(ByteOrder.LITTLE_ENDIAN)
                            for (s in 0 until count) {
                                val globalIdx = audioSamplesFed + s
                                var mixed = 0
                                if (musicLoop != null && musicLoop.isNotEmpty()) {
                                    mixed += musicLoop[globalIdx % musicLoop.size].toInt()
                                }
                                if (ttsSamples != null && globalIdx < ttsSamples.size) {
                                    mixed += ttsSamples[globalIdx].toInt()
                                }
                                inBuf.putShort(mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
                            }
                            val ptsUs = (audioSamplesFed * 1_000_000L) / sampleRate
                            enc.queueInputBuffer(inIdx, 0, count * 2, ptsUs, 0)
                            audioSamplesFed += count
                        }
                    }
                }

                // Drain audio output
                while (true) {
                    val outIdx = enc.dequeueOutputBuffer(audioBufferInfo, if (forceFlush) 5_000L else 0L)
                    if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (audioTrackIndex < 0) {
                            audioTrackIndex = muxer.addTrack(enc.outputFormat)
                            maybeStartMuxer()
                        }
                    } else if (outIdx >= 0) {
                        val outBuf = enc.getOutputBuffer(outIdx)
                        if (outBuf != null && audioBufferInfo.size > 0 &&
                            (audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                            muxerStarted
                        ) {
                            outBuf.position(audioBufferInfo.offset)
                            outBuf.limit(audioBufferInfo.offset + audioBufferInfo.size)
                            muxer.writeSampleData(audioTrackIndex, outBuf, audioBufferInfo)
                        }
                        enc.releaseOutputBuffer(outIdx, false)
                        if ((audioBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }

            // Encode video frames
            for (frameIdx in 0 until totalFrames) {
                coroutineContext.ensureActive()
                val timeMs = (frameIdx * 1000L) / fps

                // Determine active clip index and transition progress
                var activeIdx = clips.lastIndex
                for (i in clips.indices) {
                    val clipEnd = clipStartOffsets[i] + clips[i].effectiveDurationMs
                    if (timeMs < clipEnd) {
                        activeIdx = i
                        break
                    }
                }

                val activeRes = clipResources[activeIdx]
                val localClipTimeMs = (timeMs - clipStartOffsets[activeIdx]).coerceAtLeast(0L)
                val clipRemainingMs = (activeRes.clip.effectiveDurationMs - localClipTimeMs).coerceAtLeast(0L)

                val nextRes = if (activeIdx + 1 < clipResources.size) clipResources[activeIdx + 1] else null
                val transProgress = if (nextRes != null && clipRemainingMs < transitionMs) {
                    1f - (clipRemainingMs.toFloat() / transitionMs.toFloat())
                } else {
                    0f
                }

                val activeBitmap: Bitmap? = if (activeRes.retriever != null) {
                    val sampleBucket = localClipTimeMs / 400L // sample every 0.4s for snappy encoding
                    if (activeIdx != lastClipIndex || sampleBucket != lastVideoSampleBucket || cachedVideoFrame == null) {
                        lastClipIndex = activeIdx
                        lastVideoSampleBucket = sampleBucket
                        val targetUs = (activeRes.clip.trimStartMs + localClipTimeMs) * 1000L
                        val extracted = runCatching {
                            activeRes.retriever.getFrameAtTime(targetUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        }.getOrNull()
                        if (extracted != null) {
                            cachedVideoFrame?.let { if (it !== activeRes.staticBitmap && !it.isRecycled) it.recycle() }
                            cachedVideoFrame = extracted
                        }
                    }
                    cachedVideoFrame ?: activeRes.staticBitmap
                } else {
                    activeRes.staticBitmap
                }

                val canvas = inputSurface.lockCanvas(null)
                try {
                    VideoFilterRenderer.drawCompositeFrame(
                        canvas = canvas,
                        width = width,
                        height = height,
                        project = project,
                        timeMs = timeMs,
                        currentBitmap = activeBitmap,
                        nextBitmap = nextRes?.staticBitmap,
                        transitionProgress = transProgress
                    )
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Drain video encoder
                while (true) {
                    val outIdx = videoEncoder.dequeueOutputBuffer(videoBufferInfo, 1_500L)
                    if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (videoTrackIndex < 0) {
                            videoTrackIndex = muxer.addTrack(videoEncoder.outputFormat)
                            maybeStartMuxer()
                        }
                    } else if (outIdx >= 0) {
                        val outBuf = videoEncoder.getOutputBuffer(outIdx)
                        if (outBuf != null && videoBufferInfo.size > 0 &&
                            (videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                            muxerStarted
                        ) {
                            outBuf.position(videoBufferInfo.offset)
                            outBuf.limit(videoBufferInfo.offset + videoBufferInfo.size)
                            // Stamp exact monotonic PTS for smooth playback
                            videoBufferInfo.presentationTimeUs = (frameIdx * 1_000_000L) / fps
                            muxer.writeSampleData(videoTrackIndex, outBuf, videoBufferInfo)
                        }
                        videoEncoder.releaseOutputBuffer(outIdx, false)
                    }
                }

                // Interleave audio encoding
                feedAudioStep(forceFlush = false)

                if (frameIdx % 4 == 0 || frameIdx == totalFrames - 1) {
                    val pct = 0.14f + 0.72f * ((frameIdx + 1).toFloat() / totalFrames.toFloat())
                    onProgress(
                        ExportProgressUpdate(
                            progress = pct,
                            stage = ExportStage.RENDERING,
                            statusMessage = "Rendering frame ${frameIdx + 1} of $totalFrames…",
                            currentFrame = frameIdx + 1,
                            totalFrames = totalFrames
                        )
                    )
                }
            }

            onProgress(
                ExportProgressUpdate(
                    progress = 0.88f,
                    stage = ExportStage.MUXING,
                    statusMessage = "Flushing video & mixing AAC audio track…",
                    currentFrame = totalFrames,
                    totalFrames = totalFrames
                )
            )

            // Signal EOS on video & flush remaining packets
            videoEncoder.signalEndOfInputStream()
            var drainTries = 0
            while (drainTries < 60) {
                drainTries++
                val outIdx = videoEncoder.dequeueOutputBuffer(videoBufferInfo, 10_000L)
                if (outIdx == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    continue
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (videoTrackIndex < 0) {
                        videoTrackIndex = muxer.addTrack(videoEncoder.outputFormat)
                        maybeStartMuxer()
                    }
                } else if (outIdx >= 0) {
                    val outBuf = videoEncoder.getOutputBuffer(outIdx)
                    if (outBuf != null && videoBufferInfo.size > 0 &&
                        (videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                        muxerStarted
                    ) {
                        outBuf.position(videoBufferInfo.offset)
                        outBuf.limit(videoBufferInfo.offset + videoBufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, outBuf, videoBufferInfo)
                    }
                    val isEos = (videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    videoEncoder.releaseOutputBuffer(outIdx, false)
                    if (isEos) break
                }
            }

            // Finish remaining audio samples
            var audioFlushRounds = 0
            while (audioEncoder != null && !audioEosSent && audioFlushRounds < 400) {
                audioFlushRounds++
                feedAudioStep(forceFlush = true)
            }
            onProgress(
                ExportProgressUpdate(
                    progress = 0.93f,
                    stage = ExportStage.MUXING,
                    statusMessage = "Finalizing MP4 container…",
                    currentFrame = totalFrames,
                    totalFrames = totalFrames
                )
            )
            if (audioEncoder != null) {
                feedAudioStep(forceFlush = true)
            }

            cachedVideoFrame?.let { if (!it.isRecycled) it.recycle() }
            inputSurface.release()
        } catch (e: Exception) {
            Log.e(TAG, "MP4 export failed", e)
            throw e
        } finally {
            runCatching { videoEncoder.stop(); videoEncoder.release() }
            runCatching { audioEncoder?.stop(); audioEncoder?.release() }
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }
    }

    private fun readWavMonoSamples(wavFile: File, volume: Float): ShortArray? {
        return runCatching {
            val bytes = wavFile.readBytes()
            if (bytes.size <= 44) return null
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val srcChannels = bb.getShort(22).toInt().coerceAtLeast(1)
            val srcRate = bb.getInt(24).coerceAtLeast(8000)
            val dataOffset = 44
            val frameBytes = srcChannels * 2
            val srcFrames = (bytes.size - dataOffset) / frameBytes
            val targetRate = ProceduralMusicEngine.SAMPLE_RATE
            val ratio = srcRate.toDouble() / targetRate.toDouble()
            val outLen = (srcFrames / ratio).toInt().coerceAtLeast(1)
            val out = ShortArray(outLen)
            val gain = volume.coerceIn(0f, 1.2f)

            for (i in 0 until outLen) {
                val srcIdx = (i * ratio).toInt().coerceIn(0, srcFrames - 1)
                val pos = dataOffset + srcIdx * frameBytes
                if (pos + 1 < bytes.size) {
                    val sample = bb.getShort(pos).toInt()
                    out[i] = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            out
        }.getOrNull()
    }

    private fun publishToMediaStoreOrFileProvider(
        sourceFile: File,
        fileName: String,
        displayTitle: String,
        durationMs: Long
    ): Pair<Uri, Boolean> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val nowMs = System.currentTimeMillis()
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.TITLE, displayTitle.ifBlank { fileName })
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ClipPulse")
                put(MediaStore.Video.Media.DURATION, durationMs)
                put(MediaStore.Video.Media.DATE_ADDED, nowMs / 1000L)
                put(MediaStore.Video.Media.DATE_TAKEN, nowMs)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return uri to true
            }
        }

        // Fallback for API 24-28: share via FileProvider without needing storage permissions
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sourceFile
        )
        return uri to false
    }
}
