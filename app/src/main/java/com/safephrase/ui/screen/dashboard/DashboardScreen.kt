package com.safephrase.ui.screen.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.safephrase.domain.model.ServiceStatus
import com.safephrase.service.MicrophoneMonitorService

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToPhraseSetup: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val status = uiState.serviceState.status

    val isActive = status in listOf(
        ServiceStatus.MONITORING,
        ServiceStatus.SAFE_PHRASE_CANDIDATE,
        ServiceStatus.VERIFYING,
        ServiceStatus.CALL_ACTIVE,
        ServiceStatus.RECOVERING
    )

    Scaffold(
        topBar = {
            DashboardTopBar(onSettings = onNavigateToSettings, onDebug = onNavigateToDebug)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Status pulse circle ───────────────────────────────────────
            ProtectionStatusIndicator(status = status, isActive = isActive)

            Spacer(Modifier.height(32.dp))

            // ── Primary action button ─────────────────────────────────────
            if (isActive) {
                Button(
                    onClick = { stopService(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Filled.Stop, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Protection", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Button(
                    onClick = { startService(context) },
                    enabled = uiState.safePhrase != null,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Filled.Shield, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Protection", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (uiState.safePhrase == null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Configure your SafePhrase first", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }

            AnimatedVisibility(
                visible = uiState.contactCount == 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "You have no trusted contacts! If you trigger an emergency, nobody will be notified.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Info cards grid ───────────────────────────────────────────
            DashboardInfoCard(
                icon = Icons.Filled.MicNone,
                title = "SafePhrase",
                value = uiState.safePhrase?.phrase?.let { "\"${it}\"" } ?: "Not configured",
                subtitle = uiState.safePhrase?.language?.uppercase() ?: "Tap to configure",
                tint = if (uiState.safePhrase != null) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.error,
                onClick = onNavigateToPhraseSetup
            )

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardInfoCard(
                    icon = Icons.Filled.Group,
                    title = "Contacts",
                    value = uiState.contactCount.toString(),
                    subtitle = "Trusted contacts",
                    onClick = onNavigateToContacts,
                    modifier = Modifier.weight(1f)
                )
                DashboardInfoCard(
                    icon = Icons.Filled.History,
                    title = "Alerts",
                    value = uiState.pendingAlerts.toString(),
                    subtitle = "Pending sync",
                    tint = if (uiState.pendingAlerts > 0) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            DashboardInfoCard(
                icon = Icons.Filled.LocationOn,
                title = "Location",
                value = if (uiState.serviceState.isMicAvailable) "Available" else "Unavailable",
                subtitle = "Used during emergencies",
                onClick = {}
            )

            Spacer(Modifier.height(32.dp))
            
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(context, com.safephrase.ui.screen.fakecall.FakeCallActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Filled.PhoneInTalk, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Fake Call")
                }
                
                Button(
                    onClick = { viewModel.triggerManualSOS() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Manual SOS")
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProtectionStatusIndicator(status: ServiceStatus, isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    val (label, color) = when (status) {
        ServiceStatus.IDLE, ServiceStatus.STOPPED -> "Protection OFF" to Color(0xFF9E9E9E)
        ServiceStatus.STARTING -> "Starting..." to Color(0xFFFFA726)
        ServiceStatus.MONITORING -> "Monitoring" to Color(0xFF4CAF50)
        ServiceStatus.SAFE_PHRASE_CANDIDATE, ServiceStatus.VERIFYING -> "Detected..." to Color(0xFFFF7043)
        ServiceStatus.EMERGENCY_TRIGGERED -> "EMERGENCY" to Color(0xFFF44336)
        ServiceStatus.CALL_ACTIVE -> "Call Active" to Color(0xFF2196F3)
        ServiceStatus.MIC_UNAVAILABLE -> "Mic Unavailable" to Color(0xFFF44336)
        ServiceStatus.RECOVERING -> "Recovering" to Color(0xFFFFA726)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha * 0.15f))
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(2.dp, color, CircleShape)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Shield, null, tint = color, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun DashboardInfoCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(onSettings: () -> Unit, onDebug: () -> Unit) {
    TopAppBar(
        title = { Text("SafePhrase", fontWeight = FontWeight.Bold) },
        actions = {
            IconButton(onClick = onDebug) { Icon(Icons.Outlined.BugReport, "Debug") }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Settings") }
        }
    )
}

private fun startService(context: Context) {
    val intent = Intent(context, MicrophoneMonitorService::class.java).apply {
        action = MicrophoneMonitorService.ACTION_START
    }
    ContextCompat.startForegroundService(context, intent)
}

private fun stopService(context: Context) {
    val intent = Intent(context, MicrophoneMonitorService::class.java).apply {
        action = MicrophoneMonitorService.ACTION_STOP
    }
    context.startService(intent)
}
