package com.example.media

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

private const val TAG = "TtsVoiceoverManager"

data class TtsLanguageOption(
    val tag: String,
    val label: String,
    val locale: Locale
)

class TtsVoiceoverManager(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    @Volatile
    var isReady: Boolean = false
        private set

    val supportedLanguages = listOf(
        TtsLanguageOption("en-US", "English (US)", Locale.US),
        TtsLanguageOption("en-IN", "English (India)", Locale("en", "IN")),
        TtsLanguageOption("hi-IN", "Hindi (India)", Locale("hi", "IN")),
        TtsLanguageOption("en-GB", "English (UK)", Locale.UK),
        TtsLanguageOption("es-ES", "Spanish", Locale("es", "ES"))
    )

    init {
        tts = TextToSpeech(appContext) { status ->
            isReady = (status == TextToSpeech.SUCCESS)
            if (isReady) {
                tts?.language = Locale.US
            } else {
                Log.w(TAG, "TextToSpeech initialization returned status=$status")
            }
        }
    }

    fun speakPreview(
        text: String,
        languageTag: String,
        speechRate: Float,
        pitch: Float,
        onComplete: () -> Unit = {}
    ): Boolean {
        val engine = tts ?: return false
        if (!isReady || text.isBlank()) return false

        applyLocaleAndParams(engine, languageTag, speechRate, pitch)
        val utteranceId = UUID.randomUUID().toString()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) onComplete()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId) onComplete()
            }
        })
        val params = Bundle()
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        return result == TextToSpeech.SUCCESS
    }

    fun stopSpeaking() {
        runCatching { tts?.stop() }
    }

    /**
     * Synthesizes real offline WAV audio file for mixing into exported MP4.
     */
    suspend fun synthesizeToWavFile(
        text: String,
        languageTag: String,
        speechRate: Float,
        pitch: Float
    ): File? = suspendCancellableCoroutine { cont ->
        val engine = tts
        if (engine == null || !isReady || text.isBlank()) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val outDir = File(appContext.cacheDir, "tts_audio").apply { mkdirs() }
        val outFile = File(outDir, "voiceover_${System.currentTimeMillis()}.wav")
        val utteranceId = "synth_${UUID.randomUUID()}"

        applyLocaleAndParams(engine, languageTag, speechRate, pitch)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId && cont.isActive) {
                    cont.resume(if (outFile.exists() && outFile.length() > 44L) outFile else null)
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (id == utteranceId && cont.isActive) {
                    cont.resume(null)
                }
            }
        })

        val res = engine.synthesizeToFile(text, Bundle(), outFile, utteranceId)
        if (res != TextToSpeech.SUCCESS && cont.isActive) {
            cont.resume(null)
        }

        cont.invokeOnCancellation {
            runCatching { engine.stop() }
        }
    }

    private fun applyLocaleAndParams(
        engine: TextToSpeech,
        languageTag: String,
        speechRate: Float,
        pitch: Float
    ) {
        val locale = supportedLanguages.firstOrNull { it.tag == languageTag }?.locale
            ?: Locale.forLanguageTag(languageTag)
        val avail = engine.setLanguage(locale)
        if (avail == TextToSpeech.LANG_MISSING_DATA || avail == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.language = Locale.US
        }
        engine.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    fun release() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        isReady = false
    }
}
