package com.example.audioguideai.ui.screens

import android.graphics.*
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.audioguideai.R
import com.example.audioguideai.data.Repository
import com.example.audioguideai.data.model.Category
import com.example.audioguideai.location.LocationForegroundService
import com.example.audioguideai.sensors.OrientationManager
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.PlacemarkMapObject

@Composable
fun MapScreen(
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    centerTrigger: Int = 0
) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val poiList by repo.allPoi().collectAsState(initial = emptyList())
    val last by LocationForegroundService.lastLocationFlow.collectAsState(initial = null)

    val orientationManager = remember { OrientationManager(ctx) }
    val azimuth by orientationManager.azimuthFlow.collectAsState()

    DisposableEffect(Unit) {
        orientationManager.start()
        onDispose {
            orientationManager.stop()
        }
    }

    var localCenterTrigger by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        MapBox(last, poiList, centerTrigger, localCenterTrigger, azimuth)

        // Отображение текущего азимута для отладки
        Text(
            text = "Азимут: ${azimuth.toInt()}°",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        FloatingActionButton(
            onClick = { localCenterTrigger++ },
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
private fun MapBox(
    last: Location?,
    poiList: List<com.example.audioguideai.data.model.Poi>,
    centerTrigger: Int,
    localCenterTrigger: Int,
    azimuth: Float
) {
    val ctx = LocalContext.current
    var mapView by remember { mutableStateOf<com.yandex.mapkit.mapview.MapView?>(null) }
    var userLocationPlacemark by remember { mutableStateOf<PlacemarkMapObject?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    var poiPlacemarks by remember { mutableStateOf<List<PlacemarkMapObject>>(emptyList()) }

    // Анимированный азимут для плавного поворота
    val animatedAzimuth = remember { androidx.compose.animation.core.Animatable(0f) }

    // Округленный азимут для уменьшения частоты обновлений
    var targetAzimuth by remember { mutableStateOf(0f) }

    val mapViewInstance = remember {
        MapKitFactory.initialize(ctx)
        com.yandex.mapkit.mapview.MapView(ctx).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Обновляем целевой азимут с округлением
    LaunchedEffect(azimuth) {
        val rounded = (azimuth / 15f).toInt() * 15f // Округляем до 15 градусов
        if (kotlin.math.abs(rounded - targetAzimuth) >= 15f) {
            targetAzimuth = rounded
        }
    }

    // Плавная анимация к целевому азимуту
    LaunchedEffect(targetAzimuth) {
        if (targetAzimuth != 0f || animatedAzimuth.value != 0f) {
            animatedAzimuth.animateTo(
                targetValue = targetAzimuth,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 300,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
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

    LaunchedEffect(last, isInitialized) {
        if (last != null && !isInitialized) {
            mapView?.map?.move(
                com.yandex.mapkit.map.CameraPosition(
                    Point(last.latitude, last.longitude),
                    19f, 0f, 0f
                )
            )
            isInitialized = true
        }
    }

    LaunchedEffect(centerTrigger, localCenterTrigger) {
        if ((centerTrigger > 0 || localCenterTrigger > 0) && last != null) {
            mapView?.map?.move(
                com.yandex.mapkit.map.CameraPosition(
                    Point(last.latitude, last.longitude),
                    19f, 0f, 0f
                )
            )
        }
    }

    // Создаем маркер только один раз при изменении локации
    LaunchedEffect(last) {
        last?.let { location ->
            mapView?.map?.mapObjects?.let { mapObjects ->
                if (userLocationPlacemark == null) {
                    userLocationPlacemark = mapObjects.addPlacemark(
                        Point(location.latitude, location.longitude)
                    ).apply {
                        setIconStyle(
                            com.yandex.mapkit.map.IconStyle().apply {
                                scale = 1.0f
                                anchor = PointF(0.5f, 0.5f)
                            }
                        )
                        setIcon(com.yandex.runtime.image.ImageProvider.fromBitmap(
                            createUserLocationIcon(0f)
                        ))
                    }
                } else {
                    userLocationPlacemark?.geometry = Point(location.latitude, location.longitude)
                }
            }
        }
    }

    // Обновляем только иконку при изменении анимированного азимута (каждый кадр анимации)
    LaunchedEffect(animatedAzimuth.value) {
        snapshotFlow { animatedAzimuth.value }
            .collect { currentAzimuth ->
                userLocationPlacemark?.setIcon(
                    com.yandex.runtime.image.ImageProvider.fromBitmap(
                        createUserLocationIcon(currentAzimuth)
                    )
                )
            }
    }

    LaunchedEffect(poiList) {
        mapView?.map?.mapObjects?.let { mapObjects ->
            poiPlacemarks.forEach { mapObjects.remove(it) }

            val newPlacemarks = poiList.map { p ->
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
                mapObjects.addPlacemark(
                    Point(p.lat, p.lon),
                    com.yandex.runtime.image.ImageProvider.fromResource(ctx, iconRes)
                ).apply {
                    userData = p.id
                }
            }
            poiPlacemarks = newPlacemarks
        }
    }

    AndroidView(
        factory = { mapViewInstance },
        modifier = Modifier.fillMaxSize()
    )
}

// Функция создания иконки с учетом азимута (поворачиваем canvas)
private fun createUserLocationIcon(azimuth: Float): Bitmap {
    val size = 250
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centerX = size / 2f
    val centerY = size / 2f

    // ВАЖНО: Поворачиваем canvas на угол азимута
    canvas.save()
    canvas.rotate(azimuth, centerX, centerY)

    // Рисуем полупрозрачный фиолетовый круг погрешности
    val accuracyPaint = Paint().apply {
        color = Color.argb(40, 138, 43, 226)
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, 90f, accuracyPaint)

    // Рисуем белый круг с фиолетовой тенью
    val whitePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
        setShadowLayer(9f, 0f, 4.5f, Color.argb(70, 75, 0, 130))
    }
    canvas.drawCircle(centerX, centerY, 36f, whitePaint)

    // Рисуем фиолетовый круг внутри
    val purplePaint = Paint().apply {
        color = Color.rgb(138, 43, 226)
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(centerX, centerY, 18f, purplePaint)

    // Рисуем большую заметную стрелочку сверху
    val arrowPaint = Paint().apply {
        color = Color.rgb(138, 43, 226)
        isAntiAlias = true
        style = Paint.Style.FILL
        setShadowLayer(6f, 0f, 3f, Color.argb(120, 0, 0, 0))
    }

    // Треугольная стрелочка (указывает ВВЕРХ, canvas уже повернут)
    val arrowPath = Path().apply {
        moveTo(centerX, centerY - 80f) // Верхняя точка стрелочки
        lineTo(centerX - 18f, centerY - 40f) // Левая
        lineTo(centerX, centerY - 45f) // Вырез
        lineTo(centerX + 18f, centerY - 40f) // Правая
        close()
    }
    canvas.drawPath(arrowPath, arrowPaint)

    // Белая обводка стрелочки для контраста
    val arrowStrokePaint = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawPath(arrowPath, arrowStrokePaint)

    canvas.restore()

    return bitmap
}