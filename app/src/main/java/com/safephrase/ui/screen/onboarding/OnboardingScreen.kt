package com.safephrase.ui.screen.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private data class OnboardPage(val icon: ImageVector, val title: String, val body: String)

private val PAGES = listOf(
    OnboardPage(Icons.Filled.CheckCircle, "Welcome to SafePhrase",
        "SafePhrase listens for your emergency keyword in the background and immediately alerts your trusted contacts."),
    OnboardPage(Icons.Filled.Mic, "Background Monitoring",
        "Once protection is started, a notification keeps the microphone service running — even when your screen is locked."),
    OnboardPage(Icons.Filled.Group, "Trusted Contacts",
        "Add your emergency contacts. They will be notified with your location when your phrase is detected."),
    OnboardPage(Icons.Filled.Lock, "Your Privacy",
        "Audio is processed entirely on your device. No raw recording is saved or transmitted.")
)

@androidx.compose.foundation.ExperimentalFoundationApi
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val p = PAGES[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(p.icon, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(80.dp))
                Spacer(Modifier.height(32.dp))
                Text(p.title, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(p.body, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Indicator dots
        Row(
            Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(PAGES.size) { i ->
                val selected = pagerState.currentPage == i
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(if (selected) 24.dp else 8.dp, 8.dp)
                ) {}
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onComplete,
                enabled = true
            ) { Text("Skip") }

            Button(
                onClick = {
                    if (pagerState.currentPage < PAGES.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onComplete()
                    }
                }
            ) {
                Text(if (pagerState.currentPage < PAGES.size - 1) "Next" else "Get Started")
            }
        }
    }
}
