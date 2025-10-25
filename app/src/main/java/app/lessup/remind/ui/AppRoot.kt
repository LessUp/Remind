package app.lessup.remind.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.lessup.remind.ui.items.ItemEditScreen
import app.lessup.remind.ui.items.ItemsScreen
import app.lessup.remind.ui.navigation.NavRoutes
import app.lessup.remind.ui.settings.SettingsScreen
import app.lessup.remind.ui.stats.StatsScreen
import app.lessup.remind.ui.subs.SubEditScreen
import app.lessup.remind.ui.subs.SubsScreen
import app.lessup.remind.ui.theme.AppTheme

@Composable
fun AppRoot() {
    AppTheme {
        val nav = rememberNavController()
        Scaffold(
            bottomBar = {
                val entry by nav.currentBackStackEntryAsState()
                val current = entry?.destination?.route
                NavigationBar {
                    NavigationBarItem(
                        selected = current == NavRoutes.Items,
                        onClick = {
                            nav.navigate(NavRoutes.Items) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.List, contentDescription = null) },
                        label = null
                    )
                    NavigationBarItem(
                        selected = current == NavRoutes.Subs,
                        onClick = {
                            nav.navigate(NavRoutes.Subs) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = null
                    )
                    NavigationBarItem(
                        selected = current == NavRoutes.Stats,
                        onClick = {
                            nav.navigate(NavRoutes.Stats) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.AutoGraph, contentDescription = null) },
                        label = null
                    )
                    NavigationBarItem(
                        selected = current == NavRoutes.Settings,
                        onClick = {
                            nav.navigate(NavRoutes.Settings) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = null
                    )
                }
            }
        ) { padding ->
            NavHost(navController = nav, startDestination = NavRoutes.Items) {
                composable(NavRoutes.Items) { ItemsScreen(nav, padding) }
                composable(
                    route = NavRoutes.ItemEdit + "?id={id}",
                    arguments = listOf(navArgument("id") { nullable = true })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                    ItemEditScreen(nav, padding, id)
                }
                composable(NavRoutes.Subs) { SubsScreen(nav, padding) }
                composable(
                    route = NavRoutes.SubEdit + "?id={id}",
                    arguments = listOf(navArgument("id") { nullable = true })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
                    SubEditScreen(nav, padding, id)
                }
                composable(NavRoutes.Stats) { StatsScreen(padding) }
                composable(NavRoutes.Settings) { SettingsScreen(padding) }
            }
        }
    }
}
