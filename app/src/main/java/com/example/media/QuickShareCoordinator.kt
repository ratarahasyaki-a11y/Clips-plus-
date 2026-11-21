package com.example.media

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.example.model.ClipProject

enum class QuickShareTarget(
    val displayName: String,
    val shortLabel: String,
    val packageName: String?,
    val accentHex: Long
) {
    WHATSAPP("WhatsApp", "WhatsApp", "com.whatsapp", 0xFF25D366),
    INSTAGRAM_REELS("Instagram Reels", "Reels", "com.instagram.android", 0xFFFF3D5A),
    YOUTUBE_SHORTS("YouTube Shorts", "Shorts", "com.google.android.youtube", 0xFFFF2E4C),
    SYSTEM_MORE("More Apps", "More", null, 0xFF2EE6FF)
}

object QuickShareCoordinator {

    fun buildPlatformCaption(project: ClipProject, target: QuickShareTarget): String {
        val bundle = project.socialCaptions
        val body = when (target) {
            QuickShareTarget.INSTAGRAM_REELS -> bundle.instagramCaption.ifBlank { project.title }
            QuickShareTarget.YOUTUBE_SHORTS -> bundle.youtubeShortsCaption.ifBlank { "${project.title} #Shorts" }
            QuickShareTarget.WHATSAPP, QuickShareTarget.SYSTEM_MORE ->
                bundle.instagramCaption.ifBlank { bundle.youtubeShortsCaption }.ifBlank { project.title }
        }
        return buildString {
            if (bundle.viralHook.isNotBlank()) {
                appendLine(bundle.viralHook)
                appendLine()
            }
            appendLine(body)
            if (bundle.hashtags.isNotEmpty()) {
                appendLine()
                append(bundle.hashtags.joinToString(" "))
            }
        }.trim()
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        if (text.isBlank()) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareVideo(
        context: Context,
        videoUri: Uri,
        project: ClipProject,
        target: QuickShareTarget
    ): String {
        val caption = buildPlatformCaption(project, target)
        copyToClipboard(context, "${target.displayName} Caption", caption)

        if (target == QuickShareTarget.SYSTEM_MORE || target.packageName == null) {
            launchSystemVideoChooser(context, videoUri, caption, "Share ${project.title}")
            return "Caption copied — choose an app to share"
        }

        val candidatePackages = if (target == QuickShareTarget.WHATSAPP) {
            listOf("com.whatsapp", "com.whatsapp.w4b")
        } else {
            listOf(target.packageName)
        }

        for (pkg in candidatePackages) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                setPackage(pkg)
                putExtra(Intent.EXTRA_STREAM, videoUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                putExtra(Intent.EXTRA_SUBJECT, project.title)
                clipData = ClipData.newUri(context.contentResolver, project.title, videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(intent)
                return "${target.shortLabel} caption copied — opening ${target.displayName}"
            } catch (_: ActivityNotFoundException) {
                // Try next candidate or fall back to chooser below
            }
        }

        launchSystemVideoChooser(context, videoUri, caption, "Share to ${target.displayName}")
        return "${target.displayName} not installed — caption copied & share sheet opened"
    }

    fun shareTextOnly(
        context: Context,
        captionText: String,
        target: QuickShareTarget
    ): String {
        copyToClipboard(context, "${target.displayName} Caption", captionText)

        if (target == QuickShareTarget.SYSTEM_MORE || target.packageName == null) {
            val chooser = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, captionText)
            }
            context.startActivity(Intent.createChooser(chooser, "Share Caption"))
            return "Caption copied — share sheet opened"
        }

        val candidatePackages = if (target == QuickShareTarget.WHATSAPP) {
            listOf("com.whatsapp", "com.whatsapp.w4b")
        } else {
            listOf(target.packageName)
        }

        for (pkg in candidatePackages) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(pkg)
                putExtra(Intent.EXTRA_TEXT, captionText)
            }
            try {
                context.startActivity(intent)
                return "Caption copied — opening ${target.displayName}"
            } catch (_: ActivityNotFoundException) {
                // Try fallback
            }
        }

        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, captionText)
        }
        context.startActivity(Intent.createChooser(fallback, "Share Caption"))
        return "${target.displayName} not installed — caption copied & share sheet opened"
    }

    fun openInPhoneGallery(context: Context, videoUri: Uri?): String {
        if (videoUri != null) {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(videoUri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(viewIntent)
                return "Opening exported clip in Gallery"
            } catch (_: Exception) {
                // Fall through to generic gallery view
            }
        }

        val galleryIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*")
        }
        return try {
            context.startActivity(galleryIntent)
            "Opened Phone Gallery (Movies/ClipPulse)"
        } catch (_: Exception) {
            "Video is saved in your Phone Gallery → Movies/ClipPulse"
        }
    }

    private fun launchSystemVideoChooser(
        context: Context,
        videoUri: Uri,
        caption: String,
        chooserTitle: String
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, videoUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newUri(context.contentResolver, chooserTitle, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
