package com.example.audioguideai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.audioguideai.data.SettingsRepo
import com.example.audioguideai.data.model.Category

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { SettingsRepo(ctx) }
    val settings by repo.settings.collectAsState(initial = null)

    Scaffold(topBar = { TopAppBar(title = { Text(text = ctx.getString(com.example.audioguideai.R.string.title_settings)) }) }) { p ->
        settings?.let { s ->
            LazyColumn(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
