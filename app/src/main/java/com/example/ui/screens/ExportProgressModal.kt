package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import com.example.R
import com.example.media.ExportStage
import com.example.media.QuickShareCoordinator
import com.example.media.QuickShareTarget
import com.example.model.ClipProject
import com.example.model.SocialPlatformTab
import com.example.ui.components.QuickShareTargetStrip
import com.example.ui.components.formatDurationShort
import com.example.ui.theme.*
import com.example.viewmodel.ExportUiState
import kotlinx.coroutines.delay

@Composable
fun ExportProgressModal(
    project: ClipProject,
    exportState: ExportUiState,
    isGeneratingSocialCaptions: Boolean,
    onCancelExport: () -> Unit,
    onRetryExport: () -> Unit,
    onDismiss: () -> Unit,
    onQuickShareTarget: (Uri, QuickShareTarget) -> Unit,
    onOpenInGallery: (Uri) -> Unit,
    onGenerateAiCaptions: () -> Unit,
    onCopiedFeedback: (String) -> Unit
) {
    if (exportState is ExportUiState.Idle) return

    val isExporting = exportState is ExportUiState.Exporting

    Dialog(
        onDismissRequest = {
            if (!isExporting) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isExporting,
            dismissOnClickOutside = !isExporting,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 460.dp)
                .testTag("export_progress_modal"),
            shape = RoundedCornerShape(28.dp),
            color = SurfaceCard,
            tonalElevation = 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = when (exportState) {
                    is ExportUiState.Exporting -> CyanWave.copy(alpha = 0.45f)
                    is ExportUiState.Completed -> AcidLime.copy(alpha = 0.65f)
                    is ExportUiState.Error -> ErrorCrimson.copy(alpha = 0.7f)
                    ExportUiState.Idle -> OutlineHairline
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (exportState) {
                    is ExportUiState.Exporting -> ExportingModalContent(
                        project = project,
                        state = exportState,
                        onCancelExport = onCancelExport
                    )

                    is ExportUiState.Completed -> CompletedModalContent(
                        project = project,
                        state = exportState,
                        isGeneratingSocialCaptions = isGeneratingSocialCaptions,
                        onDismiss = onDismiss,
                        onQuickShareTarget = onQuickShareTarget,
                        onOpenInGallery = onOpenInGallery,
                        onGenerateAiCaptions = onGenerateAiCaptions,
                        onCopiedFeedback = onCopiedFeedback
                    )

                    is ExportUiState.Error -> ErrorModalContent(
                        errorMessage = exportState.message,
                        onRetryExport = onRetryExport,
                        onDismiss = onDismiss
                    )

                    ExportUiState.Idle -> Unit
                }
            }
        }
    }
}

@Composable
private fun ExportingModalContent(
    project: ClipProject,
    state: ExportUiState.Exporting,
    onCancelExport: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "export_progress_ring"
    )

    var elapsedSec by remember(state.startedAtMs) { mutableIntStateOf(0) }
    LaunchedEffect(state.startedAtMs) {
        while (true) {
            elapsedSec = ((System.currentTimeMillis() - state.startedAtMs) / 1000L).toInt().coerceAtLeast(0)
            delay(500L)
        }
    }

    // Header
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.export_modal_title_rendering),
            style = MaterialTheme.typography.headlineMedium,
            color = InkPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = project.title,
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    // Spec Chips Row
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExportSpecPill(project.aspectRatio.label, CoralContainer, CoralPulse)
        ExportSpecPill("${project.exportResolution.shortLabel} · ${project.exportFps}fps", CyanContainer, CyanWave)
        ExportSpecPill("${project.resolvedExportBitrateMbps()} Mbps", AcidLimeContainer, AcidLime)
    }

    // Circular Progress Ring Gauge
    Box(
        modifier = Modifier
            .size(144.dp)
            .testTag("export_progress_ring"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 11.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)

            drawArc(
                color = SurfaceBright,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(CoralPulse, CyanWave, AcidLime, CoralPulse)
                ),
                startAngle = -90f,
                sweepAngle = (animatedProgress * 360f).coerceAtLeast(6f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("export_progress_percentage")
            )
            Text(
                text = "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60),
                style = MaterialTheme.typography.labelSmall,
                color = AcidLime
            )
        }
    }

    // Live Step & Frame Counter + Linear Bar
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = state.stepLabel,
                style = MaterialTheme.typography.titleSmall,
                color = InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .testTag("export_status_message")
            )
            if (state.totalFrames > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${state.currentFrame}/${state.totalFrames} f",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanWave
                )
            }
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .testTag("export_linear_progress"),
            color = CoralPulse,
            trackColor = SurfaceBright
        )
    }

    // 4-Stage Video Processing Pipeline Checklist
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, OutlineHairline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val activeOrdinal = state.stage.ordinal
            ExportStage.entries.forEach { stage ->
                val isDone = stage.ordinal < activeOrdinal
                val isCurrent = stage.ordinal == activeOrdinal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        isDone -> Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Stage complete",
                            tint = AcidLime,
                            modifier = Modifier.size(18.dp)
                        )
                        isCurrent -> CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = CoralPulse
                        )
                        else -> Box(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(OutlineHairline)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stage.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                isDone -> AcidLime
                                isCurrent -> InkPrimary
                                else -> InkMuted
                            },
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isCurrent) {
                            Text(
                                text = stage.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    Text(
        text = stringResource(R.string.export_modal_keep_open_hint),
        style = MaterialTheme.typography.bodySmall,
        color = InkMuted,
        textAlign = TextAlign.Center
    )

    OutlinedButton(
        onClick = onCancelExport,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("cancel_export_button"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OutlineHairline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkSecondary)
    ) {
        Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.action_cancel_export), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun CompletedModalContent(
    project: ClipProject,
    state: ExportUiState.Completed,
    isGeneratingSocialCaptions: Boolean,
    onDismiss: () -> Unit,
    onQuickShareTarget: (Uri, QuickShareTarget) -> Unit,
    onOpenInGallery: (Uri) -> Unit,
    onGenerateAiCaptions: () -> Unit,
    onCopiedFeedback: (String) -> Unit
) {
    val context = LocalContext.current
    var copiedPost by remember { mutableStateOf(false) }
    LaunchedEffect(copiedPost) {
        if (copiedPost) {
            delay(1_800L)
            copiedPost = false
        }
    }
    val res = state.result
    val postPayload = project.socialCaptions.buildFullPost(
        platform = SocialPlatformTab.INSTAGRAM,
        fallbackTitle = project.title
    )
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(AcidLimeContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = stringResource(R.string.cd_export_complete),
            tint = AcidLime,
            modifier = Modifier.size(40.dp)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.export_modal_title_complete),
            style = MaterialTheme.typography.headlineMedium,
            color = AcidLime
        )
        Text(
            text = if (res.savedToGallery) {
                stringResource(R.string.gallery_saved_badge)
            } else {
                "Rendered and ready to share"
            },
            style = MaterialTheme.typography.bodySmall,
            color = CyanWave,
            textAlign = TextAlign.Center
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, OutlineHairline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            ModalDetailRow("File", res.fileName)
            ModalDetailRow("Resolution", res.resolutionLabel)
            ModalDetailRow("Duration", formatDurationShort(res.durationSec))
        }
    }

    OutlinedButton(
        onClick = { onOpenInGallery(res.outputUri) },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("open_in_gallery_button"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, CyanWave.copy(alpha = 0.75f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanWave)
    ) {
        Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_open_gallery), style = MaterialTheme.typography.labelLarge)
    }

    // Viral Caption & Hashtags Quick-Copy Card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = AcidLime,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.export_viral_post_ready),
                        style = MaterialTheme.typography.labelMedium,
                        color = AcidLime,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${project.socialCaptions.hashtags.size} #tags",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanWave
                )
            }

            Text(
                text = postPayload.ifBlank { project.title },
                style = MaterialTheme.typography.bodySmall,
                color = InkPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        QuickShareCoordinator.copyToClipboard(context, "Viral Post", postPayload)
                        copiedPost = true
                        onCopiedFeedback("Viral caption & hashtags copied!")
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(44.dp)
                        .testTag("export_copy_viral_post_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AcidLime,
                        contentColor = ObsidianBg
                    )
                ) {
                    Icon(
                        imageVector = if (copiedPost) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (copiedPost) "Copied!" else stringResource(R.string.action_copy_post_export),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onGenerateAiCaptions,
                    enabled = !isGeneratingSocialCaptions,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(44.dp)
                        .testTag("export_regenerate_ai_captions_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    border = BorderStroke(1.dp, CoralPulse.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralPulse)
                ) {
                    if (isGeneratingSocialCaptions) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = CoralPulse
                        )
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("AI Refresh", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.label_quick_share_apps),
            style = MaterialTheme.typography.labelSmall,
            color = InkSecondary
        )
        QuickShareTargetStrip(
            onShareTarget = { target -> onQuickShareTarget(res.outputUri, target) },
            includeMoreOption = true
        )
    }

    TextButton(
        onClick = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("done_export_modal_button")
    ) {
        Text("Done", style = MaterialTheme.typography.labelLarge, color = InkSecondary)
    }
}

@Composable
private fun ErrorModalContent(
    errorMessage: String,
    onRetryExport: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(CoralContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = stringResource(R.string.cd_export_failed),
            tint = ErrorCrimson,
            modifier = Modifier.size(38.dp)
        )
    }

    Text(
        text = stringResource(R.string.export_modal_title_error),
        style = MaterialTheme.typography.headlineMedium,
        color = InkPrimary
    )

    Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = InkSecondary,
        textAlign = TextAlign.Center
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("close_export_error_button"),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, OutlineHairline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = InkSecondary)
        ) {
            Text("Close")
        }
        Button(
            onClick = onRetryExport,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("retry_export_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralPulse, contentColor = OnCoral)
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun ExportSpecPill(text: String, bg: Color, fg: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ModalDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = InkPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
