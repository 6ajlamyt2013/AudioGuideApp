package com.example.audioguideai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.audioguideai.data.Repository
import com.example.audioguideai.domain.AndroidTtsEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HistoryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val history by repo.history().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    val tts = remember { AndroidTtsEngine(ctx) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(text = ctx.getString(com.example.audioguideai.R.string.title_history)) },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, "Очистить историю")
                    }
                }
            ) 
        }
    ) { p ->
        LazyColumn(
            Modifier.padding(p).padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    Text(
                        text = "История пуста",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(history.size) { i ->
                    val h = history[i]
                    HistoryCard(
                        historyItem = h,
                        onShowOnMap = {
                            onBack()
                            // Здесь можно добавить логику центрирования карты
                        },
                        onRepeatSpeech = {
                            val speechText = "${h.name}. Находится в ${formatDistance(h.distance)} от вас."
                            tts.speak(speechText)
                        }
                    )
                }
            }
        }
    }
    
    // Диалог подтверждения очистки
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистить историю?") },
            text = { Text("Все записи истории будут удалены. Это действие нельзя отменить.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repo.clearHistory()
                            showClearDialog = false
                        }
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun HistoryCard(
    historyItem: com.example.audioguideai.data.model.HistoryItem,
    onShowOnMap: () -> Unit,
    onRepeatSpeech: () -> Unit
) {
    val dateFormat = remember {
        SimpleDateFormat("HH:mm, dd MMMM yyyy", Locale("ru", "RU"))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = historyItem.categoryIcon,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = historyItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "📍 ${formatDistance(historyItem.distance)} от вас",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "🕐 ${dateFormat.format(Date(historyItem.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShowOnMap,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("На карте")
                }
                OutlinedButton(
                    onClick = onRepeatSpeech,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Озвучить")
                }
            }
        }
    }
}

private fun formatDistance(meters: Float): String {
    return if (meters < 1000) {
        "${meters.toInt()} метрах"
    } else {
        String.format(Locale("ru"), "%.1f километрах", meters / 1000f)
    }
}
