package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TextToSpeechHelper(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TextToSpeechHelper"
        private const val UTTERANCE_ID = "RIFAUL_AI_SPEECH"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingSpeechQueue = mutableListOf<Pair<String, String>>()
    var speechRate: Float = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setSpeechRate(speechRate)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.w(TAG, "TTS error code: $errorCode")
                    onSpeakingStateChanged(false)
                }
            })

            // Process any pending utterances
            synchronized(pendingSpeechQueue) {
                for ((text, lang) in pendingSpeechQueue) {
                    speakInternal(text, lang)
                }
                pendingSpeechQueue.clear()
            }
            Log.d(TAG, "TextToSpeech successfully initialized.")
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with status: $status")
            isInitialized = false
        }
    }

    fun speak(text: String, languageCode: String = "auto") {
        val cleanText = sanitizeForSpeech(text)
        if (cleanText.isBlank()) return

        if (!isInitialized) {
            synchronized(pendingSpeechQueue) {
                pendingSpeechQueue.add(cleanText to languageCode)
            }
            return
        }

        speakInternal(cleanText, languageCode)
    }

    private fun speakInternal(text: String, languageCode: String) {
        val targetLocale = when (languageCode) {
            "bn" -> Locale.forLanguageTag("bn-BD")
            "en" -> Locale.US
            else -> {
                if (containsBengali(text)) {
                    Locale.forLanguageTag("bn-BD")
                } else {
                    Locale.US
                }
            }
        }

        val result = tts?.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to standard Bengali without country code or US English
            val fallbackLocale = if (targetLocale.language == "bn") Locale.forLanguageTag("bn") else Locale.US
            val fallbackRes = tts?.setLanguage(fallbackLocale)
            if (fallbackRes == TextToSpeech.LANG_MISSING_DATA || fallbackRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language $targetLocale not supported by device TTS engine.")
                tts?.setLanguage(Locale.US)
            }
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS", e)
        } finally {
            onSpeakingStateChanged(false)
        }
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS", e)
        } finally {
            tts = null
            isInitialized = false
        }
    }

    private fun sanitizeForSpeech(text: String): String {
        return text
            .replace(Regex("[*#`_~>]"), "") // Remove markdown formatting
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // Replace links with title
            .trim()
    }

    private fun containsBengali(text: String): Boolean {
        for (char in text) {
            if (char in '\u0980'..'\u09FF') {
                return true
            }
        }
        return false
    }
}
