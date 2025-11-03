package com.example.audioguideai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.audioguideai.data.SettingsRepo
import com.example.audioguideai.data.model.Category
import com.example.audioguideai.permissions.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    onBack: () -> Unit, 
    onRequestPermissions: () -> Unit = {},
    onStart: () -> Unit = {}
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { SettingsRepo(ctx) }
    val settings by repo.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var showAdvanced by remember { mutableStateOf(false) }

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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Радиус обнаружения",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${s.radiusM} м",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Slider(
                                value = s.radiusM.toFloat(),
                                onValueChange = { newValue ->
                                    scope.launch {
                                        repo.setRadius(newValue.toInt())
                                    }
                                },
                                valueRange = 100f..5000f,
                                steps = 48, // Шаг 100м
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "100 м",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = if (s.radiusM >= 1000) "${s.radiusM / 1000f} км" else "${s.radiusM} м",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "Объекты будут обнаружены в пределах ${s.radiusM} метров от вашего местоположения",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Категории объектов",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = "${s.enabledCategories.size} из ${Category.values().size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                
                items(Category.values().size) { idx ->
                    val cat = Category.values()[idx]
                    val checked = s.enabledCategories.contains(cat.name)
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (checked) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.titleRu,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (checked) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (checked) "Включена" else "Отключена",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (checked) 
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        val currentCats = s.enabledCategories
                                        if (!isChecked && currentCats.size == 1) {
                                            // Не позволяем отключить последнюю категорию
                                            android.widget.Toast.makeText(
                                                ctx,
                                                "Должна быть выбрана хотя бы одна категория",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            val newCategories = if (isChecked) {
                                                currentCats + cat.name
                                            } else {
                                                currentCats - cat.name
                                            }
                                            repo.setCategories(newCategories)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Расширенные настройки
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showAdvanced = !showAdvanced }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Расширенные настройки",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Icon(
                                imageVector = if (showAdvanced) 
                                    Icons.Default.KeyboardArrowUp 
                                else 
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = null
                            )
                        }
                    }
                }
                
                if (showAdvanced && settings != null) {
                    item {
                        AdvancedSettingsSection(s = settings!!, repo = repo, scope = scope, ctx = ctx)
                    }
                }
                
                // Кнопка "Старт"
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                // Проверяем, выбрана ли хотя бы одна категория
                                val currentSettings = repo.settings.firstOrNull()
                                if (currentSettings?.enabledCategories?.isEmpty() == true) {
                                    android.widget.Toast.makeText(
                                        ctx,
                                        "Выберите хотя бы одну категорию",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    onStart()
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Старт",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }
        } ?: Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }
    }
}

@Composable
private fun AdvancedSettingsSection(
    s: com.example.audioguideai.data.Settings,
    repo: SettingsRepo,
    scope: CoroutineScope,
    ctx: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Минимальное смещение
        SettingSlider(
            label = "Мин. смещение для запроса",
            value = s.minDisplacementM.toFloat(),
            onValueChange = { scope.launch { repo.setMinDisplacement(it.toInt()) } },
            valueRange = 10f..200f,
            steps = 18,
            format = { "${it.toInt()} м" }
        )
        
        // Максимум объектов за цикл
        SettingSlider(
            label = "Макс. объектов за цикл",
            value = s.maxObjectsPerCycle.toFloat(),
            onValueChange = { scope.launch { repo.setMaxObjectsPerCycle(it.toInt()) } },
            valueRange = 1f..10f,
            steps = 8,
            format = { "${it.toInt()}" }
        )
        
        // Скорость речи
        SettingSlider(
            label = "Скорость речи",
            value = s.voiceSpeed,
            onValueChange = { scope.launch { repo.setVoiceSpeed(it) } },
            valueRange = 0.5f..1.5f,
            steps = 9,
            format = { String.format("%.1f", it) }
        )
        
        // Пауза между объектами
        SettingSlider(
            label = "Пауза между объектами",
            value = (s.pauseBetweenObjectsMs / 1000f),
            onValueChange = { scope.launch { repo.setPauseBetweenObjects((it * 1000).toInt()) } },
            valueRange = 0f..5f,
            steps = 9,
            format = { String.format("%.1f сек", it) }
        )
        
        // Кнопка сброса
        OutlinedButton(
            onClick = {
                scope.launch {
                    repo.resetToDefaults()
                    android.widget.Toast.makeText(
                        ctx,
                        "Настройки сброшены",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сбросить все настройки")
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = format(value),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}
