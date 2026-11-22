package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.model.AspectRatioPreset
import com.example.model.ClipProject
import com.example.model.ExportBitrateTier
import com.example.model.ExportResolution
import com.example.model.MusicTrackPreset
import com.example.model.SocialPlatformExportPreset
import com.example.model.resolveExportBitrateMbps
import com.example.ui.components.AspectRatioChipRow
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*
import com.example.viewmodel.ExportUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialogSheet(
    project: ClipProject,
    exportState: ExportUiState,
    onDismiss: () -> Unit,
    onSelectAspectRatio: (AspectRatioPreset) -> Unit,
    onSelectResolution: (ExportResolution) -> Unit,
    onSelectBitrateTier: (ExportBitrateTier) -> Unit,
    onSelectFps: (Int) -> Unit,
    onApplySocialPreset: (SocialPlatformExportPreset) -> Unit,
    onToggleProClarity: (Boolean) -> Unit,
    onStartExport: () -> Unit,
    onShareExportedVideo: (Uri, String) -> Unit,
    onOpenVideoPlayer: (Uri) -> Unit
) {
    val activePreset = project.matchingSocialPreset()
    val (outW, outH) = project.resolvedExportDimensions()
    val outMbps = project.resolvedExportBitrateMbps()
    val estSizeMb = project.estimatedExportSizeMb()
    val estSizeLabel = if (estSizeMb >= 100.0) {
        "~${estSizeMb.toInt()} MB"
    } else {
        "~${"%.1f".format(estSizeMb)} MB"
    }
    val durationLabel = formatDurationShort((project.totalDurationMs / 1000L).toInt())

    ModalBottomSheet(
        onDismissRequest = {
            if (exportState !is ExportUiState.Exporting) onDismiss()
        },
        containerColor = SurfaceCard,
        dragHandle = { BottomSheetDefaults.DragHandle(color = OutlineHairline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.export_sheet_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = InkPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (activePreset != null) CyanContainer else SurfaceElevated,
                            border = BorderStroke(1.dp, if (activePreset != null) CyanWave else OutlineHairline)
                        ) {
                            Text(
                                text = activePreset?.shortLabel ?: "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activePreset != null) CyanWave else InkSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.export_sheet_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
                Icon(
                    Icons.Rounded.HighQuality,
                    contentDescription = null,
                    tint = AcidLime,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 1. One-Tap Social Platform Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.export_section_platforms),
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SocialPlatformExportPreset.entries.forEach { preset ->
                        val isSelected = activePreset == preset
                        val accent = socialPresetAccent(preset)
                        Surface(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 104.dp, minHeight = 60.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onApplySocialPreset(preset) }
                                .testTag("social_preset_${preset.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) accent.copy(alpha = 0.16f) else SurfaceElevated,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accent else OutlineHairline
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = socialPresetIcon(preset),
                                        contentDescription = null,
                                        tint = if (isSelected) accent else InkSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = preset.shortLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) accent else InkPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${preset.resolution.shortLabel} · ${preset.fps}fps · ${preset.bitrateTier.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 2. Aspect Ratio
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
                AspectRatioChipRow(
                    selected = project.aspectRatio,
                    onSelect = onSelectAspectRatio
                )
            }

            // 3. Resolution Menu (720p, 1080p, 2K, 4K UHD)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.export_section_resolution),
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary
                    )
                    Text(
                        text = "${outW}×${outH} px",
                        style = MaterialTheme.typography.labelSmall,
                        color = AcidLime,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportResolution.entries.forEach { res ->
                        val selected = project.exportResolution == res
                        val (rw, rh) = project.aspectRatio.dimensionsFor(res)
                        val is4K = res == ExportResolution.RES_4K
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 76.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectResolution(res) }
                                .testTag("resolution_${res.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) AcidLimeContainer else SurfaceElevated,
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) AcidLime else OutlineHairline
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = res.shortLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (selected) AcidLime else InkPrimary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (is4K) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(CoralPulse)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "UHD",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = OnCoral
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = res.tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) InkPrimary else InkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${rw}×${rh}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkMuted,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 4. Frame Rate Selector (30fps / 60fps)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.export_section_fps),
                    style = MaterialTheme.typography.labelMedium,
                    color = InkSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30 to "30 fps · Cinema", 60 to "60 fps · Smooth").forEach { (fpsValue, label) ->
                        val selected = project.exportFps == fpsValue
                        Surface(
                            modifier = Modifier
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectFps(fpsValue) }
                                .testTag("fps_$fpsValue"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) CyanContainer else SurfaceElevated,
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) CyanWave else OutlineHairline
                            )
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) CyanWave else InkSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // 5. Bitrate Tier Selector (Standard, High, Master)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.export_section_bitrate),
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary
                    )
                    Text(
                        text = "$outMbps Mbps · $estSizeLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanWave,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportBitrateTier.entries.forEach { tier ->
                        val selected = project.exportBitrateTier == tier
                        val tierMbps = resolveExportBitrateMbps(project.exportResolution, tier, project.exportFps)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 76.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectBitrateTier(tier) }
                                .testTag("bitrate_tier_${tier.name.lowercase()}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) CyanContainer else SurfaceElevated,
                            border = BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) CyanWave else OutlineHairline
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = tier.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) CyanWave else InkPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$tierMbps Mbps",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) InkPrimary else AcidLime,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = tier.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Live Output Spec & Estimated Size Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, CyanWave.copy(alpha = 0.4f)),
                modifier = Modifier.testTag("export_estimate_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SdStorage,
                        contentDescription = null,
                        tint = CyanWave,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${project.exportResolution.label} · ${project.exportFps}fps · $outMbps Mbps",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (project.exportResolution == ExportResolution.RES_4K) {
                                "Estimated $estSizeLabel ($durationLabel) · Auto-adapts if hardware caps 4K"
                            } else {
                                "Estimated $estSizeLabel for $durationLabel · H.264 High + 192k AAC"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                }
            }

            // Pro Clarity / iPhone Smart-HDR Tone Polish Switch
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, OutlineHairline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "iPhone-Grade Smart Clarity",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkPrimary
                        )
                        Text(
                            text = "Crisp micro-contrast & Rec.709 color so Reels/Shorts stay sharp after upload",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = project.proClarityEnabled,
                        onCheckedChange = onToggleProClarity,
                        modifier = Modifier.testTag("pro_clarity_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBg,
                            checkedTrackColor = AcidLime
                        )
                    )
                }
            }

            // Render Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, OutlineHairline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryRow("Output Spec", "${outW}×${outH} · ${project.exportFps}fps · ${outMbps}Mbps")
                    SummaryRow("Video Length", "$durationLabel ($estSizeLabel)")
                    SummaryRow("Timeline Clips", "${project.clips.size.coerceAtLeast(1)} clips · ${project.transitionPreset.displayName}")
                    SummaryRow("Auto-Captions", if (project.autoCaptionsEnabled) "${project.subtitles.size} cues (${project.subtitleStyle.displayName})" else "Off")
                    SummaryRow("Filter & Effect", "${project.filterPreset.displayName} / ${project.effectPreset.displayName}")
                    SummaryRow(
                        "Audio Mix",
                        buildString {
                            append(if (project.musicPreset != MusicTrackPreset.NONE) project.musicPreset.title else "No Music")
                            if (project.ttsSettings.enabled && project.ttsSettings.script.isNotBlank()) {
                                append(" + TTS Voice")
                            }
                            append(" (192k AAC)")
                        }
                    )
                }
            }

            Button(
                onClick = onStartExport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_start_export_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralPulse, contentColor = OnCoral)
            ) {
                Icon(Icons.Rounded.MovieCreation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Render ${project.exportResolution.shortLabel} ${project.exportFps}fps · ${project.exportBitrateTier.label} MP4",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

private fun socialPresetIcon(preset: SocialPlatformExportPreset): ImageVector = when (preset) {
    SocialPlatformExportPreset.INSTAGRAM_REELS -> Icons.Rounded.CameraAlt
    SocialPlatformExportPreset.YOUTUBE_SHORTS -> Icons.Rounded.PlayCircle
    SocialPlatformExportPreset.TIKTOK -> Icons.Rounded.MusicVideo
    SocialPlatformExportPreset.WHATSAPP -> Icons.Rounded.Chat
    SocialPlatformExportPreset.YOUTUBE_4K -> Icons.Rounded.Tv
}

private fun socialPresetAccent(preset: SocialPlatformExportPreset): Color = when (preset) {
    SocialPlatformExportPreset.INSTAGRAM_REELS -> CoralPulse
    SocialPlatformExportPreset.YOUTUBE_SHORTS -> ErrorCrimson
    SocialPlatformExportPreset.TIKTOK -> CyanWave
    SocialPlatformExportPreset.WHATSAPP -> AcidLime
    SocialPlatformExportPreset.YOUTUBE_4K -> AmberGold
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        Text(value, style = MaterialTheme.typography.labelSmall, color = InkPrimary)
    }
}
