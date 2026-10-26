package com.example.data.repository

import android.util.Log
import com.example.data.remote.AgentStreamChunk
import com.example.data.remote.NvidiaApiClient
import com.example.domain.AgentConfig
import com.example.domain.AgentType
import com.example.domain.PipelineMode
import com.example.domain.Prompts
import com.example.domain.model.AgentExecutionState
import com.example.domain.model.AgentStepStatus
import com.example.domain.model.DebugIssue
import com.example.domain.model.DebugResult
import com.example.domain.model.ProjectSnapshot
import com.example.domain.model.Subtask
import com.example.util.JsonSanitizer
import com.example.util.SecretRedactor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject

class MultiAgentPipelineRunner(
    private val apiClient: NvidiaApiClient = NvidiaApiClient()
) {

    companion object {
        private const val TAG = "PipelineRunner"
    }

    /**
     * Executes the multi-agent AI pipeline according to the chosen PipelineMode.
     */
    suspend fun executePipeline(
        userPrompt: String,
        base64Image: String?,
        currentFiles: Map<String, String>,
        mode: PipelineMode,
        onAgentUpdate: (AgentType, AgentExecutionState) -> Unit
    ): ProjectSnapshot = coroutineScope {
        val workingFiles = currentFiles.toMutableMap()
        val isDeep = mode == PipelineMode.DEEP
        val maxDebugLoops = when (mode) {
            PipelineMode.FAST -> 1
            PipelineMode.AUTO -> 2
            PipelineMode.DEEP -> 3
        }

        // Initialize agent states to WAITING
        val activeAgents = if (mode == PipelineMode.FAST) {
            listOf(AgentType.CODER, AgentType.DEBUGGER, AgentType.HELPER, AgentType.MERGER)
        } else {
            val list = mutableListOf(AgentType.ORCHESTRATOR, AgentType.RESEARCHER)
            if (!base64Image.isNullOrBlank()) list.add(AgentType.VISION)
            list.addAll(listOf(AgentType.CODER, AgentType.DEBUGGER, AgentType.HELPER, AgentType.MERGER))
            list
        }

        activeAgents.forEach { type ->
            onAgentUpdate(type, AgentExecutionState(agentType = type, status = AgentStepStatus.WAITING))
        }

        var orchestratorNotes = ""
        var researcherNotes = ""
        var visionNotes = ""

        if (mode != PipelineMode.FAST) {
            // STEP 1: Orchestrator planning
            orchestratorNotes = runAgentStep(
                type = AgentType.ORCHESTRATOR,
                config = AgentConfig.forAgent(AgentType.ORCHESTRATOR, isDeep),
                systemPrompt = Prompts.ORCHESTRATOR,
                userPrompt = buildOrchestratorPrompt(userPrompt, currentFiles),
                base64Image = null,
                onAgentUpdate = onAgentUpdate
            )

            // STEP 2 & 3: Run Researcher and Vision in parallel
            val researchDeferred = async {
                runAgentStep(
                    type = AgentType.RESEARCHER,
                    config = AgentConfig.forAgent(AgentType.RESEARCHER, isDeep),
                    systemPrompt = Prompts.RESEARCHER,
                    userPrompt = "Task: $userPrompt\nPlan: $orchestratorNotes",
                    base64Image = null,
                    onAgentUpdate = onAgentUpdate
                )
            }

            val visionDeferred = if (!base64Image.isNullOrBlank()) {
                async {
                    runAgentStep(
                        type = AgentType.VISION,
                        config = AgentConfig.forAgent(AgentType.VISION, isDeep),
                        systemPrompt = Prompts.VISION,
                        userPrompt = "Analyze this UI screenshot and specify the components and styling for Compose.",
                        base64Image = base64Image,
                        onAgentUpdate = onAgentUpdate
                    )
                }
            } else null

            researcherNotes = researchDeferred.await()
            visionNotes = visionDeferred?.await().orEmpty()
        }

        // STEP 4: Coder generates files
        val coderPrompt = buildCoderPrompt(
            userPrompt = userPrompt,
            orchestratorNotes = orchestratorNotes,
            researcherNotes = researcherNotes,
            visionNotes = visionNotes,
            existingFiles = workingFiles
        )

        var coderOutput = runAgentStep(
            type = AgentType.CODER,
            config = AgentConfig.forAgent(AgentType.CODER, isDeep),
            systemPrompt = Prompts.CODER,
            userPrompt = coderPrompt,
            base64Image = null,
            onAgentUpdate = onAgentUpdate
        )

        var newFiles = extractFilesFromJson(coderOutput)
        workingFiles.putAll(newFiles)

        // STEP 5: Debugger review loop (up to maxDebugLoops)
        var debugLoop = 0
        while (debugLoop < maxDebugLoops) {
            debugLoop++
            val debugOutput = runAgentStep(
                type = AgentType.DEBUGGER,
                config = AgentConfig.forAgent(AgentType.DEBUGGER, isDeep),
                systemPrompt = Prompts.DEBUGGER,
                userPrompt = "Inspect these Android project files for bugs and compilation issues:\n" + formatFilesSummary(workingFiles),
                base64Image = null,
                onAgentUpdate = onAgentUpdate
            )

            val debugResult = parseDebugResult(debugOutput)
            if (debugResult.ok || debugResult.issues.isEmpty()) {
                Log.d(TAG, "Debugger approved files at loop $debugLoop")
                break
            } else {
                Log.d(TAG, "Debugger found ${debugResult.issues.size} issues. Re-running Coder...")
                // Request fixes from Coder
                val fixPrompt = "Fix the following issues in the project files:\n" +
                        debugResult.issues.joinToString("\n") { "- In ${it.file}: ${it.problem}. Fix: ${it.fix}" } +
                        "\nOutput ONLY JSON {\"files\":{\"path\":\"content\"}}."

                val fixOutput = runAgentStep(
                    type = AgentType.CODER,
                    config = AgentConfig.forAgent(AgentType.CODER, isDeep),
                    systemPrompt = Prompts.CODER,
                    userPrompt = fixPrompt,
                    base64Image = null,
                    onAgentUpdate = onAgentUpdate
                )
                workingFiles.putAll(extractFilesFromJson(fixOutput))
            }
        }

        // STEP 6: Fast Helper generates tests and README
        val helperOutput = runAgentStep(
            type = AgentType.HELPER,
            config = AgentConfig.forAgent(AgentType.HELPER, isDeep),
            systemPrompt = Prompts.HELPER,
            userPrompt = "Generate unit tests in src/test/java and a README.md for this app:\n" + formatFilesSummary(workingFiles),
            base64Image = null,
            onAgentUpdate = onAgentUpdate
        )
        workingFiles.putAll(extractFilesFromJson(helperOutput))

        // STEP 7: Merger combines into one consistent buildable project
        val mergerPrompt = "Unify all the following Android files into a complete, buildable project with settings.gradle.kts, build.gradle.kts, app/build.gradle.kts, and AndroidManifest.xml:\n" +
                formatFilesSummary(workingFiles)

        val mergerOutput = runAgentStep(
            type = AgentType.MERGER,
            config = AgentConfig.forAgent(AgentType.MERGER, isDeep),
            systemPrompt = Prompts.MERGER,
            userPrompt = mergerPrompt,
            base64Image = null,
            onAgentUpdate = onAgentUpdate
        )
        val finalMergedFiles = extractFilesFromJson(mergerOutput)
        if (finalMergedFiles.isNotEmpty()) {
            workingFiles.putAll(finalMergedFiles)
        }

        // Ensure mandatory Gradle & Manifest structure exists if omitted by model
        ensureStandardProjectStructure(workingFiles, userPrompt)

        ProjectSnapshot(files = workingFiles, timestamp = System.currentTimeMillis())
    }

    private suspend fun runAgentStep(
        type: AgentType,
        config: AgentConfig,
        systemPrompt: String,
        userPrompt: String,
        base64Image: String?,
        onAgentUpdate: (AgentType, AgentExecutionState) -> Unit
    ): String {
        val startTime = System.currentTimeMillis()
        var accumulatedContent = ""
        var accumulatedThinking = ""

        onAgentUpdate(
            type,
            AgentExecutionState(
                agentType = type,
                status = AgentStepStatus.RUNNING,
                elapsedMs = 0L,
                liveContent = "",
                liveThinking = ""
            )
        )

        try {
            val result = apiClient.streamChatCompletion(
                config = config,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                base64Image = base64Image
            ) { chunk ->
                val elapsed = System.currentTimeMillis() - startTime
                when (chunk) {
                    is AgentStreamChunk.Content -> {
                        accumulatedContent += chunk.text
                        onAgentUpdate(
                            type,
                            AgentExecutionState(
                                agentType = type,
                                status = AgentStepStatus.RUNNING,
                                elapsedMs = elapsed,
                                liveContent = accumulatedContent,
                                liveThinking = accumulatedThinking
                            )
                        )
                    }
                    is AgentStreamChunk.Thinking -> {
                        accumulatedThinking += chunk.text
                        onAgentUpdate(
                            type,
                            AgentExecutionState(
                                agentType = type,
                                status = AgentStepStatus.RUNNING,
                                elapsedMs = elapsed,
                                liveContent = accumulatedContent,
                                liveThinking = accumulatedThinking
                            )
                        )
                    }
                }
            }

            val totalElapsed = System.currentTimeMillis() - startTime
            onAgentUpdate(
                type,
                AgentExecutionState(
                    agentType = type,
                    status = AgentStepStatus.DONE,
                    elapsedMs = totalElapsed,
                    liveContent = result,
                    liveThinking = accumulatedThinking
                )
            )
            return result
        } catch (e: Exception) {
            val totalElapsed = System.currentTimeMillis() - startTime
            val safeError = SecretRedactor.redact(e.message ?: "Execution error")
            onAgentUpdate(
                type,
                AgentExecutionState(
                    agentType = type,
                    status = AgentStepStatus.FAILED,
                    elapsedMs = totalElapsed,
                    liveContent = accumulatedContent,
                    liveThinking = accumulatedThinking,
                    errorMessage = safeError
                )
            )
            throw e
        }
    }

    private fun buildOrchestratorPrompt(userPrompt: String, currentFiles: Map<String, String>): String {
        return buildString {
            append("User Goal: ").append(userPrompt).append("\n")
            if (currentFiles.isNotEmpty()) {
                append("Existing Project Files:\n")
                currentFiles.keys.take(25).forEach { path ->
                    append("- ").append(path).append("\n")
                }
                append("Plan modifications and additions to extend this project.\n")
            }
        }
    }

    private fun buildCoderPrompt(
        userPrompt: String,
        orchestratorNotes: String,
        researcherNotes: String,
        visionNotes: String,
        existingFiles: Map<String, String>
    ): String {
        return buildString {
            append("Create Android Kotlin / Jetpack Compose code for: ").append(userPrompt).append("\n")
            if (orchestratorNotes.isNotBlank()) {
                append("\nArchitecture Plan:\n").append(orchestratorNotes).append("\n")
            }
            if (researcherNotes.isNotBlank()) {
                append("\nTechnical Notes:\n").append(researcherNotes).append("\n")
            }
            if (visionNotes.isNotBlank()) {
                append("\nUI Visual Specification:\n").append(visionNotes).append("\n")
            }
            if (existingFiles.isNotEmpty()) {
                append("\nCurrent Files in Project:\n")
                append(formatFilesSummary(existingFiles))
            }
            append("\nOutput ONLY JSON {\"files\":{\"path\":\"content\"}}.")
        }
    }

    private fun extractFilesFromJson(jsonText: String): Map<String, String> {
        val files = mutableMapOf<String, String>()
        try {
            val rootObj = JsonSanitizer.parseJsonObject(jsonText)
            if (rootObj.has("files")) {
                val filesObj = rootObj.getJSONObject("files")
                val keys = filesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    files[key] = filesObj.getString(key)
                }
            } else {
                // If top-level object directly has paths as keys
                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key.contains("/") || key.endsWith(".kt") || key.endsWith(".java") || key.endsWith(".xml")) {
                        files[key] = rootObj.getString(key)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse files from JSON: ${e.message}")
        }
        return files
    }

    private fun parseDebugResult(debugText: String): DebugResult {
        return try {
            val root = JsonSanitizer.parseJsonObject(debugText)
            val ok = root.optBoolean("ok", false)
            val issuesArray = root.optJSONArray("issues")
            val issues = mutableListOf<DebugIssue>()
            if (issuesArray != null) {
                for (i in 0 until issuesArray.length()) {
                    val obj = issuesArray.getJSONObject(i)
                    issues.add(
                        DebugIssue(
                            file = obj.optString("file", "unknown"),
                            problem = obj.optString("problem", "issue found"),
                            fix = obj.optString("fix", "apply fix")
                        )
                    )
                }
            }
            DebugResult(ok = ok, issues = issues)
        } catch (e: Exception) {
            DebugResult(ok = true, issues = emptyList())
        }
    }

    private fun formatFilesSummary(files: Map<String, String>): String {
        val sb = StringBuilder()
        files.entries.take(15).forEach { (path, content) ->
            sb.append("--- ").append(path).append(" ---\n")
            val preview = if (content.length > 500) content.take(500) + "\n...[truncated]" else content
            sb.append(preview).append("\n\n")
        }
        return sb.toString()
    }

    private fun ensureStandardProjectStructure(files: MutableMap<String, String>, goal: String) {
        if (!files.containsKey("settings.gradle.kts")) {
            files["settings.gradle.kts"] = """
                rootProject.name = "SandboxApp"
                include(":app")
            """.trimIndent()
        }
        if (!files.containsKey("build.gradle.kts")) {
            files["build.gradle.kts"] = """
                plugins {
                    id("com.android.application") version "8.7.0" apply false
                    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
                }
            """.trimIndent()
        }
        if (!files.containsKey("README.md")) {
            files["README.md"] = """
                # Generated Android App - Infinity Sandbox
                
                Goal: $goal
                
                ## How to Run
                1. Open in Android Studio Hedgehog or newer.
                2. Sync Gradle files.
                3. Run on an Android 8.0+ (API 26+) emulator or physical device.
            """.trimIndent()
        }
    }
}
