package com.safephrase.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.safephrase.ui.screen.contacts.ContactsScreen
import com.safephrase.ui.screen.dashboard.DashboardScreen
import com.safephrase.ui.screen.debug.DebugScreen
import com.safephrase.ui.screen.history.HistoryScreen
import com.safephrase.ui.screen.onboarding.OnboardingScreen
import com.safephrase.ui.screen.settings.SettingsScreen
import com.safephrase.ui.screen.setup.PhraseSetupScreen

@Composable
fun SafePhraseNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToPhraseSetup = { navController.navigate(Screen.PhraseSetup.route) },
                onNavigateToContacts    = { navController.navigate(Screen.Contacts.route) },
                onNavigateToHistory     = { navController.navigate(Screen.History.route) },
                onNavigateToSettings    = { navController.navigate(Screen.Settings.route) },
                onNavigateToDebug       = { navController.navigate(Screen.Debug.route) }
            )
        }

        composable(Screen.PhraseSetup.route) {
            PhraseSetupScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Contacts.route) {
            ContactsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.History.route) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Debug.route) {
            DebugScreen(onBack = { navController.popBackStack() })
        }
    }
}
