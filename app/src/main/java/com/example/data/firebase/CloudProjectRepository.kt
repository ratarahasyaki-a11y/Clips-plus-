package com.example.data.firebase

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.model.*
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Keep
@IgnoreExtraProperties
data class CloudClipProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Untitled Viral Cut",
    val youtubeUrl: String? = null,
    val youtubeVideoId: String? = null,
    val channelName: String? = null,
    val coverThumbnailUrl: String? = null,
    val aspectRatio: String = AspectRatioPreset.RATIO_19_6.name,
    val exportQuality: String = ExportQuality.PRO_IPHONE_MASTER.name,
    val exportResolution: String? = null,
    val exportBitrateTier: String? = null,
    val exportFps: Long? = null,
    val proClarityEnabled: Boolean = true,
    val targetLengthSec: Long = 60L,
    val autoCaptionsEnabled: Boolean = true,
    val subtitleStyle: String = SubtitleStylePreset.VIRAL_POP.name,
    val subtitleFontSizeSp: Double = 20.0,
    val subtitleYFraction: Double = 0.78,
    val filterPreset: String = VideoFilterPreset.ORIGINAL.name,
    val filterIntensity: Double = 0.85,
    val effectPreset: String = VideoEffectPreset.NONE.name,
    val effectIntensity: Double = 0.65,
    val transitionPreset: String = TransitionPreset.CROSSFADE.name,
    val transitionDurationSec: Double = 0.6,
    val musicPreset: String = MusicTrackPreset.PHONK_DRIVE.name,
    val customMusicName: String? = null,
    val musicVolume: Double = 0.45,
    val clips: List<Map<String, Any?>> = emptyList(),
    val subtitles: List<Map<String, Any?>> = emptyList(),
    val textOverlays: List<Map<String, Any?>> = emptyList(),
    val ttsSettings: Map<String, Any?> = emptyMap(),
    val socialCaptions: Map<String, Any?> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class CloudProjectRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val idPattern = Regex("^[a-zA-Z0-9_\\-]+$")

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun validateDocumentId(id: String, label: String): String {
        require(id.isNotBlank() && id.length <= 128 && idPattern.matches(id)) {
            "Invalid $label ID for Firestore document path."
        }
        return id
    }

    fun observeProjects(userId: String): Flow<List<ClipProject>> = flow {
        if (userId.isBlank() || auth.currentUser?.uid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/projects"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudClipProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertUserProfile(
        userId: String,
        displayName: String,
        photoUrl: String?
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user profile write." }
            validateDocumentId(uid, "user")

            val safeName = displayName.trim().ifBlank { "ClipPulse Creator" }.take(80)
            val safePhoto = photoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500)
            val docRef = db.collection("users").document(uid)
            val existingSnap = docRef.get().await()

            if (existingSnap.exists()) {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safePhoto != null) {
                    updatePayload["photoUrl"] = safePhoto
                }
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "photoUrl" to safePhoto,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertProject(userId: String, project: ClipProject): Result<Unit> {
        val safeProjectId = project.id.ifBlank { return Result.failure(IllegalArgumentException("Blank project ID")) }
        val docPath = "users/$userId/projects/$safeProjectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }
            validateDocumentId(uid, "user")
            validateDocumentId(safeProjectId, "project")

            val docRef = db.collection("users")
                .document(uid)
                .collection("projects")
                .document(safeProjectId)

            val mutableFields = buildMutableProjectFields(project)
            val existingSnap = docRef.get().await()

            if (existingSnap.exists()) {
                val updatePayload = mutableFields.toMutableMap().apply {
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mutableFields.toMutableMap().apply {
                    put("userId", uid)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            validateDocumentId(uid, "user")
            validateDocumentId(projectId, "project")

            db.collection("users")
                .document(uid)
                .collection("projects")
                .document(projectId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildMutableProjectFields(project: ClipProject): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "title" to project.title.trim().ifBlank { "Untitled Viral Cut" }.take(120),
            "youtubeUrl" to project.youtubeUrl.trim().takeIf { it.isNotBlank() }?.take(500),
            "youtubeVideoId" to project.youtubeVideoId.trim().takeIf { it.isNotBlank() }?.take(64),
            "channelName" to project.channelName.trim().takeIf { it.isNotBlank() }?.take(120),
            "coverThumbnailUrl" to project.coverThumbnailUrl.trim().takeIf { it.isNotBlank() }?.take(500),
            "aspectRatio" to project.aspectRatio.name,
            "exportQuality" to project.exportQuality.name,
            "exportResolution" to project.exportResolution.name,
            "exportBitrateTier" to project.exportBitrateTier.name,
            "exportFps" to if (project.exportFps >= 60) 60 else 30,
            "proClarityEnabled" to project.proClarityEnabled,
            "targetLengthSec" to project.targetLengthSec.coerceIn(15, 180),
            "autoCaptionsEnabled" to project.autoCaptionsEnabled,
            "subtitleStyle" to project.subtitleStyle.name,
            "subtitleFontSizeSp" to project.subtitleFontSizeSp.coerceIn(12f, 40f).toDouble(),
            "subtitleYFraction" to project.subtitleYFraction.coerceIn(0.05f, 0.95f).toDouble(),
            "filterPreset" to project.filterPreset.name,
            "filterIntensity" to project.filterIntensity.coerceIn(0f, 1f).toDouble(),
            "effectPreset" to project.effectPreset.name,
            "effectIntensity" to project.effectIntensity.coerceIn(0f, 1f).toDouble(),
            "transitionPreset" to project.transitionPreset.name,
            "transitionDurationSec" to project.transitionDurationSec.coerceIn(0.1f, 3.0f).toDouble(),
            "musicPreset" to project.musicPreset.name,
            "customMusicName" to project.customMusicName?.trim()?.takeIf { it.isNotBlank() }?.take(120),
            "musicVolume" to project.musicVolume.coerceIn(0f, 1f).toDouble(),
            "clips" to project.clips.take(30).map { clip ->
                mapOf<String, Any?>(
                    "id" to clip.id.take(128),
                    "title" to clip.title.take(120),
                    "sourceType" to clip.sourceType.name,
                    "sourceUri" to clip.sourceUri.take(500),
                    "thumbnailUrl" to clip.thumbnailUrl.take(500),
                    "durationMs" to clip.durationMs.coerceAtLeast(3_000L),
                    "trimStartMs" to clip.trimStartMs.coerceAtLeast(0L),
                    "trimEndMs" to clip.trimEndMs.coerceAtLeast(3_000L),
                    "viralScore" to clip.viralScore.coerceIn(0, 100),
                    "hookReason" to clip.hookReason.take(300),
                    "youtubeStartLabel" to clip.youtubeStartLabel.take(20),
                    "youtubeEndLabel" to clip.youtubeEndLabel.take(20),
                    "transitionAfter" to clip.transitionAfter?.name,
                    "transitionDurationSec" to clip.transitionDurationSec?.coerceIn(0.2f, 2.0f)?.toDouble()
                ).filterValues { it != null }
            },
            "subtitles" to project.subtitles.take(150).map { cue ->
                mapOf(
                    "id" to cue.id.take(128),
                    "startMs" to cue.startMs.coerceAtLeast(0L),
                    "endMs" to cue.endMs.coerceAtLeast(cue.startMs + 200L),
                    "text" to cue.text.take(240)
                )
            },
            "textOverlays" to project.textOverlays.take(30).map { ov ->
                mapOf(
                    "id" to ov.id.take(128),
                    "text" to ov.text.take(140),
                    "xFraction" to ov.xFraction.coerceIn(0f, 1f).toDouble(),
                    "yFraction" to ov.yFraction.coerceIn(0f, 1f).toDouble(),
                    "fontSizeSp" to ov.fontSizeSp.coerceIn(10f, 64f).toDouble(),
                    "textColorArgb" to ov.textColorArgb,
                    "bgColorArgb" to ov.bgColorArgb,
                    "fontStyle" to ov.fontStyle.name,
                    "startMs" to ov.startMs.coerceAtLeast(0L),
                    "endMs" to ov.endMs.coerceAtLeast(500L)
                )
            },
            "ttsSettings" to mapOf(
                "enabled" to project.ttsSettings.enabled,
                "script" to project.ttsSettings.script.take(2000),
                "languageTag" to project.ttsSettings.languageTag.take(20),
                "speechRate" to project.ttsSettings.speechRate.coerceIn(0.5f, 2.0f).toDouble(),
                "pitch" to project.ttsSettings.pitch.coerceIn(0.5f, 2.0f).toDouble(),
                "volume" to project.ttsSettings.volume.coerceIn(0f, 1f).toDouble()
            ),
            "socialCaptions" to mapOf(
                "viralHook" to project.socialCaptions.viralHook.take(300),
                "alternateHooks" to project.socialCaptions.alternateHooks.take(5).map { it.take(300) },
                "visualInsight" to project.socialCaptions.visualInsight.take(400),
                "generatedTone" to project.socialCaptions.generatedTone.take(60),
                "instagramCaption" to project.socialCaptions.instagramCaption.take(1500),
                "youtubeShortsCaption" to project.socialCaptions.youtubeShortsCaption.take(1500),
                "tiktokCaption" to project.socialCaptions.tiktokCaption.take(1500),
                "twitterCaption" to project.socialCaptions.twitterCaption.take(500),
                "hashtags" to project.socialCaptions.hashtags.take(20).map { it.take(60) }
            )
        )
        return raw.filterValues { it != null } as Map<String, Any>
    }

    private fun CloudClipProject.toDomain(): ClipProject {
        val parsedClips = clips.mapNotNull { m ->
            val clipTitle = (m["title"] as? String).orEmpty().ifBlank { "Clip" }
            val sourceTypeName = (m["sourceType"] as? String).orEmpty()
            val transitionName = (m["transitionAfter"] as? String)?.takeIf { it.isNotBlank() }
            val parsedTransition = transitionName?.let { name ->
                enumValues<TransitionPreset>().firstOrNull { it.name == name }
            }
            val parsedTransitionDur = (m["transitionDurationSec"] as? Number)?.toFloat()?.coerceIn(0.2f, 2.0f)
            TimelineClipItem(
                id = (m["id"] as? String).orEmpty().ifBlank { java.util.UUID.randomUUID().toString() },
                title = clipTitle,
                sourceType = enumValues<ClipSourceType>().firstOrNull { it.name == sourceTypeName }
                    ?: ClipSourceType.YOUTUBE_SEGMENT,
                sourceUri = (m["sourceUri"] as? String).orEmpty(),
                thumbnailUrl = (m["thumbnailUrl"] as? String).orEmpty(),
                durationMs = (m["durationMs"] as? Number)?.toLong() ?: 30_000L,
                trimStartMs = (m["trimStartMs"] as? Number)?.toLong() ?: 0L,
                trimEndMs = (m["trimEndMs"] as? Number)?.toLong() ?: 30_000L,
                viralScore = (m["viralScore"] as? Number)?.toInt() ?: 90,
                hookReason = (m["hookReason"] as? String).orEmpty(),
                youtubeStartLabel = (m["youtubeStartLabel"] as? String).orEmpty().ifBlank { "00:00" },
                youtubeEndLabel = (m["youtubeEndLabel"] as? String).orEmpty().ifBlank { "00:30" },
                transitionAfter = parsedTransition,
                transitionDurationSec = parsedTransitionDur
            )
        }

        val parsedSubtitles = subtitles.mapNotNull { m ->
            val text = (m["text"] as? String).orEmpty()
            if (text.isBlank()) return@mapNotNull null
            SubtitleCue(
                id = (m["id"] as? String).orEmpty().ifBlank { java.util.UUID.randomUUID().toString() },
                startMs = (m["startMs"] as? Number)?.toLong() ?: 0L,
                endMs = (m["endMs"] as? Number)?.toLong() ?: 3_000L,
                text = text
            )
        }

        val parsedOverlays = textOverlays.mapNotNull { m ->
            val text = (m["text"] as? String).orEmpty()
            if (text.isBlank()) return@mapNotNull null
            val styleName = (m["fontStyle"] as? String).orEmpty()
            TextOverlayItem(
                id = (m["id"] as? String).orEmpty().ifBlank { java.util.UUID.randomUUID().toString() },
                text = text,
                xFraction = (m["xFraction"] as? Number)?.toFloat() ?: 0.5f,
                yFraction = (m["yFraction"] as? Number)?.toFloat() ?: 0.22f,
                fontSizeSp = (m["fontSizeSp"] as? Number)?.toFloat() ?: 22f,
                textColorArgb = (m["textColorArgb"] as? Number)?.toInt() ?: 0xFFFFFFFF.toInt(),
                bgColorArgb = (m["bgColorArgb"] as? Number)?.toInt() ?: 0xCCFF3D5A.toInt(),
                fontStyle = enumValues<OverlayFontStyle>().firstOrNull { it.name == styleName }
                    ?: OverlayFontStyle.BOLD_DISPLAY,
                startMs = (m["startMs"] as? Number)?.toLong() ?: 0L,
                endMs = (m["endMs"] as? Number)?.toLong() ?: 60_000L
            )
        }

        val parsedTts = TtsSettings(
            enabled = (ttsSettings["enabled"] as? Boolean) ?: false,
            script = (ttsSettings["script"] as? String).orEmpty(),
            languageTag = (ttsSettings["languageTag"] as? String).orEmpty().ifBlank { "en-US" },
            speechRate = (ttsSettings["speechRate"] as? Number)?.toFloat() ?: 1.05f,
            pitch = (ttsSettings["pitch"] as? Number)?.toFloat() ?: 1.0f,
            volume = (ttsSettings["volume"] as? Number)?.toFloat() ?: 0.9f
        )

        val rawTags = (socialCaptions["hashtags"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val rawHooks = (socialCaptions["alternateHooks"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        val parsedSocial = SocialCaptionBundle(
            viralHook = (socialCaptions["viralHook"] as? String).orEmpty(),
            alternateHooks = rawHooks,
            visualInsight = (socialCaptions["visualInsight"] as? String).orEmpty(),
            generatedTone = (socialCaptions["generatedTone"] as? String).orEmpty(),
            instagramCaption = (socialCaptions["instagramCaption"] as? String).orEmpty(),
            youtubeShortsCaption = (socialCaptions["youtubeShortsCaption"] as? String).orEmpty(),
            tiktokCaption = (socialCaptions["tiktokCaption"] as? String).orEmpty(),
            twitterCaption = (socialCaptions["twitterCaption"] as? String).orEmpty(),
            hashtags = rawTags
        )

        val resolvedUpdatedMs = (updatedAt ?: createdAt)?.toDate()?.time ?: System.currentTimeMillis()

        val resolvedQuality = enumValues<ExportQuality>().firstOrNull { it.name == exportQuality }
            ?: ExportQuality.PRO_IPHONE_MASTER
        val resolvedResolution = exportResolution?.let { resName ->
            enumValues<ExportResolution>().firstOrNull { it.name == resName }
        } ?: when (resolvedQuality) {
            ExportQuality.HD_720P -> ExportResolution.RES_720P
            ExportQuality.FULL_HD_1080P, ExportQuality.PRO_IPHONE_MASTER -> ExportResolution.RES_1080P
        }
        val resolvedBitrateTier = exportBitrateTier?.let { tierName ->
            enumValues<ExportBitrateTier>().firstOrNull { it.name == tierName }
        } ?: when (resolvedQuality) {
            ExportQuality.HD_720P -> ExportBitrateTier.STANDARD
            ExportQuality.FULL_HD_1080P -> ExportBitrateTier.HIGH
            ExportQuality.PRO_IPHONE_MASTER -> ExportBitrateTier.MASTER
        }
        val resolvedFps = when (val raw = exportFps?.toInt()) {
            30 -> 30
            60 -> 60
            else -> resolvedQuality.fps
        }

        return ClipProject(
            id = id.ifBlank { java.util.UUID.randomUUID().toString() },
            title = title.ifBlank { "Untitled Viral Cut" },
            youtubeUrl = youtubeUrl.orEmpty(),
            youtubeVideoId = youtubeVideoId.orEmpty(),
            channelName = channelName.orEmpty(),
            coverThumbnailUrl = coverThumbnailUrl.orEmpty(),
            aspectRatio = enumValues<AspectRatioPreset>().firstOrNull { it.name == aspectRatio }
                ?: AspectRatioPreset.RATIO_19_6,
            exportQuality = resolvedQuality,
            exportResolution = resolvedResolution,
            exportBitrateTier = resolvedBitrateTier,
            exportFps = resolvedFps,
            proClarityEnabled = proClarityEnabled,
            targetLengthSec = targetLengthSec.toInt().coerceIn(15, 180),
            clips = parsedClips,
            autoCaptionsEnabled = autoCaptionsEnabled,
            subtitleStyle = enumValues<SubtitleStylePreset>().firstOrNull { it.name == subtitleStyle }
                ?: SubtitleStylePreset.VIRAL_POP,
            subtitleFontSizeSp = subtitleFontSizeSp.toFloat().coerceIn(14f, 34f),
            subtitleYFraction = subtitleYFraction.toFloat().coerceIn(0.18f, 0.88f),
            subtitles = parsedSubtitles,
            textOverlays = parsedOverlays,
            filterPreset = enumValues<VideoFilterPreset>().firstOrNull { it.name == filterPreset }
                ?: VideoFilterPreset.ORIGINAL,
            filterIntensity = filterIntensity.toFloat().coerceIn(0f, 1f),
            effectPreset = enumValues<VideoEffectPreset>().firstOrNull { it.name == effectPreset }
                ?: VideoEffectPreset.NONE,
            effectIntensity = effectIntensity.toFloat().coerceIn(0f, 1f),
            transitionPreset = enumValues<TransitionPreset>().firstOrNull { it.name == transitionPreset }
                ?: TransitionPreset.CROSSFADE,
            transitionDurationSec = transitionDurationSec.toFloat().coerceIn(0.2f, 2.0f),
            musicPreset = enumValues<MusicTrackPreset>().firstOrNull { it.name == musicPreset }
                ?: MusicTrackPreset.PHONK_DRIVE,
            customMusicName = customMusicName,
            musicVolume = musicVolume.toFloat().coerceIn(0f, 1f),
            ttsSettings = parsedTts,
            socialCaptions = parsedSocial,
            updatedAtMs = resolvedUpdatedMs
        )
    }
}
