package com.safephrase.ui.screen.setup

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhraseSetupScreen(
    viewModel: PhraseSetupViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var phraseInput by remember(state.currentPhrase) { mutableStateOf(state.currentPhrase) }
    var languageInput by remember(state.currentLanguage) { mutableStateOf(state.currentLanguage) }
    var showPhrase by remember { mutableStateOf(false) }

    // Navigate back after successful save
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SafePhrase Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Choose a phrase unique to you that won't appear in normal conversation. " +
                        "Example: \"Sunflower seventeen delta\". Works in Hindi too!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Phrase Input
            OutlinedTextField(
                value = phraseInput,
                onValueChange = { phraseInput = it },
                label = { Text("Emergency Phrase") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showPhrase = !showPhrase }) {
                        Icon(
                            if (showPhrase) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPhrase) "Hide" else "Show"
                        )
                    }
                },
                visualTransformation = if (showPhrase)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else
                    androidx.compose.ui.text.input.PasswordVisualTransformation(),
                isError = state.error != null,
                supportingText = state.error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true
            )

            // Language Input
            OutlinedTextField(
                value = languageInput,
                onValueChange = { languageInput = it },
                label = { Text("Language Code (e.g. en, hi-IN, es)") },
                placeholder = { Text("en") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = { viewModel.savePhrase(phraseInput, languageInput) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = phraseInput.trim().length >= 3 && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Phrase", fontWeight = FontWeight.Bold)
                }
            }

            if (phraseInput.trim().length < 3 && phraseInput.isNotEmpty()) {
                Text(
                    "Phrase must be at least 3 characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
