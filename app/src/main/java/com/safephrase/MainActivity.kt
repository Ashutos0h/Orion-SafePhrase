package com.safephrase

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.safephrase.domain.repository.PreferencesRepository
import com.safephrase.ui.navigation.SafePhraseNavGraph
import com.safephrase.ui.navigation.Screen
import com.safephrase.ui.screen.onboarding.PermissionsGate
import com.safephrase.ui.theme.SafePhraseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesRepository: PreferencesRepository

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions result handled reactively in PermissionsGate */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val onboardingDone = runBlocking { preferencesRepository.onboardingDone.first() }
        val start = if (onboardingDone) Screen.Dashboard.route else Screen.Onboarding.route

        setContent {
            SafePhraseTheme {
                PermissionsGate(
                    onRequestPermissions = { requestAllPermissions() }
                ) {
                    val navController = rememberNavController()
                    SafePhraseNavGraph(
                        navController = navController,
                        startDestination = start
                    )
                }
            }
        }
    }

    private fun requestAllPermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.SEND_SMS)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }
}
