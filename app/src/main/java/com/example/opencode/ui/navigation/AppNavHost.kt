package com.example.opencode.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.opencode.ui.chat.ChatScreen
import com.example.opencode.ui.server.ServerListScreen
import com.example.opencode.ui.sessions.SessionListScreen
import com.example.opencode.ui.settings.SettingsScreen
import com.example.opencode.ui.stats.StatsScreen

object Routes {
    const val SERVERS = "servers"
    const val SESSIONS = "sessions"
    const val CHAT = "chat/{sessionId}"
    const val SETTINGS = "settings"
    const val STATS = "stats"
    fun chat(sessionId: String) = "chat/$sessionId"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.SERVERS,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable(Routes.SERVERS) {
            ServerListScreen(
                onConnected = { navController.navigate(Routes.SESSIONS) { popUpTo(Routes.SERVERS) { inclusive = true } } },
                onSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                onStats = { navController.navigate(Routes.STATS) { launchSingleTop = true } },
            )
        }
        composable(Routes.SESSIONS) {
            SessionListScreen(
                onSessionClick = { navController.navigate(Routes.chat(it)) },
                onDisconnect = { navController.navigate(Routes.SERVERS) { popUpTo(Routes.SESSIONS) { inclusive = true } } },
                onSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                onStats = { navController.navigate(Routes.STATS) { launchSingleTop = true } },
            )
        }
        composable(Routes.CHAT, arguments = listOf(navArgument("sessionId") { type = NavType.StringType })) { entry ->
            val sid = entry.arguments?.getString("sessionId") ?: return@composable
            ChatScreen(sessionId = sid, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onServers = { navController.navigate(Routes.SERVERS) { popUpTo(0) { inclusive = true } } },
                onStats = { navController.navigate(Routes.STATS) { launchSingleTop = true } },
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                onServers = { navController.navigate(Routes.SERVERS) { popUpTo(0) { inclusive = true } } },
                onSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
            )
        }
    }
}
