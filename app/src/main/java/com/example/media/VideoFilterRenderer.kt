package com.example.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.model.ClipProject
import com.example.model.OverlayFontStyle
import com.example.model.SubtitleCue
import com.example.model.SubtitleStylePreset
import com.example.model.TextOverlayItem
import com.example.model.TransitionPreset
import com.example.model.VideoEffectPreset
import com.example.model.VideoFilterPreset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

object VideoFilterRenderer {

    // Subtle iPhone Smart-HDR style clarity: gentle S-curve micro-contrast + clean skin-safe vibrance
    private val PRO_CLARITY_MATRIX = floatArrayOf(
        1.09f, -0.02f, -0.02f, 0f, -4f,
        -0.02f, 1.09f, -0.02f, 0f, -4f,
        -0.02f, -0.02f, 1.11f, 0f, -3f,
        0f, 0f, 0f, 1f, 0f
    )

    fun blendedColorMatrix(preset: VideoFilterPreset, intensity: Float): FloatArray {
        val t = intensity.coerceIn(0f, 1f)
        val identity = VideoFilterPreset.ORIGINAL.matrix
        val target = preset.matrix
        return FloatArray(20) { i ->
            identity[i] + (target[i] - identity[i]) * t
        }
    }

    fun drawCompositeFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        project: ClipProject,
        timeMs: Long,
        currentBitmap: Bitmap?,
        nextBitmap: Bitmap?,
        transitionProgress: Float, // 0f = normal, >0f..1f = transitioning into nextBitmap
        transitionPreset: TransitionPreset = project.transitionPreset
    ) {
        canvas.drawColor(Color.rgb(11, 12, 16))

        val filterPaint = Paint(
            Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG
        ).apply {
            val combinedMatrix = ColorMatrix()
            var hasColorGrading = false
            if (project.proClarityEnabled) {
                combinedMatrix.postConcat(ColorMatrix(PRO_CLARITY_MATRIX))
                hasColorGrading = true
            }
            if (project.filterPreset != VideoFilterPreset.ORIGINAL && project.filterIntensity > 0.01f) {
                combinedMatrix.postConcat(
                    ColorMatrix(blendedColorMatrix(project.filterPreset, project.filterIntensity))
                )
                hasColorGrading = true
            }
            if (hasColorGrading) {
                colorFilter = ColorMatrixColorFilter(combinedMatrix)
            }
        }

        val timeSec = timeMs / 1000f
        val effect = project.effectPreset
        val effectAmt = project.effectIntensity.coerceIn(0f, 1f)

        // Compute dynamic scale & shake from active effect
        var zoomScale = 1.0f + 0.04f * sin(timeSec * 0.6f) // subtle studio Ken Burns motion
        var offsetX = 0f
        var offsetY = 0f
        when (effect) {
            VideoEffectPreset.ZOOM_PULSE -> {
                val pulse = abs(sin(timeSec * PI.toFloat() * 1.8f))
                zoomScale = 1.02f + pulse * 0.14f * effectAmt
            }
            VideoEffectPreset.KINETIC_SHAKE -> {
                val amp = 14f * effectAmt
                offsetX = sin(timeSec * 38f) * amp
                offsetY = sin(timeSec * 47f + 1.1f) * amp * 0.7f
                zoomScale = 1.06f
            }
            else -> Unit
        }

        if (transitionProgress <= 0f || nextBitmap == null || transitionPreset == TransitionPreset.NONE) {
            drawScaledFrame(canvas, currentBitmap, width, height, filterPaint, zoomScale, offsetX, offsetY)
        } else {
            drawTransitionFrames(
                canvas = canvas,
                width = width,
                height = height,
                fromBmp = currentBitmap,
                toBmp = nextBitmap,
                preset = transitionPreset,
                progress = transitionProgress.coerceIn(0f, 1f),
                paint = filterPaint,
                zoomScale = zoomScale
            )
        }

        // Apply post-processing visual effects
        drawVideoEffectOverlay(canvas, width, height, effect, effectAmt, timeSec)

        // Draw custom text overlays active at timeMs
        for (overlay in project.textOverlays) {
            if (timeMs in overlay.startMs..overlay.endMs && overlay.text.isNotBlank()) {
                drawTextOverlay(canvas, width, height, overlay)
            }
        }

        // Draw automatic synchronized subtitles
        if (project.autoCaptionsEnabled) {
            val activeCue = project.subtitles.firstOrNull { timeMs in it.startMs..it.endMs }
            if (activeCue != null) {
                drawSubtitleCue(
                    canvas = canvas,
                    width = width,
                    height = height,
                    cue = activeCue,
                    timeMs = timeMs,
                    style = project.subtitleStyle,
                    fontSizeSp = project.subtitleFontSizeSp,
                    yFraction = project.subtitleYFraction
                )
            }
        }
    }

    private fun drawScaledFrame(
        canvas: Canvas,
        bitmap: Bitmap?,
        width: Int,
        height: Int,
        paint: Paint,
        scale: Float,
        dx: Float,
        dy: Float,
        alpha: Int = 255
    ) {
        paint.alpha = alpha
        if (bitmap == null || bitmap.isRecycled) {
            val fallbackPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.rgb(20, 24, 38), Color.rgb(42, 16, 30), Color.rgb(12, 26, 38)),
                    null,
                    Shader.TileMode.CLAMP
                )
                this.alpha = alpha
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fallbackPaint)
            return
        }

        val bw = bitmap.width.toFloat().coerceAtLeast(1f)
        val bh = bitmap.height.toFloat().coerceAtLeast(1f)
        val baseScale = maxOf(width / bw, height / bh) * scale
        val drawW = bw * baseScale
        val drawH = bh * baseScale
        val left = (width - drawW) / 2f + dx
        val top = (height - drawH) / 2f + dy
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), paint)
        paint.alpha = 255
    }

    private fun drawTransitionFrames(
        canvas: Canvas,
        width: Int,
        height: Int,
        fromBmp: Bitmap?,
        toBmp: Bitmap?,
        preset: TransitionPreset,
        progress: Float,
        paint: Paint,
        zoomScale: Float
    ) {
        when (preset) {
            TransitionPreset.CROSSFADE -> {
                drawScaledFrame(canvas, fromBmp, width, height, paint, zoomScale, 0f, 0f, ((1f - progress) * 255).toInt())
                drawScaledFrame(canvas, toBmp, width, height, paint, zoomScale, 0f, 0f, (progress * 255).toInt())
            }
            TransitionPreset.SLIDE_LEFT -> {
                drawScaledFrame(canvas, fromBmp, width, height, paint, zoomScale, -width * progress, 0f)
                drawScaledFrame(canvas, toBmp, width, height, paint, zoomScale, width * (1f - progress), 0f)
            }
            TransitionPreset.SLIDE_UP -> {
                drawScaledFrame(canvas, fromBmp, width, height, paint, zoomScale, 0f, -height * progress)
                drawScaledFrame(canvas, toBmp, width, height, paint, zoomScale, 0f, height * (1f - progress))
            }
            TransitionPreset.ZOOM_IN -> {
                val outScale = zoomScale + progress * 0.45f
                val inScale = (zoomScale * 0.85f) + progress * 0.18f
                drawScaledFrame(canvas, fromBmp, width, height, paint, outScale, 0f, 0f, ((1f - progress) * 255).toInt())
                drawScaledFrame(canvas, toBmp, width, height, paint, inScale, 0f, 0f, (progress * 255).toInt())
            }
            TransitionPreset.FLASH_WHITE -> {
                val bmp = if (progress < 0.5f) fromBmp else toBmp
                drawScaledFrame(canvas, bmp, width, height, paint, zoomScale, 0f, 0f)
                val flashAlpha = (sin(progress * PI.toFloat()) * 235).toInt().coerceIn(0, 240)
                canvas.drawColor(Color.argb(flashAlpha, 255, 255, 255))
            }
            TransitionPreset.GLITCH_CUT -> {
                val bmp = if (progress < 0.5f) fromBmp else toBmp
                val jitter = sin(progress * 45f) * 26f
                drawScaledFrame(canvas, bmp, width, height, paint, zoomScale, jitter, 0f)
                val barPaint = Paint().apply {
                    color = Color.argb(140, 46, 230, 255)
                }
                val y = (height * progress)
                canvas.drawRect(0f, y, width.toFloat(), y + height * 0.07f, barPaint)
                barPaint.color = Color.argb(140, 255, 61, 90)
                canvas.drawRect(0f, height - y, width.toFloat(), height - y + height * 0.05f, barPaint)
            }
            TransitionPreset.WIPE_RIGHT -> {
                drawScaledFrame(canvas, fromBmp, width, height, paint, zoomScale, 0f, 0f)
                canvas.save()
                canvas.clipRect(0f, 0f, width * progress, height.toFloat())
                drawScaledFrame(canvas, toBmp, width, height, paint, zoomScale, 0f, 0f)
                canvas.restore()
            }
            TransitionPreset.NONE -> {
                drawScaledFrame(canvas, toBmp, width, height, paint, zoomScale, 0f, 0f)
            }
        }
    }

    private fun drawVideoEffectOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        effect: VideoEffectPreset,
        intensity: Float,
        timeSec: Float
    ) {
        if (effect == VideoEffectPreset.NONE || intensity <= 0.02f) return
        when (effect) {
            VideoEffectPreset.VIGNETTE -> {
                val radius = maxOf(width, height) * 0.72f
                val vignettePaint = Paint().apply {
                    shader = RadialGradient(
                        width / 2f,
                        height / 2f,
                        radius,
                        intArrayOf(Color.TRANSPARENT, Color.argb((185 * intensity).toInt(), 0, 0, 0)),
                        floatArrayOf(0.45f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
            }
            VideoEffectPreset.RGB_GLITCH -> {
                val p = Paint()
                val bandH = (height * 0.035f).coerceAtLeast(8f)
                val shift = ((timeSec * 120f) % height)
                p.color = Color.argb((95 * intensity).toInt(), 255, 61, 90)
                canvas.drawRect(0f, shift, width.toFloat(), shift + bandH, p)
                p.color = Color.argb((85 * intensity).toInt(), 46, 230, 255)
                val y2 = (shift + height * 0.42f) % height
                canvas.drawRect(0f, y2, width.toFloat(), y2 + bandH * 0.8f, p)
            }
            VideoEffectPreset.RETRO_VHS -> {
                val scanPaint = Paint().apply {
                    color = Color.argb((58 * intensity).toInt(), 0, 0, 0)
                    strokeWidth = 2f
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, scanPaint)
                    y += 7f
                }
                val tint = Paint().apply {
                    color = Color.argb((38 * intensity).toInt(), 255, 190, 120)
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), tint)
            }
            VideoEffectPreset.GLOW_BLOOM -> {
                val glow = Paint().apply {
                    shader = RadialGradient(
                        width * 0.5f,
                        height * 0.38f,
                        min(width, height) * 0.75f,
                        intArrayOf(
                            Color.argb((95 * intensity).toInt(), 255, 110, 150),
                            Color.argb((45 * intensity).toInt(), 46, 230, 255),
                            Color.TRANSPARENT
                        ),
                        floatArrayOf(0f, 0.55f, 1f),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glow)
            }
            VideoEffectPreset.ZOOM_PULSE, VideoEffectPreset.KINETIC_SHAKE, VideoEffectPreset.NONE -> Unit
        }
    }

    private fun drawTextOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        overlay: TextOverlayItem
    ) {
        val scaleFactor = min(width, height) / 400f
        val textSizePx = (overlay.fontSizeSp * scaleFactor).coerceIn(18f, 96f)
        val displayText = if (overlay.fontStyle == OverlayFontStyle.UPPER_STAMP) {
            overlay.text.uppercase()
        } else {
            overlay.text
        }

        val textPaint = Paint(
            Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG
        ).apply {
            color = overlay.textColorArgb
            textSize = textSizePx
            textAlign = Paint.Align.CENTER
            typeface = when (overlay.fontStyle) {
                OverlayFontStyle.BOLD_DISPLAY, OverlayFontStyle.UPPER_STAMP ->
                    Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                OverlayFontStyle.MONO_TECH ->
                    Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                OverlayFontStyle.CLEAN_SANS ->
                    Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            setShadowLayer(6f, 0f, 3f, Color.argb(170, 0, 0, 0))
        }

        val bounds = Rect()
        textPaint.getTextBounds(displayText, 0, displayText.length, bounds)
        val cx = width * overlay.xFraction.coerceIn(0.12f, 0.88f)
        val cy = height * overlay.yFraction.coerceIn(0.10f, 0.90f)
        val padH = textSizePx * 0.55f
        val padV = textSizePx * 0.34f

        if (Color.alpha(overlay.bgColorArgb) > 10) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = overlay.bgColorArgb }
            val rect = RectF(
                cx - bounds.width() / 2f - padH,
                cy - bounds.height() / 2f - padV,
                cx + bounds.width() / 2f + padH,
                cy + bounds.height() / 2f + padV
            )
            canvas.drawRoundRect(rect, padV * 1.1f, padV * 1.1f, bgPaint)
        }

        canvas.drawText(displayText, cx, cy + bounds.height() / 2.4f, textPaint)
    }

    private fun drawSubtitleCue(
        canvas: Canvas,
        width: Int,
        height: Int,
        cue: SubtitleCue,
        timeMs: Long,
        style: SubtitleStylePreset,
        fontSizeSp: Float,
        yFraction: Float
    ) {
        val rawText = if (style.uppercase) cue.text.uppercase() else cue.text
        val words = rawText.split(" ").filter { it.isNotBlank() }
        if (words.isEmpty()) return

        val cueDur = (cue.endMs - cue.startMs).coerceAtLeast(1L)
        val progress = ((timeMs - cue.startMs).toFloat() / cueDur).coerceIn(0f, 0.999f)
        val activeWordIdx = (progress * words.size).toInt().coerceIn(0, words.lastIndex)

        val scaleFactor = min(width, height) / 380f
        val textSizePx = (fontSizeSp * scaleFactor).coerceIn(20f, 84f)
        val basePaint = Paint(
            Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG
        ).apply {
            textSize = textSizePx
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(8f, 0f, 4f, Color.argb(210, 0, 0, 0))
        }

        // Wrap into max 2 lines within 86% canvas width
        val maxLineW = width * 0.84f
        val lines = mutableListOf<List<Pair<Int, String>>>()
        var currentLine = mutableListOf<Pair<Int, String>>()
        var currentWidth = 0f
        val spaceW = basePaint.measureText(" ")

        words.forEachIndexed { idx, w ->
            val ww = basePaint.measureText(w)
            if (currentLine.isNotEmpty() && currentWidth + spaceW + ww > maxLineW) {
                lines.add(currentLine)
                currentLine = mutableListOf(idx to w)
                currentWidth = ww
            } else {
                if (currentLine.isNotEmpty()) currentWidth += spaceW
                currentLine.add(idx to w)
                currentWidth += ww
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        val lineHeight = textSizePx * 1.32f
        val totalH = lines.size * lineHeight
        val centerY = height * yFraction.coerceIn(0.2f, 0.88f)
        val startY = centerY - totalH / 2f + textSizePx * 0.85f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = style.bgColorArgb }
        val maxMeasuredW = lines.maxOf { line ->
            basePaint.measureText(line.joinToString(" ") { it.second })
        }
        val boxPadH = textSizePx * 0.55f
        val boxPadV = textSizePx * 0.32f
        canvas.drawRoundRect(
            RectF(
                width / 2f - maxMeasuredW / 2f - boxPadH,
                centerY - totalH / 2f - boxPadV,
                width / 2f + maxMeasuredW / 2f + boxPadH,
                centerY + totalH / 2f + boxPadV
            ),
            textSizePx * 0.45f,
            textSizePx * 0.45f,
            bgPaint
        )

        lines.forEachIndexed { lineIdx, lineWords ->
            val lineStr = lineWords.joinToString(" ") { it.second }
            val lineW = basePaint.measureText(lineStr)
            var cursorX = (width - lineW) / 2f
            val y = startY + lineIdx * lineHeight
            lineWords.forEach { (wordIdx, word) ->
                basePaint.color = if (wordIdx == activeWordIdx) {
                    style.highlightColorArgb
                } else {
                    style.primaryColorArgb
                }
                canvas.drawText(word, cursorX, y, basePaint)
                cursorX += basePaint.measureText(word) + spaceW
            }
        }
    }
}
