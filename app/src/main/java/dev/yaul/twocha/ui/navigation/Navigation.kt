package dev.yaul.twocha.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.yaul.twocha.ui.screens.ConfigScreen
import dev.yaul.twocha.ui.screens.HomeScreen
import dev.yaul.twocha.ui.screens.LogsScreen
import dev.yaul.twocha.ui.screens.SettingsScreen
import dev.yaul.twocha.viewmodel.VpnViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Config : Screen("config")
    object Settings : Screen("settings")
    object Logs : Screen("logs")
}

@Composable
fun TwochaNavHost(
    navController: NavHostController,
    viewModel: VpnViewModel,
    @Suppress("UNUSED_PARAMETER") onRequestVpnPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            // HomeScreen handles VPN permission internally
            HomeScreen(
                viewModel = viewModel,
                onNavigateToConfig = { navController.navigate(Screen.Config.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToLogs = { navController.navigate(Screen.Logs.route) }
            )
        }

        composable(Screen.Config.route) {
            ConfigScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Logs.route) {
            LogsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}