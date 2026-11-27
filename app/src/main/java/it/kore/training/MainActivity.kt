package it.kore.training

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.kore.training.ui.K
import it.kore.training.ui.KoreTheme
import it.kore.training.ui.screens.BodyWeightScreen
import it.kore.training.ui.screens.ExerciseDetailScreen
import it.kore.training.ui.screens.GuideScreen
import it.kore.training.ui.screens.HomeScreen
import it.kore.training.ui.screens.LibraryScreen
import it.kore.training.ui.screens.MembershipScreen
import it.kore.training.ui.screens.NutritionScreen
import it.kore.training.ui.screens.OneRmScreen
import it.kore.training.ui.screens.OnboardingScreen
import it.kore.training.ui.screens.PlatesScreen
import it.kore.training.ui.screens.ProfileScreen
import it.kore.training.ui.screens.ProgramScreen
import it.kore.training.ui.screens.ProgressScreen
import it.kore.training.ui.screens.RirScreen
import it.kore.training.ui.screens.SessionDetailScreen
import it.kore.training.ui.screens.ToolsScreen
import it.kore.training.ui.screens.WorkoutScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KoreTheme { KoreApp() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Oggi", Icons.Filled.Home),
    Tab("program", "Programma", Icons.Filled.CalendarMonth),
    Tab("progress", "Progressi", Icons.AutoMirrored.Filled.ShowChart),
    Tab("tools", "Strumenti", Icons.Filled.Handyman),
    Tab("profile", "Profilo", Icons.Filled.Person)
)

@Composable
fun KoreApp(vm: AppViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val start = remember { if (data.profile.onboarded) "home" else "onboarding" }

    Scaffold(
        containerColor = K.Bg,
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar(containerColor = Color(0xFF101010), tonalElevation = 0.dp) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { nav.goTab(t.route) },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                indicatorColor = K.Accent,
                                selectedTextColor = K.Accent,
                                unselectedIconColor = K.Muted,
                                unselectedTextColor = K.Muted
                            )
                        )
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            NavHost(nav, startDestination = start) {
                composable("onboarding") {
                    OnboardingScreen(vm) {
                        nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                    }
                }
                composable("home") { HomeScreen(vm, nav) }
                composable("program") { ProgramScreen(vm, nav) }
                composable("progress") { ProgressScreen(vm, nav) }
                composable("tools") { ToolsScreen(nav) }
                composable("profile") { ProfileScreen(vm, nav) }
                composable("workout") { WorkoutScreen(vm, nav) }
                composable("guide") { GuideScreen(nav) }
                composable("library") { LibraryScreen(nav) }
                composable("exercise/{id}") { b -> ExerciseDetailScreen(vm, nav, b.arguments?.getString("id") ?: "") }
                composable("session/{id}") { b -> SessionDetailScreen(vm, nav, b.arguments?.getString("id")?.toLongOrNull() ?: 0L) }
                composable("bodyweight") { BodyWeightScreen(vm, nav) }
                composable("nutrition") { NutritionScreen(vm, nav) }
                composable("onerm") { OneRmScreen(vm, nav) }
                composable("plates") { PlatesScreen(vm, nav) }
                composable("rir") { RirScreen(nav) }
                composable("membership") { MembershipScreen(nav) }
            }
        }
    }
}


fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
