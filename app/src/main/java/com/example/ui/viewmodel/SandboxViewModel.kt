package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SandboxDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.preferences.SettingsDataStore
import com.example.data.repository.SandboxRepository
import com.example.domain.AgentType
import com.example.domain.PipelineMode
import com.example.domain.model.AgentExecutionState
import com.example.domain.model.AgentStepStatus
import com.example.util.SecretRedactor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SandboxViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SandboxDatabase.getInstance(application)
    private val settingsDataStore = SettingsDataStore(application)
    private val repository = SandboxRepository(database, settingsDataStore)

    private val _currentSessionId = MutableStateFlow<String>("")
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentMode: StateFlow<PipelineMode> = repository.currentMode
        .stateIn(viewModelScope, SharingStarted.Lazily, PipelineMode.AUTO)

    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId
        .flatMapLatest { id ->
            if (id.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.getMessagesForSession(id)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val projectFiles: StateFlow<Map<String, String>> = _currentSessionId
        .flatMapLatest { id ->
            if (id.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyMap())
            else repository.getProjectFilesForSession(id)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _agentStates = MutableStateFlow<Map<AgentType, AgentExecutionState>>(emptyMap())
    val agentStates: StateFlow<Map<AgentType, AgentExecutionState>> = _agentStates.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isListeningVoice = MutableStateFlow(false)
    val isListeningVoice: StateFlow<Boolean> = _isListeningVoice.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var activeJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        viewModelScope.launch {
            // Restore last active session or create initial session
            repository.allSessions.collect { list ->
                if (list.isNotEmpty() && _currentSessionId.value.isEmpty()) {
                    _currentSessionId.value = list.first().id
                } else if (list.isEmpty() && _currentSessionId.value.isEmpty()) {
                    val newId = repository.createNewSession()
                    _currentSessionId.value = newId
                }
            }
        }
    }

    fun onInputTextChange(text: String) {
        _inputText.value = text
    }

    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun setPipelineMode(mode: PipelineMode) {
        viewModelScope.launch {
            repository.setPipelineMode(mode)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            repository.setDarkTheme(!isDarkTheme.value)
        }
    }

    fun switchSession(sessionId: String) {
        cancelCurrentExecution()
        _currentSessionId.value = sessionId
        _agentStates.value = emptyMap()
    }

    fun createNewChat() {
        viewModelScope.launch {
            cancelCurrentExecution()
            val newId = repository.createNewSession()
            _currentSessionId.value = newId
            _agentStates.value = emptyMap()
            _inputText.value = ""
            _selectedImageUri.value = null
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameSession(sessionId, newTitle)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = ""
                _agentStates.value = emptyMap()
            }
        }
    }

    fun clearChat() {
        val sessionId = _currentSessionId.value
        if (sessionId.isNotEmpty()) {
            viewModelScope.launch {
                repository.clearSessionChat(sessionId)
                _agentStates.value = emptyMap()
            }
        }
    }

    /**
     * Sends the current user prompt, attaches optional image, and kicks off the multi-agent pipeline.
     */
    fun sendTask() {
        val prompt = _inputText.value.trim()
        val imageUri = _selectedImageUri.value
        val sessionId = _currentSessionId.value

        if (prompt.isBlank() && imageUri == null) return
        if (sessionId.isBlank()) return

        // Clear input bar fields
        _inputText.value = ""
        _selectedImageUri.value = null
        _isRunning.value = true
        _errorMessage.value = null

        activeJob = viewModelScope.launch {
            try {
                // 1. Save user message to database
                repository.saveUserMessage(
                    sessionId = sessionId,
                    text = prompt,
                    imageUri = imageUri?.toString()
                )

                // 2. Convert image to base64 if present
                val base64Image = imageUri?.let { uri ->
                    convertUriToBase64(getApplication(), uri)
                }

                // 3. Run multi-agent pipeline
                val snapshot = repository.runAgentPipeline(
                    sessionId = sessionId,
                    userPrompt = prompt,
                    base64Image = base64Image,
                    mode = currentMode.value,
                    onAgentUpdate = { type, state ->
                        val updated = _agentStates.value.toMutableMap()
                        updated[type] = state
                        _agentStates.value = updated
                    }
                )

                Log.d("SandboxViewModel", "Pipeline completed with ${snapshot.files.size} files")
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d("SandboxViewModel", "Pipeline cancelled by user")
            } catch (e: Exception) {
                val safeErr = SecretRedactor.redact(e.message ?: "Pipeline execution error")
                _errorMessage.value = safeErr
                Log.e("SandboxViewModel", "Pipeline error: $safeErr", e)
            } finally {
                _isRunning.value = false
            }
        }
    }

    /**
     * Cancels any running agent pipeline immediately.
     */
    fun cancelCurrentExecution() {
        activeJob?.cancel()
        activeJob = null
        _isRunning.value = false

        // Update running agents to FAILED/Cancelled state
        val updated = _agentStates.value.toMutableMap()
        updated.forEach { (type, state) ->
            if (state.status == AgentStepStatus.RUNNING) {
                updated[type] = state.copy(
                    status = AgentStepStatus.FAILED,
                    errorMessage = "Execution stopped by user"
                )
            }
        }
        _agentStates.value = updated
    }

    /**
     * Exports the current project to a ZIP file.
     */
    suspend fun exportProjectZip(): File? {
        val sessionId = _currentSessionId.value
        if (sessionId.isBlank()) return null
        return repository.exportProjectZip(sessionId, getApplication<Application>().cacheDir)
    }

    /**
     * Initializes and toggles SpeechRecognizer for voice typing.
     */
    fun toggleVoiceTyping() {
        if (_isListeningVoice.value) {
            stopVoiceTyping()
        } else {
            startVoiceTyping()
        }
    }

    private fun startVoiceTyping() {
        val context = getApplication<Application>()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorMessage.value = "Speech recognition is not available on this device"
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListeningVoice.value = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isListeningVoice.value = false
                    }
                    override fun onError(error: Int) {
                        _isListeningVoice.value = false
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            val existing = _inputText.value
                            _inputText.value = if (existing.isBlank()) recognized else "$existing $recognized"
                        }
                        _isListeningVoice.value = false
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListeningVoice.value = false
            _errorMessage.value = "Voice typing error: ${e.message}"
        }
    }

    private fun stopVoiceTyping() {
        speechRecognizer?.stopListening()
        _isListeningVoice.value = false
    }

    private fun convertUriToBase64(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                // Compress to JPEG for high quality yet fast inference upload
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e("SandboxViewModel", "Failed to decode image: ${e.message}")
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
        activeJob?.cancel()
    }
}
