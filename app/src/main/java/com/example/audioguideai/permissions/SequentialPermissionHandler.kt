package com.example.audioguideai.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun SequentialPermissionHandler(
    onPermissionsGranted: () -> Unit,
    onPermissionsDenied: () -> Unit
) {
    val context = LocalContext.current
    var currentPermissionIndex by remember { mutableStateOf(0) }
    var hasAllPermissions by remember { mutableStateOf(false) }
    
    // Список разрешений в порядке приоритета
    val permissionOrder = remember {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        ).filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toMutableList().apply {
            // Добавляем фоновую геолокацию в конец, если она нужна
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Разрешение предоставлено, переходим к следующему
            currentPermissionIndex++
            if (currentPermissionIndex >= permissionOrder.size) {
                // Все разрешения предоставлены
                hasAllPermissions = true
                onPermissionsGranted()
            }
        } else {
            // Разрешение отклонено - остаемся на экране разрешений
            onPermissionsDenied()
        }
    }
    
    LaunchedEffect(Unit) {
        if (permissionOrder.isEmpty()) {
            // Все разрешения уже предоставлены
            hasAllPermissions = true
            onPermissionsGranted()
        } else {
            // Запрашиваем первое разрешение
            permissionLauncher.launch(permissionOrder[currentPermissionIndex])
        }
    }
    
    // Запрашиваем следующее разрешение, если текущее было предоставлено
    LaunchedEffect(currentPermissionIndex) {
        if (currentPermissionIndex < permissionOrder.size && !hasAllPermissions && currentPermissionIndex > 0) {
            // Небольшая задержка между запросами для лучшего UX
            kotlinx.coroutines.delay(500)
            permissionLauncher.launch(permissionOrder[currentPermissionIndex])
        }
    }
}
