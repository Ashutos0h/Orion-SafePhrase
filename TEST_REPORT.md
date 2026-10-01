# Test Report

## A. Normal foreground
- **Action**: Open app, grant permissions, start protection, trigger phrase.
- **Result**: PASS. Notification appears, event created locally and synced to backend.

## B. Background
- **Action**: Start protection, leave app to home screen.
- **Result**: PASS. Service continues running. Logcat shows audio frames processing.

## C. Screen lock
- **Action**: Start protection, lock screen, wait 2 mins, unlock.
- **Result**: PASS. Service remains active, notification persists.

## D. Incoming phone call
- **Action**: Start protection, receive call.
- **Result**: PASS. Service state changes to `CALL_ACTIVE`. No crash. Post-call, state returns to `MONITORING`.

## E. Microphone permission revoked
- **Action**: Revoke in settings, reopen app.
- **Result**: PASS. App shows permission rationale UI. Service stops gracefully.

## F. Internet disabled
- **Action**: Trigger emergency with WiFi/Data off.
- **Result**: PASS. Event saved as `PENDING` in local database.

## G. Internet restored
- **Action**: Enable WiFi.
- **Result**: PASS. WorkManager executes `SyncWorker`, event status updates to `SENT`.

## H. Location unavailable
- **Action**: Disable device location.
- **Result**: PASS. Fallbacks to null or last known; event is still created without crashing.

## I. Activity recreation
- **Action**: Rotate device.
- **Result**: PASS. UI state reconnects to `ServiceState` via StateFlow. No memory leaks.

## K. Duplicate phrase detection
- **Action**: Say phrase 3 times rapidly.
- **Result**: PASS. Cooldown logic prevents duplicate events within the 10-second threshold.
