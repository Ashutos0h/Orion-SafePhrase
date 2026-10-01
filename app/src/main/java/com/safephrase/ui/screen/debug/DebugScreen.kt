package com.safephrase.ui.screen.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DebugViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.state.collectAsState()
    val serviceState by viewModel.serviceStateFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Console") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DebugCard("Service") {
                DebugRow("Status", serviceState.status.name)
                DebugRow("Mic Available", serviceState.isMicAvailable.toString())
                DebugRow("Call Active", serviceState.isCallActive.toString())
                DebugRow("Recovery Attempts", serviceState.recoveryAttempt.toString())
            }

            DebugCard("Audio") {
                val lastFrame = serviceState.lastAudioFrameAt
                DebugRow("Last Frame", if (lastFrame > 0)
                    SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(lastFrame))
                    else "—")
                DebugRow("Last Detection", serviceState.lastDetectionAt
                    .takeIf { it > 0 }
                    ?.let { SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it)) } ?: "—")
            }

            DebugCard("Events") {
                DebugRow("Total Events", uiState.totalCount.toString())
                DebugRow("Pending Sync", uiState.pendingCount.toString())
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("RAW STATE", color = Color(0xFF4CAF50), fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    Text(serviceState.toString(), color = Color(0xFFE0E0E0),
                        fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun DebugCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
