package com.safephrase.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding  : Screen("onboarding")
    object Permissions : Screen("permissions")
    object Dashboard   : Screen("dashboard")
    object PhraseSetup : Screen("phrase_setup")
    object Contacts    : Screen("contacts")
    object History     : Screen("history")
    object Settings    : Screen("settings")
    object Debug       : Screen("debug")
}
