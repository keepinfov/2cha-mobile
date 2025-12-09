package dev.yaul.twocha.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.yaul.twocha.ui.screens.ConfigScreen
import dev.yaul.twocha.ui.screens.HomeScreen
import dev.yaul.twocha.ui.screens.LogsScreen
import dev.yaul.twocha.ui.screens.SettingsScreen
import dev.yaul.twocha.ui.theme.Duration
import dev.yaul.twocha.ui.theme.SpringPhysics
import dev.yaul.twocha.viewmodel.VpnViewModel

/**
 * Navigation Routes
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Config : Screen("config")
    object Settings : Screen("settings")
    object Logs : Screen("logs")
}

/**
 * Material 3 Expressive Navigation Host
 *
 * Features:
 * - Spring-based page transitions
 * - Shared element animations
 * - Predictive back gesture support
 */
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
        modifier = modifier,
        // Expressive enter transition
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = spring(
                    dampingRatio = SpringPhysics.responsiveDamping,
                    stiffness = SpringPhysics.responsiveStiffness
                ),
                initialOffset = { it / 4 }
            ) + fadeIn(
                animationSpec = tween(Duration.fast)
            )
        },
        // Expressive exit transition
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(Duration.medium),
                targetOffset = { -it / 4 }
            ) + fadeOut(
                animationSpec = tween(Duration.ultraFast)
            )
        },
        // Pop enter transition (back navigation)
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = spring(
                    dampingRatio = SpringPhysics.responsiveDamping,
                    stiffness = SpringPhysics.responsiveStiffness
                ),
                initialOffset = { -it / 4 }
            ) + fadeIn(
                animationSpec = tween(Duration.fast)
            )
        },
        // Pop exit transition (back navigation)
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(Duration.medium),
                targetOffset = { it / 4 }
            ) + fadeOut(
                animationSpec = tween(Duration.ultraFast)
            )
        }
    ) {
        // Home Screen
        composable(
            route = Screen.Home.route,
            enterTransition = {
                // Special fade-in for home
                fadeIn(animationSpec = tween(Duration.fast)) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = spring(
                        dampingRatio = SpringPhysics.responsiveDamping,
                        stiffness = SpringPhysics.responsiveStiffness
                    )
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(Duration.ultraFast)) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(Duration.fast)
                )
            }
        ) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToConfig = {
                    navController.navigate(Screen.Config.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToLogs = {
                    navController.navigate(Screen.Logs.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Config Screen
        composable(route = Screen.Config.route) {
            ConfigScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings Screen
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Logs Screen
        composable(route = Screen.Logs.route) {
            LogsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
