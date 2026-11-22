package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
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

fun transitionPresetIcon(preset: TransitionPreset): ImageVector = when (preset) {
    TransitionPreset.NONE -> Icons.Rounded.ContentCut
    TransitionPreset.CROSSFADE -> Icons.Rounded.Gradient
    TransitionPreset.SLIDE_LEFT -> Icons.Rounded.CompareArrows
    TransitionPreset.SLIDE_UP -> Icons.Rounded.SwapVert
    TransitionPreset.ZOOM_IN -> Icons.Rounded.ZoomIn
    TransitionPreset.FLASH_WHITE -> Icons.Rounded.Bolt
    TransitionPreset.GLITCH_CUT -> Icons.Rounded.AutoFixHigh
    TransitionPreset.WIPE_RIGHT -> Icons.Rounded.ViewColumn
}

@Composable
fun TransitionsToolPanel(
    project: ClipProject,
    onSelectGlobalTransition: (TransitionPreset) -> Unit,
    onGlobalDurationChange: (Float) -> Unit,
    onSelectClipTransition: (String, TransitionPreset) -> Unit,
    onClipDurationChange: (String, Float) -> Unit,
    onApplyToAll: (TransitionPreset, Float) -> Unit,
    onPreviewSplice: (Int) -> Unit,
    onAddSecondClip: () -> Unit
) {
    val spliceCount = (project.clips.size - 1).coerceAtLeast(0)
    // -1 = All Splices (global default), 0..spliceCount-1 = specific splice between C(i+1) & C(i+2)
    var selectedSpliceIdx by remember(spliceCount) {
        mutableIntStateOf(if (spliceCount > 0) 0 else -1)
    }

    val editingClip = if (selectedSpliceIdx in 0 until spliceCount) {
        project.clips.getOrNull(selectedSpliceIdx)
    } else null

    val activeTransition = if (editingClip != null) {
        project.resolvedTransitionAfter(selectedSpliceIdx)
    } else {
        project.transitionPreset
    }

    val activeDuration = if (editingClip != null) {
        project.resolvedTransitionDurationSec(selectedSpliceIdx)
    } else {
        project.transitionDurationSec
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(
            title = "Clip Transitions",
            subtitle = if (spliceCount > 0) {
                "Pick Fade, Slide, Zoom & more per splice or across all $spliceCount cuts"
            } else {
                "Add a 2nd clip to splice & preview transitions between clips"
            },
            trailing = if (spliceCount > 0) {
                {
                    OutlinedButton(
                        onClick = { onPreviewSplice(selectedSpliceIdx.coerceAtLeast(0)) },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("transitions_preview_cut_button"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        border = BorderStroke(1.dp, CyanWave.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanWave)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_preview_cut), style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else null
        )

        if (spliceCount == 0) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, CyanWave.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CompareArrows,
                        contentDescription = null,
                        tint = CyanWave,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Single clip on timeline",
                            style = MaterialTheme.typography.titleSmall,
                            color = InkPrimary
                        )
                        Text(
                            text = "Choose a default transition below, or add another clip to customize every splice.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                    Button(
                        onClick = onAddSecondClip,
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("transitions_add_clip_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidLimeContainer,
                            contentColor = AcidLime
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Clip", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            // Splice Point Selector Strip (All Splices vs individual C1 → C2, C2 → C3...)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSpliceIdx == -1,
                    onClick = { selectedSpliceIdx = -1 },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .testTag("splice_target_all"),
                    label = {
                        Text(
                            text = "All ($spliceCount)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedSpliceIdx == -1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceElevated,
                        selectedContainerColor = AcidLimeContainer,
                        selectedLabelColor = AcidLime
                    )
                )
                for (i in 0 until spliceCount) {
                    val presetAtSplice = project.resolvedTransitionAfter(i)
                    val isSelected = selectedSpliceIdx == i
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSpliceIdx = i },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("splice_target_$i"),
                        leadingIcon = {
                            Icon(
                                imageVector = transitionPresetIcon(presetAtSplice),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "C${i + 1}→C${i + 2} · ${presetAtSplice.shortLabel}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceElevated,
                            selectedContainerColor = CyanContainer,
                            selectedLabelColor = CyanWave,
                            selectedLeadingIconColor = CyanWave
                        )
                    )
                }
            }
        }

        // Transition Presets Selector Row (Fade, Slide, Zoom, Flash, Glitch, Wipe, Cut)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TransitionPreset.entries.forEach { preset ->
                val isSelected = preset == activeTransition
                Surface(
                    modifier = Modifier
                        .width(106.dp)
                        .defaultMinSize(minHeight = 86.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (editingClip != null) {
                                onSelectClipTransition(editingClip.id, preset)
                            } else {
                                onApplyToAll(preset, activeDuration)
                            }
                        }
                        .testTag("transition_${preset.name.lowercase()}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) CyanContainer else SurfaceElevated,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CyanWave else OutlineHairline
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CyanWave.copy(alpha = 0.2f) else SurfaceBright),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = transitionPresetIcon(preset),
                                contentDescription = null,
                                tint = if (isSelected) CyanWave else InkSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = preset.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) CyanWave else InkPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = InkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (activeTransition != TransitionPreset.NONE) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (editingClip != null) {
                            "Splice #${selectedSpliceIdx + 1} Duration"
                        } else {
                            "Transition Speed / Duration"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                    Text(
                        text = "${"%.1f".format(activeDuration)}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanWave,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = activeDuration,
                    onValueChange = { dur ->
                        if (editingClip != null) {
                            onClipDurationChange(editingClip.id, dur)
                        } else {
                            onGlobalDurationChange(dur)
                        }
                    },
                    valueRange = 0.2f..2.0f,
                    colors = SliderDefaults.colors(thumbColor = CyanWave, activeTrackColor = CyanWave)
                )

                // Quick speed chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.3f to "Snappy 0.3s", 0.6f to "Smooth 0.6s", 1.2f to "Cinema 1.2s").forEach { (sec, label) ->
                        val isMatch = kotlin.math.abs(activeDuration - sec) < 0.08f
                        FilterChip(
                            selected = isMatch,
                            onClick = {
                                if (editingClip != null) {
                                    onClipDurationChange(editingClip.id, sec)
                                } else {
                                    onGlobalDurationChange(sec)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceElevated,
                                selectedContainerColor = CyanContainer,
                                selectedLabelColor = CyanWave
                            )
                        )
                    }
                }
            }
        }

        if (editingClip != null && spliceCount > 1) {
            OutlinedButton(
                onClick = { onApplyToAll(activeTransition, activeDuration) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("transitions_apply_all_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AcidLime)
            ) {
                Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Apply ${activeTransition.displayName} to All $spliceCount Splices",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpliceTransitionSelectorSheet(
    project: ClipProject,
    spliceIndex: Int,
    onDismiss: () -> Unit,
    onSelectTransition: (String, TransitionPreset) -> Unit,
    onDurationChange: (String, Float) -> Unit,
    onApplyToAll: (TransitionPreset, Float) -> Unit,
    onPreviewSplice: (Int) -> Unit
) {
    val fromClip = project.clips.getOrNull(spliceIndex) ?: return
    val toClip = project.clips.getOrNull(spliceIndex + 1) ?: return
    val activeTransition = project.resolvedTransitionAfter(spliceIndex)
    val activeDuration = project.resolvedTransitionDurationSec(spliceIndex)
    val spliceCount = (project.clips.size - 1).coerceAtLeast(1)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        contentColor = InkPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle(color = InkMuted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.splice_transition_sheet_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "C${spliceIndex + 1} ${fromClip.title.take(18)} → C${spliceIndex + 2} ${toClip.title.take(18)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanWave,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_splice_sheet_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = InkSecondary)
                }
            }

            // 2-Column Visual Grid of Transition Presets
            val presets = TransitionPreset.entries
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                presets.chunked(2).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = preset == activeTransition
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 68.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectTransition(fromClip.id, preset) }
                                    .testTag("splice_transition_${preset.name.lowercase()}"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) CyanContainer else SurfaceElevated,
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) CyanWave else OutlineHairline
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) CyanWave.copy(alpha = 0.22f) else SurfaceBright),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = transitionPresetIcon(preset),
                                            contentDescription = null,
                                            tint = if (isSelected) CyanWave else InkSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.displayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSelected) CyanWave else InkPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = preset.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InkMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = CyanWave,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (rowPresets.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // Duration Control
            if (activeTransition != TransitionPreset.NONE) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, OutlineHairline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transition Duration",
                                style = MaterialTheme.typography.labelMedium,
                                color = InkSecondary
                            )
                            Text(
                                text = "${"%.1f".format(activeDuration)}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanWave,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = activeDuration,
                            onValueChange = { onDurationChange(fromClip.id, it) },
                            valueRange = 0.2f..2.0f,
                            modifier = Modifier.testTag("splice_duration_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanWave,
                                activeTrackColor = CyanWave
                            )
                        )
                    }
                }
            }

            // Actions Row: Preview Cut + Apply to All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onPreviewSplice(spliceIndex)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("preview_splice_transition_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CyanWave),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanWave)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.action_preview_cut), fontWeight = FontWeight.SemiBold)
                }

                if (spliceCount > 1) {
                    Button(
                        onClick = {
                            onApplyToAll(activeTransition, activeDuration)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("apply_transition_all_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AcidLime,
                            contentColor = OnAcidLime
                        )
                    ) {
                        Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Apply to All", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("done_splice_transition_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPulse,
                            contentColor = OnCoral
                        )
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
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
