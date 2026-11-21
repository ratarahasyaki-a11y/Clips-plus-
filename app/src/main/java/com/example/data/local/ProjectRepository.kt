package com.example.data.local

import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ClipProject>> = projectDao.getAllProjects().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun saveProject(project: ClipProject) {
        projectDao.upsertProject(project.copy(updatedAtMs = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteProject(projectId: String) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun getProject(projectId: String): ClipProject? {
        return projectDao.getProjectById(projectId)?.toDomain()
    }

    private fun ClipProject.toEntity(): ProjectEntity = ProjectEntity(
        id = id,
        title = title,
        youtubeUrl = youtubeUrl,
        youtubeVideoId = youtubeVideoId,
        channelName = channelName,
        coverThumbnailUrl = coverThumbnailUrl,
        aspectRatioName = aspectRatio.name,
        exportQualityName = exportQuality.name,
        targetLengthSec = targetLengthSec,
        clipsJson = serializeClips(clips),
        autoCaptionsEnabled = autoCaptionsEnabled,
        subtitleStyleName = subtitleStyle.name,
        subtitleFontSizeSp = subtitleFontSizeSp,
        subtitleYFraction = subtitleYFraction,
        subtitlesJson = serializeSubtitles(subtitles),
        textOverlaysJson = serializeOverlays(textOverlays),
        filterPresetName = filterPreset.name,
        filterIntensity = filterIntensity,
        effectPresetName = effectPreset.name,
        effectIntensity = effectIntensity,
        transitionPresetName = transitionPreset.name,
        transitionDurationSec = transitionDurationSec,
        musicPresetName = musicPreset.name,
        customMusicUri = customMusicUri,
        customMusicName = customMusicName,
        musicVolume = musicVolume,
        ttsJson = serializeTts(ttsSettings),
        socialCaptionsJson = serializeSocial(socialCaptions),
        lastExportedUri = lastExportedUri,
        updatedAtMs = updatedAtMs
    )

    private fun ProjectEntity.toDomain(): ClipProject = ClipProject(
        id = id,
        title = title,
        youtubeUrl = youtubeUrl,
        youtubeVideoId = youtubeVideoId,
        channelName = channelName,
        coverThumbnailUrl = coverThumbnailUrl,
        aspectRatio = runCatching { AspectRatioPreset.valueOf(aspectRatioName) }
            .getOrDefault(AspectRatioPreset.RATIO_19_6),
        exportQuality = runCatching { ExportQuality.valueOf(exportQualityName) }
            .getOrDefault(ExportQuality.FULL_HD_1080P),
        targetLengthSec = targetLengthSec,
        clips = deserializeClips(clipsJson),
        autoCaptionsEnabled = autoCaptionsEnabled,
        subtitleStyle = runCatching { SubtitleStylePreset.valueOf(subtitleStyleName) }
            .getOrDefault(SubtitleStylePreset.VIRAL_POP),
        subtitleFontSizeSp = subtitleFontSizeSp,
        subtitleYFraction = subtitleYFraction,
        subtitles = deserializeSubtitles(subtitlesJson),
        textOverlays = deserializeOverlays(textOverlaysJson),
        filterPreset = runCatching { VideoFilterPreset.valueOf(filterPresetName) }
            .getOrDefault(VideoFilterPreset.ORIGINAL),
        filterIntensity = filterIntensity,
        effectPreset = runCatching { VideoEffectPreset.valueOf(effectPresetName) }
            .getOrDefault(VideoEffectPreset.NONE),
        effectIntensity = effectIntensity,
        transitionPreset = runCatching { TransitionPreset.valueOf(transitionPresetName) }
            .getOrDefault(TransitionPreset.CROSSFADE),
        transitionDurationSec = transitionDurationSec,
        musicPreset = runCatching { MusicTrackPreset.valueOf(musicPresetName) }
            .getOrDefault(MusicTrackPreset.PHONK_DRIVE),
        customMusicUri = customMusicUri,
        customMusicName = customMusicName,
        musicVolume = musicVolume,
        ttsSettings = deserializeTts(ttsJson),
        socialCaptions = deserializeSocial(socialCaptionsJson),
        lastExportedUri = lastExportedUri,
        updatedAtMs = updatedAtMs
    )

    private fun serializeClips(clips: List<TimelineClipItem>): String {
        val arr = JSONArray()
        clips.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("title", c.title)
                    put("sourceType", c.sourceType.name)
                    put("sourceUri", c.sourceUri)
                    put("thumbnailUrl", c.thumbnailUrl)
                    put("durationMs", c.durationMs)
                    put("trimStartMs", c.trimStartMs)
                    put("trimEndMs", c.trimEndMs)
                    put("viralScore", c.viralScore)
                    put("hookReason", c.hookReason)
                    put("youtubeStartLabel", c.youtubeStartLabel)
                    put("youtubeEndLabel", c.youtubeEndLabel)
                }
            )
        }
        return arr.toString()
    }

    private fun deserializeClips(json: String): List<TimelineClipItem> {
        val result = mutableListOf<TimelineClipItem>()
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    TimelineClipItem(
                        id = o.optString("id"),
                        title = o.optString("title", "Clip ${i + 1}"),
                        sourceType = runCatching {
                            ClipSourceType.valueOf(o.optString("sourceType"))
                        }.getOrDefault(ClipSourceType.YOUTUBE_SEGMENT),
                        sourceUri = o.optString("sourceUri"),
                        thumbnailUrl = o.optString("thumbnailUrl"),
                        durationMs = o.optLong("durationMs", 30_000L),
                        trimStartMs = o.optLong("trimStartMs", 0L),
                        trimEndMs = o.optLong("trimEndMs", 30_000L),
                        viralScore = o.optInt("viralScore", 90),
                        hookReason = o.optString("hookReason"),
                        youtubeStartLabel = o.optString("youtubeStartLabel", "00:00"),
                        youtubeEndLabel = o.optString("youtubeEndLabel", "00:30")
                    )
                )
            }
        }
        return result
    }

    private fun serializeSubtitles(subtitles: List<SubtitleCue>): String {
        val arr = JSONArray()
        subtitles.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("startMs", s.startMs)
                    put("endMs", s.endMs)
                    put("text", s.text)
                }
            )
        }
        return arr.toString()
    }

    private fun deserializeSubtitles(json: String): List<SubtitleCue> {
        val result = mutableListOf<SubtitleCue>()
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    SubtitleCue(
                        id = o.optString("id"),
                        startMs = o.optLong("startMs", 0L),
                        endMs = o.optLong("endMs", 3_000L),
                        text = o.optString("text")
                    )
                )
            }
        }
        return result
    }

    private fun serializeOverlays(overlays: List<TextOverlayItem>): String {
        val arr = JSONArray()
        overlays.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("xFraction", item.xFraction.toDouble())
                    put("yFraction", item.yFraction.toDouble())
                    put("fontSizeSp", item.fontSizeSp.toDouble())
                    put("textColorArgb", item.textColorArgb)
                    put("bgColorArgb", item.bgColorArgb)
                    put("fontStyle", item.fontStyle.name)
                    put("startMs", item.startMs)
                    put("endMs", item.endMs)
                }
            )
        }
        return arr.toString()
    }

    private fun deserializeOverlays(json: String): List<TextOverlayItem> {
        val result = mutableListOf<TextOverlayItem>()
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                result.add(
                    TextOverlayItem(
                        id = o.optString("id"),
                        text = o.optString("text"),
                        xFraction = o.optDouble("xFraction", 0.5).toFloat(),
                        yFraction = o.optDouble("yFraction", 0.22).toFloat(),
                        fontSizeSp = o.optDouble("fontSizeSp", 22.0).toFloat(),
                        textColorArgb = o.optInt("textColorArgb", 0xFFFFFFFF.toInt()),
                        bgColorArgb = o.optInt("bgColorArgb", 0xCCFF3D5A.toInt()),
                        fontStyle = runCatching {
                            OverlayFontStyle.valueOf(o.optString("fontStyle"))
                        }.getOrDefault(OverlayFontStyle.BOLD_DISPLAY),
                        startMs = o.optLong("startMs", 0L),
                        endMs = o.optLong("endMs", 60_000L)
                    )
                )
            }
        }
        return result
    }

    private fun serializeTts(tts: TtsSettings): String = JSONObject().apply {
        put("enabled", tts.enabled)
        put("script", tts.script)
        put("languageTag", tts.languageTag)
        put("speechRate", tts.speechRate.toDouble())
        put("pitch", tts.pitch.toDouble())
        put("volume", tts.volume.toDouble())
        put("synthesizedWavPath", tts.synthesizedWavPath ?: "")
    }.toString()

    private fun deserializeTts(json: String): TtsSettings = runCatching {
        val o = JSONObject(json)
        TtsSettings(
            enabled = o.optBoolean("enabled", false),
            script = o.optString("script", ""),
            languageTag = o.optString("languageTag", "en-US"),
            speechRate = o.optDouble("speechRate", 1.05).toFloat(),
            pitch = o.optDouble("pitch", 1.0).toFloat(),
            volume = o.optDouble("volume", 0.9).toFloat(),
            synthesizedWavPath = o.optString("synthesizedWavPath").ifBlank { null }
        )
    }.getOrDefault(TtsSettings())

    private fun serializeSocial(bundle: SocialCaptionBundle): String = JSONObject().apply {
        put("viralHook", bundle.viralHook)
        put("instagramCaption", bundle.instagramCaption)
        put("youtubeShortsCaption", bundle.youtubeShortsCaption)
        put("tiktokCaption", bundle.tiktokCaption)
        put("twitterCaption", bundle.twitterCaption)
        val tags = JSONArray()
        bundle.hashtags.forEach { tags.put(it) }
        put("hashtags", tags)
    }.toString()

    private fun deserializeSocial(json: String): SocialCaptionBundle = runCatching {
        val o = JSONObject(json)
        val tagsArr = o.optJSONArray("hashtags") ?: JSONArray()
        val tags = (0 until tagsArr.length()).map { tagsArr.getString(it) }
        SocialCaptionBundle(
            viralHook = o.optString("viralHook"),
            instagramCaption = o.optString("instagramCaption"),
            youtubeShortsCaption = o.optString("youtubeShortsCaption"),
            tiktokCaption = o.optString("tiktokCaption"),
            twitterCaption = o.optString("twitterCaption"),
            hashtags = tags
        )
    }.getOrDefault(SocialCaptionBundle())
}
