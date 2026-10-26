package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiAssistantService
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.service.AppControlManager
import com.example.service.AppTarget
import com.example.service.SpecialCommand
import com.example.ui.components.OrbVisualState
import com.example.voice.SpeechRecognizerHelper
import com.example.voice.SpeechState
import com.example.voice.TextToSpeechHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AssistantUiState {
    data object Idle : AssistantUiState
    data class Listening(val rmsDb: Float) : AssistantUiState
    data class Thinking(val currentStreamingText: String = "") : AssistantUiState
    data class Speaking(val text: String) : AssistantUiState
    data class Error(val messageBn: String, val messageEn: String) : AssistantUiState
}

data class AppLaunchConfirmation(
    val appTarget: AppTarget,
    val isInstalled: Boolean
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AssistantViewModel"
    }

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.chatMessageDao()
    private val appControl = AppControlManager(application)
    private val aiService = AiAssistantService()

    val allMessages: StateFlow<List<ChatMessage>> = dao.getAllMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private val _orbState = MutableStateFlow(OrbVisualState.IDLE)
    val orbState: StateFlow<OrbVisualState> = _orbState.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    private val _activeLanguage = MutableStateFlow("auto") // "auto", "bn", "en"
    val activeLanguage: StateFlow<String> = _activeLanguage.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _inputQuery = MutableStateFlow("")
    val inputQuery: StateFlow<String> = _inputQuery.asStateFlow()

    private val _pendingLaunchConfirmation = MutableStateFlow<AppLaunchConfirmation?>(null)
    val pendingLaunchConfirmation: StateFlow<AppLaunchConfirmation?> = _pendingLaunchConfirmation.asStateFlow()

    private val _showClearHistoryDialog = MutableStateFlow(false)
    val showClearHistoryDialog: StateFlow<Boolean> = _showClearHistoryDialog.asStateFlow()

    private var speechRecognizerHelper: SpeechRecognizerHelper? = null
    private var ttsHelper: TextToSpeechHelper? = null

    init {
        // Initialize Speech Recognizer
        speechRecognizerHelper = SpeechRecognizerHelper(application) { speechState ->
            handleSpeechState(speechState)
        }

        // Initialize Text To Speech
        ttsHelper = TextToSpeechHelper(application) { isSpeaking ->
            if (isSpeaking) {
                _orbState.value = OrbVisualState.SPEAKING
            } else {
                if (_uiState.value is AssistantUiState.Speaking) {
                    _uiState.value = AssistantUiState.Idle
                    _orbState.value = OrbVisualState.IDLE
                }
            }
        }

        // Welcome message on first launch if empty
        viewModelScope.launch {
            val count = dao.getRecentMessages(1).size
            if (count == 0) {
                dao.insert(
                    ChatMessage(
                        sender = "AI",
                        text = "Hello! I am RIFAUL AI, your futuristic personal assistant.\n\n" +
                                "আমি বাংলা এবং ইংরেজি দুটো ভাষাতেই কথা বলতে ও ডিভাইস অ্যাপস পরিচালনা করতে পারি। " +
                                "Try asking 'YouTube চালু করো', 'এখন কয়টা বাজে?', or anything you like!",
                        language = "bn"
                    )
                )
            }
        }
    }

    private fun handleSpeechState(state: SpeechState) {
        when (state) {
            is SpeechState.Idle -> {
                if (_uiState.value is AssistantUiState.Listening) {
                    _uiState.value = AssistantUiState.Idle
                    _orbState.value = OrbVisualState.IDLE
                }
            }
            is SpeechState.Ready -> {
                _uiState.value = AssistantUiState.Listening(0f)
                _orbState.value = OrbVisualState.LISTENING
            }
            is SpeechState.Listening -> {
                _audioRmsDb.value = state.rmsDb
                _uiState.value = AssistantUiState.Listening(state.rmsDb)
                _orbState.value = OrbVisualState.LISTENING
            }
            is SpeechState.Processing -> {
                _uiState.value = AssistantUiState.Thinking()
                _orbState.value = OrbVisualState.THINKING
            }
            is SpeechState.Result -> {
                _inputQuery.value = state.text
                if (state.isFinal) {
                    processUserInput(state.text)
                }
            }
            is SpeechState.Error -> {
                _uiState.value = AssistantUiState.Error(state.messageBn, state.messageEn)
                _orbState.value = OrbVisualState.ERROR
                Log.w(TAG, "Speech Error: ${state.messageEn}")
            }
        }
    }

    fun onInputChange(newText: String) {
        _inputQuery.value = newText
    }

    fun toggleListening() {
        if (_uiState.value is AssistantUiState.Listening) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        stopAllSpeech()
        _uiState.value = AssistantUiState.Listening(0f)
        _orbState.value = OrbVisualState.LISTENING
        speechRecognizerHelper?.startListening(_activeLanguage.value)
    }

    fun stopListening() {
        speechRecognizerHelper?.stopListening()
        _uiState.value = AssistantUiState.Idle
        _orbState.value = OrbVisualState.IDLE
        _audioRmsDb.value = 0f
    }

    fun stopAllSpeech() {
        ttsHelper?.stop()
        speechRecognizerHelper?.stopListening()
        _uiState.value = AssistantUiState.Idle
        _orbState.value = OrbVisualState.IDLE
    }

    fun submitCurrentInput() {
        val query = _inputQuery.value.trim()
        if (query.isNotEmpty()) {
            _inputQuery.value = ""
            processUserInput(query)
        }
    }

    fun processUserInput(query: String) {
        viewModelScope.launch {
            // Stop active speech first
            ttsHelper?.stop()

            // 1. Insert User Message
            val userMsg = ChatMessage(
                sender = "USER",
                text = query,
                language = if (containsBengali(query)) "bn" else "en"
            )
            dao.insert(userMsg)

            // 2. Check for Stop Voice Command
            val specialCommand = appControl.detectSpecialCommand(query)
            if (specialCommand is SpecialCommand.StopListening) {
                stopAllSpeech()
                val stopMsg = if (containsBengali(query)) "কথা ও রেকগনিশন বন্ধ করা হয়েছে।" else "Stopped listening and speaking."
                dao.insert(ChatMessage(sender = "AI", text = stopMsg, language = userMsg.language))
                _uiState.value = AssistantUiState.Idle
                _orbState.value = OrbVisualState.IDLE
                return@launch
            }

            // 3. Check for Special System Commands (Time, Weather, Greeting)
            if (specialCommand != null) {
                val isBn = containsBengali(query) || _activeLanguage.value == "bn"
                val responseText = when (specialCommand) {
                    is SpecialCommand.CurrentTime -> if (isBn) specialCommand.bengaliText else specialCommand.englishText
                    is SpecialCommand.WeatherQuery -> if (isBn) specialCommand.bengaliText else specialCommand.englishText
                    is SpecialCommand.Greeting -> if (isBn) specialCommand.bengaliText else specialCommand.englishText
                    else -> ""
                }

                dao.insert(
                    ChatMessage(
                        sender = "AI",
                        text = responseText,
                        language = if (isBn) "bn" else "en",
                        actionType = "SYSTEM_INFO"
                    )
                )

                _uiState.value = AssistantUiState.Speaking(responseText)
                _orbState.value = OrbVisualState.SPEAKING
                if (_isTtsEnabled.value) {
                    ttsHelper?.speak(responseText, if (isBn) "bn" else "en")
                }
                return@launch
            }

            // 4. Check for Android App Launch Commands
            val matchedApp = appControl.matchAppLaunchCommand(query)
            if (matchedApp != null) {
                val isInstalled = appControl.isAppInstalled(matchedApp.packageName)
                if (isInstalled) {
                    // Require User Confirmation before sensitive app launch
                    _pendingLaunchConfirmation.value = AppLaunchConfirmation(matchedApp, isInstalled = true)
                    val promptText = if (containsBengali(query)) {
                        "${matchedApp.displayName} চালু করার অনুরোধ এসেছে। আপনি কি এটি ওপেন করতে চান?"
                    } else {
                        "Request received to open ${matchedApp.displayName}. Would you like to launch it now?"
                    }
                    dao.insert(
                        ChatMessage(
                            sender = "AI",
                            text = promptText,
                            actionType = "APP_LAUNCH",
                            actionTargetName = matchedApp.displayName,
                            actionTargetPackage = matchedApp.packageName,
                            actionFallbackUrl = matchedApp.webFallbackUrl,
                            actionStatus = "PENDING_CONFIRMATION"
                        )
                    )
                    if (_isTtsEnabled.value) {
                        ttsHelper?.speak(promptText, if (containsBengali(query)) "bn" else "en")
                    }
                } else {
                    // App is NOT installed -> Explain clearly and provide safe web link or Play Store
                    val explanation = if (containsBengali(query)) {
                        "${matchedApp.displayName} অ্যাপ্লিকেশনটি আপনার ফোনে ইনস্টল করা নেই। আপনি চাইলে এটি ওয়েব ব্রাউজারে ব্যবহার করতে পারেন অথবা প্লে স্টোর থেকে ইনস্টল করতে পারেন।"
                    } else {
                        "${matchedApp.displayName} is not installed on this device. You can open its safe web version or install it from Google Play Store."
                    }
                    dao.insert(
                        ChatMessage(
                            sender = "AI",
                            text = explanation,
                            actionType = "APP_UNAVAILABLE",
                            actionTargetName = matchedApp.displayName,
                            actionTargetPackage = matchedApp.packageName,
                            actionFallbackUrl = matchedApp.webFallbackUrl,
                            actionStatus = "UNAVAILABLE"
                        )
                    )
                    _pendingLaunchConfirmation.value = AppLaunchConfirmation(matchedApp, isInstalled = false)
                    if (_isTtsEnabled.value) {
                        ttsHelper?.speak(explanation, if (containsBengali(query)) "bn" else "en")
                    }
                }
                _uiState.value = AssistantUiState.Idle
                _orbState.value = OrbVisualState.IDLE
                return@launch
            }

            // 5. Default: Stream Natural Conversation via Gemini (Firebase AI Logic)
            _uiState.value = AssistantUiState.Thinking()
            _orbState.value = OrbVisualState.THINKING

            try {
                val recentHistory = dao.getRecentMessages(10).reversed()
                val accumulatedResponse = StringBuilder()

                // Insert placeholder AI message for streaming
                val aiMsgId = dao.insert(
                    ChatMessage(
                        sender = "AI",
                        text = "...",
                        language = userMsg.language
                    )
                )

                aiService.streamResponse(query, recentHistory).collect { token ->
                    accumulatedResponse.append(token)
                    _uiState.value = AssistantUiState.Thinking(accumulatedResponse.toString())
                    dao.update(
                        ChatMessage(
                            id = aiMsgId,
                            sender = "AI",
                            text = accumulatedResponse.toString(),
                            language = userMsg.language
                        )
                    )
                }

                val fullAnswer = accumulatedResponse.toString().trim()
                if (fullAnswer.isNotEmpty()) {
                    _uiState.value = AssistantUiState.Speaking(fullAnswer)
                    _orbState.value = OrbVisualState.SPEAKING
                    if (_isTtsEnabled.value) {
                        ttsHelper?.speak(fullAnswer, userMsg.language)
                    }
                } else {
                    _uiState.value = AssistantUiState.Idle
                    _orbState.value = OrbVisualState.IDLE
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error generating AI response", e)
                val isBn = containsBengali(query)
                val errBn = "Gemini AI সার্ভিসের সাথে সংযোগ করা সম্ভব হয়নি (${e.localizedMessage})। ইন্টারনেট সংযোগ চেক করুন।"
                val errEn = "Could not reach Gemini AI service: ${e.localizedMessage}. Please check internet connection."
                dao.insert(
                    ChatMessage(
                        sender = "AI",
                        text = if (isBn) errBn else errEn,
                        language = if (isBn) "bn" else "en",
                        actionType = "SYSTEM_INFO"
                    )
                )
                _uiState.value = AssistantUiState.Error(errBn, errEn)
                _orbState.value = OrbVisualState.ERROR
            }
        }
    }

    /**
     * User confirmed to launch the application.
     * Never pretends an action succeeded unless launch is verified via package manager.
     */
    fun confirmAppLaunch(confirmation: AppLaunchConfirmation) {
        _pendingLaunchConfirmation.value = null
        val app = confirmation.appTarget
        val launched = appControl.launchApplication(app.packageName)

        viewModelScope.launch {
            if (launched) {
                dao.insert(
                    ChatMessage(
                        sender = "AI",
                        text = "Verified: Successfully launched ${app.displayName}.",
                        actionType = "APP_LAUNCH",
                        actionTargetName = app.displayName,
                        actionStatus = "VERIFIED_LAUNCHED"
                    )
                )
            } else {
                dao.insert(
                    ChatMessage(
                        sender = "AI",
                        text = "Unable to launch ${app.displayName}. Please verify device permissions.",
                        actionType = "APP_LAUNCH",
                        actionTargetName = app.displayName,
                        actionStatus = "FAILED"
                    )
                )
            }
        }
    }

    fun dismissAppLaunchConfirmation() {
        _pendingLaunchConfirmation.value = null
    }

    fun openWebFallback(url: String) {
        appControl.openWebUrl(url)
    }

    fun openInPlayStore(packageName: String) {
        appControl.openPlayStoreForApp(packageName)
    }

    fun speakMessage(message: ChatMessage) {
        ttsHelper?.speak(message.text, message.language)
    }

    fun setLanguage(langCode: String) {
        _activeLanguage.value = langCode
    }

    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled.value = enabled
        if (!enabled) {
            ttsHelper?.stop()
        }
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        ttsHelper?.speechRate = rate
    }

    fun setShowClearHistoryDialog(show: Boolean) {
        _showClearHistoryDialog.value = show
    }

    fun confirmClearHistory() {
        viewModelScope.launch {
            dao.clearAll()
            _showClearHistoryDialog.value = false
        }
    }

    private fun containsBengali(text: String): Boolean {
        for (char in text) {
            if (char in '\u0980'..'\u09FF') return true
        }
        return false
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerHelper?.destroy()
        ttsHelper?.shutdown()
    }
}
