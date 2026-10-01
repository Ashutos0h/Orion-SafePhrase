package com.safephrase.ui.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safephrase.domain.model.ProtectionSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var kwThreshold by remember(settings) { mutableFloatStateOf(settings.keywordConfidenceThreshold) }
    var verifThreshold by remember(settings) { mutableFloatStateOf(settings.verificationThreshold) }
    var cooldownSec by remember(settings) { mutableFloatStateOf(settings.cooldownMs / 1000f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("Detection Thresholds", style = MaterialTheme.typography.titleMedium)

            SliderSetting(
                label = "Keyword Confidence",
                value = kwThreshold,
                onValueChange = { kwThreshold = it },
                displayValue = "${(kwThreshold * 100).roundToInt()}%"
            )

            SliderSetting(
                label = "Verification Confidence",
                value = verifThreshold,
                onValueChange = { verifThreshold = it },
                displayValue = "${(verifThreshold * 100).roundToInt()}%"
            )

            Text("Timing", style = MaterialTheme.typography.titleMedium)

            SliderSetting(
                label = "Alert Cooldown",
                value = cooldownSec,
                onValueChange = { cooldownSec = it },
                displayValue = "${cooldownSec.roundToInt()}s",
                valueRange = 5f..120f
            )

            Button(
                onClick = {
                    viewModel.save(ProtectionSettings(
                        keywordConfidenceThreshold = kwThreshold,
                        verificationThreshold = verifThreshold,
                        cooldownMs = (cooldownSec * 1000).toLong()
                    ))
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save Settings")
            }
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    displayValue: String,
    valueRange: ClosedFloatingPointRange<Float> = 0.5f..1.0f
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(displayValue, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange,
            modifier = Modifier.fillMaxWidth())
    }
}
