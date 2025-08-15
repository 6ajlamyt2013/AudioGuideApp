package com.rashot.audioguideai.presentation.ui.components

@Composable
fun YandexMapView(
    modifier: Modifier = Modifier,
    cameraPosition: CameraPosition = CameraPosition(Point(55.751574, 37.573856), 11.0f, 0.0f, 0.0f),
    onMapLoaded: (YandexMap) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                map.mapType = MapType.MAP
                map.move(cameraPosition)
                map.addInputListener(object : InputListener {
                    override fun onMapTap(map: YandexMap, point: Point) {}
                    override fun onMapLongTap(map: YandexMap, point: Point) {}
                })
                onMapLoaded(map)
            }
        }
    )
}