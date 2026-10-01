package com.safephrase.audio

import com.safephrase.core.util.SafeLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

import com.safephrase.data.local.preferences.UserPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Validates that the detected phrase was spoken by the authorized user.
 * 
 * MVP implementation uses fundamental frequency (pitch) tracking via 
 * zero-crossing rate approximation. A real production app would use 
 * a d-vector or x-vector neural network model for biometric speaker verification.
 */
@Singleton
class SpeakerVerifier @Inject constructor(
    private val dataStore: UserPreferencesDataStore
) {

    private val tag = "SpeakerVerifier"

    // In a real app, this would be a d-vector array loaded from DataStore
    // For MVP, we simulate an enrolled user pitch range (approx 100-250 Hz for adult voices)
    private var enrolledPitchMin = 80f
    private var enrolledPitchMax = 280f
    
    init {
        CoroutineScope(Dispatchers.IO).launch {
            enrolledPitchMin = dataStore.userPitchMin.first()
            enrolledPitchMax = dataStore.userPitchMax.first()
        }
    }

    /**
     * Enrolls the user's voice profile using 3 seconds of audio data.
     * Computes the median pitch to filter out noise/silence anomalies.
     */
    fun enrollVoiceProfile(pitchSamples: List<Float>) {
        val validPitches = pitchSamples.filter { it > 50f && it < 450f }.sorted()
        if (validPitches.isNotEmpty()) {
            val medianPitch = validPitches[validPitches.size / 2]
            // Allow a tight +/- 25% margin for natural voice fluctuation
            enrolledPitchMin = (medianPitch * 0.75f).coerceAtLeast(60f)
            enrolledPitchMax = (medianPitch * 1.25f).coerceAtMost(400f)
            
            CoroutineScope(Dispatchers.IO).launch {
                dataStore.saveVoiceProfile(enrolledPitchMin, enrolledPitchMax)
            }
            SafeLogger.i(tag, "Advanced Voice Enrolled. Median pitch: $medianPitch Hz. Allowed: $enrolledPitchMin - $enrolledPitchMax Hz")
        } else {
            SafeLogger.w(tag, "Failed to enroll voice: No valid speech detected in sample.")
        }
    }

    /**
     * Verifies if the current audio buffer matches the enrolled user's voice.
     * Returns a confidence score [0.0 - 1.0].
     */
    fun verifySpeaker(samples: ShortArray): Float {
        val currentPitch = estimatePitch(samples)
        if (currentPitch == 0f) return 0f // No speech detected

        SafeLogger.d(tag, "Extracted pitch (AMDF): $currentPitch Hz")

        return if (currentPitch in enrolledPitchMin..enrolledPitchMax) {
            // Speaker matches enrolled profile
            0.95f
        } else {
            // Speaker differs significantly (e.g., child vs adult, male vs female)
            0.15f
        }
    }

    /**
     * Calculates fundamental frequency (F0) using the Average Magnitude Difference Function (AMDF).
     * This is an industry-standard DSP technique for highly accurate pitch detection.
     */
    fun estimatePitch(samples: ShortArray): Float {
        if (samples.size < 100) return 0f
        
        var totalEnergy = 0f
        for (i in samples.indices) {
            totalEnergy += abs(samples[i].toFloat())
        }
        val avgEnergy = totalEnergy / samples.size
        if (avgEnergy < 150f) return 0f // Too quiet to estimate pitch accurately

        val sampleRate = 16000
        val minPitch = 60   // Deep male voice min Hz
        val maxPitch = 400  // High female/child voice max Hz
        
        val minLag = sampleRate / maxPitch
        val maxLag = sampleRate / minPitch
        
        var minAmdf = Float.MAX_VALUE
        var bestLag = -1
        
        for (lag in minLag..maxLag) {
            if (lag >= samples.size) break
            var amdf = 0f
            for (i in 0 until samples.size - lag) {
                amdf += abs(samples[i] - samples[i + lag].toFloat())
            }
            amdf /= (samples.size - lag)
            if (amdf < minAmdf) {
                minAmdf = amdf
                bestLag = lag
            }
        }
        
        if (bestLag == -1) return 0f
        return (sampleRate.toFloat() / bestLag)
    }
}
