package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AspectRatioPreset
import com.example.model.ClipProject
import com.example.model.ExportQuality
import com.example.model.MusicTrackPreset
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
    onSelectQuality: (ExportQuality) -> Unit,
    onStartExport: () -> Unit,
    onShareExportedVideo: (Uri, String) -> Unit,
    onOpenVideoPlayer: (Uri) -> Unit
) {
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
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Export High-Quality MP4",
                        style = MaterialTheme.typography.headlineMedium,
                        color = InkPrimary
                    )
                    Text(
                        text = "Offline on-device H.264 + AAC renderer · Saves to Movies/ClipPulse",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
                Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = AcidLime, modifier = Modifier.size(28.dp))
            }

            Text("Aspect Ratio", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
            AspectRatioChipRow(
                selected = project.aspectRatio,
                onSelect = onSelectAspectRatio
            )

            Text("Resolution & Quality", style = MaterialTheme.typography.labelMedium, color = InkSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExportQuality.entries.forEach { q ->
                    val selected = project.exportQuality == q
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectQuality(q) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quality_${q.name.lowercase()}"),
                        label = {
                            Text(
                                text = q.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceElevated,
                            selectedContainerColor = AcidLimeContainer,
                            selectedLabelColor = AcidLime
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
                    SummaryRow("Video Length", formatDurationShort((project.totalDurationMs / 1000L).toInt()))
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
                    text = "Render & Save ${project.aspectRatio.label} Video",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(6.dp))
        }
    }
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
