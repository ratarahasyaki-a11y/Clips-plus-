package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.StudioSectionHeader
import com.example.ui.theme.*

@Composable
fun FiltersToolPanel(
    selectedFilter: VideoFilterPreset,
    intensity: Float,
    onSelectFilter: (VideoFilterPreset) -> Unit,
    onIntensityChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(
            title = "Color Filters",
            subtitle = "Real-time cinema color grading burned into exported video"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VideoFilterPreset.entries.forEach { preset ->
                val isSelected = preset == selectedFilter
                Surface(
                    modifier = Modifier
                        .width(96.dp)
                        .defaultMinSize(minHeight = 84.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectFilter(preset) }
                        .testTag("filter_${preset.name.lowercase()}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) SurfaceBright else SurfaceElevated,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AcidLime else OutlineHairline
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(preset.accentHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = ObsidianBg,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) AcidLime else InkPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (selectedFilter != VideoFilterPreset.ORIGINAL) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Filter Strength", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                Text("${(intensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = AcidLime)
            }
            Slider(
                value = intensity,
                onValueChange = onIntensityChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = AcidLime, activeTrackColor = AcidLime)
            )
        }
    }
}

@Composable
fun EffectsToolPanel(
    selectedEffect: VideoEffectPreset,
    intensity: Float,
    onSelectEffect: (VideoEffectPreset) -> Unit,
    onIntensityChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(
            title = "Video Effects",
            subtitle = "Dynamic motion, glitch, VHS, bloom & vignette shaders"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VideoEffectPreset.entries.forEach { preset ->
                val isSelected = preset == selectedEffect
                Surface(
                    modifier = Modifier
                        .width(108.dp)
                        .defaultMinSize(minHeight = 82.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectEffect(preset) }
                        .testTag("effect_${preset.name.lowercase()}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) CoralContainer else SurfaceElevated,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CoralPulse else OutlineHairline
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) CoralPulse else SurfaceBright
                        ) {
                            Text(
                                text = preset.badge.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = InkPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = InkPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        if (selectedEffect != VideoEffectPreset.NONE) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Effect Intensity", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                Text("${(intensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = CoralPulse)
            }
            Slider(
                value = intensity,
                onValueChange = onIntensityChange,
                valueRange = 0.1f..1f,
                colors = SliderDefaults.colors(thumbColor = CoralPulse, activeTrackColor = CoralPulse)
            )
        }
    }
}

@Composable
fun TransitionsToolPanel(
    selectedTransition: TransitionPreset,
    durationSec: Float,
    clipCount: Int,
    onSelectTransition: (TransitionPreset) -> Unit,
    onDurationChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(
            title = "Clip Transitions",
            subtitle = if (clipCount > 1) {
                "Applied seamlessly across all ${clipCount - 1} clip cuts"
            } else {
                "Add a 2nd clip in Clips tab to see seamless transitions"
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TransitionPreset.entries.forEach { preset ->
                val isSelected = preset == selectedTransition
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 92.dp, minHeight = 68.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTransition(preset) }
                        .testTag("transition_${preset.name.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) CyanContainer else SurfaceElevated,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CyanWave else OutlineHairline
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixNormal,
                            contentDescription = null,
                            tint = if (isSelected) CyanWave else InkSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) CyanWave else InkPrimary
                        )
                    }
                }
            }
        }

        if (selectedTransition != TransitionPreset.NONE) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Transition Speed / Duration", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                Text("${"%.1f".format(durationSec)}s", style = MaterialTheme.typography.labelSmall, color = CyanWave)
            }
            Slider(
                value = durationSec,
                onValueChange = onDurationChange,
                valueRange = 0.2f..2.0f,
                colors = SliderDefaults.colors(thumbColor = CyanWave, activeTrackColor = CyanWave)
            )
        }
    }
}

@Composable
fun MusicToolPanel(
    selectedPreset: MusicTrackPreset,
    customMusicName: String?,
    musicVolume: Float,
    isPreviewing: Boolean,
    onSelectPreset: (MusicTrackPreset) -> Unit,
    onTogglePreview: () -> Unit,
    onPickCustomAudio: () -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(
            title = "Background Music",
            subtitle = "Built-in offline royalty-free beats or your own audio track",
            trailing = {
                OutlinedButton(
                    onClick = onPickCustomAudio,
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("pick_custom_music_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AcidLime)
                ) {
                    Icon(Icons.Rounded.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Device Audio", style = MaterialTheme.typography.labelSmall)
                }
            }
        )

        if (selectedPreset == MusicTrackPreset.CUSTOM_AUDIO && !customMusicName.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AcidLimeContainer,
                border = BorderStroke(1.dp, AcidLime)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = AcidLime)
                    Text(
                        text = "Selected: $customMusicName",
                        style = MaterialTheme.typography.labelMedium,
                        color = AcidLime
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MusicTrackPreset.entries.filter { it != MusicTrackPreset.CUSTOM_AUDIO }.forEach { track ->
                val isSelected = track == selectedPreset
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectPreset(track) }
                        .testTag("music_track_${track.id}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) SurfaceBright else SurfaceElevated,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) CoralPulse else OutlineHairline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (track == MusicTrackPreset.NONE) Icons.Rounded.MusicOff else Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = if (isSelected) CoralPulse else CyanWave
                            )
                            Column {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = InkPrimary
                                )
                                Text(
                                    text = if (track.bpm > 0) "${track.mood} · ${track.bpm} BPM · Offline Synth" else track.mood,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }
                        }

                        if (isSelected && track != MusicTrackPreset.NONE) {
                            IconButton(onClick = onTogglePreview) {
                                Icon(
                                    imageVector = if (isPreviewing) Icons.Rounded.StopCircle else Icons.Rounded.PlayCircle,
                                    contentDescription = "Preview music track",
                                    tint = AcidLime
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedPreset != MusicTrackPreset.NONE) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Background Music Volume", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                Text("${(musicVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = CoralPulse)
            }
            Slider(
                value = musicVolume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(thumbColor = CoralPulse, activeTrackColor = CoralPulse)
            )
        }
    }
}
