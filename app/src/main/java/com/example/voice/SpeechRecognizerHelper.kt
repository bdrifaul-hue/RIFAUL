package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

sealed interface SpeechState {
    data object Idle : SpeechState
    data object Ready : SpeechState
    data class Listening(val rmsDb: Float) : SpeechState
    data object Processing : SpeechState
    data class Result(val text: String, val isFinal: Boolean) : SpeechState
    data class Error(val messageBn: String, val messageEn: String, val errorCode: Int) : SpeechState
}

class SpeechRecognizerHelper(
    private val context: Context,
    private val onStateChanged: (SpeechState) -> Unit
) {
    companion object {
        private const val TAG = "SpeechRecognizerHelper"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningActive = false

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening(languageCode: String = "auto") {
        if (!isAvailable()) {
            onStateChanged(
                SpeechState.Error(
                    messageBn = "আপনার ডিভাইসে গুগল স্পিচ সার্ভিস ইনস্টল করা নেই বা সক্রিয় নেই।",
                    messageEn = "Speech recognition service is not available on this device.",
                    errorCode = -1
                )
            )
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "onReadyForSpeech")
                        onStateChanged(SpeechState.Ready)
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "onBeginningOfSpeech")
                        onStateChanged(SpeechState.Listening(0f))
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (isListeningActive) {
                            onStateChanged(SpeechState.Listening(rmsdB))
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "onEndOfSpeech")
                        onStateChanged(SpeechState.Processing)
                    }

                    override fun onError(error: Int) {
                        Log.w(TAG, "SpeechRecognizer error: $error")
                        isListeningActive = false
                        val (msgBn, msgEn) = when (error) {
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "ইন্টারনেট সংযোগ পাওয়া যায়নি। অনুগ্রহ করে সংযোগ পরীক্ষা করুন।" to
                                        "Internet connection unavailable. Please check your network."

                            SpeechRecognizer.ERROR_NO_MATCH ->
                                "কোনো কথা শনাক্ত করা যায়নি। অনুগ্রহ করে আবার বলুন।" to
                                        "No speech detected. Please speak again."

                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "কথা বলার সময় শেষ হয়েছে।" to
                                        "No speech heard before timeout."

                            SpeechRecognizer.ERROR_AUDIO ->
                                "মাইক্রোফোন অডিও রেকর্ডিংয়ে সমস্যা হয়েছে।" to
                                        "Audio recording error occurred."

                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "মাইক্রোফোন ব্যবহারের অনুমতি প্রয়োজন।" to
                                        "Microphone permission is required."

                            else ->
                                "ভয়েস রেকগনিশনে সমস্যা হয়েছে (কোড: $error)।" to
                                        "Speech recognition issue encountered (code $error)."
                        }
                        onStateChanged(SpeechState.Error(msgBn, msgEn, error))
                    }

                    override fun onResults(results: Bundle?) {
                        isListeningActive = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        Log.d(TAG, "onResults: $text")
                        if (text.isNotBlank()) {
                            onStateChanged(SpeechState.Result(text, isFinal = true))
                        } else {
                            onStateChanged(
                                SpeechState.Error(
                                    messageBn = "কোনো কথা বুঝতে পারিনি।",
                                    messageEn = "Could not understand any speech.",
                                    errorCode = SpeechRecognizer.ERROR_NO_MATCH
                                )
                            )
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onStateChanged(SpeechState.Result(text, isFinal = false))
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                when (languageCode) {
                    "bn" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-IN", "en-US"))
                    }
                    "en" -> {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD"))
                    }
                    else -> {
                        // Multi-language support (English and Bengali)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "en-US"))
                    }
                }
            }

            isListeningActive = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognition", e)
            isListeningActive = false
            onStateChanged(
                SpeechState.Error(
                    messageBn = "স্পিচ রেকগনিশন শুরু করা যায়নি: ${e.localizedMessage}",
                    messageEn = "Unable to start speech recognition: ${e.localizedMessage}",
                    errorCode = -2
                )
            )
        }
    }

    fun stopListening() {
        isListeningActive = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning speech recognizer", e)
        } finally {
            speechRecognizer = null
            onStateChanged(SpeechState.Idle)
        }
    }

    fun destroy() {
        stopListening()
    }
}
