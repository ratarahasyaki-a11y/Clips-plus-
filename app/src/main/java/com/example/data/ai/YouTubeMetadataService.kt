package com.example.data.ai

import com.example.model.YouTubeVideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class YouTubeMetadataService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    fun extractVideoId(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return null

        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?.*v=)([a-zA-Z0-9_-]{11})"),
            Regex("(?:youtu\\.be/)([a-zA-Z0-9_-]{11})"),
            Regex("(?:youtube\\.com/shorts/)([a-zA-Z0-9_-]{11})"),
            Regex("(?:youtube\\.com/embed/)([a-zA-Z0-9_-]{11})"),
            Regex("(?:youtube\\.com/live/)([a-zA-Z0-9_-]{11})"),
            Regex("^([a-zA-Z0-9_-]{11})$")
        )
        for (pattern in patterns) {
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return null
    }

    suspend fun fetchMetadata(rawUrl: String): Result<YouTubeVideoInfo> = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(rawUrl)
            ?: return@withContext Result.failure(
                IllegalArgumentException("Please enter a valid YouTube video or Shorts URL.")
            )

        val canonicalUrl = "https://www.youtube.com/watch?v=$videoId"
        val bestThumbnail = resolveBestThumbnailUrl(videoId)

        return@withContext try {
            val encoded = URLEncoder.encode(canonicalUrl, "UTF-8")
            val request = Request.Builder()
                .url("https://www.youtube.com/oembed?url=$encoded&format=json")
                .header("User-Agent", "ClipPulse-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.success(
                        YouTubeVideoInfo(
                            videoId = videoId,
                            url = canonicalUrl,
                            title = "YouTube Video ($videoId)",
                            authorName = "YouTube Creator",
                            thumbnailUrl = bestThumbnail
                        )
                    )
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val title = json.optString("title").ifBlank { "YouTube Video ($videoId)" }
                val author = json.optString("author_name").ifBlank { "YouTube Creator" }
                val oembedThumb = json.optString("thumbnail_url")
                val finalThumb = bestThumbnail.ifBlank { oembedThumb.ifBlank { "https://img.youtube.com/vi/$videoId/hqdefault.jpg" } }
                Result.success(
                    YouTubeVideoInfo(
                        videoId = videoId,
                        url = canonicalUrl,
                        title = title,
                        authorName = author,
                        thumbnailUrl = finalThumb
                    )
                )
            }
        } catch (_: Exception) {
            // Works offline or if oEmbed is unreachable while still keeping canonical ID
            Result.success(
                YouTubeVideoInfo(
                    videoId = videoId,
                    url = canonicalUrl,
                    title = "YouTube Clip ($videoId)",
                    authorName = "Creator Video",
                    thumbnailUrl = bestThumbnail
                )
            )
        }
    }

    private fun resolveBestThumbnailUrl(videoId: String): String {
        val candidates = listOf(
            "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
            "https://img.youtube.com/vi/$videoId/sddefault.jpg",
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        )
        for (url in candidates) {
            val ok = runCatching {
                val headReq = Request.Builder().url(url).head().build()
                client.newCall(headReq).execute().use { it.isSuccessful }
            }.getOrDefault(false)
            if (ok) return url
        }
        return candidates.last()
    }
}
