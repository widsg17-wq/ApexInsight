package com.example.investmentassistant.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private enum class TopDest(val label: String, val icon: ImageVector, val route: String) {
    DASHBOARD("대시보드", Icons.Default.Home, "dashboard"),
    NEWS("뉴스 검색", Icons.Default.Search, "news"),
    CALENDAR("캘린더", Icons.Default.CalendarMonth, "calendar"),
    ARCHIVE("보관함", Icons.Default.Bookmark, "archive"),
    SUBSCRIBE("구독", Icons.Default.Notifications, "subscribe"),
}

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { ApexBottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDest.DASHBOARD.route,
        ) {
            composable(TopDest.DASHBOARD.route) {
                MacroDashboardScreen(bottomPadding = padding)
            }
            composable(TopDest.NEWS.route) {
                NewsSearchScreen(bottomPadding = padding)
            }
            composable(TopDest.CALENDAR.route) {
                CalendarScreen(bottomPadding = padding)
            }
            composable(TopDest.ARCHIVE.route) {
                SavedReportsScreen(bottomPadding = padding)
            }
            composable(TopDest.SUBSCRIBE.route) {
                WatchedKeywordsScreen(bottomPadding = padding)
            }
        }
    }
}

@Composable
private fun ApexBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        TopDest.entries.forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = {
                    navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
            )
        }
    }
}
