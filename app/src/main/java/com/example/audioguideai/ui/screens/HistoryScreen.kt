package com.example.audioguideai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.audioguideai.data.Repository
import com.example.audioguideai.data.model.Poi

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HistoryScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val history by repo.history().collectAsState(initial = emptyList())
    val poiList by repo.allPoi().collectAsState(initial = emptyList())
    val byId = remember(poiList) { poiList.associateBy { it.id } }

    Scaffold(topBar = { TopAppBar(title = { Text(text = ctx.getString(com.example.audioguideai.R.string.title_history)) }) }) { p ->
        LazyColumn(Modifier.padding(p).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history.size) { i ->
                val h = history[i]
                val poi: Poi? = byId[h.poiId]
                if (poi != null) ListItem(headlineContent = { Text(poi.title) }, supportingContent = { Text(poi.description.take(120)) })
            }
        }
    }
}
