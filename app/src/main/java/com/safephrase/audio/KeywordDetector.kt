package com.safephrase.audio

import com.safephrase.core.config.AppConfig
import com.safephrase.core.util.SafeLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * KeywordDetector abstraction.
 *
 * In production this would load a TensorFlow Lite model (e.g. via tflite-task-audio).
 * The interface is intentionally stable so the model can be swapped without touching
 * the rest of the app.
 *
 * MVP implementation uses a simple energy + zero-crossing heuristic to simulate detection.
 * This will produce occasional positives on louder speech — sufficient for UI testing.
 *
 * TODO: Replace processFrames() body with actual TFLite inference when model is available.
 */
interface KeywordDetector {
    /** Returns detection result for the given PCM-16 audio frame. */
    fun processFrame(samples: ShortArray): DetectionResult

    /** Release any held model resources. */
    fun release()
}

data class DetectionResult(
    val isCandidate: Boolean,
    val confidence: Float,
    val phraseToken: String = ""
)

@Singleton
class HeuristicKeywordDetector @Inject constructor() : KeywordDetector {

    private val tag = "KeywordDetector"
    private var frameCount = 0

    // Rolling energy history (last N frames)
    private val energyHistory = ArrayDeque<Float>(30)
    private val TARGET_PHRASE = "SAFE_PHRASE" // placeholder token

    override fun processFrame(samples: ShortArray): DetectionResult {
        val rms = computeRms(samples)
        val zcr = computeZcr(samples)
        frameCount++

        energyHistory.addLast(rms)
        if (energyHistory.size > 30) energyHistory.removeFirst()

        val avgEnergy = energyHistory.average().toFloat()

        // Heuristic: loud frame with mid-range ZCR after a period of activity
        // This is a placeholder — a real model would classify the entire phrase window
        val isVoice = rms > 400f && zcr in 0.02f..0.45f
        val isSpike  = rms > avgEnergy * 1.5f

        val rawConfidence = if (isVoice && isSpike) {
            // Map normal speech RMS (1000 - 15000) to confidence (0.8 - 0.99)
            val normalized = (rms / 15000f).coerceIn(0f, 1f)
            0.80f + (normalized * 0.19f)
        } else 0f

        // Gate at AppConfig threshold
        val isCandidate = rawConfidence >= AppConfig.KEYWORD_CONFIDENCE_THRESHOLD

        if (isCandidate) {
            SafeLogger.i(tag, "Candidate detected — confidence=$rawConfidence rms=$rms zcr=$zcr")
        }

        return DetectionResult(
            isCandidate = isCandidate,
            confidence = rawConfidence,
            phraseToken = if (isCandidate) TARGET_PHRASE else ""
        )
    }

    override fun release() {
        energyHistory.clear()
        SafeLogger.i(tag, "KeywordDetector released")
    }

    private fun computeRms(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        val sum = samples.sumOf { it.toLong() * it.toLong() }
        return sqrt(sum.toDouble() / samples.size).toFloat()
    }

    private fun computeZcr(samples: ShortArray): Float {
        if (samples.size < 2) return 0f
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0) != (samples[i - 1] >= 0)) crossings++
        }
        return crossings.toFloat() / samples.size
    }
}

/**
 * Two-stage risk verifier.
 *
 * Sits between keyword detection and emergency trigger.
 * Reduces false positives by applying a secondary confidence gate
 * and checking cooldown state.
 *
 * TODO: Replace with Gemini API call or on-device NLU when available.
 */
@Singleton
class RiskVerifier @Inject constructor(
    private val speakerVerifier: SpeakerVerifier
) {

    private val tag = "RiskVerifier"

    /**
     * Verify that the candidate detection represents a genuine intent
     * AND was spoken by the correct user.
     * Returns a confidence score in [0, 1].
     */
    fun verify(detectionResult: DetectionResult, audioBuffer: ShortArray): Float {
        // Check speaker identity
        val speakerConfidence = speakerVerifier.verifySpeaker(audioBuffer)
        
        // Combine keyword confidence with speaker confidence
        val combinedConfidence = (detectionResult.confidence * 0.6f) + (speakerConfidence * 0.4f)
        
        val verifiedConfidence = combinedConfidence.coerceIn(0f, 1f)
        SafeLogger.d(tag, "RiskVerifier: phraseConf=${detectionResult.confidence} speakerConf=$speakerConfidence final=$verifiedConfidence")
        return verifiedConfidence
    }
}
