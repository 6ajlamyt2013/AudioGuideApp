package com.example.audioguideai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audioguideai.permissions.PermissionManager

@Composable
fun PermissionScreen(
    onRequestPermissions: () -> Unit,
    isRequestingPermissions: Boolean = false
) {
    val context = LocalContext.current
    var missingPermissions by remember { mutableStateOf(PermissionManager.getMissingPermissions(context)) }
    
    // Отслеживаем изменения в разрешениях для обновления списка
    LaunchedEffect(Unit) {
        // Периодически проверяем изменения в разрешениях
        while (true) {
            kotlinx.coroutines.delay(500) // Проверяем каждые 500мс
            val currentMissing = PermissionManager.getMissingPermissions(context)
            if (currentMissing != missingPermissions) {
                missingPermissions = currentMissing
            }
        }
    }
    
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Иконка приложения
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "AudioGuideAI",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Заголовок
        Text(
            text = "AudioGuideAI",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Для работы приложения необходимы следующие разрешения:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Список разрешений
        missingPermissions.forEach { permission ->
            PermissionItem(
                permission = permission,
                icon = getPermissionIcon(permission),
                title = getPermissionTitle(permission),
                description = getPermissionDescription(permission)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Кнопки
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    onRequestPermissions()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRequestingPermissions
            ) {
                if (isRequestingPermissions) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when {
                        isRequestingPermissions -> "Запрос разрешений..."
                        missingPermissions.isEmpty() -> "Все разрешения предоставлены"
                        else -> "Предоставить разрешения"
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    permission: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getPermissionIcon(permission: String): ImageVector {
    return when (permission) {
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION -> Icons.Default.LocationOn
        android.Manifest.permission.RECORD_AUDIO -> Icons.Default.Settings
        else -> Icons.Default.Lock
    }
}

private fun getPermissionTitle(permission: String): String {
    return when (permission) {
        android.Manifest.permission.ACCESS_FINE_LOCATION -> "Точная геолокация"
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Фоновая геолокация"
        android.Manifest.permission.RECORD_AUDIO -> "Микрофон"
        else -> "Системное разрешение"
    }
}

private fun getPermissionDescription(permission: String): String {
    return when (permission) {
        android.Manifest.permission.ACCESS_FINE_LOCATION -> "Определение точного местоположения для работы аудиогида"
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Отслеживание местоположения в фоновом режиме"
        android.Manifest.permission.RECORD_AUDIO -> "Распознавание голосовых команд"
        else -> "Необходимо для корректной работы приложения"
    }
}
