package com.pixellab.suite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pixellab.suite.core.datastore.UserSettingsState
import com.pixellab.suite.core.designsystem.PixelLabTheme
import com.pixellab.suite.core.designsystem.ThemeMode
import com.pixellab.suite.data.PixelLabRepository
import com.pixellab.suite.feature.automation.AutopilotScreen
import com.pixellab.suite.feature.battery.EnergixScreen
import com.pixellab.suite.feature.dashboard.DashboardScreen
import com.pixellab.suite.feature.device.PixelTwinScreen
import com.pixellab.suite.feature.localai.LocalMindScreen
import com.pixellab.suite.feature.network.SignalXScreen
import com.pixellab.suite.feature.privacy.PrivacyCenterScreen
import com.pixellab.suite.feature.productivity.EdgeFlowScreen
import com.pixellab.suite.feature.sensors.SensoriumScreen
import com.pixellab.suite.feature.settings.SettingsScreen
import com.pixellab.suite.feature.studio.MotionXScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as PixelLabApplication).repository
        setContent {
            PixelLabRootApp(repository = repo)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelLabRootApp(repository: PixelLabRepository) {
    val settings by repository.preferences.settingsFlow.collectAsState(initial = UserSettingsState())
    val systemDark = isSystemInDarkTheme()
    val useDark = when (settings.themeMode) {
        ThemeMode.OBSIDIAN_DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "dashboard"

    val bottomNavDestinations = listOf(
        Triple("dashboard", "Command", Icons.Default.Dashboard),
        Triple("device", "Twin", Icons.Default.PhoneAndroid),
        Triple("sensors", "Sensors", Icons.Default.Sensors),
        Triple("localai", "LocalMind", Icons.Default.Memory),
        Triple("productivity", "EdgeFlow", Icons.Default.Build)
    )

    PixelLabTheme(
        darkTheme = useDark,
        highContrast = settings.highContrast,
        reducedMotion = settings.reducedMotion
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                "dashboard" -> "PIXEL LAB • COMMAND CENTER"
                                "device" -> "MOD 01 • PIXEL TWIN"
                                "automation" -> "MOD 02 • AUTOPILOT"
                                "sensors" -> "MOD 03 • SENSORIUM"
                                "localai" -> "MOD 04 • LOCALMIND"
                                "network" -> "MOD 05 • SIGNALX"
                                "battery" -> "MOD 06 • ENERGIX"
                                "studio" -> "MOD 07 • MOTIONX"
                                "productivity" -> "MOD 08 • EDGEFLOW"
                                "privacy" -> "PRIVACY & SECURITY CENTER"
                                "settings" -> "SYSTEM SETTINGS"
                                else -> "PIXEL LAB"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        if (currentRoute != "dashboard") {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("studio") { launchSingleTop = true } }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "MotionX Studio")
                        }
                        IconButton(onClick = { navController.navigate("privacy") { launchSingleTop = true } }) {
                            Icon(Icons.Default.PrivacyTip, contentDescription = "Privacy Center")
                        }
                        IconButton(onClick = { navController.navigate("settings") { launchSingleTop = true } }) {
                            Icon(Icons.Default.Settings, contentDescription = "System Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomNavDestinations.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        repository = repository,
                        onNavigateToModule = { route ->
                            navController.navigate(route) { launchSingleTop = true }
                        }
                    )
                }
                composable("device") { PixelTwinScreen(repository = repository) }
                composable("automation") { AutopilotScreen(repository = repository) }
                composable("sensors") { SensoriumScreen(repository = repository) }
                composable("localai") { LocalMindScreen(repository = repository) }
                composable("network") { SignalXScreen(repository = repository) }
                composable("battery") { EnergixScreen(repository = repository) }
                composable("studio") { MotionXScreen(repository = repository) }
                composable("productivity") { EdgeFlowScreen(repository = repository) }
                composable("privacy") { PrivacyCenterScreen(repository = repository) }
                composable("settings") { SettingsScreen(repository = repository) }
            }
        }
    }
}
