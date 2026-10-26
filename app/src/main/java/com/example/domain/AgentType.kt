package com.example.domain

enum class AgentType(val displayName: String, val description: String) {
    ORCHESTRATOR("Orchestrator", "Task decomposition & architecture planning"),
    RESEARCHER("Researcher", "Technical notes, patterns & API docs"),
    VISION("UI Analyst", "Multimodal UI & layout breakdown"),
    CODER("Coder", "Complete Kotlin & Compose implementation"),
    DEBUGGER("Debugger", "Static analysis, lint & safety review"),
    HELPER("Fast Helper", "Unit tests & project documentation"),
    MERGER("Merger", "Project unification & Gradle validation")
}

enum class PipelineMode(val label: String, val description: String) {
    AUTO("Auto", "Full pipeline with specialist agents"),
    FAST("Fast", "Coder & Helper with 1 debug pass"),
    DEEP("Deep", "All agents with thinking enabled & 3 debug loops")
}
