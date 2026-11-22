package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.media.QuickShareTarget
import com.example.media.TtsLanguageOption
import com.example.media.VideoFilterRenderer
import com.example.model.*
import com.example.ui.components.ViralCaptionStudioContent
import com.example.ui.components.formatDurationShort
import com.example.ui.components.formatTimecode
import com.example.ui.theme.*
import com.example.viewmodel.EditorToolTab
import com.example.viewmodel.SocialCaptionUiState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun StudioEditorScreen(
    project: ClipProject,
    isOnline: Boolean,
    isPlaying: Boolean,
    playheadMs: Long,
    selectedTool: EditorToolTab,
    isMusicPreviewing: Boolean,
    isTtsPreviewing: Boolean,
    isGeneratingAutoCaptions: Boolean,
    isRegeneratingSocialCaptions: Boolean,
    socialCaptionState: SocialCaptionUiState,
    ttsLanguages: List<TtsLanguageOption>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSelectTool: (EditorToolTab) -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateAspectRatio: (AspectRatioPreset) -> Unit,
    onOpenExportSheet: () -> Unit,
    onAddLocalVideo: () -> Unit,
    onAddLocalPhoto: () -> Unit,
    onAddExtraSegment: (String, Int) -> Unit,
    onApplyProjectLength: (Int) -> Unit,
    onUpdateClipDuration: (String, Int) -> Unit,
    onMoveClip: (Int, Int) -> Unit,
    onRemoveClip: (String) -> Unit,
    onToggleAutoCaptions: (Boolean) -> Unit,
    onSubtitleStyleChange: (SubtitleStylePreset) -> Unit,
    onSubtitleFontSizeChange: (Float) -> Unit,
    onSubtitleYPosChange: (Float) -> Unit,
    onAutoGenerateCaptions: (String?) -> Unit,
    onAddSubtitleCue: (String) -> Unit,
    onUpdateSubtitleCue: (String, String, Long, Long) -> Unit,
    onDeleteSubtitleCue: (String) -> Unit,
    onAddTextOverlay: (String, Int, Int, OverlayFontStyle) -> Unit,
    onUpdateTextOverlay: (TextOverlayItem) -> Unit,
    onMoveTextOverlay: (String, Float, Float) -> Unit,
    onDeleteTextOverlay: (String) -> Unit,
    onUpdateTts: ((TtsSettings) -> TtsSettings) -> Unit,
    onFillTtsFromSubtitles: () -> Unit,
    onPreviewTts: () -> Unit,
    onStopTts: () -> Unit,
    onSelectFilter: (VideoFilterPreset) -> Unit,
    onFilterIntensityChange: (Float) -> Unit,
    onSelectEffect: (VideoEffectPreset) -> Unit,
    onEffectIntensityChange: (Float) -> Unit,
    onSelectTransition: (TransitionPreset) -> Unit,
    onTransitionDurationChange: (Float) -> Unit,
    onUpdateClipTransition: (String, TransitionPreset) -> Unit,
    onUpdateClipTransitionDuration: (String, Float) -> Unit,
    onApplyTransitionToAll: (TransitionPreset, Float) -> Unit,
    onPreviewSpliceTransition: (Int) -> Unit,
    onSelectMusicPreset: (MusicTrackPreset) -> Unit,
    onToggleMusicPreview: () -> Unit,
    onPickCustomAudio: () -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onUpdateSocialCaptions: (SocialCaptionBundle) -> Unit,
    onGenerateSocialCaptions: (String, String) -> Unit,
    onDismissSocialCaptionError: () -> Unit,
    onQuickShareCaptionText: (String) -> Unit,
    onQuickShareCaptionTarget: (QuickShareTarget, String) -> Unit,
    onCopiedFeedback: (String) -> Unit
) {
    BackHandler { onBack() }

    var showTitleDialog by remember { mutableStateOf(false) }
    var draftTitle by remember(project.title) { mutableStateOf(project.title) }
    var activeSpliceSheetIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = InkPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showTitleDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${project.aspectRatio.label} · ${formatDurationShort((project.totalDurationMs / 1000L).toInt())} · Tap to rename",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanWave
                    )
                }

                // Quick Aspect Ratio Cycle Chip
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val entries = AspectRatioPreset.entries
                            val nextIdx = (entries.indexOf(project.aspectRatio) + 1) % entries.size
                            onUpdateAspectRatio(entries[nextIdx])
                        }
                        .testTag("editor_aspect_toggle"),
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceElevated,
                    border = BorderStroke(1.dp, OutlineHairline)
                ) {
                    Text(
                        text = project.aspectRatio.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = AcidLime,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                // Quick Resolution & Bitrate Menu Badge
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenExportSheet() }
                        .testTag("editor_quality_badge"),
                    shape = RoundedCornerShape(10.dp),
                    color = CyanContainer,
                    border = BorderStroke(1.dp, CyanWave.copy(alpha = 0.65f))
                ) {
                    Text(
                        text = "${project.exportResolution.shortLabel}·${project.exportFps}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanWave,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }

                Spacer(Modifier.width(6.dp))

                Button(
                    onClick = onOpenExportSheet,
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("editor_export_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPulse, contentColor = OnCoral)
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBg)
        ) {
            // 1. Live Interactive Aspect-Ratio Video Canvas Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(258.dp)
                    .background(Color(0xFF07080B))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                LiveStudioPreviewViewport(
                    project = project,
                    playheadMs = playheadMs,
                    onMoveTextOverlay = onMoveTextOverlay
                )
            }

            // 2. Transport Controls + Multi-Clip Scrubber Strip
            Surface(
                color = SurfaceDark,
                border = BorderStroke(0.5.dp, OutlineHairline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onTogglePlayback,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CoralPulse)
                                .testTag("editor_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_pause),
                                tint = OnCoral
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTimecode(playheadMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AcidLime
                                )
                                Text(
                                    text = formatTimecode(project.totalDurationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary
                                )
                            }
                            Slider(
                                value = playheadMs.toFloat(),
                                onValueChange = { onSeekTo(it.toLong()) },
                                valueRange = 0f..project.totalDurationMs.coerceAtLeast(1_000L).toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = CoralPulse,
                                    activeTrackColor = CoralPulse,
                                    inactiveTrackColor = SurfaceBright
                                )
                            )
                        }
                    }

                    // Compact Multi-Clip Sequence Visual Ribbon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        project.clips.forEachIndexed { idx, clip ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceElevated,
                                border = BorderStroke(1.dp, OutlineHairline)
                            ) {
                                Text(
                                    text = "C${idx + 1} · ${formatDurationShort((clip.effectiveDurationMs / 1000L).toInt())}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (idx < project.clips.lastIndex) {
                                val splicePreset = project.resolvedTransitionAfter(idx)
                                val isCustomSplice = clip.transitionAfter != null || clip.transitionDurationSec != null
                                Surface(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { activeSpliceSheetIndex = idx }
                                        .testTag("ribbon_splice_transition_$idx"),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (splicePreset == TransitionPreset.NONE) SurfaceCard else CyanContainer,
                                    border = BorderStroke(
                                        width = if (isCustomSplice) 1.5.dp else 1.dp,
                                        color = if (splicePreset == TransitionPreset.NONE) OutlineHairline else CyanWave.copy(alpha = 0.8f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = transitionPresetIcon(splicePreset),
                                            contentDescription = stringResource(R.string.cd_edit_splice_transition),
                                            tint = if (splicePreset == TransitionPreset.NONE) InkSecondary else CyanWave,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = splicePreset.shortLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (splicePreset == TransitionPreset.NONE) InkSecondary else CyanWave,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddLocalVideo() }
                                .testTag("ribbon_add_clip_button"),
                            shape = RoundedCornerShape(8.dp),
                            color = AcidLimeContainer,
                            border = BorderStroke(1.dp, AcidLime.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = AcidLime, modifier = Modifier.size(14.dp))
                                Text("Add Clip", style = MaterialTheme.typography.labelSmall, color = AcidLime)
                            }
                        }
                    }
                }
            }

            // 3. Tool Dock Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceCard)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorToolTab.entries.forEach { tab ->
                    val active = tab == selectedTool
                    val icon = when (tab) {
                        EditorToolTab.CLIPS -> Icons.Rounded.VideoLibrary
                        EditorToolTab.CAPTIONS -> Icons.Rounded.ClosedCaption
                        EditorToolTab.AI_POST -> Icons.Rounded.AutoAwesome
                        EditorToolTab.TEXT -> Icons.Rounded.TextFields
                        EditorToolTab.TTS -> Icons.Rounded.RecordVoiceOver
                        EditorToolTab.FILTERS -> Icons.Rounded.ColorLens
                        EditorToolTab.EFFECTS -> Icons.Rounded.AutoFixHigh
                        EditorToolTab.TRANSITIONS -> Icons.Rounded.CompareArrows
                        EditorToolTab.MUSIC -> Icons.Rounded.MusicNote
                    }
                    Surface(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectTool(tab) }
                            .testTag("tool_tab_${tab.name.lowercase()}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) CoralContainer else SurfaceElevated,
                        border = BorderStroke(
                            width = if (active) 1.5.dp else 1.dp,
                            color = if (active) CoralPulse else OutlineHairline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (active) CoralPulse else InkSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) InkPrimary else InkSecondary,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 4. Active Tool Panel Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                when (selectedTool) {
                    EditorToolTab.CLIPS -> ClipsToolPanel(
                        project = project,
                        onAddLocalVideo = onAddLocalVideo,
                        onAddLocalPhoto = onAddLocalPhoto,
                        onAddExtraSegment = onAddExtraSegment,
                        onApplyProjectLength = onApplyProjectLength,
                        onUpdateClipDuration = onUpdateClipDuration,
                        onMoveClip = onMoveClip,
                        onRemoveClip = onRemoveClip,
                        onOpenSpliceTransition = { idx -> activeSpliceSheetIndex = idx },
                        onPreviewSplice = onPreviewSpliceTransition
                    )
                    EditorToolTab.CAPTIONS -> CaptionsToolPanel(
                        project = project,
                        isOnline = isOnline,
                        isGenerating = isGeneratingAutoCaptions,
                        onToggleAutoCaptions = onToggleAutoCaptions,
                        onStyleChange = onSubtitleStyleChange,
                        onFontSizeChange = onSubtitleFontSizeChange,
                        onYPosChange = onSubtitleYPosChange,
                        onAutoGenerate = onAutoGenerateCaptions,
                        onAddCue = onAddSubtitleCue,
                        onUpdateCue = onUpdateSubtitleCue,
                        onDeleteCue = onDeleteSubtitleCue
                    )
                    EditorToolTab.AI_POST -> ViralCaptionStudioContent(
                        project = project,
                        isOnline = isOnline,
                        isRegenerating = isRegeneratingSocialCaptions,
                        captionState = socialCaptionState,
                        onUpdateBundle = onUpdateSocialCaptions,
                        onGenerateAi = onGenerateSocialCaptions,
                        onDismissError = onDismissSocialCaptionError,
                        onQuickShareText = onQuickShareCaptionText,
                        onQuickShareTarget = onQuickShareCaptionTarget,
                        onCopiedFeedback = onCopiedFeedback,
                        compactMode = true
                    )
                    EditorToolTab.TEXT -> TextOverlayToolPanel(
                        project = project,
                        onAddOverlay = onAddTextOverlay,
                        onUpdateOverlay = onUpdateTextOverlay,
                        onDeleteOverlay = onDeleteTextOverlay
                    )
                    EditorToolTab.TTS -> TtsVoiceoverToolPanel(
                        ttsSettings = project.ttsSettings,
                        languages = ttsLanguages,
                        isPreviewing = isTtsPreviewing,
                        onUpdateTts = onUpdateTts,
                        onFillFromSubtitles = onFillTtsFromSubtitles,
                        onPreviewTts = onPreviewTts,
                        onStopTts = onStopTts
                    )
                    EditorToolTab.FILTERS -> FiltersToolPanel(
                        selectedFilter = project.filterPreset,
                        intensity = project.filterIntensity,
                        onSelectFilter = onSelectFilter,
                        onIntensityChange = onFilterIntensityChange
                    )
                    EditorToolTab.EFFECTS -> EffectsToolPanel(
                        selectedEffect = project.effectPreset,
                        intensity = project.effectIntensity,
                        onSelectEffect = onSelectEffect,
                        onIntensityChange = onEffectIntensityChange
                    )
                    EditorToolTab.TRANSITIONS -> TransitionsToolPanel(
                        project = project,
                        onSelectGlobalTransition = onSelectTransition,
                        onGlobalDurationChange = onTransitionDurationChange,
                        onSelectClipTransition = onUpdateClipTransition,
                        onClipDurationChange = onUpdateClipTransitionDuration,
                        onApplyToAll = onApplyTransitionToAll,
                        onPreviewSplice = onPreviewSpliceTransition,
                        onAddSecondClip = onAddLocalVideo
                    )
                    EditorToolTab.MUSIC -> MusicToolPanel(
                        selectedPreset = project.musicPreset,
                        customMusicName = project.customMusicName,
                        musicVolume = project.musicVolume,
                        isPreviewing = isMusicPreviewing,
                        onSelectPreset = onSelectMusicPreset,
                        onTogglePreview = onToggleMusicPreview,
                        onPickCustomAudio = onPickCustomAudio,
                        onVolumeChange = onMusicVolumeChange
                    )
                }
            }
        }
    }

    if (showTitleDialog) {
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            containerColor = SurfaceCard,
            title = { Text("Rename Project", color = InkPrimary) },
            text = {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    singleLine = true,
                    label = { Text("Project Title") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (draftTitle.isNotBlank()) onUpdateTitle(draftTitle.trim())
                        showTitleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPulse)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) { Text("Cancel") }
            }
        )
    }

    val spliceIdx = activeSpliceSheetIndex
    if (spliceIdx != null && spliceIdx in 0 until project.clips.lastIndex) {
        SpliceTransitionSelectorSheet(
            project = project,
            spliceIndex = spliceIdx,
            onDismiss = { activeSpliceSheetIndex = null },
            onSelectTransition = onUpdateClipTransition,
            onDurationChange = onUpdateClipTransitionDuration,
            onApplyToAll = onApplyTransitionToAll,
            onPreviewSplice = onPreviewSpliceTransition
        )
    }
}

@Composable
private fun LiveStudioPreviewViewport(
    project: ClipProject,
    playheadMs: Long,
    onMoveTextOverlay: (String, Float, Float) -> Unit
) {
    var viewportSize by remember { mutableStateOf(IntSize(1, 1)) }
    val timeSec = playheadMs / 1000f

    // Determine active clip, incoming next clip, and per-splice transition progress
    val clips = project.clips
    var activeIdx = 0
    var activeClip: TimelineClipItem? = clips.firstOrNull()
    var nextClip: TimelineClipItem? = null
    var activeTransition = project.transitionPreset
    var transitionProgress = 0f
    var acc = 0L
    for (i in clips.indices) {
        val end = acc + clips[i].effectiveDurationMs
        if (playheadMs < end || i == clips.lastIndex) {
            activeIdx = i
            activeClip = clips[i]
            nextClip = clips.getOrNull(i + 1)
            activeTransition = project.resolvedTransitionAfter(i)
            val transMs = (project.resolvedTransitionDurationSec(i) * 1000f).toLong().coerceIn(200L, 2_000L)
            val remaining = (end - playheadMs).coerceAtLeast(0L)
            if (nextClip != null && activeTransition != TransitionPreset.NONE && remaining < transMs) {
                transitionProgress = (1f - (remaining.toFloat() / transMs.toFloat())).coerceIn(0f, 1f)
            }
            break
        }
        acc = end
    }

    val previewModel = activeClip?.thumbnailUrl?.ifBlank { project.coverThumbnailUrl }
        ?: project.coverThumbnailUrl
    val nextPreviewModel = nextClip?.thumbnailUrl?.ifBlank { project.coverThumbnailUrl }
        ?: project.coverThumbnailUrl

    val colorMatrixFilter = remember(project.filterPreset, project.filterIntensity) {
        if (project.filterPreset == VideoFilterPreset.ORIGINAL || project.filterIntensity <= 0.01f) {
            null
        } else {
            ColorFilter.colorMatrix(
                ColorMatrix(VideoFilterRenderer.blendedColorMatrix(project.filterPreset, project.filterIntensity))
            )
        }
    }

    // Dynamic zoom & shake for preview
    val effectAmt = project.effectIntensity
    val dynScale = when (project.effectPreset) {
        VideoEffectPreset.ZOOM_PULSE -> 1.03f + abs(sin(timeSec * PI.toFloat() * 1.8f)) * 0.14f * effectAmt
        VideoEffectPreset.KINETIC_SHAKE -> 1.06f
        else -> 1.0f + 0.03f * sin(timeSec * 0.6f)
    }
    val shakeX = if (project.effectPreset == VideoEffectPreset.KINETIC_SHAKE) sin(timeSec * 38f) * 8f * effectAmt else 0f
    val shakeY = if (project.effectPreset == VideoEffectPreset.KINETIC_SHAKE) sin(timeSec * 47f) * 6f * effectAmt else 0f

    val isTransitioning = transitionProgress > 0f && nextClip != null && activeTransition != TransitionPreset.NONE
    val p = transitionProgress
    val vpW = viewportSize.width.toFloat().coerceAtLeast(1f)
    val vpH = viewportSize.height.toFloat().coerceAtLeast(1f)

    Box(
        modifier = Modifier
            .aspectRatio(project.aspectRatio.aspectRatio, matchHeightConstraintsFirst = project.aspectRatio.aspectRatio < 1f)
            .clip(RoundedCornerShape(14.dp))
            .clipToBounds()
            .background(SurfaceCard)
            .border(1.5.dp, OutlineHairline, RoundedCornerShape(14.dp))
            .onSizeChanged { viewportSize = it }
            .testTag("studio_preview_viewport")
    ) {
        @Composable
        fun PreviewClipFrame(
            model: String,
            clipIndex: Int,
            modifier: Modifier
        ) {
            if (model.isNotBlank()) {
                AsyncImage(
                    model = model,
                    contentDescription = "Live video preview",
                    contentScale = ContentScale.Crop,
                    colorFilter = colorMatrixFilter,
                    modifier = modifier
                )
            } else {
                val gradColors = if (clipIndex % 2 == 0) {
                    listOf(SurfaceElevated, CoralContainer.copy(alpha = 0.75f), CyanContainer.copy(alpha = 0.75f))
                } else {
                    listOf(SurfaceElevated, CyanContainer.copy(alpha = 0.85f), AcidLimeContainer.copy(alpha = 0.75f))
                }
                Box(
                    modifier = modifier.background(Brush.linearGradient(gradColors))
                )
            }
        }

        if (!isTransitioning) {
            PreviewClipFrame(
                model = previewModel,
                clipIndex = activeIdx,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = dynScale
                        scaleY = dynScale
                        translationX = shakeX
                        translationY = shakeY
                    }
            )
        } else {
            when (activeTransition) {
                TransitionPreset.CROSSFADE -> {
                    PreviewClipFrame(
                        model = previewModel,
                        clipIndex = activeIdx,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = dynScale
                                scaleY = dynScale
                                alpha = 1f - p
                            }
                    )
                    PreviewClipFrame(
                        model = nextPreviewModel,
                        clipIndex = activeIdx + 1,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = dynScale
                                scaleY = dynScale
                                alpha = p
                            }
                    )
                }
                TransitionPreset.SLIDE_LEFT -> {
                    PreviewClipFrame(
                        model = previewModel,
                        clipIndex = activeIdx,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = dynScale
                                scaleY = dynScale
                                translationX = -vpW * p
                            }
                    )
                    PreviewClipFrame(
                        model = nextPreviewModel,
                        clipIndex = activeIdx + 1,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = dynScale
                                scaleY = dynScale
                                translationX = vpW * (1f - p)
                            }
                    )
                }
                TransitionPreset.SLIDE_UP -> {
                    PreviewClipFrame(
                        model = previewModel,
                        clipIndex = activeIdx,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = dynScale
                                scaleY = dynScale
                                translationY = -vpH * p
                            }
                    )
                    PreviewClipFrame(
                        model = nextPreviewModel,
                        clipIndex = activeIdx + 1,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = dynScale
                                scaleY = dynScale
                                translationY = vpH * (1f - p)
                            }
                    )
                }
                TransitionPreset.ZOOM_IN -> {
                    val outScale = dynScale + p * 0.45f
                    val inScale = (dynScale * 0.85f) + p * 0.18f
                    PreviewClipFrame(
                        model = previewModel,
                        clipIndex = activeIdx,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = outScale
                                scaleY = outScale
                                alpha = 1f - p
                            }
                    )
                    PreviewClipFrame(
                        model = nextPreviewModel,
                        clipIndex = activeIdx + 1,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = inScale
                                scaleY = inScale
                                alpha = p
                            }
                    )
                }
                TransitionPreset.FLASH_WHITE -> {
                    val showNext = p >= 0.5f
                    PreviewClipFrame(
                        model = if (showNext) nextPreviewModel else previewModel,
                        clipIndex = if (showNext) activeIdx + 1 else activeIdx,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = dynScale
                                scaleY = dynScale
                            }
                    )
                }
                TransitionPreset.GLITCH_CUT -> {
                    val showNext = p >= 0.5f
                    val jitter = sin(p * 45f) * 18f
                    PreviewClipFrame(
                        model = if (showNext) nextPreviewModel else previewModel,
                        clipIndex = if (showNext) activeIdx + 1 else activeIdx,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = dynScale
                                scaleY = dynScale
                                translationX = jitter
                            }
                    )
                }
                TransitionPreset.WIPE_RIGHT -> {
                    val wipeShape = GenericShape { size, _ ->
                        moveTo(0f, 0f)
                        lineTo(size.width * p, 0f)
                        lineTo(size.width * p, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    PreviewClipFrame(
                        model = previewModel,
                        clipIndex = activeIdx,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = dynScale
                                scaleY = dynScale
                            }
                    )
                    PreviewClipFrame(
                        model = nextPreviewModel,
                        clipIndex = activeIdx + 1,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                clip = true
                                shape = wipeShape
                                scaleX = dynScale
                                scaleY = dynScale
                            }
                    )
                }
                TransitionPreset.NONE -> {
                    PreviewClipFrame(
                        model = previewModel,
                        clipIndex = activeIdx,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Live Video Effects & Transition Canvas Overlay (Flash & Glitch Bars)
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (project.effectPreset) {
                VideoEffectPreset.VIGNETTE -> {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f * effectAmt)),
                            center = center,
                            radius = size.maxDimension * 0.72f
                        )
                    )
                }
                VideoEffectPreset.RGB_GLITCH -> {
                    val y = (timeSec * 90f) % size.height
                    drawRect(CoralPulse.copy(alpha = 0.38f * effectAmt), Offset(0f, y), Size(size.width, 14f))
                    drawRect(CyanWave.copy(alpha = 0.35f * effectAmt), Offset(0f, (y + size.height * 0.45f) % size.height), Size(size.width, 10f))
                }
                VideoEffectPreset.RETRO_VHS -> {
                    var y = 0f
                    while (y < size.height) {
                        drawLine(Color.Black.copy(alpha = 0.28f * effectAmt), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                        y += 8f
                    }
                    drawRect(AmberGold.copy(alpha = 0.14f * effectAmt))
                }
                VideoEffectPreset.GLOW_BLOOM -> {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(CoralPulse.copy(alpha = 0.34f * effectAmt), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.38f),
                            radius = size.minDimension * 0.8f
                        )
                    )
                }
                else -> Unit
            }

            if (isTransitioning && activeTransition == TransitionPreset.FLASH_WHITE) {
                val flash = sin(p * PI.toFloat()).coerceIn(0f, 1f)
                drawRect(Color.White.copy(alpha = flash * 0.88f))
            }
            if (isTransitioning && activeTransition == TransitionPreset.GLITCH_CUT) {
                val y = size.height * p
                drawRect(CyanWave.copy(alpha = 0.55f), Offset(0f, y), Size(size.width, size.height * 0.07f))
                drawRect(CoralPulse.copy(alpha = 0.55f), Offset(0f, size.height - y), Size(size.width, size.height * 0.05f))
            }
        }

        // Draggable Custom Text Overlays
        project.textOverlays.filter { playheadMs in it.startMs..it.endMs && it.text.isNotBlank() }.forEach { overlay ->
            val currentOverlay by rememberUpdatedState(overlay)
            val offsetX = (overlay.xFraction - 0.5f) * viewportSize.width
            val offsetY = (overlay.yFraction - 0.5f) * viewportSize.height

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationX = offsetX
                        translationY = offsetY
                    }
                    .pointerInput(overlay.id, viewportSize) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val w = viewportSize.width.coerceAtLeast(1).toFloat()
                            val h = viewportSize.height.coerceAtLeast(1).toFloat()
                            onMoveTextOverlay(currentOverlay.id, dragAmount.x / w, dragAmount.y / h)
                        }
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(overlay.bgColorArgb))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (overlay.fontStyle == OverlayFontStyle.UPPER_STAMP) overlay.text.uppercase() else overlay.text,
                    color = Color(overlay.textColorArgb),
                    fontSize = (overlay.fontSizeSp * 0.72f).coerceIn(11f, 26f).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = when (overlay.fontStyle) {
                        OverlayFontStyle.MONO_TECH -> JetBrainsMonoFontFamily
                        OverlayFontStyle.CLEAN_SANS -> PlusJakartaFontFamily
                        else -> OutfitFontFamily
                    },
                    textAlign = TextAlign.Center
                )
            }
        }

        // Synchronized Automatic Subtitles Preview
        if (project.autoCaptionsEnabled) {
            val activeCue = project.subtitles.firstOrNull { playheadMs in it.startMs..it.endMs }
            if (activeCue != null) {
                val style = project.subtitleStyle
                val displayText = if (style.uppercase) activeCue.text.uppercase() else activeCue.text
                val yOffset = (project.subtitleYFraction - 0.5f) * viewportSize.height
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { translationY = yOffset }
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(style.bgColorArgb))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = displayText,
                        color = Color(style.highlightColorArgb),
                        fontSize = (project.subtitleFontSizeSp * 0.72f).coerceIn(11f, 24f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }

        // Top-left Ratio + Active Filter Pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
            shape = RoundedCornerShape(6.dp),
            color = ObsidianBg.copy(alpha = 0.72f)
        ) {
            Text(
                text = "${project.aspectRatio.label} · ${project.filterPreset.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = InkPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Top-right Live Transition Indicator Pill when crossing a splice
        if (isTransitioning) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                shape = RoundedCornerShape(6.dp),
                color = CyanContainer.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, CyanWave)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = transitionPresetIcon(activeTransition),
                        contentDescription = null,
                        tint = CyanWave,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = activeTransition.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanWave,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
