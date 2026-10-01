package com.safephrase.ui.screen.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safephrase.domain.model.EmergencyEvent
import com.safephrase.domain.model.EmergencyStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val events by viewModel.events.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency History") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.HistoryToggleOff, null, modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No events recorded", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                items(events, key = { it.id }) { event ->
                    EventCard(event)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: EmergencyEvent) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    val statusColor = when (event.status) {
        EmergencyStatus.SENT -> Color(0xFF4CAF50)
        EmergencyStatus.PENDING, EmergencyStatus.SENDING -> Color(0xFFFFA726)
        EmergencyStatus.FAILED -> Color(0xFFF44336)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = Color(0xFFF44336), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Emergency Triggered", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                SuggestionChip(
                    onClick = {},
                    label = { Text(event.status.name, color = statusColor) }
                )
            }
            Text(sdf.format(Date(event.timestamp)), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Divider(Modifier.padding(vertical = 4.dp))
            event.location?.let { loc ->
                Row {
                    Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("%.5f, %.5f (${loc.source.name})".format(loc.latitude, loc.longitude),
                        style = MaterialTheme.typography.bodySmall)
                }
            } ?: Text("Location: unavailable", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
            Text("Confidence: ${"%.0f".format(event.keywordConfidence * 100)}%",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
