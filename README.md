# Infinity Sandbox 🚀

**Infinity Sandbox** is a native Android multi-agent AI coding sandbox. It coordinates a team of specialized AI agents working in parallel to plan, research, code, debug, and merge complete native Android projects (Kotlin & Java, Jetpack Compose, Gradle) — all within one on-device app, with no external Docker or backend server required.

---

## Architecture & Technology Stack

- **Platform:** Native Android (Min SDK 26, Target SDK 35)
- **UI Framework:** Jetpack Compose with Material Design 3, Dark Cyberpunk & Cosmic Theme
- **Architecture:** MVVM + Clean Architecture (Presentation, Domain, Data)
- **Concurrency:** Kotlin Coroutines & Flow (parallel subtasks with `async` / `awaitAll`)
- **Networking:** OkHttp with real-time SSE streaming (`delta.content` and `delta.reasoning_content`)
- **Local Persistence:** Room Database (chat sessions, messages, project files) & DataStore Preferences
- **Languages:** 
  - **Kotlin:** Primary application logic, Compose UI, ViewModel, Repository, Networking
  - **Java:** High-performance utility classes (`util/ZipExporter.java`, `util/JsonSanitizer.java`, `util/SecretRedactor.java`)

---

## Specialist AI Agent Swarm

| Agent Role | Model | Key Slot | Settings | Responsibilities |
|---|---|---|---|---|
| **Orchestrator** | `nvidia/nemotron-3-ultra-550b-a55b` | KEY1 | Temp 1.0, Top_p 0.95, Thinking ON | Task decomposition & architecture planning |
| **Researcher** | `z-ai/glm-5.3` | KEY2 | Temp 0.5, Top_p 1.0 | Android API documentation, libraries & patterns |
| **UI Analyst (Vision)** | `minimaxai/minimax-m3.1-flash` | KEY3 | Temp 1.0, Top_p 0.95 | Multimodal UI breakdown from screenshots/photos |
| **Coder** | `deepseek-ai/deepseek-v4.1-flash` | KEY1 | Temp 1.0, Top_p 0.95 | Complete Jetpack Compose & Java source files |
| **Debugger** | `nvidia/nemotron-3.5-bolt-e4b` | KEY4 | Temp 1.0, Top_p 0.95, Thinking ON | Static lint, null-safety & compile review (loops) |
| **Fast Helper** | `z-ai/glm-5.3-flash` | KEY5 | Temp 0.5, Top_p 1.0 | Generates unit tests and README.md |
| **Merger** | `nvidia/nemotron-3-ultra-550b-a55b` | KEY1 | Temp 1.0, Top_p 0.95, Thinking ON | Project unification, Gradle & Manifest validation |

---

## Setup & Running in Android Studio

1. **Clone or Open the Project:**
   Open the root directory in Android Studio (Hedgehog 2023.1 or newer).

2. **Configure API Keys:**
   Create or verify `.env` or `local.properties` in the project root:
   ```properties
   NV_KEY1=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
   NV_KEY2=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
   NV_KEY3=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
   NV_KEY4=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
   NV_KEY5=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
   GEMINI_API_KEY=your_gemini_key_here
   ```
   *Note:* Keys are automatically redacted and masked by `SecretRedactor.java` across all UI and error logs.

3. **Sync & Build:**
   Click **File > Sync Project with Gradle Files**.

4. **Run:**
   Select an Android Emulator or connected device running Android 8.0+ (API 26+) and press **Run 'app'**.
