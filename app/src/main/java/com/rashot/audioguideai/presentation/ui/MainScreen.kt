package com.rashot.audioguideai.presentation.ui

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioGuideAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

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
            // Замените GoogleMap на YandexMapView
            YandexMapView(
                modifier = Modifier.weight(1f),
                cameraPosition = CameraPosition(
                    Point(
                        uiState.currentLocation?.latitude ?: 55.751574,
                        uiState.currentLocation?.longitude ?: 37.573856
                    ),
                    15f
                )
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