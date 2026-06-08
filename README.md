```markdown
# ORCA - ABYSSAL NEON v2.0.0

> *An autonomous AI agent that lives inside your Android phone.*
> *Codename: Abyssal Neon*

---

## 🧠 WHAT IS ORCA?

Orca is an **agentic AI** for Android that can see, plan, and act on your phone just like a human. It uses **Gemini 2.5 Flash** with a **1 million token context window** for visual understanding, task planning, and autonomous execution.

Orca doesn't just respond to commands — it **anticipates needs**, **learns from experience**, and **executes complex multi-step tasks** across any app.

---

## ⚡ CORE CAPABILITIES

| Capability | Description |
|------------|-------------|
| **Full Phone Control** | Tap, swipe, type, gesture — any app, any screen |
| **Visual Understanding** | Screenshot-to-Gemini for UI parsing and verification |
| **Multi-Step Tasks** | Execute 50+ step task chains with error recovery |
| **Cross-App Workflows** | Move data between apps (Browser → Sheets → Email) |
| **AutoPilot Mode** | Proactive agent that anticipates needs |
| **Memory Stream** | Persistent chat history with resume, fork, and merge |
| **Learning System** | Observes user behavior, extracts patterns, improves over time |
| **Security First** | Biometric checkpoints for sensitive actions |
| **Offline Fallback** | Cached workflows when internet is unavailable |
| **Pause & Resume** | Touch-and-hold to pause, notification-based resume with countdown |
| **Multi-Device** | Coordinates across multiple phones to prevent conflicts |

---

## 🏗️ ARCHITECTURE

```

┌─────────────────────────────────────────────────────────────┐
│                        ORCA CORE                            │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌──────────────┐  │
│  │  BRAIN  │  │ MEMORY  │  │EXECUTION │  │    AGENT     │  │
│  ├─────────┤  ├─────────┤  ├──────────┤  ├──────────────┤  │
│  │Conscious│  │ Stream  │  │Bridge    │  │Digital Twin  │  │
│  │Subconsc │  │ Lake    │  │Touch     │  │Goal Manager  │  │
│  │Context  │  │ Graph   │  │Gesture   │  │Task Chain    │  │
│  │Temporal │  │Journal  │  │Parser    │  │Auto Reply    │  │
│  │Emotion  │  │DeepSave │  │Validator │  │Cross App     │  │
│  │Failure  │  │Hygiene  │  │Watchdog  │  │Silent Task   │  │
│  │Social   │  │         │  │Adaptation│  │              │  │
│  └─────────┘  └─────────┘  └──────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────┐ │
│  │ SECURITY │  │  VOICE   │  │   DATA   │  │     UI      │ │
│  ├──────────┤  ├──────────┤  ├──────────┤  ├─────────────┤ │
│  │Manager   │  │TTS Engine│  │Database  │  │Abyssal Neon │ │
│  │Biometric │  │          │  │Gemini API│  │Orb          │ │
│  │Injection │  │          │  │DataStore │  │Command Deck │ │
│  │Credential│  │          │  │Models    │  │Memory View  │ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘

```

---

## 📁 PROJECT STRUCTURE (102 FILES)

```

orca/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
├── proguard-rules.pro
│
└── app/
├── build.gradle.kts
└── src/main/
├── AndroidManifest.xml
├── res/
│   ├── values/strings.xml
│   ├── values/themes.xml
│   ├── values/colors.xml
│   ├── drawable/ic_orca_orb.xml
│   ├── drawable/ic_launcher_background.xml
│   ├── drawable/ic_launcher_foreground.xml
│   ├── mipmap-anydpi-v26/ic_launcher.xml
│   └── xml/
│       ├── accessibility_service_config.xml
│       ├── backup_rules.xml
│       └── data_extraction_rules.xml
│
└── java/com/orca/agent/
├── OrcaApplication.kt
├── MainActivity.kt
│
├── core/ (22 files)
│   ├── CognitiveEngine.kt
│   ├── ScreenState.kt
│   ├── OrcaCore.kt
│   ├── TaskChain.kt
│   ├── TaskExecutor.kt
│   ├── OrcaForegroundService.kt
│   ├── RecoveryManager.kt
│   ├── TaskGuardian.kt
│   ├── TaskPersistenceManager.kt
│   ├── TaskOrchestrator.kt
│   ├── EnvironmentGuardian.kt
│   ├── LatencyOptimizer.kt
│   ├── TrustManager.kt
│   ├── AgentSelfPreservationSystem.kt
│   ├── EmergencyStopHandler.kt
│   ├── EthicalBoundaryGuard.kt
│   ├── SystemStateRespecter.kt
│   ├── MultiDeviceCoordinator.kt
│   ├── PauseResumeManager.kt
│   ├── PauseResumeNotificationManager.kt
│   ├── PauseResumeReceiver.kt
│   └── PauseResumeService.kt
│
├── brain/ (12 files)
│   ├── ConsciousMind.kt
│   ├── SubconsciousEngine.kt
│   ├── IntentPredictor.kt
│   ├── ThreatDetector.kt
│   ├── SkillForge.kt
│   ├── ContextWindowManager.kt
│   ├── OfflineFallbackManager.kt
│   ├── GeminiResponseValidator.kt
│   ├── TemporalContextEngine.kt
│   ├── EmotionalContextDetector.kt
│   ├── FailurePatternRecognizer.kt
│   └── SocialContextAwareness.kt
│
├── memory/ (7 files)
│   ├── MemoryCortex.kt
│   ├── MemoryStream.kt
│   ├── SemanticLake.kt
│   ├── ProceduralGraph.kt
│   ├── EpisodicJournal.kt
│   ├── DeepSave.kt
│   └── ContextHygieneManager.kt
│
├── execution/ (11 files)
│   ├── AccessibilityBridge.kt
│   ├── TouchController.kt
│   ├── GestureEngine.kt
│   ├── ScreenParser.kt
│   ├── AppNavigator.kt
│   ├── ScreenCaptureService.kt
│   ├── OrcaNotificationListener.kt
│   ├── UIElementValidator.kt
│   ├── AccessibilityServiceWatchdog.kt
│   ├── EnvironmentAdaptationEngine.kt
│   └── TouchHoldDetector.kt
│
├── agent/ (5 files)
│   ├── DigitalTwin.kt
│   ├── SilentTask.kt
│   ├── AutoReply.kt
│   ├── CrossAppWorkflow.kt
│   └── GoalManager.kt
│
├── security/ (4 files)
│   ├── SecurityManager.kt
│   ├── BiometricTimeoutHandler.kt
│   ├── SecureCredentialManager.kt
│   └── PromptInjectionGuard.kt
│
├── voice/ (1 file)
│   └── OrcaVoiceEngine.kt
│
├── data/ (5 files)
│   ├── models/ChatSession.kt
│   ├── database/OrcaDatabase.kt
│   ├── network/GeminiApi.kt
│   ├── network/GeminiModels.kt
│   └── datastore/PreferencesDataStore.kt
│
├── di/ (1 file)
│   └── AppModule.kt
│
└── ui/ (14 files)
├── theme/OrcaColors.kt
├── theme/AbyssalNeonTheme.kt
├── components/
│   ├── OrcaOrb.kt
│   ├── CommandDeck.kt
│   ├── ThoughtStream.kt
│   ├── MemoryStreamView.kt
│   └── TaskTimeline.kt
├── screens/
│   ├── MainOrbScreen.kt
│   ├── ChatScreen.kt
│   ├── MemoryStreamScreen.kt
│   ├── SettingsScreen.kt
│   ├── WarRoomScreen.kt
│   ├── MainOrbViewModel.kt
│   └── ChatViewModel.kt
└── OrcaNavGraph.kt

```

---

## 🔧 SETUP

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 34
- Kotlin 1.9.20
- JDK 17
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/)

### Installation

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/orca.git
cd orca

# 2. Set your Gemini API key
# Edit app/build.gradle.kts and replace YOUR_API_KEY
# OR set environment variable:
export GEMINI_API_KEY="your_api_key_here"

# 3. Open in Android Studio
# File → Open → Select the orca/ folder

# 4. Sync Gradle and Build
```

Device Setup

1. Enable Developer Options on your Android device
2. Enable Accessibility Service: Settings → Accessibility → Orca → Enable
3. Grant Overlay Permission: Settings → Apps → Orca → Allow display over other apps
4. Grant Notification Access: Settings → Apps → Special access → Notification access → Orca
5. Disable Battery Optimization for Orca (prevents background kill)

---

🎮 KEY FEATURES

🔴 Pause & Resume

· Pause: Touch and hold anywhere on screen for 3 seconds
· Visual feedback: Red overlay with countdown
· Notification: Shows task progress and pause time
· Resume: Tap RESUME in notification → 10-second countdown → continues
· Two-User Mode: Second user can use phone while Orca waits

🛡️ Safety Systems

· Emergency Stop: Three rapid power button presses
· Biometric Checkpoints: Fingerprint required for payments and passwords
· Prompt Injection Guard: Detects and blocks malicious screen text
· Ethical Boundaries: Refuses harmful, illegal, or privacy-violating commands
· Dead Man's Switch: Auto-shutdown after 14 days of no user interaction

🧠 Intelligence

· Screenshot-to-Gemini: Visual fallback for any UI problem
· Recovery System: Automatic error diagnosis and correction
· Pattern Learning: Extracts user habits for proactive assistance
· Context Hygiene: Prevents knowledge corruption over time
· Emotion Detection: Adapts tone based on user behavior

📱 Environment Awareness

· Battery Protection: Refuses tasks below 5% battery
· Airplane Mode: Pauses cloud-dependent actions
· Data Saver: Warns before large uploads on metered connections
· Screen Recording Detection: Pauses visible actions
· Emergency Call Detection: Completely suspends during calls

---

🎨 DESIGN: ABYSSAL NEON

Element Color
Background #000100 VantaBlack
Primary #FF3A2D Neon Red
Intelligence #00E5FF Cyan
Text #FFFFFF Pure White
Glass #1AFFFFFF with blur

The UI centers on a morphing plasma orb that visualizes Orca's cognitive state:

· Idle: Slow-breathing red
· Planning: Cyan with rotating ring
· Executing: Red with energy lines
· Paused: Static with PAUSED text

---

⚠️ KNOWN LIMITATIONS

Limitation Status
Google Play rejection risk (Accessibility Service policy) Requires accessibility justification
Android 15+ restrictions on background services Mitigation in place, ongoing adaptation needed
Custom UI elements (Flutter, Unity) invisible to accessibility tree Screenshot-to-Gemini fallback
WebView content (browsers) limited accessibility Platform limitation
Cannot universally undo actions OS limitation
Gemini API costs at scale On-device fallback for common tasks
Manufacturer-specific background kill (Xiaomi, Huawei) Requires per-device user setup

---

🤝 CONTRIBUTING

This is the architecture and design for Orca. The files in this repository are placeholder structure ready for implementation.

What needs to be built:

1. Fill each .kt file with the production code from the complete design
2. Implement the Gemini API integration with proper error handling
3. Build the Compose UI with the Abyssal Neon design system
4. Test across Android 8-15 on multiple manufacturers
5. Set up CI/CD for testing and deployment

---

📄 LICENSE

MIT License - See LICENSE file

---

🏴‍☠️ ABYSSAL NEON

```
"We don't build tools. We build digital versions of ourselves."

- Orca v2.0.0
```

---

Orca is not just an app. It's an architecture for agentic AI on mobile devices.
Built to see. Built to think. Built to act.

```
