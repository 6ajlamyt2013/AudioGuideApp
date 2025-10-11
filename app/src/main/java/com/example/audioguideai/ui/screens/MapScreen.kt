package com.example.audioguideai.ui.screens

import android.location.Location
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.audioguideai.data.Repository
import com.example.audioguideai.data.model.Category
import com.example.audioguideai.location.LocationForegroundService
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import com.example.audioguideai.R

@Composable
fun MapScreen(onOpenSettings: () -> Unit,
              onOpenHistory: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val poiList by repo.allPoi().collectAsState(initial = emptyList())
    val last by LocationForegroundService.lastLocationFlow.collectAsState(initial = null)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { MapBox(last, poiList) }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = onOpenSettings) {
                Text(text = ctx.getString(R.string.btn_settings))
            }
            Button(onClick = onOpenHistory) {
                Text(text = ctx.getString(R.string.btn_history))
            }
        }
    }
}


@Composable
private fun MapBox(last: Location?, poiList: List<com.example.audioguideai.data.model.Poi>) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val mapView = remember {
        MapKitFactory.initialize(ctx)
        com.yandex.mapkit.mapview.MapView(ctx).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
    DisposableEffect(Unit) {
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        onDispose {
            mapView.onStop()
            MapKitFactory.getInstance().onStop()
        }
    }
    AndroidView(
        factory = { mapView },
        update = { mv ->
            last?.let {
                mv.map.move(com.yandex.mapkit.map.CameraPosition(
                    Point(it.latitude, it.longitude), 15f, 0f, 0f
                ))
            }
            val map = mv.map
            map.mapObjects.clear()
            for (p in poiList) {
                val iconRes = when (p.category) {
                    Category.HISTORICAL -> R.drawable.ic_cat_historical
                    Category.STRUCTURES -> R.drawable.ic_cat_structures
                    Category.ART -> R.drawable.ic_cat_art
                    Category.NATURE -> R.drawable.ic_cat_nature
                    Category.ARCHITECTURE -> R.drawable.ic_cat_architecture
                    Category.CULTURE -> R.drawable.ic_cat_culture
                    Category.LEGENDS -> R.drawable.ic_cat_legends
                    Category.ROUTES -> R.drawable.ic_cat_routes
                    Category.SCIENCE -> R.drawable.ic_cat_science
                }
                val pm = map.mapObjects.addPlacemark(
                    Point(p.lat, p.lon),
                    com.yandex.runtime.image.ImageProvider.fromResource(mv.context, iconRes)
                )
                pm.userData = p.id
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}