package com.example.domain

/**
 * Model specifications and inference hyper-parameters for each agent.
 */
data class AgentConfig(
    val model: String,
    val slot: KeyVault.Slot,
    val temperature: Double,
    val topP: Double,
    val maxTokens: Int,
    val enableThinking: Boolean
) {
    companion object {
        fun forAgent(type: AgentType, isDeepMode: Boolean = false): AgentConfig {
            return when (type) {
                AgentType.ORCHESTRATOR, AgentType.MERGER -> AgentConfig(
                    model = "nvidia/nemotron-3-ultra-550b-a55b",
                    slot = KeyVault.Slot.KEY1,
                    temperature = 1.0,
                    topP = 0.95,
                    maxTokens = 16384,
                    enableThinking = true
                )
                AgentType.RESEARCHER -> AgentConfig(
                    model = "z-ai/glm-5.3",
                    slot = KeyVault.Slot.KEY2,
                    temperature = 0.5,
                    topP = 1.0,
                    maxTokens = 4096,
                    enableThinking = false
                )
                AgentType.CODER -> AgentConfig(
                    model = "deepseek-ai/deepseek-v4.1-flash",
                    slot = KeyVault.Slot.KEY1,
                    temperature = 1.0,
                    topP = 0.95,
                    maxTokens = 16384,
                    enableThinking = false
                )
                AgentType.VISION -> AgentConfig(
                    model = "minimaxai/minimax-m3.1-flash",
                    slot = KeyVault.Slot.KEY3,
                    temperature = 1.0,
                    topP = 0.95,
                    maxTokens = 8192,
                    enableThinking = false
                )
                AgentType.DEBUGGER -> AgentConfig(
                    model = "nvidia/nemotron-3.5-bolt-e4b",
                    slot = KeyVault.Slot.KEY4,
                    temperature = 1.0,
                    topP = 0.95,
                    maxTokens = 4096,
                    enableThinking = true
                )
                AgentType.HELPER -> AgentConfig(
                    model = "z-ai/glm-5.3-flash",
                    slot = KeyVault.Slot.KEY5,
                    temperature = 0.5,
                    topP = 1.0,
                    maxTokens = 4096,
                    enableThinking = false
                )
            }
        }

        val FALLBACK_CONFIG = AgentConfig(
            model = "z-ai/glm-5.3-flash",
            slot = KeyVault.Slot.KEY5,
            temperature = 0.5,
            topP = 1.0,
            maxTokens = 4096,
            enableThinking = false
        )
    }
}
