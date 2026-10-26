package com.example.ai

import android.util.Log
import com.example.data.model.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class AiAssistantService {

    companion object {
        private const val TAG = "AiAssistantService"
        private const val MODEL_NAME = "gemini-3.8-flash"

        private const val SYSTEM_INSTRUCTION = """
You are RIFAUL AI, an advanced, futuristic personal AI assistant for Android.
- Your developer and assistant identity is RIFAUL AI.
- You can communicate naturally and fluently in Bengali (বাংলা) and English.
- Always detect the language of the user's message and reply in the same language, or in a friendly bilingual manner if appropriate.
- When answering in Bengali, use polite, modern Bengali words and clear phrasing.
- Keep spoken assistant answers informative, engaging, concise, and easy to listen to.
- You are equipped with Android app control capabilities on this phone (such as YouTube, Chrome, Facebook, WhatsApp, Instagram, Gmail, Google Maps, and Play Store).
- Never claim to have performed a device action unless you are providing information or advice; actual app launches are handled and verified through Android intents.
- When internet or services are disrupted, explain clearly without confusing technical jargon.
"""
    }

    // Lazy initialization mandated by Firebase AI Logic to prevent startup crashes
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content { text(SYSTEM_INSTRUCTION) }
        )
    }

    /**
     * Sends a message with recent conversation context and streams the tokens back.
     */
    fun streamResponse(
        prompt: String,
        recentHistory: List<ChatMessage>
    ): Flow<String> = flow {
        try {
            // Convert recent history (excluding system messages) to Gemini Content turns
            val historyContent = recentHistory.takeLast(10).mapNotNull { msg ->
                when (msg.sender) {
                    "USER" -> content("user") { text(msg.text) }
                    "AI" -> content("model") { text(msg.text) }
                    else -> null
                }
            }

            val chat = model.startChat(history = historyContent)

            // Realistic 60s timeout mandated by Firebase AI Logic
            withTimeout(60_000L) {
                chat.sendMessageStream(prompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        emit(chunkText)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini stream timed out", e)
            throw Exception("Request timed out. Please check your internet connection and try again.")
        } catch (e: CancellationException) {
            // Mandated: Always re-throw CancellationException
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed", e)
            val message = e.localizedMessage ?: "AI service currently unavailable."
            throw Exception(message)
        }
    }
}
