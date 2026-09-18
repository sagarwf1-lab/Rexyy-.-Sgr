package com.rexyy.app.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("api_key_setup")
    object Chat : Screen("chat_main")
    object Settings : Screen("settings")
}
