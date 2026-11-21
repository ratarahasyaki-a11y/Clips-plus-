package com.example.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ViralClipAiService
import com.example.data.ai.YouTubeMetadataService
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectRepository
import com.example.data.network.ConnectivityObserver
import com.example.media.ExportResult
import com.example.media.ExportStage
import com.example.media.Mp4VideoExporter
import com.example.media.ProceduralMusicEngine
import com.example.media.TtsVoiceoverManager
import com.example.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private const val TAG = "ClipStudioViewModel"

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data class Loading(val statusMessage: String) : AnalysisUiState
    data class Success(
        val videoInfo: YouTubeVideoInfo,
        val candidates: List<ViralClipCandidate>,
        val offlineGenerated: Boolean = false
    ) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(
        val progress: Float,
        val stage: ExportStage,
        val stepLabel: String,
        val currentFrame: Int = 0,
        val totalFrames: Int = 0,
        val startedAtMs: Long = System.currentTimeMillis()
    ) : ExportUiState
    data class Completed(val result: ExportResult) : ExportUiState
    data class Error(val message: String) : ExportUiState
}

enum class MainTab { CREATE, PROJECTS, CAPTIONS }

enum class EditorToolTab(val label: String) {
    CLIPS("Clips"),
    CAPTIONS("Captions"),
    TEXT("Text"),
    TTS("TTS Voice"),
    FILTERS("Filters"),
    EFFECTS("Effects"),
    TRANSITIONS("Transitions"),
    MUSIC("Music")
}

class ClipStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(AppDatabase.getInstance(application).projectDao())
    private val youtubeService = YouTubeMetadataService()
    private val aiService = ViralClipAiService()
    private val connectivityObserver = ConnectivityObserver(application)
    val musicEngine = ProceduralMusicEngine()
    val ttsManager = TtsVoiceoverManager(application)
    private val mp4Exporter = Mp4VideoExporter(application, musicEngine, ttsManager)

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), connectivityObserver.isCurrentlyOnline())

    val savedProjects: StateFlow<List<ClipProject>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedTab = MutableStateFlow(MainTab.CREATE)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _inEditorScreen = MutableStateFlow(false)
    val inEditorScreen: StateFlow<Boolean> = _inEditorScreen.asStateFlow()

    private val _selectedEditorTool = MutableStateFlow(EditorToolTab.CLIPS)
    val selectedEditorTool: StateFlow<EditorToolTab> = _selectedEditorTool.asStateFlow()

    private val _youtubeUrlInput = MutableStateFlow("")
    val youtubeUrlInput: StateFlow<String> = _youtubeUrlInput.asStateFlow()

    private val _targetDurationSec = MutableStateFlow(60)
    val targetDurationSec: StateFlow<Int> = _targetDurationSec.asStateFlow()

    private val _selectedAspectRatio = MutableStateFlow(AspectRatioPreset.RATIO_19_6)
    val selectedAspectRatio: StateFlow<AspectRatioPreset> = _selectedAspectRatio.asStateFlow()

    private val _captionLanguage = MutableStateFlow("English / Hinglish")
    val captionLanguage: StateFlow<String> = _captionLanguage.asStateFlow()

    private val _analysisState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _activeProject = MutableStateFlow(ClipProject())
    val activeProject: StateFlow<ClipProject> = _activeProject.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _isMusicPreviewing = MutableStateFlow(false)
    val isMusicPreviewing: StateFlow<Boolean> = _isMusicPreviewing.asStateFlow()

    private val _isTtsPreviewing = MutableStateFlow(false)
    val isTtsPreviewing: StateFlow<Boolean> = _isTtsPreviewing.asStateFlow()

    private val _isRegeneratingCaptions = MutableStateFlow(false)
    val isRegeneratingCaptions: StateFlow<Boolean> = _isRegeneratingCaptions.asStateFlow()

    private val _isGeneratingAutoSubtitles = MutableStateFlow(false)
    val isGeneratingAutoSubtitles: StateFlow<Boolean> = _isGeneratingAutoSubtitles.asStateFlow()

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var playbackJob: Job? = null
    private var saveJob: Job? = null

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun selectEditorTool(tool: EditorToolTab) {
        _selectedEditorTool.value = tool
    }

    fun updateYoutubeUrl(url: String) {
        _youtubeUrlInput.value = url
    }

    fun updateTargetDuration(sec: Int) {
        _targetDurationSec.value = sec.coerceIn(15, 180)
    }

    fun updateDefaultAspectRatio(ratio: AspectRatioPreset) {
        _selectedAspectRatio.value = ratio
    }

    fun updateCaptionLanguage(lang: String) {
        _captionLanguage.value = lang
    }

    fun dismissBanner() {
        _statusBanner.value = null
    }

    fun closeEditor() {
        stopPlayback()
        stopMusicPreview()
        stopTtsPreview()
        _inEditorScreen.value = false
    }

    fun openProjectInEditor(project: ClipProject) {
        stopPlayback()
        _activeProject.value = project
        _playheadMs.value = 0L
        _selectedEditorTool.value = EditorToolTab.CLIPS
        _inEditorScreen.value = true
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            _statusBanner.value = "Project deleted"
        }
    }

    /**
     * Online + Offline YouTube link analysis:
     * - Online: fetches YouTube oEmbed metadata and runs Firebase AI Logic (gemini-3.8-flash)
     *   to detect ranked viral clips, timed subtitles, TTS script, and social media captions.
     * - Offline: parses the YouTube URL locally and creates an offline-ready studio template
     *   so the user can edit, add local clips, TTS, filters, effects, music, and export immediately.
     */
    fun analyzeYoutubeLink() {
        val rawUrl = _youtubeUrlInput.value.trim()
        if (rawUrl.isBlank()) {
            _analysisState.value = AnalysisUiState.Error("Please paste a YouTube video link first.")
            return
        }

        val targetSec = _targetDurationSec.value
        val aspect = _selectedAspectRatio.value
        val language = _captionLanguage.value

        viewModelScope.launch {
            _analysisState.value = AnalysisUiState.Loading("Fetching YouTube video details…")
            val metaResult = youtubeService.fetchMetadata(rawUrl)
            val videoInfo = metaResult.getOrElse { err ->
                _analysisState.value = AnalysisUiState.Error(
                    err.message ?: "Invalid YouTube link. Try youtube.com/watch?v=... or youtu.be/..."
                )
                return@launch
            }

            if (!connectivityObserver.isCurrentlyOnline()) {
                // Full Offline Mode: construct offline editable clip cuts immediately
                val offlineCandidates = buildOfflineCandidates(videoInfo, targetSec)
                _analysisState.value = AnalysisUiState.Success(
                    videoInfo = videoInfo,
                    candidates = offlineCandidates,
                    offlineGenerated = true
                )
                _statusBanner.value = "Offline Studio Mode: clip draft ready — edit or add local videos!"
                return@launch
            }

            _analysisState.value = AnalysisUiState.Loading("Gemini AI is finding viral moments & auto-captions…")
            try {
                val candidates = aiService.extractViralClips(
                    videoInfo = videoInfo,
                    targetDurationSec = targetSec,
                    aspectRatioLabel = aspect.label,
                    captionLanguage = language
                )
                if (candidates.isEmpty()) {
                    _analysisState.value = AnalysisUiState.Error("No viral clips were returned. Please retry.")
                } else {
                    _analysisState.value = AnalysisUiState.Success(
                        videoInfo = videoInfo,
                        candidates = candidates,
                        offlineGenerated = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI viral clip extraction error", e)
                _analysisState.value = AnalysisUiState.Error(
                    e.localizedMessage ?: "AI analysis failed. Check connection and try again."
                )
            }
        }
    }

    private fun buildOfflineCandidates(videoInfo: YouTubeVideoInfo, targetSec: Int): List<ViralClipCandidate> {
        val durMs = targetSec * 1000L
        val baseScript = "Offline cut ready for ${videoInfo.title}. Edit captions, add local video clips, apply filters and effects, and export in high quality."
        val cues = aiService.buildOfflineAutoCaptions(baseScript, durMs)
        val endLabel = "%02d:%02d".format(targetSec / 60, targetSec % 60)
        return listOf(
            ViralClipCandidate(
                title = "${videoInfo.title.take(36)} — Cut #1",
                startTimeLabel = "00:00",
                endTimeLabel = endLabel,
                durationSec = targetSec,
                viralScore = 90,
                hookReason = "Offline draft cut ready for custom editing, local clips, TTS voiceover & export.",
                subtitles = cues,
                ttsScript = baseScript,
                socialCaptions = SocialCaptionBundle(
                    viralHook = "Wait until you see this moment!",
                    instagramCaption = "${videoInfo.title} — edited in ClipPulse Studio.",
                    youtubeShortsCaption = "${videoInfo.title} #Shorts",
                    tiktokCaption = "Watch till the end! ${videoInfo.title}",
                    twitterCaption = "Just cut this ${targetSec}s highlight from ${videoInfo.title}.",
                    hashtags = listOf("#ViralClips", "#Reels", "#Shorts", "#ClipPulse", "#Creator")
                )
            )
        )
    }

    fun openCandidateInEditor(videoInfo: YouTubeVideoInfo, candidate: ViralClipCandidate) {
        val durMs = candidate.durationSec * 1000L
        val initialClip = TimelineClipItem(
            title = candidate.title,
            sourceType = ClipSourceType.YOUTUBE_SEGMENT,
            sourceUri = videoInfo.url,
            thumbnailUrl = videoInfo.thumbnailUrl,
            durationMs = durMs,
            trimStartMs = 0L,
            trimEndMs = durMs,
            viralScore = candidate.viralScore,
            hookReason = candidate.hookReason,
            youtubeStartLabel = candidate.startTimeLabel,
            youtubeEndLabel = candidate.endTimeLabel
        )
        val project = ClipProject(
            title = candidate.title,
            youtubeUrl = videoInfo.url,
            youtubeVideoId = videoInfo.videoId,
            channelName = videoInfo.authorName,
            coverThumbnailUrl = videoInfo.thumbnailUrl,
            aspectRatio = _selectedAspectRatio.value,
            targetLengthSec = candidate.durationSec,
            clips = listOf(initialClip),
            autoCaptionsEnabled = true,
            subtitles = candidate.subtitles,
            ttsSettings = TtsSettings(
                enabled = false,
                script = candidate.ttsScript,
                languageTag = "en-US"
            ),
            socialCaptions = candidate.socialCaptions
        )
        _activeProject.value = project
        _playheadMs.value = 0L
        _selectedEditorTool.value = EditorToolTab.CLIPS
        _inEditorScreen.value = true
        persistProjectDebounced(project)
    }

    fun createOfflineProjectFromLocalMedia(uri: Uri, isVideo: Boolean) {
        viewModelScope.launch {
            val clipItem = buildLocalTimelineClip(uri, isVideo, index = 1)
            val targetSec = _targetDurationSec.value.coerceAtLeast((clipItem.effectiveDurationMs / 1000L).toInt().coerceIn(15, 180))
            val adjustedClip = clipItem.copy(
                durationMs = targetSec * 1000L,
                trimEndMs = targetSec * 1000L
            )
            val offlineScript = "Check out this custom highlight reel. Add your own captions, filters, effects, and music offline."
            val cues = aiService.buildOfflineAutoCaptions(offlineScript, adjustedClip.effectiveDurationMs)

            val project = ClipProject(
                title = "Local Cut ${System.currentTimeMillis() % 1000}",
                aspectRatio = _selectedAspectRatio.value,
                targetLengthSec = targetSec,
                clips = listOf(adjustedClip),
                autoCaptionsEnabled = true,
                subtitles = cues,
                ttsSettings = TtsSettings(enabled = false, script = offlineScript),
                socialCaptions = SocialCaptionBundle(
                    viralHook = "Wait for the transition at the end!",
                    instagramCaption = "Freshly edited clip with custom effects & subtitles.",
                    youtubeShortsCaption = "Custom Viral Cut #Shorts",
                    tiktokCaption = "Edited on mobile with custom filters & audio!",
                    twitterCaption = "New short-form edit ready to drop.",
                    hashtags = listOf("#Reels", "#Shorts", "#VideoEdit", "#ClipPulse")
                )
            )
            _activeProject.value = project
            _playheadMs.value = 0L
            _selectedEditorTool.value = EditorToolTab.CLIPS
            _inEditorScreen.value = true
            persistProjectDebounced(project)
        }
    }

    fun addLocalClipToTimeline(uri: Uri, isVideo: Boolean) {
        viewModelScope.launch {
            val nextIdx = _activeProject.value.clips.size + 1
            val newClip = buildLocalTimelineClip(uri, isVideo, nextIdx)
            updateProject { proj ->
                val updatedClips = proj.clips + newClip
                val totalSec = (updatedClips.sumOf { it.effectiveDurationMs } / 1000L).toInt().coerceIn(15, 180)
                proj.copy(clips = updatedClips, targetLengthSec = totalSec)
            }
            _statusBanner.value = "Added clip to timeline (${_activeProject.value.clips.size} clips)"
        }
    }

    fun addExtraYoutubeClipToTimeline(title: String, durationSec: Int) {
        val proj = _activeProject.value
        val durMs = durationSec.coerceIn(10, 120) * 1000L
        val extra = TimelineClipItem(
            title = title.ifBlank { "Bonus Clip #${proj.clips.size + 1}" },
            sourceType = ClipSourceType.YOUTUBE_SEGMENT,
            sourceUri = proj.youtubeUrl,
            thumbnailUrl = proj.coverThumbnailUrl,
            durationMs = durMs,
            trimStartMs = 0L,
            trimEndMs = durMs,
            viralScore = 91
        )
        updateProject { p ->
            val updated = p.clips + extra
            val totalSec = (updated.sumOf { it.effectiveDurationMs } / 1000L).toInt().coerceIn(15, 180)
            p.copy(clips = updated, targetLengthSec = totalSec)
        }
        _statusBanner.value = "Added segment to timeline"
    }

    private suspend fun buildLocalTimelineClip(uri: Uri, isVideo: Boolean, index: Int): TimelineClipItem =
        withContext(Dispatchers.IO) {
            val context = getApplication<Application>()
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            var detectedMs = if (isVideo) 30_000L else 15_000L
            if (isVideo) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val parsed = durStr?.toLongOrNull()
                    if (parsed != null && parsed >= 3_000L) {
                        detectedMs = parsed.coerceAtMost(180_000L)
                    }
                    retriever.release()
                }
            }
            TimelineClipItem(
                title = if (isVideo) "Video Clip #$index" else "Photo Scene #$index",
                sourceType = if (isVideo) ClipSourceType.LOCAL_VIDEO else ClipSourceType.LOCAL_IMAGE,
                sourceUri = uri.toString(),
                thumbnailUrl = uri.toString(),
                durationMs = detectedMs,
                trimStartMs = 0L,
                trimEndMs = detectedMs,
                viralScore = 94
            )
        }

    fun removeClip(clipId: String) {
        updateProject { p ->
            if (p.clips.size <= 1) return@updateProject p
            val updated = p.clips.filterNot { it.id == clipId }
            val totalSec = (updated.sumOf { it.effectiveDurationMs } / 1000L).toInt().coerceIn(15, 180)
            p.copy(clips = updated, targetLengthSec = totalSec)
        }
    }

    fun moveClip(index: Int, delta: Int) {
        updateProject { p ->
            val target = index + delta
            if (index !in p.clips.indices || target !in p.clips.indices) return@updateProject p
            val mutable = p.clips.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(target, item)
            p.copy(clips = mutable)
        }
    }

    fun updateClipDurationSec(clipId: String, newDurationSec: Int) {
        val durMs = newDurationSec.coerceIn(5, 180) * 1000L
        updateProject { p ->
            val updated = p.clips.map { clip ->
                if (clip.id == clipId) {
                    clip.copy(durationMs = maxOf(clip.durationMs, durMs), trimStartMs = 0L, trimEndMs = durMs)
                } else clip
            }
            val totalSec = (updated.sumOf { it.effectiveDurationMs } / 1000L).toInt().coerceIn(15, 180)
            p.copy(clips = updated, targetLengthSec = totalSec)
        }
    }

    fun applyProjectLengthPreset(targetSec: Int) {
        val clampedSec = targetSec.coerceIn(15, 180)
        val targetMs = clampedSec * 1000L
        updateProject { p ->
            val clips = if (p.clips.isEmpty()) {
                listOf(
                    TimelineClipItem(
                        title = p.title,
                        sourceType = ClipSourceType.YOUTUBE_SEGMENT,
                        thumbnailUrl = p.coverThumbnailUrl,
                        durationMs = targetMs,
                        trimStartMs = 0L,
                        trimEndMs = targetMs
                    )
                )
            } else {
                val perClipMs = (targetMs / p.clips.size).coerceAtLeast(5_000L)
                p.clips.map { c ->
                    c.copy(durationMs = maxOf(c.durationMs, perClipMs), trimStartMs = 0L, trimEndMs = perClipMs)
                }
            }
            // Rescale existing subtitles cleanly across new duration
            val oldTotal = p.totalDurationMs.coerceAtLeast(1L)
            val ratio = targetMs.toDouble() / oldTotal.toDouble()
            val rescaledSubs = p.subtitles.map { cue ->
                val newStart = (cue.startMs * ratio).toLong().coerceIn(0L, targetMs - 500L)
                val newEnd = (cue.endMs * ratio).toLong().coerceIn(newStart + 500L, targetMs)
                cue.copy(startMs = newStart, endMs = newEnd)
            }
            p.copy(targetLengthSec = clampedSec, clips = clips, subtitles = rescaledSubs)
        }
    }

    fun updateProjectTitle(title: String) = updateProject { it.copy(title = title) }
    fun updateAspectRatio(ratio: AspectRatioPreset) = updateProject { it.copy(aspectRatio = ratio) }
    fun updateExportQuality(quality: ExportQuality) = updateProject { it.copy(exportQuality = quality) }

    // Subtitles & Auto-Captions
    fun toggleAutoCaptions(enabled: Boolean) = updateProject { it.copy(autoCaptionsEnabled = enabled) }
    fun updateSubtitleStyle(style: SubtitleStylePreset) = updateProject { it.copy(subtitleStyle = style) }
    fun updateSubtitleFontSize(sizeSp: Float) = updateProject { it.copy(subtitleFontSizeSp = sizeSp.coerceIn(14f, 34f)) }
    fun updateSubtitleYPosition(yFraction: Float) = updateProject { it.copy(subtitleYFraction = yFraction.coerceIn(0.18f, 0.88f)) }

    fun updateSubtitleCue(cueId: String, newText: String, startMs: Long, endMs: Long) {
        updateProject { p ->
            p.copy(
                subtitles = p.subtitles.map { cue ->
                    if (cue.id == cueId) {
                        cue.copy(text = newText, startMs = startMs.coerceAtLeast(0L), endMs = endMs.coerceAtLeast(startMs + 500L))
                    } else cue
                }.sortedBy { it.startMs }
            )
        }
    }

    fun addSubtitleCue(text: String) {
        val proj = _activeProject.value
        val start = _playheadMs.value.coerceIn(0L, (proj.totalDurationMs - 2_000L).coerceAtLeast(0L))
        val end = (start + 3_500L).coerceAtMost(proj.totalDurationMs)
        val cue = SubtitleCue(startMs = start, endMs = end, text = text.ifBlank { "New viral caption line" })
        updateProject { p -> p.copy(subtitles = (p.subtitles + cue).sortedBy { it.startMs }) }
    }

    fun deleteSubtitleCue(cueId: String) {
        updateProject { p -> p.copy(subtitles = p.subtitles.filterNot { it.id == cueId }) }
    }

    fun autoGenerateSubtitles(customOfflineTranscript: String? = null) {
        val proj = _activeProject.value
        viewModelScope.launch {
            _isGeneratingAutoSubtitles.value = true
            try {
                if (!connectivityObserver.isCurrentlyOnline() || !customOfflineTranscript.isNullOrBlank()) {
                    val sourceText = customOfflineTranscript?.takeIf { it.isNotBlank() }
                        ?: proj.ttsSettings.script.takeIf { it.isNotBlank() }
                        ?: proj.title
                    val cues = aiService.buildOfflineAutoCaptions(sourceText, proj.totalDurationMs)
                    updateProject { it.copy(autoCaptionsEnabled = true, subtitles = cues) }
                    _statusBanner.value = "Auto-captions synced offline (${cues.size} cues)"
                } else {
                    val (cues, script) = aiService.generateAutoCaptions(
                        projectTitle = proj.title,
                        clipTitles = proj.clips.map { it.title },
                        totalDurationSec = (proj.totalDurationMs / 1000L).toInt().coerceAtLeast(15),
                        languageStyle = _captionLanguage.value
                    )
                    updateProject {
                        it.copy(
                            autoCaptionsEnabled = true,
                            subtitles = cues,
                            ttsSettings = if (it.ttsSettings.script.isBlank()) {
                                it.ttsSettings.copy(script = script)
                            } else it.ttsSettings
                        )
                    }
                    _statusBanner.value = "AI auto-captions generated (${cues.size} cues)"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Auto-caption generation failed", e)
                _statusBanner.value = e.localizedMessage ?: "Auto-caption generation failed"
            } finally {
                _isGeneratingAutoSubtitles.value = false
            }
        }
    }

    // Custom Text Overlays
    fun addTextOverlay(
        text: String,
        textColorArgb: Int = 0xFFFFFFFF.toInt(),
        bgColorArgb: Int = 0xCCFF3D5A.toInt(),
        fontStyle: OverlayFontStyle = OverlayFontStyle.BOLD_DISPLAY
    ) {
        val proj = _activeProject.value
        val item = TextOverlayItem(
            text = text.ifBlank { "WAIT FOR IT 🔥" },
            xFraction = 0.5f,
            yFraction = 0.22f,
            fontSizeSp = 22f,
            textColorArgb = textColorArgb,
            bgColorArgb = bgColorArgb,
            fontStyle = fontStyle,
            startMs = 0L,
            endMs = proj.totalDurationMs
        )
        updateProject { it.copy(textOverlays = it.textOverlays + item) }
    }

    fun updateTextOverlay(updated: TextOverlayItem) {
        updateProject { p ->
            p.copy(textOverlays = p.textOverlays.map { if (it.id == updated.id) updated else it })
        }
    }

    fun moveTextOverlay(overlayId: String, dxFraction: Float, dyFraction: Float) {
        updateProject { p ->
            p.copy(
                textOverlays = p.textOverlays.map { item ->
                    if (item.id == overlayId) {
                        item.copy(
                            xFraction = (item.xFraction + dxFraction).coerceIn(0.12f, 0.88f),
                            yFraction = (item.yFraction + dyFraction).coerceIn(0.10f, 0.88f)
                        )
                    } else item
                }
            )
        }
    }

    fun deleteTextOverlay(overlayId: String) {
        updateProject { p -> p.copy(textOverlays = p.textOverlays.filterNot { it.id == overlayId }) }
    }

    // TTS Voiceover (100% offline capable)
    fun updateTtsSettings(transform: (TtsSettings) -> TtsSettings) {
        updateProject { it.copy(ttsSettings = transform(it.ttsSettings)) }
    }

    fun fillTtsFromSubtitles() {
        val joined = _activeProject.value.subtitles.joinToString(". ") { it.text }
        if (joined.isNotBlank()) {
            updateTtsSettings { it.copy(script = joined, enabled = true) }
            _statusBanner.value = "Voiceover script filled from auto-captions"
        }
    }

    fun previewTtsVoiceover() {
        val tts = _activeProject.value.ttsSettings
        if (tts.script.isBlank()) {
            _statusBanner.value = "Enter a voiceover script first"
            return
        }
        _isTtsPreviewing.value = true
        val started = ttsManager.speakPreview(
            text = tts.script,
            languageTag = tts.languageTag,
            speechRate = tts.speechRate,
            pitch = tts.pitch
        ) {
            _isTtsPreviewing.value = false
        }
        if (!started) {
            _isTtsPreviewing.value = false
            _statusBanner.value = "Device TTS engine initializing — try again in a moment"
        }
    }

    fun stopTtsPreview() {
        ttsManager.stopSpeaking()
        _isTtsPreviewing.value = false
    }

    // Filters, Effects, Transitions
    fun updateFilter(preset: VideoFilterPreset) = updateProject { it.copy(filterPreset = preset) }
    fun updateFilterIntensity(value: Float) = updateProject { it.copy(filterIntensity = value.coerceIn(0f, 1f)) }
    fun updateEffect(preset: VideoEffectPreset) = updateProject { it.copy(effectPreset = preset) }
    fun updateEffectIntensity(value: Float) = updateProject { it.copy(effectIntensity = value.coerceIn(0f, 1f)) }
    fun updateTransition(preset: TransitionPreset) = updateProject { it.copy(transitionPreset = preset) }
    fun updateTransitionDuration(sec: Float) = updateProject { it.copy(transitionDurationSec = sec.coerceIn(0.2f, 2.0f)) }

    // Background Music
    fun selectMusicPreset(preset: MusicTrackPreset) {
        updateProject { it.copy(musicPreset = preset) }
        if (preset == MusicTrackPreset.NONE) {
            stopMusicPreview()
        } else if (preset != MusicTrackPreset.CUSTOM_AUDIO) {
            toggleMusicPreview(forceStart = true)
        }
    }

    fun setCustomMusicAudio(uri: Uri, displayName: String) {
        val context = getApplication<Application>()
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        stopMusicPreview()
        updateProject {
            it.copy(
                musicPreset = MusicTrackPreset.CUSTOM_AUDIO,
                customMusicUri = uri.toString(),
                customMusicName = displayName
            )
        }
        _statusBanner.value = "Custom audio selected: $displayName"
    }

    fun updateMusicVolume(vol: Float) = updateProject { it.copy(musicVolume = vol.coerceIn(0f, 1f)) }

    fun toggleMusicPreview(forceStart: Boolean = false) {
        val proj = _activeProject.value
        if (!forceStart && _isMusicPreviewing.value) {
            stopMusicPreview()
            return
        }
        if (proj.musicPreset == MusicTrackPreset.NONE || proj.musicPreset == MusicTrackPreset.CUSTOM_AUDIO) {
            stopMusicPreview()
            return
        }
        _isMusicPreviewing.value = true
        musicEngine.startPreview(viewModelScope, proj.musicPreset, proj.musicVolume) {
            _isMusicPreviewing.value = false
        }
    }

    fun stopMusicPreview() {
        musicEngine.stopPreview()
        _isMusicPreviewing.value = false
    }

    // Social Media Captions
    fun updateSocialCaptions(bundle: SocialCaptionBundle) {
        updateProject { it.copy(socialCaptions = bundle) }
    }

    fun regenerateSocialCaptions(tone: String) {
        val proj = _activeProject.value
        if (!connectivityObserver.isCurrentlyOnline()) {
            _statusBanner.value = "Offline mode: edit captions directly, or connect online for Gemini AI rewrite"
            return
        }
        viewModelScope.launch {
            _isRegeneratingCaptions.value = true
            try {
                val contextScript = proj.subtitles.joinToString(" ") { it.text }
                    .ifBlank { proj.ttsSettings.script }
                    .ifBlank { proj.title }
                val fresh = aiService.regenerateSocialCaptions(
                    projectTitle = proj.title,
                    subtitleContext = contextScript,
                    tone = tone
                )
                updateProject { it.copy(socialCaptions = fresh) }
                _statusBanner.value = "Fresh $tone social captions generated!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Social caption regeneration failed", e)
                _statusBanner.value = e.localizedMessage ?: "Failed to generate captions"
            } finally {
                _isRegeneratingCaptions.value = false
            }
        }
    }

    // Studio Playback Loop
    fun togglePlayback() {
        if (_isPlaying.value) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        stopPlayback()
        val proj = _activeProject.value
        _isPlaying.value = true
        if (proj.musicPreset != MusicTrackPreset.NONE && proj.musicPreset != MusicTrackPreset.CUSTOM_AUDIO) {
            toggleMusicPreview(forceStart = true)
        }
        if (proj.ttsSettings.enabled && proj.ttsSettings.script.isNotBlank() && _playheadMs.value < 1_000L) {
            previewTtsVoiceover()
        }
        playbackJob = viewModelScope.launch {
            val total = _activeProject.value.totalDurationMs.coerceAtLeast(3_000L)
            if (_playheadMs.value >= total - 150L) _playheadMs.value = 0L
            while (isActive && _isPlaying.value) {
                delay(50L)
                val next = _playheadMs.value + 50L
                val maxDur = _activeProject.value.totalDurationMs.coerceAtLeast(3_000L)
                if (next >= maxDur) {
                    _playheadMs.value = 0L
                } else {
                    _playheadMs.value = next
                }
            }
        }
    }

    fun stopPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        stopMusicPreview()
        stopTtsPreview()
    }

    fun seekTo(ms: Long) {
        val maxDur = _activeProject.value.totalDurationMs.coerceAtLeast(1_000L)
        _playheadMs.value = ms.coerceIn(0L, maxDur)
    }

    private var exportJob: Job? = null

    // Real MP4 Export
    fun startExport() {
        stopPlayback()
        exportJob?.cancel()
        val proj = _activeProject.value
        val startedAt = System.currentTimeMillis()
        exportJob = viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting(
                progress = 0.02f,
                stage = ExportStage.PREPARING,
                stepLabel = "Initializing studio exporter…",
                startedAtMs = startedAt
            )
            try {
                val result = mp4Exporter.exportProjectToMp4(proj) { update ->
                    _exportState.value = ExportUiState.Exporting(
                        progress = update.progress,
                        stage = update.stage,
                        stepLabel = update.statusMessage,
                        currentFrame = update.currentFrame,
                        totalFrames = update.totalFrames,
                        startedAtMs = startedAt
                    )
                }
                updateProject { it.copy(lastExportedUri = result.outputUri.toString()) }
                _exportState.value = ExportUiState.Completed(result)
            } catch (e: CancellationException) {
                _exportState.value = ExportUiState.Idle
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "MP4 export error", e)
                _exportState.value = ExportUiState.Error(
                    e.localizedMessage ?: "Export failed on this device codec."
                )
            } finally {
                exportJob = null
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        exportJob = null
        _exportState.value = ExportUiState.Idle
        _statusBanner.value = "Video export cancelled"
    }

    fun resetExportState() {
        exportJob?.cancel()
        exportJob = null
        _exportState.value = ExportUiState.Idle
    }

    private fun updateProject(transform: (ClipProject) -> ClipProject) {
        val updated = transform(_activeProject.value).copy(updatedAtMs = System.currentTimeMillis())
        _activeProject.value = updated
        persistProjectDebounced(updated)
    }

    private fun persistProjectDebounced(project: ClipProject) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350L)
            repository.saveProject(project)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
        musicEngine.stopPreview()
        ttsManager.release()
    }
}
