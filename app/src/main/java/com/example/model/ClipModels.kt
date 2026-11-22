package com.example.model

import java.util.UUID

enum class AspectRatioPreset(
    val label: String,
    val subtitle: String,
    val widthRatio: Float,
    val heightRatio: Float,
    val exportWidth720: Int,
    val exportHeight720: Int,
    val exportWidth1080: Int,
    val exportHeight1080: Int
) {
    RATIO_19_6(
        label = "19:6",
        subtitle = "Ultra-Wide",
        widthRatio = 19f,
        heightRatio = 6f,
        exportWidth720 = 1280,
        exportHeight720 = 400,
        exportWidth1080 = 1904,
        exportHeight1080 = 608
    ),
    RATIO_9_16(
        label = "9:16",
        subtitle = "Reels / Shorts",
        widthRatio = 9f,
        heightRatio = 16f,
        exportWidth720 = 720,
        exportHeight720 = 1280,
        exportWidth1080 = 1080,
        exportHeight1080 = 1920
    ),
    RATIO_16_9(
        label = "16:9",
        subtitle = "YouTube HD",
        widthRatio = 16f,
        heightRatio = 9f,
        exportWidth720 = 1280,
        exportHeight720 = 720,
        exportWidth1080 = 1920,
        exportHeight1080 = 1080
    ),
    RATIO_1_1(
        label = "1:1",
        subtitle = "Square Feed",
        widthRatio = 1f,
        heightRatio = 1f,
        exportWidth720 = 720,
        exportHeight720 = 720,
        exportWidth1080 = 1080,
        exportHeight1080 = 1080
    );

    val aspectRatio: Float get() = widthRatio / heightRatio

    fun dimensionsFor(resolution: ExportResolution): Pair<Int, Int> {
        val shortSide = resolution.shortSidePx
        val rawW: Int
        val rawH: Int
        if (widthRatio >= heightRatio) {
            rawH = shortSide
            rawW = kotlin.math.round(shortSide * (widthRatio / heightRatio)).toInt()
        } else {
            rawW = shortSide
            rawH = kotlin.math.round(shortSide * (heightRatio / widthRatio)).toInt()
        }
        val maxLongSide = when (resolution) {
            ExportResolution.RES_720P -> 1280
            ExportResolution.RES_1080P -> 1920
            ExportResolution.RES_2K -> 2560
            ExportResolution.RES_4K -> 3840
        }
        val scaleDown = if (maxOf(rawW, rawH) > maxLongSide) {
            maxLongSide.toFloat() / maxOf(rawW, rawH).toFloat()
        } else 1f
        val w = ((rawW * scaleDown).toInt() / 16 * 16).coerceAtLeast(320)
        val h = ((rawH * scaleDown).toInt() / 16 * 16).coerceAtLeast(320)
        return w to h
    }
}

enum class ExportResolution(
    val label: String,
    val shortLabel: String,
    val shortSidePx: Int,
    val tag: String
) {
    RES_720P("720p HD", "720p", 720, "Compact"),
    RES_1080P("1080p Full HD", "1080p", 1080, "Social HD"),
    RES_2K("2K QHD", "2K", 1440, "Crisp 2K"),
    RES_4K("4K UHD", "4K", 2160, "Ultra HD")
}

enum class ExportBitrateTier(
    val label: String,
    val subtitle: String
) {
    STANDARD("Standard", "Fast Upload"),
    HIGH("High", "Social Crisp"),
    MASTER("Master", "Pro Master")
}

fun resolveExportBitrateMbps(
    resolution: ExportResolution,
    tier: ExportBitrateTier,
    fps: Int
): Int {
    val isHighFps = fps >= 60
    return when (resolution) {
        ExportResolution.RES_720P -> when (tier) {
            ExportBitrateTier.STANDARD -> if (isHighFps) 6 else 5
            ExportBitrateTier.HIGH -> if (isHighFps) 10 else 8
            ExportBitrateTier.MASTER -> if (isHighFps) 14 else 12
        }
        ExportResolution.RES_1080P -> when (tier) {
            ExportBitrateTier.STANDARD -> if (isHighFps) 12 else 10
            ExportBitrateTier.HIGH -> if (isHighFps) 18 else 16
            ExportBitrateTier.MASTER -> if (isHighFps) 24 else 22
        }
        ExportResolution.RES_2K -> when (tier) {
            ExportBitrateTier.STANDARD -> if (isHighFps) 22 else 18
            ExportBitrateTier.HIGH -> if (isHighFps) 30 else 26
            ExportBitrateTier.MASTER -> if (isHighFps) 38 else 34
        }
        ExportResolution.RES_4K -> when (tier) {
            ExportBitrateTier.STANDARD -> if (isHighFps) 34 else 28
            ExportBitrateTier.HIGH -> if (isHighFps) 44 else 38
            ExportBitrateTier.MASTER -> if (isHighFps) 50 else 48
        }
    }
}

enum class SocialPlatformExportPreset(
    val label: String,
    val shortLabel: String,
    val tagline: String,
    val aspectRatio: AspectRatioPreset,
    val resolution: ExportResolution,
    val bitrateTier: ExportBitrateTier,
    val fps: Int,
    val proClarity: Boolean
) {
    INSTAGRAM_REELS(
        label = "Instagram Reels",
        shortLabel = "Reels",
        tagline = "9:16 · 1080p · 60fps · High",
        aspectRatio = AspectRatioPreset.RATIO_9_16,
        resolution = ExportResolution.RES_1080P,
        bitrateTier = ExportBitrateTier.HIGH,
        fps = 60,
        proClarity = true
    ),
    YOUTUBE_SHORTS(
        label = "YouTube Shorts",
        shortLabel = "Shorts",
        tagline = "9:16 · 1080p · 60fps · Master",
        aspectRatio = AspectRatioPreset.RATIO_9_16,
        resolution = ExportResolution.RES_1080P,
        bitrateTier = ExportBitrateTier.MASTER,
        fps = 60,
        proClarity = true
    ),
    TIKTOK(
        label = "TikTok",
        shortLabel = "TikTok",
        tagline = "9:16 · 1080p · 30fps · High",
        aspectRatio = AspectRatioPreset.RATIO_9_16,
        resolution = ExportResolution.RES_1080P,
        bitrateTier = ExportBitrateTier.HIGH,
        fps = 30,
        proClarity = true
    ),
    WHATSAPP(
        label = "WhatsApp Status",
        shortLabel = "WhatsApp",
        tagline = "9:16 · 720p · 30fps · Standard",
        aspectRatio = AspectRatioPreset.RATIO_9_16,
        resolution = ExportResolution.RES_720P,
        bitrateTier = ExportBitrateTier.STANDARD,
        fps = 30,
        proClarity = true
    ),
    YOUTUBE_4K(
        label = "YouTube 4K",
        shortLabel = "YT 4K",
        tagline = "16:9 · 4K UHD · 60fps · Master",
        aspectRatio = AspectRatioPreset.RATIO_16_9,
        resolution = ExportResolution.RES_4K,
        bitrateTier = ExportBitrateTier.MASTER,
        fps = 60,
        proClarity = true
    )
}

enum class ExportQuality(
    val label: String,
    val specBadge: String,
    val bitrateBps: Int,
    val fps: Int
) {
    HD_720P(
        label = "720p HD",
        specBadge = "30fps · 8 Mbps",
        bitrateBps = 8_000_000,
        fps = 30
    ),
    FULL_HD_1080P(
        label = "1080p Full HD",
        specBadge = "30fps · 16 Mbps",
        bitrateBps = 16_000_000,
        fps = 30
    ),
    PRO_IPHONE_MASTER(
        label = "iPhone Pro Master",
        specBadge = "60fps · 22 Mbps",
        bitrateBps = 22_000_000,
        fps = 60
    )
}

enum class VideoFilterPreset(
    val displayName: String,
    val accentHex: Long,
    val matrix: FloatArray
) {
    ORIGINAL(
        "Original",
        0xFF8B92AD,
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    CINEMATIC_TEAL(
        "Teal & Orange",
        0xFF2EE6FF,
        floatArrayOf(
            1.18f, 0.04f, -0.05f, 0f, 8f,
            -0.02f, 1.04f, 0.06f, 0f, 0f,
            -0.08f, 0.12f, 1.16f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    VIVID_VIRAL(
        "Vivid Pop",
        0xFFFF3D5A,
        floatArrayOf(
            1.28f, -0.10f, -0.10f, 0f, 6f,
            -0.10f, 1.28f, -0.10f, 0f, 6f,
            -0.10f, -0.10f, 1.28f, 0f, 6f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    WARM_SUNSET(
        "Warm Sunset",
        0xFFFF8C42,
        floatArrayOf(
            1.24f, 0.08f, 0f, 0f, 14f,
            0.03f, 1.05f, 0f, 0f, 4f,
            0f, 0f, 0.84f, 0f, -8f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    NOIR_BW(
        "Noir B&W",
        0xFFE2E6F0,
        floatArrayOf(
            0.35f, 0.65f, 0.12f, 0f, -12f,
            0.35f, 0.65f, 0.12f, 0f, -12f,
            0.35f, 0.65f, 0.12f, 0f, -12f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    CYBER_NEON(
        "Cyber Neon",
        0xFFD946EF,
        floatArrayOf(
            1.22f, 0f, 0.20f, 0f, 10f,
            -0.05f, 0.94f, 0.15f, 0f, -4f,
            0.18f, 0.08f, 1.32f, 0f, 16f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    VINTAGE_FILM(
        "Vintage 35mm",
        0xFFD4A373,
        floatArrayOf(
            1.05f, 0.25f, 0.08f, 0f, 10f,
            0.14f, 0.96f, 0.08f, 0f, 6f,
            0.08f, 0.14f, 0.72f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    COOL_CRISP(
        "Cool Crisp",
        0xFF38BDF8,
        floatArrayOf(
            0.94f, 0f, 0.04f, 0f, -4f,
            0f, 1.04f, 0.06f, 0f, 4f,
            0.04f, 0.08f, 1.22f, 0f, 14f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    GOLDEN_HOUR(
        "Golden Hour",
        0xFFFFC947,
        floatArrayOf(
            1.20f, 0.12f, 0f, 0f, 12f,
            0.06f, 1.12f, 0f, 0f, 8f,
            0f, 0.04f, 0.78f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

enum class VideoEffectPreset(val displayName: String, val badge: String) {
    NONE("None", "Off"),
    ZOOM_PULSE("Zoom Pulse", "Beat"),
    VIGNETTE("Cinema Vignette", "Edge"),
    RGB_GLITCH("RGB Glitch", "Viral"),
    RETRO_VHS("Retro VHS", "Grain"),
    GLOW_BLOOM("Neon Bloom", "Soft"),
    KINETIC_SHAKE("Impact Shake", "Hype")
}

enum class TransitionPreset(
    val displayName: String,
    val shortLabel: String,
    val description: String
) {
    NONE("Cut", "Cut", "Instant hard cut"),
    CROSSFADE("Crossfade", "Fade", "Smooth opacity blend"),
    SLIDE_LEFT("Slide Left", "Slide", "Horizontal push sweep"),
    SLIDE_UP("Slide Up", "Up", "Vertical upward push"),
    ZOOM_IN("Zoom In", "Zoom", "Dynamic punch-in zoom"),
    FLASH_WHITE("Flash Cut", "Flash", "Bright strobe flash"),
    GLITCH_CUT("Glitch Cut", "Glitch", "Digital RGB snap"),
    WIPE_RIGHT("Cinema Wipe", "Wipe", "Left-to-right reveal")
}

enum class SubtitleStylePreset(
    val displayName: String,
    val primaryColorArgb: Int,
    val highlightColorArgb: Int,
    val bgColorArgb: Int,
    val uppercase: Boolean
) {
    VIRAL_POP("Viral Pop", 0xFFFFFFFF.toInt(), 0xFFC8FF2E.toInt(), 0xCC0B0C10.toInt(), true),
    NEON_GLOW("Neon Coral", 0xFFFFFFFF.toInt(), 0xFFFF3D5A.toInt(), 0xAA170A12.toInt(), true),
    PILL_BOX("Cyan Pill", 0xFFF5F6FA.toInt(), 0xFF2EE6FF.toInt(), 0xE614161F.toInt(), false),
    YELLOW_BOLD("Hormozi Gold", 0xFFFFD83B.toInt(), 0xFFFFFFFF.toInt(), 0xB3000000.toInt(), true),
    CINEMA_BAR("Cinema Sub", 0xFFF5F6FA.toInt(), 0xFFFFB830.toInt(), 0x99000000.toInt(), false)
}

enum class OverlayFontStyle(val displayName: String) {
    BOLD_DISPLAY("Display"),
    CLEAN_SANS("Modern"),
    MONO_TECH("Mono"),
    UPPER_STAMP("Stamp")
}

enum class MusicTrackPreset(
    val id: String,
    val title: String,
    val mood: String,
    val bpm: Int
) {
    NONE("none", "No Music", "Silent", 0),
    PHONK_DRIVE("phonk_drive", "Viral Phonk Pulse", "High Energy", 132),
    LOFI_CHILL("lofi_chill", "Midnight Lo-Fi", "Chill Flow", 86),
    CINEMATIC_PULSE("cinematic_pulse", "Cinema Trailer", "Dramatic", 104),
    INDIE_UPBEAT("indie_upbeat", "Creator Groove", "Upbeat", 118),
    CYBER_SYNTH("cyber_synth", "Neon Synthwave", "Futuristic", 124),
    CUSTOM_AUDIO("custom", "Device Audio", "Custom Track", 0)
}

enum class ClipSourceType {
    YOUTUBE_SEGMENT,
    LOCAL_VIDEO,
    LOCAL_IMAGE
}

data class SubtitleCue(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class TextOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.22f,
    val fontSizeSp: Float = 22f,
    val textColorArgb: Int = 0xFFFFFFFF.toInt(),
    val bgColorArgb: Int = 0xCCFF3D5A.toInt(),
    val fontStyle: OverlayFontStyle = OverlayFontStyle.BOLD_DISPLAY,
    val startMs: Long = 0L,
    val endMs: Long = 60_000L
)

data class TimelineClipItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sourceType: ClipSourceType = ClipSourceType.YOUTUBE_SEGMENT,
    val sourceUri: String = "",
    val thumbnailUrl: String = "",
    val durationMs: Long = 30_000L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 30_000L,
    val viralScore: Int = 92,
    val hookReason: String = "",
    val youtubeStartLabel: String = "00:00",
    val youtubeEndLabel: String = "00:30",
    val transitionAfter: TransitionPreset? = null,
    val transitionDurationSec: Float? = null
) {
    val effectiveDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(3_000L)
}

enum class ViralCaptionTone(
    val label: String,
    val hint: String
) {
    VIRAL_PUNCHY("Viral Punchy", "High-energy short hook + curiosity"),
    HINGLISH_DESI("Hinglish Desi", "Relatable Hindi-English creator vibe"),
    STORYTELLING("Storytelling", "Mini narrative with emotional payoff"),
    CURIOSITY_HOOK("Curiosity Hook", "Open loop that drives watch-time & comments"),
    EDUCATIONAL("Value Tips", "Actionable takeaway & save-worthy format"),
    BOLD_CTA("Bold CTA", "Direct debate question & share trigger")
}

enum class SocialPlatformTab(
    val label: String,
    val shortLabel: String,
    val recommendedMaxChars: Int
) {
    INSTAGRAM("Instagram Reels", "Reels", 2200),
    YOUTUBE("YT Shorts", "Shorts", 500),
    TIKTOK("TikTok", "TikTok", 400),
    TWITTER("X / LinkedIn", "X / Post", 280);

    fun captionFrom(bundle: SocialCaptionBundle): String = when (this) {
        INSTAGRAM -> bundle.instagramCaption
        YOUTUBE -> bundle.youtubeShortsCaption
        TIKTOK -> bundle.tiktokCaption
        TWITTER -> bundle.twitterCaption
    }

    fun updateBundle(bundle: SocialCaptionBundle, updatedCaption: String): SocialCaptionBundle = when (this) {
        INSTAGRAM -> bundle.copy(instagramCaption = updatedCaption)
        YOUTUBE -> bundle.copy(youtubeShortsCaption = updatedCaption)
        TIKTOK -> bundle.copy(tiktokCaption = updatedCaption)
        TWITTER -> bundle.copy(twitterCaption = updatedCaption)
    }
}

data class SocialCaptionBundle(
    val viralHook: String = "",
    val alternateHooks: List<String> = emptyList(),
    val visualInsight: String = "",
    val generatedTone: String = "",
    val instagramCaption: String = "",
    val youtubeShortsCaption: String = "",
    val tiktokCaption: String = "",
    val twitterCaption: String = "",
    val hashtags: List<String> = emptyList()
) {
    fun buildFullPost(
        platform: SocialPlatformTab,
        fallbackTitle: String,
        selectedHashtags: List<String> = hashtags
    ): String {
        val body = platform.captionFrom(this).ifBlank { fallbackTitle }
        return buildString {
            if (viralHook.isNotBlank()) {
                appendLine(viralHook.trim())
                appendLine()
            }
            append(body.trim())
            if (selectedHashtags.isNotEmpty()) {
                appendLine()
                appendLine()
                append(selectedHashtags.joinToString(" "))
            }
        }.trim()
    }

    fun buildCaptionOnly(
        platform: SocialPlatformTab,
        fallbackTitle: String
    ): String {
        val body = platform.captionFrom(this).ifBlank { fallbackTitle }
        return buildString {
            if (viralHook.isNotBlank()) {
                appendLine(viralHook.trim())
                appendLine()
            }
            append(body.trim())
        }.trim()
    }
}

data class TtsSettings(
    val enabled: Boolean = false,
    val script: String = "",
    val languageTag: String = "en-US",
    val speechRate: Float = 1.05f,
    val pitch: Float = 1.0f,
    val volume: Float = 0.9f,
    val synthesizedWavPath: String? = null
)

data class ViralClipCandidate(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startTimeLabel: String,
    val endTimeLabel: String,
    val durationSec: Int,
    val viralScore: Int,
    val hookReason: String,
    val subtitles: List<SubtitleCue>,
    val ttsScript: String,
    val socialCaptions: SocialCaptionBundle
)

data class YouTubeVideoInfo(
    val videoId: String,
    val url: String,
    val title: String,
    val authorName: String,
    val thumbnailUrl: String
)

data class ClipProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Viral Cut",
    val youtubeUrl: String = "",
    val youtubeVideoId: String = "",
    val channelName: String = "",
    val coverThumbnailUrl: String = "",
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_19_6,
    val exportQuality: ExportQuality = ExportQuality.PRO_IPHONE_MASTER,
    val exportResolution: ExportResolution = ExportResolution.RES_1080P,
    val exportBitrateTier: ExportBitrateTier = ExportBitrateTier.MASTER,
    val exportFps: Int = 60,
    val proClarityEnabled: Boolean = true,
    val targetLengthSec: Int = 60,
    val clips: List<TimelineClipItem> = emptyList(),
    val autoCaptionsEnabled: Boolean = true,
    val subtitleStyle: SubtitleStylePreset = SubtitleStylePreset.VIRAL_POP,
    val subtitleFontSizeSp: Float = 20f,
    val subtitleYFraction: Float = 0.78f,
    val subtitles: List<SubtitleCue> = emptyList(),
    val textOverlays: List<TextOverlayItem> = emptyList(),
    val filterPreset: VideoFilterPreset = VideoFilterPreset.ORIGINAL,
    val filterIntensity: Float = 0.85f,
    val effectPreset: VideoEffectPreset = VideoEffectPreset.NONE,
    val effectIntensity: Float = 0.65f,
    val transitionPreset: TransitionPreset = TransitionPreset.CROSSFADE,
    val transitionDurationSec: Float = 0.6f,
    val musicPreset: MusicTrackPreset = MusicTrackPreset.PHONK_DRIVE,
    val customMusicUri: String? = null,
    val customMusicName: String? = null,
    val musicVolume: Float = 0.45f,
    val ttsSettings: TtsSettings = TtsSettings(),
    val socialCaptions: SocialCaptionBundle = SocialCaptionBundle(),
    val lastExportedUri: String? = null,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    val totalDurationMs: Long
        get() {
            val clipSum = clips.sumOf { it.effectiveDurationMs }
            return if (clipSum > 0L) clipSum else targetLengthSec * 1000L
        }

    fun resolvedExportDimensions(): Pair<Int, Int> = aspectRatio.dimensionsFor(exportResolution)

    fun resolvedExportBitrateMbps(): Int = resolveExportBitrateMbps(exportResolution, exportBitrateTier, exportFps)

    fun resolvedExportBitrateBps(): Int = resolvedExportBitrateMbps() * 1_000_000

    fun estimatedExportSizeMb(): Double {
        val totalBitsPerSec = resolvedExportBitrateBps().toDouble() + 192_000.0
        val seconds = (totalDurationMs / 1000.0).coerceAtLeast(1.0)
        return (totalBitsPerSec * seconds) / (8.0 * 1024.0 * 1024.0)
    }

    fun matchingSocialPreset(): SocialPlatformExportPreset? {
        return SocialPlatformExportPreset.entries.firstOrNull { preset ->
            preset.aspectRatio == aspectRatio &&
                preset.resolution == exportResolution &&
                preset.bitrateTier == exportBitrateTier &&
                preset.fps == exportFps
        }
    }

    fun resolvedTransitionAfter(clipIndex: Int): TransitionPreset {
        return clips.getOrNull(clipIndex)?.transitionAfter ?: transitionPreset
    }

    fun resolvedTransitionDurationSec(clipIndex: Int): Float {
        return (clips.getOrNull(clipIndex)?.transitionDurationSec ?: transitionDurationSec)
            .coerceIn(0.2f, 2.0f)
    }

    fun spliceBoundaryMs(clipIndex: Int): Long {
        if (clips.isEmpty() || clipIndex !in clips.indices) return 0L
        var acc = 0L
        for (i in 0..clipIndex) {
            acc += clips[i].effectiveDurationMs
        }
        return acc.coerceAtMost(totalDurationMs)
    }
}
