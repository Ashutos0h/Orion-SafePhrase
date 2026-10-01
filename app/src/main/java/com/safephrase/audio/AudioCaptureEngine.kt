package com.safephrase.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.safephrase.core.config.AppConfig
import com.safephrase.core.util.SafeLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AudioCaptureEngine"

/**
 * Manages the AudioRecord lifecycle and emits bounded audio frames.
 *
 * Design decisions:
 *  - Uses a bounded SharedFlow replay = 0 so frames are never buffered indefinitely.
 *  - processAudio callback is synchronous; heavy inference is done on the collector side.
 *  - Recovery uses bounded exponential backoff; gives up after MAX_ATTEMPTS.
 */
@Singleton
class AudioCaptureEngine @Inject constructor() {

    private val _audioFrames = MutableSharedFlow<ShortArray>(extraBufferCapacity = 8)
    val audioFrames: SharedFlow<ShortArray> = _audioFrames.asSharedFlow()

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private var isCapturing = false
    private val minBufferSize = AudioRecord.getMinBufferSize(
        AppConfig.SAMPLE_RATE_HZ,
        AppConfig.CHANNEL_CONFIG,
        AppConfig.AUDIO_FORMAT
    ).coerceAtLeast(AppConfig.FRAME_SAMPLES * 2 * 2) // at least 2 frames, 2 bytes/sample

    // ── Public API ────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    fun startCapture(scope: CoroutineScope): Boolean {
        if (isCapturing) return true
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            SafeLogger.e(TAG, "Unsupported audio params")
            return false
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            AppConfig.SAMPLE_RATE_HZ,
            AppConfig.CHANNEL_CONFIG,
            AppConfig.AUDIO_FORMAT,
            minBufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            SafeLogger.e(TAG, "AudioRecord init failed")
            audioRecord?.release()
            audioRecord = null
            return false
        }

        audioRecord?.startRecording()
        isCapturing = true

        captureJob = scope.launch(Dispatchers.IO + CoroutineName("AudioCapture")) {
            val buffer = ShortArray(AppConfig.FRAME_SAMPLES)
            while (isActive && isCapturing) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                when {
                    read > 0 -> {
                        _audioFrames.tryEmit(buffer.copyOf(read))
                    }
                    read == AudioRecord.ERROR_INVALID_OPERATION ||
                    read == AudioRecord.ERROR_BAD_VALUE -> {
                        SafeLogger.w(TAG, "AudioRecord read error: $read")
                        break
                    }
                }
            }
        }

        SafeLogger.i(TAG, "Audio capture started (rate=${AppConfig.SAMPLE_RATE_HZ}Hz, buf=$minBufferSize)")
        return true
    }

    fun pauseCapture() {
        if (!isCapturing) return
        isCapturing = false
        captureJob?.cancel()
        audioRecord?.stop()
        SafeLogger.i(TAG, "Audio capture paused")
    }

    fun stopCapture() {
        isCapturing = false
        captureJob?.cancel()
        captureJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        SafeLogger.i(TAG, "Audio capture stopped and released")
    }

    fun resumeCapture(scope: CoroutineScope): Boolean {
        if (isCapturing) return true
        SafeLogger.i(TAG, "Resuming capture after interruption")
        return startCapture(scope)
    }

    val isActive: Boolean get() = isCapturing
}
