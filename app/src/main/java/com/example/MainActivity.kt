package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.media.QuickShareCoordinator
import com.example.media.QuickShareTarget
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.ClipStudioViewModel
import com.example.viewmodel.MainTab
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            ClipPulseTheme {
                ClipPulseApp()
            }
        }
    }
}

@Composable
fun ClipPulseApp(
    viewModel: ClipStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiScope = rememberCoroutineScope()

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val inEditor by viewModel.inEditorScreen.collectAsStateWithLifecycle()
    val selectedTool by viewModel.selectedEditorTool.collectAsStateWithLifecycle()
    val youtubeUrl by viewModel.youtubeUrlInput.collectAsStateWithLifecycle()
    val targetDurationSec by viewModel.targetDurationSec.collectAsStateWithLifecycle()
    val selectedAspectRatio by viewModel.selectedAspectRatio.collectAsStateWithLifecycle()
    val captionLanguage by viewModel.captionLanguage.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playheadMs by viewModel.playheadMs.collectAsStateWithLifecycle()
    val isMusicPreviewing by viewModel.isMusicPreviewing.collectAsStateWithLifecycle()
    val isTtsPreviewing by viewModel.isTtsPreviewing.collectAsStateWithLifecycle()
    val isRegeneratingCaptions by viewModel.isRegeneratingCaptions.collectAsStateWithLifecycle()
    val isGeneratingAutoSubs by viewModel.isGeneratingAutoSubtitles.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    var showExportSheet by remember { mutableStateOf(false) }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.dismissBanner()
        }
    }

    // Zero-permission Android Photo Picker launchers (Google Play Policy compliant)
    val homeVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.createOfflineProjectFromLocalMedia(uri, isVideo = true)
    }

    val homePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.createOfflineProjectFromLocalMedia(uri, isVideo = false)
    }

    val addClipVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.addLocalClipToTimeline(uri, isVideo = true)
    }

    val addClipPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.addLocalClipToTimeline(uri, isVideo = false)
    }

    // Zero-permission SAF Audio Picker for custom background music
    val customAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = resolveDisplayName(context, uri) ?: "Custom Track"
            viewModel.setCustomMusicAudio(uri, name)
        }
    }

    fun showToastMessage(msg: String) {
        uiScope.launch {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    fun shareText(text: String) {
        val msg = QuickShareCoordinator.shareTextOnly(context, text, QuickShareTarget.SYSTEM_MORE)
        showToastMessage(msg)
    }

    fun shareVideoToTarget(uri: Uri, project: com.example.model.ClipProject, target: QuickShareTarget) {
        val msg = QuickShareCoordinator.shareVideo(context, uri, project, target)
        showToastMessage(msg)
    }

    fun openVideoInGallery(uri: Uri?) {
        val msg = QuickShareCoordinator.openInPhoneGallery(context, uri)
        showToastMessage(msg)
    }

    if (inEditor) {
        StudioEditorScreen(
            project = activeProject,
            isOnline = isOnline,
            isPlaying = isPlaying,
            playheadMs = playheadMs,
            selectedTool = selectedTool,
            isMusicPreviewing = isMusicPreviewing,
            isTtsPreviewing = isTtsPreviewing,
            isGeneratingAutoCaptions = isGeneratingAutoSubs,
            ttsLanguages = viewModel.ttsManager.supportedLanguages,
            snackbarHostState = snackbarHostState,
            onBack = { viewModel.closeEditor() },
            onSelectTool = { viewModel.selectEditorTool(it) },
            onTogglePlayback = { viewModel.togglePlayback() },
            onSeekTo = { viewModel.seekTo(it) },
            onUpdateTitle = { viewModel.updateProjectTitle(it) },
            onUpdateAspectRatio = { viewModel.updateAspectRatio(it) },
            onOpenExportSheet = {
                viewModel.resetExportState()
                showExportSheet = true
            },
            onAddLocalVideo = {
                addClipVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            },
            onAddLocalPhoto = {
                addClipPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onAddExtraSegment = { title, dur -> viewModel.addExtraYoutubeClipToTimeline(title, dur) },
            onApplyProjectLength = { viewModel.applyProjectLengthPreset(it) },
            onUpdateClipDuration = { id, sec -> viewModel.updateClipDurationSec(id, sec) },
            onMoveClip = { idx, delta -> viewModel.moveClip(idx, delta) },
            onRemoveClip = { viewModel.removeClip(it) },
            onToggleAutoCaptions = { viewModel.toggleAutoCaptions(it) },
            onSubtitleStyleChange = { viewModel.updateSubtitleStyle(it) },
            onSubtitleFontSizeChange = { viewModel.updateSubtitleFontSize(it) },
            onSubtitleYPosChange = { viewModel.updateSubtitleYPosition(it) },
            onAutoGenerateCaptions = { viewModel.autoGenerateSubtitles(it) },
            onAddSubtitleCue = { viewModel.addSubtitleCue(it) },
            onUpdateSubtitleCue = { id, txt, s, e -> viewModel.updateSubtitleCue(id, txt, s, e) },
            onDeleteSubtitleCue = { viewModel.deleteSubtitleCue(it) },
            onAddTextOverlay = { txt, tc, bc, style -> viewModel.addTextOverlay(txt, tc, bc, style) },
            onUpdateTextOverlay = { viewModel.updateTextOverlay(it) },
            onMoveTextOverlay = { id, dx, dy -> viewModel.moveTextOverlay(id, dx, dy) },
            onDeleteTextOverlay = { viewModel.deleteTextOverlay(it) },
            onUpdateTts = { viewModel.updateTtsSettings(it) },
            onFillTtsFromSubtitles = { viewModel.fillTtsFromSubtitles() },
            onPreviewTts = { viewModel.previewTtsVoiceover() },
            onStopTts = { viewModel.stopTtsPreview() },
            onSelectFilter = { viewModel.updateFilter(it) },
            onFilterIntensityChange = { viewModel.updateFilterIntensity(it) },
            onSelectEffect = { viewModel.updateEffect(it) },
            onEffectIntensityChange = { viewModel.updateEffectIntensity(it) },
            onSelectTransition = { viewModel.updateTransition(it) },
            onTransitionDurationChange = { viewModel.updateTransitionDuration(it) },
            onSelectMusicPreset = { viewModel.selectMusicPreset(it) },
            onToggleMusicPreview = { viewModel.toggleMusicPreview() },
            onPickCustomAudio = { customAudioPicker.launch(arrayOf("audio/*")) },
            onMusicVolumeChange = { viewModel.updateMusicVolume(it) }
        )

        if (showExportSheet) {
            ExportDialogSheet(
                project = activeProject,
                exportState = exportState,
                onDismiss = { showExportSheet = false },
                onSelectAspectRatio = { viewModel.updateAspectRatio(it) },
                onSelectQuality = { viewModel.updateExportQuality(it) },
                onStartExport = {
                    showExportSheet = false
                    viewModel.startExport()
                },
                onShareExportedVideo = { uri, _ -> shareVideoToTarget(uri, activeProject, QuickShareTarget.SYSTEM_MORE) },
                onOpenVideoPlayer = { uri -> openVideoInGallery(uri) }
            )
        }

        ExportProgressModal(
            project = activeProject,
            exportState = exportState,
            onCancelExport = { viewModel.cancelExport() },
            onRetryExport = { viewModel.startExport() },
            onDismiss = { viewModel.resetExportState() },
            onQuickShareTarget = { uri, target -> shareVideoToTarget(uri, activeProject, target) },
            onOpenInGallery = { uri -> openVideoInGallery(uri) }
        )
    } else {
        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceDark,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.CREATE,
                        onClick = { viewModel.selectTab(MainTab.CREATE) },
                        modifier = Modifier.testTag("nav_tab_create"),
                        icon = {
                            Icon(
                                if (selectedTab == MainTab.CREATE) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = stringResource(R.string.tab_create)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_create)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CoralPulse,
                            selectedTextColor = CoralPulse,
                            indicatorColor = CoralContainer,
                            unselectedIconColor = InkSecondary,
                            unselectedTextColor = InkSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.PROJECTS,
                        onClick = { viewModel.selectTab(MainTab.PROJECTS) },
                        modifier = Modifier.testTag("nav_tab_projects"),
                        icon = {
                            Icon(
                                if (selectedTab == MainTab.PROJECTS) Icons.Rounded.FolderOpen else Icons.Outlined.FolderOpen,
                                contentDescription = stringResource(R.string.tab_projects)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_projects)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AcidLime,
                            selectedTextColor = AcidLime,
                            indicatorColor = AcidLimeContainer,
                            unselectedIconColor = InkSecondary,
                            unselectedTextColor = InkSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.CAPTIONS,
                        onClick = { viewModel.selectTab(MainTab.CAPTIONS) },
                        modifier = Modifier.testTag("nav_tab_captions"),
                        icon = {
                            Icon(
                                if (selectedTab == MainTab.CAPTIONS) Icons.Rounded.Share else Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.tab_captions)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_captions)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanWave,
                            selectedTextColor = CyanWave,
                            indicatorColor = CyanContainer,
                            unselectedIconColor = InkSecondary,
                            unselectedTextColor = InkSecondary
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    MainTab.CREATE -> HomeCreateScreen(
                        isOnline = isOnline,
                        youtubeUrl = youtubeUrl,
                        targetDurationSec = targetDurationSec,
                        selectedAspectRatio = selectedAspectRatio,
                        captionLanguage = captionLanguage,
                        analysisState = analysisState,
                        onYoutubeUrlChange = { viewModel.updateYoutubeUrl(it) },
                        onTargetDurationChange = { viewModel.updateTargetDuration(it) },
                        onAspectRatioChange = { viewModel.updateDefaultAspectRatio(it) },
                        onCaptionLanguageChange = { viewModel.updateCaptionLanguage(it) },
                        onExtractViralClips = { viewModel.analyzeYoutubeLink() },
                        onPickLocalVideo = {
                            homeVideoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                        },
                        onPickLocalImage = {
                            homePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onOpenCandidate = { info, cand -> viewModel.openCandidateInEditor(info, cand) }
                    )
                    MainTab.PROJECTS -> ProjectsLibraryScreen(
                        projects = savedProjects,
                        isOnline = isOnline,
                        onOpenProject = { viewModel.openProjectInEditor(it) },
                        onDeleteProject = { viewModel.deleteProject(it) },
                        onQuickShareProject = { proj, target ->
                            val uriStr = proj.lastExportedUri
                            if (!uriStr.isNullOrBlank()) {
                                shareVideoToTarget(Uri.parse(uriStr), proj, target)
                            }
                        },
                        onOpenProjectInGallery = { uriStr ->
                            openVideoInGallery(Uri.parse(uriStr))
                        },
                        onGoToCreate = { viewModel.selectTab(MainTab.CREATE) }
                    )
                    MainTab.CAPTIONS -> SocialCaptionsScreen(
                        project = activeProject,
                        isOnline = isOnline,
                        isRegenerating = isRegeneratingCaptions,
                        onUpdateBundle = { viewModel.updateSocialCaptions(it) },
                        onRegenerateAi = { tone -> viewModel.regenerateSocialCaptions(tone) },
                        onQuickShareText = { shareText(it) },
                        onQuickShareTarget = { target, captionPayload ->
                            val exportedUriStr = activeProject.lastExportedUri
                            if (!exportedUriStr.isNullOrBlank()) {
                                shareVideoToTarget(Uri.parse(exportedUriStr), activeProject, target)
                            } else {
                                val msg = QuickShareCoordinator.shareTextOnly(context, captionPayload, target)
                                showToastMessage(msg)
                            }
                        },
                        onCopiedFeedback = { msg -> showToastMessage(msg) }
                    )
                }
            }
        }
    }
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()
}
