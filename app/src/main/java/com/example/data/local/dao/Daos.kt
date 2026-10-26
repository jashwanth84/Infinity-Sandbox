package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.ProjectFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("UPDATE chat_sessions SET title = :newTitle, updatedAt = :timestamp WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: String, newTitle: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)
}

@Dao
interface ProjectDao {

    @Query("SELECT * FROM project_files WHERE sessionId = :sessionId")
    fun getFilesForSessionFlow(sessionId: String): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE sessionId = :sessionId")
    suspend fun getFilesForSession(sessionId: String): List<ProjectFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFileEntity>)

    @Query("DELETE FROM project_files WHERE sessionId = :sessionId")
    suspend fun clearFilesForSession(sessionId: String)

    @Transaction
    suspend fun replaceSessionFiles(sessionId: String, files: Map<String, String>) {
        clearFilesForSession(sessionId)
        val entities = files.map { (path, content) ->
            ProjectFileEntity(
                sessionId = sessionId,
                path = path,
                content = content
            )
        }
        insertFiles(entities)
    }
}
