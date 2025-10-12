package com.example.audioguideai.ui.screens

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.audioguideai.data.Repository
import com.example.audioguideai.data.model.Category
import com.example.audioguideai.location.LocationForegroundService
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.example.audioguideai.R

@Composable
fun MapScreen(
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    centerTrigger: Int = 0
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val poiList by repo.allPoi().collectAsState(initial = emptyList())
    val last by LocationForegroundService.lastLocationFlow.collectAsState(initial = null)
    
    // Локальное состояние для кнопки геолокации
    var localCenterTrigger by remember { mutableStateOf(0) }
    
    Box(Modifier.fillMaxSize()) {
        MapBox(last, poiList, centerTrigger, localCenterTrigger)
        
        // Кнопка геолокации в правом верхнем углу
        FloatingActionButton(
            onClick = { localCenterTrigger++ }, // Увеличиваем счетчик для триггера центрирования
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(30.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Определить местоположение"
            )
        }
        
        // Кнопки навигации внизу
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
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
private fun MapBox(last: Location?, poiList: List<com.example.audioguideai.data.model.Poi>, centerTrigger: Int, localCenterTrigger: Int) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var mapView by remember { mutableStateOf<com.yandex.mapkit.mapview.MapView?>(null) }
    var userLocationPlacemark by remember { mutableStateOf<com.yandex.mapkit.map.MapObject?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    
    val mapViewInstance = remember {
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
        mapViewInstance.onStart()
        mapView = mapViewInstance
        onDispose {
            mapViewInstance.onStop()
            MapKitFactory.getInstance().onStop()
        }
    }
    
    // Инициализация карты при первом получении локации
    LaunchedEffect(last, isInitialized) {
        if (last != null && !isInitialized) {
            mapView?.map?.move(
                com.yandex.mapkit.map.CameraPosition(
                    Point(last.latitude, last.longitude), 
                    19f, 0f, 0f  // Более приближенный зум
                )
            )
            isInitialized = true
        }
    }
    
    // Реагируем на изменение centerTrigger для центрирования карты
    LaunchedEffect(centerTrigger, localCenterTrigger) {
        if ((centerTrigger > 0 || localCenterTrigger > 0) && last != null) {
            mapView?.map?.move(
                com.yandex.mapkit.map.CameraPosition(
                    Point(last.latitude, last.longitude), 
                    19f, 0f, 0f  // Более приближенный зум
                )
            )
        }
    }
    
    AndroidView(
        factory = { mapViewInstance },
        update = { mv ->
            val map = mv.map
            
            // Очищаем все объекты карты
            map.mapObjects.clear()
            
            // Добавляем маркер пользователя
            last?.let { location ->
                userLocationPlacemark = map.mapObjects.addPlacemark(
                    Point(location.latitude, location.longitude)
                ).apply {
                    // Настраиваем стиль маркера пользователя
                    setIconStyle(
                        com.yandex.mapkit.map.IconStyle().apply {
                            scale = 2.0f
                        }
                    )
                    // Создаем простой цветной маркер
                    setIcon(com.yandex.runtime.image.ImageProvider.fromBitmap(
                        android.graphics.Bitmap.createBitmap(216, 216, android.graphics.Bitmap.Config.ARGB_8888).apply {
                            val canvas = android.graphics.Canvas(this)
                            val centerX = 108f
                            val centerY = 108f

                            // Рисуем полупрозрачный фиолетовый круг погрешности
                            val accuracyPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(40, 138, 43, 226) // Полупрозрачный фиолетовый
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                            }
                            canvas.drawCircle(centerX, centerY, 90f, accuracyPaint)

                            // Рисуем белый круг с фиолетовой тенью
                            val whitePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                                setShadowLayer(9f, 0f, 4.5f, android.graphics.Color.argb(70, 75, 0, 130))  // Фиолетовая тень
                            }
                            canvas.drawCircle(centerX, centerY, 36f, whitePaint)

                            // Рисуем фиолетовый круг внутри
                            val purplePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.rgb(138, 43, 226) // Фиолетовый (BlueViolet)
                                isAntiAlias = true
                                style = android.graphics.Paint.Style.FILL
                            }
                            canvas.drawCircle(centerX, centerY, 18f, purplePaint)
                        }
                    ))
                }
            }
            
            // Добавляем POI маркеры
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