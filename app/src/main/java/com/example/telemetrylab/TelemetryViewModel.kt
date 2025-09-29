package com.example.telemetrylab

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.*
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.ArrayDeque

data class UiState(
    val running: Boolean = false,
    val computeLoad: Int = 2,
    val latestFrameMs: Double = 0.0,
    val avgFrameMs: Double = 0.0,
    val jankPercent30s: Double = 0.0,
    val isPowerSave: Boolean = false,
    val sampleCount: Int = 0
)

class TelemetryViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _jankSamples = MutableSharedFlow<Boolean>(extraBufferCapacity = 64)

    @SuppressLint("StaticFieldLeak")
    private val context: Context = app.applicationContext
    private val telemetryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val frameTime = intent?.getLongExtra("frameTimeMs", 0) ?: 0
            _uiState.update { state ->
                val newSampleCount = state.sampleCount + 1
                val newAvg = ((state.avgFrameMs * state.sampleCount) + frameTime) / newSampleCount
                state.copy(
                    latestFrameMs = frameTime.toDouble(),
                    avgFrameMs = newAvg,
                    sampleCount = newSampleCount
                )
            }
        }
    }

    init {
        // Register BroadcastReceiver
        val filter = IntentFilter(TelemetryService.ACTION_TELEMETRY_UPDATE)
        ContextCompat.registerReceiver(
            context,
            telemetryReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Rolling window of jank ~30s @ 60Hz
        viewModelScope.launch {
            val window = ArrayDeque<Boolean>(1800)
            _jankSamples.collect { isJank ->
                if (window.size >= 1800) window.removeFirst()
                window.addLast(isJank)
                val janks = window.count { it }
                val percent = if (window.isEmpty()) 0.0 else janks * 100.0 / window.size
                _uiState.update { it.copy(jankPercent30s = percent) }
            }
        }

        // Poll Battery Saver
        viewModelScope.launch {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            while (true) {
                val isSaver = pm.isPowerSaveMode
                _uiState.update { it.copy(isPowerSave = isSaver) }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    fun onJankFrame(isJank: Boolean) {
        viewModelScope.launch { _jankSamples.emit(isJank) }
    }

    fun setComputeLoad(n: Int) {
        _uiState.update { it.copy(computeLoad = n) }

        // Update the running service if any
        val intent = Intent(TelemetryService.ACTION_TELEMETRY_UPDATE)
        context.sendBroadcast(intent) // optional trigger if needed
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startService(activity: Activity) {
        val intent = Intent(activity, TelemetryService::class.java).apply {
            putExtra(TelemetryService.EXTRA_LOAD, _uiState.value.computeLoad)
        }
        activity.startForegroundService(intent)
        _uiState.update { it.copy(running = true) }
    }

    fun stopService(activity: Activity) {
        val intent = Intent(activity, TelemetryService::class.java)
        activity.stopService(intent)
        _uiState.update { it.copy(running = false) }
    }

    override fun onCleared() {
        super.onCleared()
        context.unregisterReceiver(telemetryReceiver)
    }

    fun updateFrameStats(latency: Long, avg: Long) {
        _uiState.update { it.copy(latestFrameMs = latency.toDouble(), avgFrameMs = avg.toDouble()) }
    }

}
