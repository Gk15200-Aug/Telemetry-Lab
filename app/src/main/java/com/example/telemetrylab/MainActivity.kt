package com.example.telemetrylab

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats

class MainActivity : ComponentActivity() {

    private val viewModel: TelemetryViewModel by viewModels()

    private var lastFrameTimeNs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TelemetryScreen(viewModel, this)

            /*LaunchedEffect(Unit) {
                while (true) {
                    withFrameNanos { frameTimeNs ->
                        if (lastFrameTimeNs != 0L) {
                            val deltaMs = (frameTimeNs - lastFrameTimeNs) / 1_000_000.0
                            val jankOccurred = deltaMs > 16.66
                            viewModel.onJankFrame(jankOccurred)
                            viewModel.updateFrameStats(deltaMs.toLong(), deltaMs.toLong())
                        }
                        lastFrameTimeNs = frameTimeNs
                    }
                    kotlinx.coroutines.delay(16L) // approx 60Hz
                }
            }*/
        }
    }
}


@Composable
fun TelemetryScreen(viewModel: TelemetryViewModel, activity: Activity) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Telemetry Lab", style = MaterialTheme.typography.headlineMedium)

        Button(onClick = {
            if (uiState.running) {
                viewModel.stopService(activity)
            } else {
                viewModel.startService(activity)
            }
        }) {
            Text(if (uiState.running) "Stop" else "Start")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Compute Load: ${uiState.computeLoad}")
            Slider(
                value = uiState.computeLoad.toFloat(),
                onValueChange = { viewModel.setComputeLoad(it.toInt()) },
                valueRange = 1f..5f,
                steps = 3
            )
        }

        HorizontalDivider()

        // Display frame statistics
        Text("Current Latency: ${uiState.latestFrameMs} ms")
        Text("Average Latency: ${uiState.avgFrameMs} ms")
        Text("Jank % (last 30s): ${"%.2f".format(uiState.jankPercent30s)} %")
        if (uiState.isPowerSave) Text("Power-save mode enabled")

        Spacer(modifier = Modifier.height(20.dp))

        var counter by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(500)
                counter++
            }
        }
        Text("Counter: $counter")
    }
}