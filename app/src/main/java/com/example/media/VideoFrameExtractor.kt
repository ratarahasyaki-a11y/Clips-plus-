package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.model.ClipProject
import com.example.model.ClipSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "VideoFrameExtractor"
private const val MAX_FRAME_EDGE_PX = 512
private const val MAX_TOTAL_FRAMES = 3

data class ExtractedVideoFrames(
    val bitmaps: List<Bitmap>,
    val sourceSummary: String
) {
    fun recycleAll() {
        bitmaps.forEach { bmp ->
            if (!bmp.isRecycled) bmp.recycle()
        }
    }
}

object VideoFrameExtractor {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Extracts up to 3 compact keyframe Bitmaps (<= 512px max edge) from the project's
     * timeline clips (local videos, local images, or YouTube cover thumbnail) so Gemini
     * can visually analyze the actual video content while staying far below the 20MB limit.
     */
    suspend fun extractFrames(
        context: Context,
        project: ClipProject
    ): ExtractedVideoFrames = withContext(Dispatchers.IO) {
        val frames = mutableListOf<Bitmap>()
        var localVideoCount = 0
        var localImageCount = 0
        var remoteThumbCount = 0

        val clips = project.clips
        if (clips.size == 1 && clips.first().sourceType == ClipSourceType.LOCAL_VIDEO) {
            val singleVideo = clips.first()
            val grabbed = extractMultipleVideoKeyframes(context, singleVideo.sourceUri, count = MAX_TOTAL_FRAMES)
            frames.addAll(grabbed)
            if (grabbed.isNotEmpty()) localVideoCount++
        } else {
            for (clip in clips) {
                if (frames.size >= MAX_TOTAL_FRAMES) break
                when (clip.sourceType) {
                    ClipSourceType.LOCAL_VIDEO -> {
                        val bmp = extractMultipleVideoKeyframes(context, clip.sourceUri, count = 1).firstOrNull()
                        if (bmp != null) {
                            frames.add(bmp)
                            localVideoCount++
                        }
                    }
                    ClipSourceType.LOCAL_IMAGE -> {
                        val bmp = decodeLocalImageSampled(context, clip.sourceUri)
                        if (bmp != null) {
                            frames.add(bmp)
                            localImageCount++
                        }
                    }
                    ClipSourceType.YOUTUBE_SEGMENT -> {
                        val url = clip.thumbnailUrl.ifBlank { project.coverThumbnailUrl }
                        if (url.startsWith("http") && remoteThumbCount == 0) {
                            val bmp = downloadRemoteImageSampled(url)
                            if (bmp != null) {
                                frames.add(bmp)
                                remoteThumbCount++
                            }
                        }
                    }
                }
            }
        }

        // Fallback to project cover thumbnail if no clip frame was captured yet
        if (frames.isEmpty() && project.coverThumbnailUrl.startsWith("http")) {
            val bmp = downloadRemoteImageSampled(project.coverThumbnailUrl)
            if (bmp != null) {
                frames.add(bmp)
                remoteThumbCount++
            }
        }

        val summary = buildString {
            when {
                frames.isEmpty() -> append("metadata & script")
                localVideoCount > 0 && localImageCount > 0 -> append("${frames.size} video & photo frames")
                localVideoCount > 0 -> append("${frames.size} video keyframe${if (frames.size == 1) "" else "s"}")
                localImageCount > 0 -> append("${frames.size} scene photo${if (frames.size == 1) "" else "s"}")
                else -> append("video cover visual")
            }
        }

        ExtractedVideoFrames(bitmaps = frames, sourceSummary = summary)
    }

    private fun extractMultipleVideoKeyframes(
        context: Context,
        uriString: String,
        count: Int
    ): List<Bitmap> {
        if (uriString.isBlank()) return emptyList()
        val retriever = MediaMetadataRetriever()
        val result = mutableListOf<Bitmap>()
        return try {
            retriever.setDataSource(context, Uri.parse(uriString))
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(1_000L)
                ?: 10_000L

            val fractions = when (count) {
                1 -> listOf(0.35)
                2 -> listOf(0.20, 0.72)
                else -> listOf(0.12, 0.50, 0.85)
            }
            for (fraction in fractions) {
                val timeUs = (durationMs * fraction * 1_000.0).toLong().coerceAtLeast(0L)
                val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (raw != null) {
                    val scaled = downscaleBitmap(raw, MAX_FRAME_EDGE_PX)
                    if (scaled !== raw && !raw.isRecycled) raw.recycle()
                    result.add(scaled)
                }
            }
            result
        } catch (e: Exception) {
            Log.w(TAG, "Unable to extract keyframes from local video URI: ${e.message}")
            result
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decodeLocalImageSampled(context: Context, uriString: String): Bitmap? {
        if (uriString.isBlank()) return null
        val uri = Uri.parse(uriString)
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
            val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_FRAME_EDGE_PX)
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            } ?: return null
            val scaled = downscaleBitmap(raw, MAX_FRAME_EDGE_PX)
            if (scaled !== raw && !raw.isRecycled) raw.recycle()
            scaled
        } catch (e: Exception) {
            Log.w(TAG, "Unable to decode local image URI: ${e.message}")
            null
        }
    }

    private fun downloadRemoteImageSampled(url: String): Bitmap? {
        return try {
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bytes = resp.body?.bytes() ?: return null
                if (bytes.isEmpty()) return null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_FRAME_EDGE_PX)
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null
                val scaled = downscaleBitmap(raw, MAX_FRAME_EDGE_PX)
                if (scaled !== raw && !raw.isRecycled) raw.recycle()
                scaled
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to fetch thumbnail for AI visual scan: ${e.message}")
            null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        val largest = max(width, height)
        while ((largest / sample) > maxEdge * 2) {
            sample *= 2
        }
        return sample
    }

    private fun downscaleBitmap(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxEdge) return bitmap
        val ratio = maxEdge.toFloat() / largest.toFloat()
        val targetW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
