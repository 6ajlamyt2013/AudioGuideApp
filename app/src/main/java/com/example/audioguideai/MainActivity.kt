package com.example.audioguideai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.audioguideai.location.LocationForegroundService
import com.example.audioguideai.ui.screens.HistoryScreen
import com.example.audioguideai.ui.screens.MapScreen
import com.example.audioguideai.ui.screens.SettingsScreen
import com.example.audioguideai.ui.theme.AppTheme
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { LocationForegroundService.start(ctx) }

    AppTheme {
        val nav = rememberNavController()
        Scaffold { padding ->
            NavHost(navController = nav, startDestination = "map", modifier = Modifier) {
                composable("map") { MapScreen(onOpenSettings = { nav.navigate("settings") }, onOpenHistory = { nav.navigate("history") }) }
                composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
                composable("history") { HistoryScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}
