package com.safephrase.wear

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

const val WEAR_SOS_PATH = "/safephrase/sos"

/**
 * Wear OS companion app main activity.
 * Shows a large SOS button on the watch face.
 * When pressed, sends a Wearable Data Layer message to the paired phone.
 * The phone's WearSosListenerService picks it up and fires SMS + emergency pipeline.
 */
class WearSosActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearSosScreen(
                onSosTrigger = { sendSosToPhone() }
            )
        }
    }

    private fun sendSosToPhone() {
        // Vibrate watch to confirm press
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 400), -1)
        )

        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(applicationContext)
                    .connectedNodes.await()

                if (nodes.isEmpty()) {
                    android.util.Log.w("WearSOS", "No connected phone found!")
                    return@launch
                }

                for (node in nodes) {
                    Wearable.getMessageClient(applicationContext).sendMessage(
                        node.id,
                        WEAR_SOS_PATH,
                        "SOS".toByteArray()
                    ).await()
                    android.util.Log.i("WearSOS", "✅ SOS sent to phone: ${node.displayName}")
                }
            } catch (e: Exception) {
                android.util.Log.e("WearSOS", "❌ Failed to send SOS: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

@Composable
fun WearSosScreen(onSosTrigger: () -> Unit) {
    var triggered by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(3) }

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "scale"
    )

    LaunchedEffect(triggered) {
        if (triggered) {
            for (i in 3 downTo 1) {
                countdown = i
                delay(1000)
            }
            onSosTrigger()
        }
    }

    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (triggered) Color(0xFF1A0000) else Color(0xFF0D0D0D)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!triggered) {
                        // Big SOS button
                        Button(
                            onClick = { triggered = true },
                            modifier = Modifier
                                .size(100.dp)
                                .scale(pulseScale)
                                .clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFD32F2F)
                            )
                        ) {
                            Text(
                                "SOS",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Hold firmly then\npress SOS",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Countdown state
                        Text(
                            countdown.toString(),
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 48.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sending SOS\nto your phone…",
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        // Cancel button
                        Chip(
                            onClick = {
                                triggered = false
                                countdown = 3
                            },
                            label = { Text("Cancel", fontSize = 12.sp) },
                            colors = ChipDefaults.chipColors(backgroundColor = Color.DarkGray)
                        )
                    }
                }
            }
        }
    }
}
