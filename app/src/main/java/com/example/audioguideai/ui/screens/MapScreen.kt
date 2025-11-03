package com.example.audioguideai.ui.screens

import android.graphics.*
import android.location.Location
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.audioguideai.R
import com.example.audioguideai.data.Repository
import com.example.audioguideai.data.SettingsRepo
import com.example.audioguideai.data.model.Category
import com.example.audioguideai.location.LocationForegroundService
import com.example.audioguideai.sensors.OrientationManager
import com.example.audioguideai.viewmodel.GuideViewModel
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.PlacemarkMapObject
import kotlinx.coroutines.delay

@Composable
fun MapScreen(
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    centerTrigger: Int = 0,
    viewModel: GuideViewModel
) {
    val ctx = LocalContext.current
    val repo = remember { Repository.get(ctx) }
    val settingsRepo = remember { SettingsRepo(ctx) }
    val settings by settingsRepo.settings.collectAsState(initial = null)
    val allPoiList by repo.allPoi().collectAsState(initial = emptyList())
    val last by LocationForegroundService.lastLocationFlow.collectAsState(initial = null)
    val currentPoi by viewModel.currentPoi.collectAsState(initial = null)
    
    val poiList = remember(allPoiList, settings) {
        allPoiList.filter { poi -> settings?.enabledCategories?.contains(poi.category.name) == true }
    }

    val orientationManager = remember { OrientationManager(ctx) }
    val azimuth by orientationManager.azimuthFlow.collectAsState()

    DisposableEffect(Unit) {
        orientationManager.start()
        onDispose { orientationManager.stop() }
    }

    var localCenterTrigger by remember { mutableStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        MapBox(last, poiList, centerTrigger, localCenterTrigger, azimuth, currentPoi, settings)

        val isRunning by com.example.audioguideai.service.GeoGuideService.isRunningFlow.collectAsState(initial = false)
        val allPoiCount = poiList.size


        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onOpenHistory) {
                Text(text = ctx.getString(R.string.btn_history))

            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(40.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(text = if (isRunning) "Поиск активен" else "Поиск не активен", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    settings?.let {
                        Text(text = "Радиус: ${if (it.radiusM >= 1000) "${it.radiusM / 1000f} км" else "${it.radiusM} м"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                    }
                    Text(text = "Найдено: $allPoiCount объектов", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 0.dp, end = 0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            FloatingActionButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(66.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = ctx.getString(R.string.btn_settings)
                )
            }

            FloatingActionButton(
                onClick = {
                    if (isRunning) com.example.audioguideai.service.GeoGuideService.stop(ctx)
                    else com.example.audioguideai.service.GeoGuideService.start(ctx)
                },
                modifier = Modifier.size(110.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = if (isRunning) "Остановить" else "Старт",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            FloatingActionButton(
                onClick = { localCenterTrigger++ },
                modifier = Modifier.size(66.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Определить местоположение"
                )
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
    azimuth: Float,
    currentPoi: com.example.audioguideai.data.model.Poi?,
    settings: com.example.audioguideai.data.Settings?
) {
    val ctx = LocalContext.current
    var mapView by remember { mutableStateOf<com.yandex.mapkit.mapview.MapView?>(null) }
    var userLocationPlacemark by remember { mutableStateOf<PlacemarkMapObject?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    var poiPlacemarks by remember { mutableStateOf<List<PlacemarkMapObject>>(emptyList()) }
    var currentPoiPlacemark by remember { mutableStateOf<PlacemarkMapObject?>(null) }
    var searchRadiusCircle by remember { mutableStateOf<com.yandex.mapkit.map.CircleMapObject?>(null) }

    val animatedAzimuth = remember { androidx.compose.animation.core.Animatable(0f) }
    var targetAzimuth by remember { mutableStateOf(0f) }

    val mapViewInstance = remember {
        MapKitFactory.initialize(ctx)
        com.yandex.mapkit.mapview.MapView(ctx).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    LaunchedEffect(azimuth) {
        val rounded = (azimuth / 15f).toInt() * 15f
        if (kotlin.math.abs(rounded - targetAzimuth) >= 15f) targetAzimuth = rounded
    }

    LaunchedEffect(targetAzimuth) {
        if (targetAzimuth != 0f || animatedAzimuth.value != 0f) {
            animatedAzimuth.animateTo(targetValue = targetAzimuth, animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        }
    }

    DisposableEffect(Unit) {
        MapKitFactory.getInstance().onStart()
        mapViewInstance.onStart()
        mapView = mapViewInstance
        onDispose {
            mapViewInstance.onStop(); MapKitFactory.getInstance().onStop()
        }
    }

    LaunchedEffect(last, isInitialized) {
        if (last != null && !isInitialized) {
            mapView?.map?.move(com.yandex.mapkit.map.CameraPosition(Point(last.latitude, last.longitude), 19f, 0f, 0f))
            isInitialized = true
        }
    }

    LaunchedEffect(centerTrigger, localCenterTrigger) {
        if ((centerTrigger > 0 || localCenterTrigger > 0) && last != null) {
            mapView?.map?.move(com.yandex.mapkit.map.CameraPosition(Point(last.latitude, last.longitude), 19f, 0f, 0f))
        }
    }

    LaunchedEffect(last) {
        last?.let { location ->
            mapView?.map?.mapObjects?.let { mapObjects ->
                if (userLocationPlacemark == null) {
                    userLocationPlacemark = mapObjects.addPlacemark(Point(location.latitude, location.longitude)).apply {
                        setIconStyle(com.yandex.mapkit.map.IconStyle().apply { scale = 1.0f; anchor = PointF(0.5f, 0.5f) })
                        setIcon(com.yandex.runtime.image.ImageProvider.fromBitmap(createUserLocationIcon(0f)))
                    }
                } else {
                    userLocationPlacemark?.geometry = Point(location.latitude, location.longitude)
                }
            }
        }
    }

    LaunchedEffect(last, settings?.radiusM) {
        last?.let { location ->
            settings?.let { s ->
                mapView?.map?.mapObjects?.let { mapObjects ->
                    if (searchRadiusCircle == null) {
                        searchRadiusCircle = mapObjects.addCircle(Circle(Point(location.latitude, location.longitude), s.radiusM.toFloat()))
                        searchRadiusCircle?.setFillColor(android.graphics.Color.argb(50, 255, 255, 255))
                        searchRadiusCircle?.setStrokeColor(android.graphics.Color.argb(100, 255, 255, 255))
                        searchRadiusCircle?.setStrokeWidth(2f)
                    } else {
                        searchRadiusCircle?.apply { geometry = Circle(Point(location.latitude, location.longitude), s.radiusM.toFloat()) }
                    }
                }
            }
        }
    }

    LaunchedEffect(animatedAzimuth.value) {
        snapshotFlow { animatedAzimuth.value }.collect { currentAzimuth ->
            userLocationPlacemark?.setIcon(com.yandex.runtime.image.ImageProvider.fromBitmap(createUserLocationIcon(currentAzimuth)))
        }
    }

    LaunchedEffect(poiList) {
        mapView?.map?.mapObjects?.let { mapObjects ->
            poiPlacemarks.forEach { mapObjects.remove(it) }

            val newPlacemarks = poiList.map { p ->
                val iconRes = when (p.category) {
                    Category.HISTORICAL -> R.drawable.ic_cat_historical
                    Category.RELIGIOUS_BUILDINGS -> R.drawable.ic_cat_structures
                    Category.RELIGION -> R.drawable.ic_cat_structures
                    Category.DENOMINATION -> R.drawable.ic_cat_structures
                    Category.TOURISM -> R.drawable.ic_cat_culture
                }
                mapObjects.addPlacemark(Point(p.lat, p.lon), com.yandex.runtime.image.ImageProvider.fromResource(ctx, iconRes)).apply { userData = p.id }
            }
            poiPlacemarks = newPlacemarks
        }
    }

    LaunchedEffect(currentPoi) {
        mapView?.map?.mapObjects?.let { mapObjects ->
            currentPoiPlacemark?.let { mapObjects.remove(it) }
            currentPoiPlacemark = null
            currentPoi?.let { poi ->
                currentPoiPlacemark = mapObjects.addPlacemark(Point(poi.lat, poi.lon)).apply {
                    var scale = 1.0f
                    while (true) {
                        scale = if (scale >= 1.5f) 1.0f else scale + 0.05f
                        setIconStyle(com.yandex.mapkit.map.IconStyle().apply { this.scale = scale; anchor = PointF(0.5f, 0.5f) })
                        setIcon(com.yandex.runtime.image.ImageProvider.fromBitmap(createPulsingRedPoint()))
                        delay(50)
                    }
                }
            }
        }
    }

    AndroidView(factory = { mapViewInstance }, modifier = Modifier.fillMaxSize())
}

private fun createUserLocationIcon(azimuth: Float): Bitmap {
    val size = 250
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centerX = size / 2f
    val centerY = size / 2f
    canvas.save(); canvas.rotate(azimuth, centerX, centerY)
    val accuracyPaint = Paint().apply { color = Color.argb(40, 138, 43, 226); isAntiAlias = true; style = Paint.Style.FILL }
    canvas.drawCircle(centerX, centerY, 90f, accuracyPaint)
    val whitePaint = Paint().apply { color = Color.WHITE; isAntiAlias = true; style = Paint.Style.FILL; setShadowLayer(9f, 0f, 4.5f, Color.argb(70, 75, 0, 130)) }
    canvas.drawCircle(centerX, centerY, 36f, whitePaint)
    val purplePaint = Paint().apply { color = Color.rgb(138, 43, 226); isAntiAlias = true; style = Paint.Style.FILL }
    canvas.drawCircle(centerX, centerY, 18f, purplePaint)
    val arrowPaint = Paint().apply { color = Color.rgb(138, 43, 226); isAntiAlias = true; style = Paint.Style.FILL; setShadowLayer(6f, 0f, 3f, Color.argb(120, 0, 0, 0)) }
    val arrowPath = Path().apply { moveTo(centerX, centerY - 80f); lineTo(centerX - 18f, centerY - 40f); lineTo(centerX, centerY - 45f); lineTo(centerX + 18f, centerY - 40f); close() }
    canvas.drawPath(arrowPath, arrowPaint)
    val arrowStrokePaint = Paint().apply { color = Color.WHITE; isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 3f }
    canvas.drawPath(arrowPath, arrowStrokePaint)
    canvas.restore()
    return bitmap
}

private fun createPulsingRedPoint(): Bitmap {
    val size = 150
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val centerX = size / 2f
    val centerY = size / 2f
    val pulsePaint = Paint().apply { color = Color.argb(100, 255, 0, 0); isAntiAlias = true; style = Paint.Style.FILL }
    canvas.drawCircle(centerX, centerY, 50f, pulsePaint)
    val whitePaint = Paint().apply { color = Color.WHITE; isAntiAlias = true; style = Paint.Style.FILL; setShadowLayer(8f, 0f, 4f, Color.argb(150, 255, 0, 0)) }
    canvas.drawCircle(centerX, centerY, 30f, whitePaint)
    val redPaint = Paint().apply { color = Color.rgb(255, 0, 0); isAntiAlias = true; style = Paint.Style.FILL }
    canvas.drawCircle(centerX, centerY, 20f, redPaint)
    return bitmap
}