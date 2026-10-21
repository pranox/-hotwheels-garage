package com.hotwheelsgarage

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hotwheelsgarage.ui.drops.DropsScreen
import com.hotwheelsgarage.ui.garage.CarDetailScreen
import com.hotwheelsgarage.ui.garage.GarageScreen
import com.hotwheelsgarage.ui.hype.HypeScreen
import com.hotwheelsgarage.ui.scan.ScanScreen
import com.hotwheelsgarage.ui.settings.SettingsScreen
import com.hotwheelsgarage.ui.stats.StatsScreen
import com.hotwheelsgarage.ui.theme.GarageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always dark: light status and navigation bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { GarageTheme { App() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("garage", "Garage", Icons.Filled.DirectionsCar),
    Tab("scan", "Scan", Icons.Filled.CameraAlt),
    Tab("drops", "Drops", Icons.Filled.NotificationsActive),
    Tab("hype", "Hype", Icons.Filled.Whatshot),
    Tab("stats", "Stats", Icons.Filled.BarChart),
)

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun App() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Scaffold(
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { nav.openTab(tab.route) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "garage", modifier = Modifier.padding(padding).consumeWindowInsets(padding)) {
            composable("garage") {
                GarageScreen(
                    onCar = { nav.navigate("car/$it") },
                    onScan = { nav.openTab("scan") },
                    onSettings = { nav.navigate("settings") },
                )
            }
            composable("scan") { ScanScreen(onSaved = { nav.openTab("garage") }) }
            composable("drops") { DropsScreen(onSettings = { nav.navigate("settings") }) }
            composable("hype") { HypeScreen() }
            composable("stats") { StatsScreen() }
            composable("car/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                CarDetailScreen(onBack = { nav.popBackStack() })
            }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
