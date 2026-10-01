# SafePhrase Android — Industry-Grade MVP

A native Android application for **continuous background keyword monitoring and emergency alerting**.

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                         UI Layer                                 │
│  Jetpack Compose · Navigation · ViewModels · StateFlow           │
│  Screens: Onboarding · Dashboard · Setup · Contacts ·            │
│           History · Settings · Debug Console                     │
└───────────────────┬──────────────────────────────────────────────┘
                    │
┌───────────────────▼──────────────────────────────────────────────┐
│                       Domain Layer                               │
│  Pure Kotlin models · Repository interfaces · Use cases          │
│  ServiceState (state machine) · EmergencyEvent · SafePhrase      │
└──────┬────────────┬─────────────────────────────┬───────────────┘
       │            │                             │
┌──────▼──┐  ┌──────▼──────────┐  ┌──────────────▼───────────────┐
│  Data   │  │   Service       │  │   Sync / Network             │
│  Layer  │  │   Layer         │  │   Layer                      │
│         │  │                 │  │                              │
│ Room    │  │ MicMonitorSvc   │  │ AlertSyncWorker (WorkManager) │
│ DataStr │  │ AudioCapture    │  │ Retrofit + OkHttp            │
│ Retrofit│  │ KeywordDetector │  │ EmergencyApi (FastAPI)       │
│ Repos   │  │ RiskVerifier    │  │ Exponential backoff retry    │
└─────────┘  │ CallStateMonitor│  └──────────────────────────────┘
             │ NotificationHelp│
             │ LocationProvider│
             └─────────────────┘
                    │
         Hilt DI wires all layers
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| DI | Hilt + KSP |
| Local DB | Room |
| Preferences | DataStore |
| Background | Foreground Service (microphone type) |
| Sync | WorkManager (exponential backoff) |
| Network | Retrofit + OkHttp |
| Location | Fused Location Provider |
| Logging | Timber |
| Architecture | MVVM + Clean Architecture |

---

## Setup Instructions

1. Open `C:\Users\vashu\Desktop\Orion` in **Android Studio Hedgehog (2023.1.1+)**.
2. Let Gradle sync — all dependencies are downloaded automatically.
3. Set your backend URL in [`app/build.gradle.kts`](app/build.gradle.kts):
   ```kotlin
   // Debug (emulator → host machine)
   buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/\"")
   // Release
   buildConfigField("String", "BASE_URL", "\"https://api.yourbackend.io/\"")
   ```
4. Build → Run on emulator or device.

---

## Permissions

| Permission | Purpose |
|---|---|
| `RECORD_AUDIO` | Microphone monitoring |
| `FOREGROUND_SERVICE` | Keep service alive |
| `FOREGROUND_SERVICE_MICROPHONE` | Android 14 microphone service type |
| `POST_NOTIFICATIONS` | Monitoring notification (Android 13+) |
| `ACCESS_FINE_LOCATION` | Emergency location |
| `READ_PHONE_STATE` | Call detection |
| `INTERNET` / `ACCESS_NETWORK_STATE` | Alert sync |
| `RECEIVE_BOOT_COMPLETED` | Boot reminder (no auto-start) |

---

## How Microphone Foreground Service Works

1. User taps **Start Protection** → `ContextCompat.startForegroundService()`.
2. `MicrophoneMonitorService.onStartCommand()` calls `ServiceCompat.startForeground()` with `FOREGROUND_SERVICE_TYPE_MICROPHONE`.
3. A persistent notification appears — status updates in real time.
4. `AudioCaptureEngine` starts an `AudioRecord` session and emits 30ms PCM-16 frames via a bounded `SharedFlow`.
5. `KeywordDetector.processFrame()` runs energy/ZCR heuristics (MVP) — swap for TFLite without changing service code.
6. On detection, `RiskVerifier` applies a secondary confidence gate.
7. `TriggerEmergencyUseCase` checks cooldown, fetches location, and persists an `EmergencyEvent` to Room.
8. `AlertSyncWorker` is enqueued; WorkManager delivers the alert when connectivity is available.

---

## How Offline Alerts Work

```
SafePhrase detected
    ↓
EmergencyEvent saved to Room (status=PENDING)   ← survives process death
    ↓
AlertSyncWorker enqueued (requires CONNECTED)
    ↓
Network available → POST /api/v1/emergency/alert
    ↓
Success → status=SENT
Failure → WorkManager retries (exponential backoff, max 4 attempts)
```

---

## Phone-Call Limitations

When a cellular call is active:
- `CallStateMonitor` receives `ACTION_PHONE_STATE_CHANGED`.
- Service state transitions to `CALL_ACTIVE`.
- `AudioCaptureEngine.pauseCapture()` is called — no crash, no aggressive retry.
- Notification updates to "Call Active — monitoring paused".
- When call ends → bounded recovery (up to 3 attempts, 2s apart).
- If recovery fails → `MIC_UNAVAILABLE` state shown in UI.

> **Platform limitation**: A normal third-party app cannot capture microphone input simultaneously with an active cellular call on most devices. This is correct and expected behavior — not a bug.

---

## Known Android Limitations

1. **OEM battery management**: Manufacturers (Samsung One UI, Xiaomi MIUI, etc.) may aggressively kill background services. Users should exclude SafePhrase from battery optimization.
2. **Reboot**: Android 12+ restricts microphone foreground services from auto-starting on boot. After reboot, protection must be manually restarted. `BootReceiver` exists for a future reminder notification.
3. **Force-stop**: If the user force-stops the app from Settings, Android guarantees the service will not restart.
4. **Keyword detection accuracy**: The MVP uses a simple energy/ZCR heuristic. A production model requires a trained TFLite keyword-spotting model. Substitute `HeuristicKeywordDetector` with a `TFLiteKeywordDetector` implementation without changing any other code.
5. **AI verification**: Risk verification is described as reducing false positives, not guaranteeing emergency intent.

---

## Build Commands (Android Studio Terminal)

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease

# Run tests
./gradlew test
```

---

## Project Structure

```
app/src/main/java/com/safephrase/
├── core/
│   ├── config/      AppConfig.kt — all tunable constants
│   ├── di/          AppModules.kt — Hilt modules
│   └── util/        SafeLogger.kt, Result.kt
├── domain/
│   ├── model/       DomainModels.kt — state machine, entities
│   ├── repository/  Repositories.kt — interfaces
│   └── usecase/     UseCases.kt — business rules
├── data/
│   ├── local/db/    SafePhraseDatabase, DAOs, Entities
│   ├── local/prefs  UserPreferencesDataStore
│   ├── remote/      SafePhraseApi, DTOs
│   └── repository/  RepositoryImpls.kt — with mappers
├── service/
│   ├── MicrophoneMonitorService.kt  ← CORE foreground service
│   ├── CallStateMonitor.kt
│   └── NotificationHelper.kt
├── audio/
│   ├── AudioCaptureEngine.kt
│   ├── KeywordDetector.kt    ← replace with TFLite here
│   └── AudioModule.kt
├── location/        LocationProvider.kt
├── sync/            AlertSyncWorker.kt
├── receiver/        BootReceiver.kt
└── ui/
    ├── navigation/  Screen.kt, NavGraph.kt
    ├── theme/       Color.kt, Theme.kt
    └── screen/
        ├── dashboard/   DashboardScreen + ViewModel
        ├── onboarding/  OnboardingScreen, PermissionsGate
        ├── setup/       PhraseSetupScreen + ViewModel
        ├── contacts/    ContactsScreen + ViewModel
        ├── history/     HistoryScreen + ViewModel
        ├── settings/    SettingsScreen + ViewModel
        └── debug/       DebugScreen + ViewModel
```
