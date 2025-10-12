package com.example.audioguideai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.audioguideai.data.SettingsRepo
import com.example.audioguideai.data.model.Category
import com.example.audioguideai.permissions.PermissionManager

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(onBack: () -> Unit, onRequestPermissions: () -> Unit = {}) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { SettingsRepo(ctx) }
    val settings by repo.settings.collectAsState(initial = null)

    Scaffold(topBar = { TopAppBar(title = { Text(text = ctx.getString(com.example.audioguideai.R.string.title_settings)) }) }) { p ->
        settings?.let { s ->
            LazyColumn(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Кнопка разрешений
                item {
                    val hasAllPermissions = PermissionManager.hasAllPermissions(ctx)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasAllPermissions) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (hasAllPermissions) "Все разрешения предоставлены" else "Требуются разрешения",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (hasAllPermissions) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = if (hasAllPermissions) 
                                        "Приложение работает в полном режиме" 
                                    else 
                                        "Некоторые функции недоступны",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (hasAllPermissions) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            if (!hasAllPermissions) {
                                Button(onClick = onRequestPermissions) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Настроить")
                                }
                            }
                        }
                    }
                }
                
                item {
                    Text("Радиус обнаружения (м): ${'$'}{s.radiusM}")
                }
                items(Category.values().size) { idx ->
                    val cat = Category.values()[idx]
                    val checked = s.enabledCategories.contains(cat.name)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(cat.titleRu)
                        Switch(checked = checked, onCheckedChange = { /* save via repo.setCategories(...) */ })
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }
    }
}
