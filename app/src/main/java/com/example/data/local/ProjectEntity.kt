package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clip_projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val youtubeUrl: String,
    val youtubeVideoId: String,
    val channelName: String,
    val coverThumbnailUrl: String,
    val aspectRatioName: String,
    val exportQualityName: String,
    val exportResolutionName: String = "RES_1080P",
    val exportBitrateTierName: String = "MASTER",
    val exportFps: Int = 60,
    val proClarityEnabled: Boolean = true,
    val targetLengthSec: Int,
    val clipsJson: String,
    val autoCaptionsEnabled: Boolean,
    val subtitleStyleName: String,
    val subtitleFontSizeSp: Float,
    val subtitleYFraction: Float,
    val subtitlesJson: String,
    val textOverlaysJson: String,
    val filterPresetName: String,
    val filterIntensity: Float,
    val effectPresetName: String,
    val effectIntensity: Float,
    val transitionPresetName: String,
    val transitionDurationSec: Float,
    val musicPresetName: String,
    val customMusicUri: String?,
    val customMusicName: String?,
    val musicVolume: Float,
    val ttsJson: String,
    val socialCaptionsJson: String,
    val lastExportedUri: String?,
    val updatedAtMs: Long
)
