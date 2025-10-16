package com.example.audioguideai

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.audioguideai.location.LocationForegroundService
import com.example.audioguideai.permissions.PermissionHandler
import com.example.audioguideai.permissions.PermissionManager
import com.example.audioguideai.permissions.SequentialPermissionHandler
import com.example.audioguideai.ui.screens.HistoryScreen
import com.example.audioguideai.ui.screens.MapScreen
import com.example.audioguideai.ui.screens.PermissionScreen
import com.example.audioguideai.ui.screens.SettingsScreen
import com.example.audioguideai.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val hasMagnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
        val hasAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

        Log.d("MainActivity", "Has magnetometer: $hasMagnetometer")
        Log.d("MainActivity", "Has accelerometer: $hasAccelerometer")
    }
}

@Composable
fun AppRoot() {
    val ctx = LocalContext.current
    var hasPermissions by remember { mutableStateOf(PermissionManager.hasAllPermissions(ctx)) }
    var requestPermissions by remember { mutableStateOf(false) }
    var mapCenterTrigger by remember { mutableStateOf(0) }

    AppTheme {
        // Если все разрешения предоставлены, показываем основное приложение
        if (hasPermissions) {
            // Запускаем сервис геолокации при наличии всех разрешений
            LaunchedEffect(Unit) {
                LocationForegroundService.start(ctx)
            }
            
            // Основное приложение
            val nav = rememberNavController()
            Scaffold { padding ->
                NavHost(navController = nav, startDestination = "map", modifier = Modifier) {
                    composable("map") { 
                        MapScreen(
                            onOpenSettings = { nav.navigate("settings") }, 
                            onOpenHistory = { nav.navigate("history") },
                            centerTrigger = mapCenterTrigger
                        ) 
                    }
                    composable("settings") { 
                        SettingsScreen(
                            onBack = { 
                                mapCenterTrigger++ // Центрируем карту при возвращении
                                nav.popBackStack() 
                            },
                            onRequestPermissions = { 
                                hasPermissions = false
                                requestPermissions = true
                            }
                        ) 
                    }
                    composable("history") { 
                        HistoryScreen(
                            onBack = { 
                                mapCenterTrigger++ // Центрируем карту при возвращении
                                nav.popBackStack() 
                            }
                        ) 
                    }
                }
            }
        } else {
            // Показываем экран разрешений
            PermissionScreen(
                onRequestPermissions = {
                    requestPermissions = true
                },
                isRequestingPermissions = requestPermissions
            )
            
            // SequentialPermissionHandler работает поверх PermissionScreen
            if (requestPermissions) {
                SequentialPermissionHandler(
                    onPermissionsGranted = {
                        hasPermissions = true
                        requestPermissions = false
                    },
                    onPermissionsDenied = {
                        // Остаемся на экране разрешений для повторного запроса
                        requestPermissions = false
                        // Принудительно обновляем состояние разрешений
                        hasPermissions = PermissionManager.hasAllPermissions(ctx)
                    }
                )
            }
        }
    }
}
