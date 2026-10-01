package com.safephrase.ui.screen.fakecall

import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class FakeCallActivity : ComponentActivity() {
    
    private var ringtone: android.media.Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Play default ringtone
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone?.play()

        setContent {
            var isAnswered by remember { mutableStateOf(false) }
            var callDuration by remember { mutableStateOf(0) }
            
            LaunchedEffect(isAnswered) {
                if (isAnswered) {
                    while(true) {
                        delay(1000)
                        callDuration++
                    }
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E1E)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 80.dp)
                    ) {
                        Text(
                            text = if (isAnswered) "00:${callDuration.toString().padStart(2, '0')}" else "Incoming call",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Mom", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Medium)
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 80.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Decline
                            FloatingActionButton(
                                onClick = { finish() },
                                containerColor = Color(0xFFE53935),
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(Icons.Filled.CallEnd, contentDescription = "Decline", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            
                            // Answer
                            if (!isAnswered) {
                                FloatingActionButton(
                                    onClick = { 
                                        isAnswered = true
                                        ringtone?.stop()
                                    },
                                    containerColor = Color(0xFF43A047),
                                    shape = CircleShape,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = "Answer", tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
    }
}
