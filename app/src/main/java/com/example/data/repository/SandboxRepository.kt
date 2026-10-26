package com.example.data.repository

import android.content.Context
import com.example.data.local.SandboxDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.preferences.SettingsDataStore
import com.example.domain.AgentType
import com.example.domain.PipelineMode
import com.example.domain.model.AgentExecutionState
import com.example.domain.model.ProjectSnapshot
import com.example.util.ZipExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

class SandboxRepository(
    private val database: SandboxDatabase,
    private val settingsDataStore: SettingsDataStore,
    private val pipelineRunner: MultiAgentPipelineRunner = MultiAgentPipelineRunner()
) {

    val allSessions: Flow<List<ChatSessionEntity>> = database.chatDao().getAllSessions()

    val currentMode: Flow<PipelineMode> = settingsDataStore.pipelineModeFlow
    val isDarkTheme: Flow<Boolean> = settingsDataStore.isDarkThemeFlow

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> =
        database.chatDao().getMessagesForSession(sessionId)

    fun getProjectFilesForSession(sessionId: String): Flow<Map<String, String>> =
        database.projectDao().getFilesForSessionFlow(sessionId).map { list ->
            list.associate { it.path to it.content }
        }

    suspend fun setPipelineMode(mode: PipelineMode) {
        settingsDataStore.setPipelineMode(mode)
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        settingsDataStore.setDarkTheme(isDark)
    }

    suspend fun createNewSession(): String = withContext(Dispatchers.IO) {
        val newId = UUID.randomUUID().toString()
        val session = ChatSessionEntity(
            id = newId,
            title = "New Project",
            lastMessagePreview = "Ready to build"
        )
        database.chatDao().insertOrUpdateSession(session)
        newId
    }

    suspend fun renameSession(sessionId: String, newTitle: String) = withContext(Dispatchers.IO) {
        database.chatDao().updateSessionTitle(sessionId, newTitle)
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        database.chatDao().deleteSession(sessionId)
        database.chatDao().deleteMessagesForSession(sessionId)
        database.projectDao().clearFilesForSession(sessionId)
    }

    suspend fun clearSessionChat(sessionId: String) = withContext(Dispatchers.IO) {
        database.chatDao().deleteMessagesForSession(sessionId)
        database.projectDao().clearFilesForSession(sessionId)
    }

    suspend fun saveUserMessage(
        sessionId: String,
        text: String,
        imageUri: String? = null
    ): ChatMessageEntity = withContext(Dispatchers.IO) {
        val message = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            text = text,
            imageUri = imageUri
        )
        database.chatDao().insertMessage(message)
        // Update session title if first message
        val existingMessages = database.chatDao().getSessionById(sessionId)
        if (existingMessages?.title == "New Project" || existingMessages?.title.isNullOrBlank()) {
            val title = if (text.length > 28) text.take(28) + "..." else text
            database.chatDao().updateSessionTitle(sessionId, title)
        }
        message
    }

    suspend fun runAgentPipeline(
        sessionId: String,
        userPrompt: String,
        base64Image: String?,
        mode: PipelineMode,
        onAgentUpdate: (AgentType, AgentExecutionState) -> Unit
    ): ProjectSnapshot = withContext(Dispatchers.IO) {
        // Fetch current project files for incremental follow-ups
        val currentEntities = database.projectDao().getFilesForSession(sessionId)
        val currentFiles = currentEntities.associate { it.path to it.content }

        val snapshot = pipelineRunner.executePipeline(
            userPrompt = userPrompt,
            base64Image = base64Image,
            currentFiles = currentFiles,
            mode = mode,
            onAgentUpdate = onAgentUpdate
        )

        // Persist updated files in Room
        database.projectDao().replaceSessionFiles(sessionId, snapshot.files)

        // Save assistant completion message in Room
        val filesJson = JSONObject().apply {
            snapshot.files.forEach { (path, content) ->
                put(path, content)
            }
        }.toString()

        val assistantMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "assistant",
            text = "Project updated successfully with ${snapshot.files.size} files.",
            isFinalResult = true,
            projectJson = filesJson
        )
        database.chatDao().insertMessage(assistantMessage)

        snapshot
    }

    /**
     * Exports session files into a ZIP archive using the Java ZipExporter utility.
     */
    suspend fun exportProjectZip(sessionId: String, cacheDir: File): File = withContext(Dispatchers.IO) {
        val entities = database.projectDao().getFilesForSession(sessionId)
        val files = entities.associate { it.path to it.content }
        val exportsDir = File(cacheDir, "exports")
        ZipExporter.exportProjectToZip(files, exportsDir)
    }
}
