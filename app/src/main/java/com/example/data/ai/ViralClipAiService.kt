package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.model.SocialCaptionBundle
import com.example.model.SubtitleCue
import com.example.model.ViralClipCandidate
import com.example.model.YouTubeVideoInfo
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "ViralClipAiService"
private const val MODEL_NAME = "gemini-3.8-flash"

class ViralClipAiService {

    private val viralClipsSchema = Schema.obj(
        properties = mapOf(
            "clips" to Schema.array(
                description = "List of 3 ranked viral video clip candidates",
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string("Punchy viral clip title"),
                        "startTimeLabel" to Schema.string("Start timestamp in MM:SS format"),
                        "endTimeLabel" to Schema.string("End timestamp in MM:SS format"),
                        "durationSec" to Schema.integer("Exact duration in seconds"),
                        "viralScore" to Schema.integer("Viral potential score between 75 and 99"),
                        "hookReason" to Schema.string("Why this segment hooks viewers in the first 3 seconds"),
                        "ttsScript" to Schema.string("Voiceover narration script matching this clip"),
                        "subtitles" to Schema.array(
                            description = "Timed subtitle lines covering the clip duration",
                            items = Schema.obj(
                                properties = mapOf(
                                    "startSec" to Schema.double("Start time in seconds from 0"),
                                    "endSec" to Schema.double("End time in seconds"),
                                    "text" to Schema.string("Short punchy subtitle phrase (3-7 words)")
                                )
                            )
                        ),
                        "socialCaptions" to Schema.obj(
                            properties = mapOf(
                                "viralHook" to Schema.string("Scroll-stopping 1-line opening hook"),
                                "instagramCaption" to Schema.string("Instagram Reels ready caption"),
                                "youtubeShortsCaption" to Schema.string("YouTube Shorts title and description"),
                                "tiktokCaption" to Schema.string("TikTok short punchy caption"),
                                "twitterCaption" to Schema.string("X / LinkedIn engaging post"),
                                "hashtags" to Schema.array(Schema.string("Hashtag starting with #"))
                            )
                        )
                    )
                )
            )
        )
    )

    private val socialCaptionsSchema = Schema.obj(
        properties = mapOf(
            "viralHook" to Schema.string("Primary scroll-stopping 1-line opening hook (under 90 chars)"),
            "alternateHooks" to Schema.array(
                items = Schema.string("Alternative punchy opening hook"),
                description = "3 distinct scroll-stopping 3-second hook variations"
            ),
            "visualInsight" to Schema.string("One-sentence insight describing the key visual/story elements detected in the video and why this caption angle hooks viewers"),
            "instagramCaption" to Schema.string("Instagram Reels caption with engaging micro-story, line breaks, and save/share CTA"),
            "youtubeShortsCaption" to Schema.string("YouTube Shorts SEO-friendly title and punchy description ending with #Shorts"),
            "tiktokCaption" to Schema.string("TikTok ultra-punchy curiosity caption with comment trigger"),
            "twitterCaption" to Schema.string("X / LinkedIn high-signal short post under 260 characters"),
            "hashtags" to Schema.array(
                items = Schema.string("Trending or niche hashtag starting with #"),
                description = "12 curated hashtags mixing high-reach viral tags and content-specific niche tags"
            )
        )
    )

    private val autoCaptionsSchema = Schema.obj(
        properties = mapOf(
            "ttsScript" to Schema.string("Engaging voiceover narration script"),
            "subtitles" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "startSec" to Schema.double("Start offset in seconds"),
                        "endSec" to Schema.double("End offset in seconds"),
                        "text" to Schema.string("Short viral subtitle caption (3-7 words)")
                    )
                )
            )
        )
    )

    private val viralClipsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = viralClipsSchema
            }
        )
    }

    private val socialCaptionsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = socialCaptionsSchema
            }
        )
    }

    private val autoCaptionsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = autoCaptionsSchema
            }
        )
    }

    suspend fun extractViralClips(
        videoInfo: YouTubeVideoInfo,
        targetDurationSec: Int,
        aspectRatioLabel: String,
        captionLanguage: String = "English / Hinglish"
    ): List<ViralClipCandidate> {
        val cueCount = (targetDurationSec / 5).coerceIn(6, 18)
        val prompt = """
            You are an elite short-form video editor and viral content strategist.
            Analyze this YouTube video and design 3 high-retention viral clip cuts ready for $aspectRatioLabel export:
            - YouTube URL: ${videoInfo.url}
            - Video Title: ${videoInfo.title}
            - Channel / Creator: ${videoInfo.authorName}
            - Target Clip Length: $targetDurationSec seconds (must support 30s, 60s, or 1+ minute cuts accurately).
            - Caption Tone / Language Preference: $captionLanguage

            Requirements:
            1. Create 3 distinct viral clip moments ranked by viralScore (84-99), each approximately $targetDurationSec seconds long.
            2. For each clip, generate $cueCount sequential timed subtitle cues starting at 0.0s up to ${targetDurationSec}.0s (each 3-7 punchy words).
            3. Include a crisp TTS voiceover narration script (ttsScript) and complete social media captions (Instagram Reels, YouTube Shorts, TikTok, X/LinkedIn) with 6-8 relevant hashtags.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                viralClipsModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
            return parseViralClips(rawJson, targetDurationSec)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Viral clip analysis timed out", e)
            throw IllegalStateException("AI analysis timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI viral clip extraction failed", e)
            throw e
        }
    }

    suspend fun regenerateSocialCaptions(
        projectTitle: String,
        subtitleContext: String,
        tone: String,
        channelName: String = "",
        clipTitles: List<String> = emptyList(),
        overlayTexts: List<String> = emptyList(),
        durationSec: Int = 30,
        aspectRatioLabel: String = "9:16",
        extraContext: String = "",
        videoFrames: List<Bitmap> = emptyList()
    ): SocialCaptionBundle {
        val prompt = buildString {
            appendLine("You are an elite viral social media strategist and short-form copywriter.")
            if (videoFrames.isNotEmpty()) {
                appendLine("Closely inspect the attached ${videoFrames.size} keyframe image(s) extracted from the user's video along with the timeline context below.")
            } else {
                appendLine("Analyze the video timeline metadata and script below to craft viral social media copy.")
            }
            appendLine()
            appendLine("Video Content Context:")
            appendLine("- Project Title: ${projectTitle.ifBlank { "Untitled Video Cut" }}")
            if (channelName.isNotBlank()) appendLine("- Creator / Source Channel: $channelName")
            appendLine("- Format & Length: $aspectRatioLabel · ${durationSec}s")
            if (clipTitles.isNotEmpty()) appendLine("- Timeline Clips: ${clipTitles.joinToString(" → ")}")
            if (overlayTexts.isNotEmpty()) appendLine("- On-Screen Text Overlays: ${overlayTexts.joinToString(" | ")}")
            appendLine("- Spoken / Subtitle Transcript: ${subtitleContext.ifBlank { projectTitle }}")
            appendLine("- Requested Tone / Vibe: $tone")
            if (extraContext.isNotBlank()) appendLine("- Creator Focus / CTA Hint: $extraContext")
            appendLine()
            appendLine("Output Requirements:")
            appendLine("1. Write a primary scroll-stopping 'viralHook' plus 3 distinct 'alternateHooks' (including the primary hook or fresh angles).")
            appendLine("2. Write a 1-sentence 'visualInsight' summarizing what stands out in the video content and why this angle drives retention.")
            appendLine("3. Craft platform-native captions (do NOT stuff raw hashtag blocks inside the caption body; keep hashtags in the dedicated array, except #Shorts for YouTube if helpful):")
            appendLine("   - instagramCaption: High-retention Reels caption with clean spacing and a save/comment CTA.")
            appendLine("   - youtubeShortsCaption: Search-friendly Shorts headline + crisp description.")
            appendLine("   - tiktokCaption: Ultra-short curiosity loop or relatable comment bait.")
            appendLine("   - twitterCaption: Punchy high-signal post under 260 characters.")
            appendLine("4. Generate 12 high-converting 'hashtags' starting with # (mix of 4 broad viral discovery tags, 5 specific topic/visual tags matching what's in the video, and 3 community/niche tags). No spaces inside hashtags.")
        }.trim()

        val inputContent = content {
            videoFrames.take(3).forEach { frame ->
                if (!frame.isRecycled) {
                    image(frame)
                }
            }
            text(prompt)
        }

        try {
            val response = withTimeout(60_000L) {
                socialCaptionsModel.generateContent(inputContent)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI returned empty captions.")
            return parseSocialBundle(JSONObject(rawJson), generatedTone = tone)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Caption regeneration timed out", e)
            throw IllegalStateException("Caption generation timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI caption generation failed", e)
            throw e
        }
    }

    suspend fun generateAutoCaptions(
        projectTitle: String,
        clipTitles: List<String>,
        totalDurationSec: Int,
        languageStyle: String
    ): Pair<List<SubtitleCue>, String> {
        val cueCount = (totalDurationSec / 5).coerceIn(6, 20)
        val prompt = """
            Generate synchronized automatic captions and a matching voiceover narration script for a $totalDurationSec-second multi-clip video.
            - Project Title: $projectTitle
            - Clips in Timeline: ${clipTitles.joinToString(", ")}
            - Language / Style: $languageStyle
            Produce $cueCount sequential subtitle cues evenly paced from 0.0s to ${totalDurationSec}.0s (3-7 words per cue) and a cohesive ttsScript.
        """.trimIndent()

        try {
            val response = withTimeout(60_000L) {
                autoCaptionsModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI returned empty auto-captions.")
            val obj = JSONObject(rawJson)
            val script = obj.optString("ttsScript")
            val cues = parseSubtitleArray(obj, totalDurationSec)
            return cues to script
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Auto-captions timed out", e)
            throw IllegalStateException("Auto-caption generation timed out after 60s.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI auto-captions failed", e)
            throw e
        }
    }

    /**
     * Offline utility: splits any user-typed script or clip titles into evenly timed
     * subtitle cues across the full timeline duration (works 100% without internet).
     */
    fun buildOfflineAutoCaptions(
        scriptOrTitle: String,
        totalDurationMs: Long
    ): List<SubtitleCue> {
        val cleaned = scriptOrTitle.trim().ifBlank {
            "Watch this highlight until the end. Every single frame tells the story. Save and share this cut now."
        }
        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        val chunkSize = if (words.size <= 12) 3 else 5
        val phrases = words.chunked(chunkSize).map { it.joinToString(" ") }
        val safeTotal = totalDurationMs.coerceAtLeast(6_000L)
        val sliceMs = (safeTotal / phrases.size.coerceAtLeast(1)).coerceAtLeast(1_500L)

        return phrases.mapIndexed { idx, phrase ->
            val start = (idx * sliceMs).coerceAtMost(safeTotal - 500L)
            val end = ((idx + 1) * sliceMs).coerceAtMost(safeTotal)
            SubtitleCue(
                id = UUID.randomUUID().toString(),
                startMs = start,
                endMs = end.coerceAtLeast(start + 800L),
                text = phrase
            )
        }
    }

    private fun parseViralClips(rawJson: String, fallbackDurationSec: Int): List<ViralClipCandidate> {
        val root = JSONObject(rawJson)
        val clipsArr = root.optJSONArray("clips") ?: return emptyList()
        val list = mutableListOf<ViralClipCandidate>()

        for (i in 0 until clipsArr.length()) {
            val c = clipsArr.getJSONObject(i)
            val durSec = c.optInt("durationSec", fallbackDurationSec).coerceIn(15, 180)
            val cues = parseSubtitleArray(c, durSec)
            val socialObj = c.optJSONObject("socialCaptions") ?: JSONObject()
            list.add(
                ViralClipCandidate(
                    id = UUID.randomUUID().toString(),
                    title = c.optString("title", "Viral Highlight #${i + 1}"),
                    startTimeLabel = c.optString("startTimeLabel", "00:15"),
                    endTimeLabel = c.optString("endTimeLabel", formatSec(15 + durSec)),
                    durationSec = durSec,
                    viralScore = c.optInt("viralScore", 92).coerceIn(70, 99),
                    hookReason = c.optString("hookReason", "High-retention opening hook"),
                    subtitles = cues,
                    ttsScript = c.optString("ttsScript").ifBlank {
                        cues.joinToString(" ") { it.text }
                    },
                    socialCaptions = parseSocialBundle(socialObj)
                )
            )
        }
        return list
    }

    private fun parseSubtitleArray(container: JSONObject, durationSec: Int): List<SubtitleCue> {
        val arr = container.optJSONArray("subtitles") ?: return emptyList()
        val cues = mutableListOf<SubtitleCue>()
        val maxMs = durationSec * 1000L
        for (j in 0 until arr.length()) {
            val s = arr.getJSONObject(j)
            val startMs = (s.optDouble("startSec", j * 4.0) * 1000.0).toLong().coerceIn(0L, maxMs)
            val endMs = (s.optDouble("endSec", (j + 1) * 4.0) * 1000.0).toLong()
                .coerceIn(startMs + 800L, maxMs.coerceAtLeast(startMs + 800L))
            val text = s.optString("text").trim()
            if (text.isNotEmpty()) {
                cues.add(
                    SubtitleCue(
                        id = UUID.randomUUID().toString(),
                        startMs = startMs,
                        endMs = endMs,
                        text = text
                    )
                )
            }
        }
        return cues
    }

    private fun parseSocialBundle(
        obj: JSONObject,
        generatedTone: String = ""
    ): SocialCaptionBundle {
        val tagsArr = obj.optJSONArray("hashtags")
        val tags = LinkedHashSet<String>()
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) {
                val raw = tagsArr.optString(i).trim().replace(" ", "")
                if (raw.isNotEmpty() && raw != "#") {
                    tags.add(if (raw.startsWith("#")) raw else "#$raw")
                }
            }
        }
        val hooksArr = obj.optJSONArray("alternateHooks")
        val hooks = LinkedHashSet<String>()
        val primaryHook = obj.optString("viralHook").trim()
        if (primaryHook.isNotEmpty()) hooks.add(primaryHook)
        if (hooksArr != null) {
            for (i in 0 until hooksArr.length()) {
                val h = hooksArr.optString(i).trim()
                if (h.isNotEmpty()) hooks.add(h)
            }
        }
        return SocialCaptionBundle(
            viralHook = primaryHook.ifBlank { hooks.firstOrNull().orEmpty() },
            alternateHooks = hooks.take(4),
            visualInsight = obj.optString("visualInsight").trim(),
            generatedTone = generatedTone.ifBlank { obj.optString("generatedTone").trim() },
            instagramCaption = obj.optString("instagramCaption").trim(),
            youtubeShortsCaption = obj.optString("youtubeShortsCaption").trim(),
            tiktokCaption = obj.optString("tiktokCaption").trim(),
            twitterCaption = obj.optString("twitterCaption").trim(),
            hashtags = tags.take(20)
        )
    }

    private fun formatSec(totalSec: Int): String {
        val m = totalSec / 60
        val s = totalSec % 60
        return "%02d:%02d".format(m, s)
    }
}
