package com.example.telemetrylab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

class TelemetryService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var computeLoad: Int = 2

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        computeLoad = intent?.getIntExtra(EXTRA_LOAD, 2) ?: 2

        createNotificationChannel()
        val notification = buildNotification("Telemetry running")
        startForeground(NOTIFICATION_ID, notification)

        Log.d("TelemetryService", "Service started with load=$computeLoad")

        // Cancel any old loops first
        serviceJob.cancelChildren()

        // Start single compute loop
        serviceScope.launch {
            while (isActive) {
                val start = System.nanoTime()
                busyWork(computeLoad)
                val end = System.nanoTime()
                val frameTimeMs = (end - start) / 1_000_000

                // Broadcast result to ViewModel/UI
                val intentUpdate = Intent(ACTION_TELEMETRY_UPDATE).apply {
                    putExtra("frameTimeMs", frameTimeMs)
                }
                sendBroadcast(intentUpdate)

                delay(50L) // ~20Hz
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Telemetry Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Telemetry Service")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Telemetry Service")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build()
        }
    }

    fun updateComputeLoad(load: Int) {
        // just update the load, the running loop will use the new value
        computeLoad = load
    }

    private fun busyWork(load: Int) {
        var result = 0.0
        for (i in 0 until load * 100_000) {
            result += Math.sqrt(i.toDouble())
        }
    }

    companion object {
        private const val CHANNEL_ID = "telemetry_channel"
        private const val NOTIFICATION_ID = 1
        const val EXTRA_LOAD = "extra_compute_load"

        const val ACTION_TELEMETRY_UPDATE = "com.example.telemetrylab.TELEMETRY_UPDATE"
    }
}
