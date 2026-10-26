package com.example.domain.model

import com.example.domain.AgentType

enum class AgentStepStatus {
    WAITING,
    RUNNING,
    DONE,
    FAILED
}

data class AgentExecutionState(
    val agentType: AgentType,
    val status: AgentStepStatus = AgentStepStatus.WAITING,
    val elapsedMs: Long = 0L,
    val liveContent: String = "",
    val liveThinking: String = "",
    val isExpanded: Boolean = false,
    val isFallbackUsed: Boolean = false,
    val errorMessage: String? = null
)

data class Subtask(
    val id: String,
    val role: String,
    val goal: String,
    val dependsOn: List<String> = emptyList()
)

data class DebugIssue(
    val file: String,
    val problem: String,
    val fix: String
)

data class DebugResult(
    val ok: Boolean,
    val issues: List<DebugIssue>
)

data class ProjectSnapshot(
    val files: Map<String, String> = emptyMap(),
    val totalFiles: Int = files.size,
    val timestamp: Long = System.currentTimeMillis()
)
