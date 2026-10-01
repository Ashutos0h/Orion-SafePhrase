# SafePhrase Android Implementation Details

## Architecture Diagram

```
[ UI Layer (Compose + ViewModels) ]
       |                  |
[ Domain Layer (Models + Interfaces) ]
       |                  |
[ Data Layer (Room + DataStore + Retrofit + WorkManager) ]
       |                  
[ Service Layer (MicrophoneService + CallStateListener + AudioCapture) ]
```

## Service Lifecycle
1. **Activity** calls `ContextCompat.startForegroundService(intent)`.
2. **Service** `onCreate()`: Initializes AudioCapture, KeywordDetector.
3. **Service** `onStartCommand()`: Calls `ServiceCompat.startForeground()`, begins audio loop.
4. **Service** `onDestroy()`: Releases AudioRecord, stops coroutines.

## State Machine
- `IDLE`: Initial state.
- `STARTING`: Preparing audio resources.
- `MONITORING`: Actively capturing and analyzing.
- `CALL_ACTIVE`: Phone call in progress, mic paused.
- `MIC_UNAVAILABLE`: Error reading audio (e.g., permission revoked).
- `RECOVERING`: Attempting to reconnect audio.

## Permission Flow
1. Check `RECORD_AUDIO`, `POST_NOTIFICATIONS`, `ACCESS_FINE_LOCATION`.
2. If denied, show rationale in Compose UI.
3. Request via `rememberLauncherForActivityResult`.
4. Proceed only if `RECORD_AUDIO` is granted.

## Emergency Event Flow
1. `KeywordDetector` detects phrase.
2. `RiskVerifier` confirms (confidence >= threshold).
3. `LocationTracker` fetches last known / current location.
4. Create `EmergencyEvent` with `status = PENDING`.
5. Save to Room database.
6. Enqueue WorkManager `SyncWorker`.

## Offline Synchronization Flow
1. `SyncWorker` reads `PENDING` events from Room.
2. Attempts `EmergencyApi.sendEmergencyAlert()`.
3. If success: Update status to `SENT`.
4. If failure: Return `Result.retry()`.
5. WorkManager handles exponential backoff.

## API Integration
Uses Retrofit with Coroutines. Base URL configured via `BuildConfig`.
Endpoints:
- `POST /api/emergency/alert`
- `GET /api/user/settings`
- `GET /api/contacts`

## Known Limitations
- Background execution limits on strict OEMs.
- SpeechRecognizer is not used for continuous monitoring; custom TFLite / dummy KeywordDetector is used as MVP.
