package com.rashot.audioguideai.presentation.ui

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AudioGuideAI") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.startTracking() },
                containerColor = if (uiState.isActive) Color.Red else Color.Green
            ) {
                Icon(
                    imageVector = if (uiState.isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Start/Stop"
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            GoogleMap(
                modifier = Modifier.weight(1f),
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapType = MapType.NORMAL
                ),
                cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        LatLng(
                            uiState.currentLocation?.latitude ?: 0.0,
                            uiState.currentLocation?.longitude ?: 0.0
                        ), 15f
                    )
                }
            )

            uiState.currentPoi?.let { poi ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = poi.name, style = MaterialTheme.typography.titleLarge)
                        Text(text = poi.description)
                        Text(text = "Distance: ${uiState.distanceToPoi.toInt()}m")
                    }
                }
            }
        }
    }
}