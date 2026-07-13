package com.smartnotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartnotes.app.domain.model.ThemeMode
import com.smartnotes.app.ui.noteeditor.NOTE_ID_ARG
import com.smartnotes.app.ui.noteeditor.NO_NOTE_ID
import com.smartnotes.app.ui.noteeditor.NoteEditorScreen
import com.smartnotes.app.ui.notelist.NoteListScreen
import com.smartnotes.app.ui.search.SearchScreen
import com.smartnotes.app.ui.settings.SettingsScreen
import com.smartnotes.app.ui.splash.SplashScreen
import com.smartnotes.app.ui.theme.SmartNotesTheme
import dagger.hilt.android.AndroidEntryPoint

private const val ROUTE_SPLASH = "splash"
private const val ROUTE_NOTE_LIST = "note_list"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_NOTE_EDITOR = "note_editor"
private val BOTTOM_NAV_ROUTES = setOf(ROUTE_NOTE_LIST, ROUTE_SEARCH, ROUTE_SETTINGS)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SmartNotesTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartNotesRoot()
                }
            }
        }
    }
}

@Composable
private fun SmartNotesRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomNav = currentRoute in BOTTOM_NAV_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                BottomNavBar(currentRoute = currentRoute, navController = navController)
            }
        }
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_SPLASH,
            modifier = Modifier.padding(bottom = outerPadding.calculateBottomPadding())
        ) {
            composable(ROUTE_SPLASH) {
                SplashScreen(
                    onTimeout = {
                        navController.navigate(ROUTE_NOTE_LIST) {
                            popUpTo(ROUTE_SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(ROUTE_NOTE_LIST) {
                NoteListScreen(
                    onAddNote = { navController.navigate("$ROUTE_NOTE_EDITOR?$NOTE_ID_ARG=$NO_NOTE_ID") },
                    onSearchOpen = { navController.navigate(ROUTE_SEARCH) },
                    onNoteClick = { id -> navController.navigate("$ROUTE_NOTE_EDITOR?$NOTE_ID_ARG=$id") }
                )
            }
            composable(ROUTE_SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onNoteClick = { id -> navController.navigate("$ROUTE_NOTE_EDITOR?$NOTE_ID_ARG=$id") }
                )
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = "$ROUTE_NOTE_EDITOR?$NOTE_ID_ARG={$NOTE_ID_ARG}",
                arguments = listOf(
                    navArgument(NOTE_ID_ARG) {
                        type = NavType.LongType
                        defaultValue = NO_NOTE_ID
                    }
                )
            ) {
                NoteEditorScreen(
                    onBack = { navController.popBackStack() },
                    onGoToSettings = {
                        navController.navigate(ROUTE_SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(currentRoute: String?, navController: NavHostController) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == ROUTE_NOTE_LIST,
            onClick = {
                navController.navigate(ROUTE_NOTE_LIST) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Description, contentDescription = null) },
            label = { Text("Notes") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
        NavigationBarItem(
            selected = currentRoute == ROUTE_SETTINGS,
            onClick = {
                navController.navigate(ROUTE_SETTINGS) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
    }
}
