package com.elendheim.recorder.audio

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.elendheim.recorder.MainActivity
import com.elendheim.recorder.R
import com.elendheim.recorder.data.SettingsStore
import com.elendheim.recorder.library.RecordingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * Keeps recording alive with the screen off or the app backgrounded. Without a
 * foreground service the OS throttles or kills background mic capture, so this
 * is what makes the app a real on-the-go recorder rather than a toy.
 */
class RecordingService : Service() {

    private val recorder = Recorder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var store: RecordingStore

    private var pcmFile: File? = null
    private var wavName: String = ""
    private var startedAt: Long = 0L

    override fun onCreate() {
        super.onCreate()
        store = RecordingStore(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    @Suppress("MissingPermission") // callers request RECORD_AUDIO before starting the service
    private fun startRecording() {
        if (recorder.isRecording) return

        startedAt = System.currentTimeMillis()
        // The display name (with its running number) is assigned at finalize, so
        // a mic that fails to open never burns a number.
        wavName = "rec_${startedAt}.wav"
        pcmFile = File(cacheDir, "rec_${startedAt}.pcm")

        // Go foreground first: once startForegroundService is called we must
        // call startForeground promptly, even if the mic then fails to open.
        startForegroundNotification(0L)

        val settings = SettingsStore.get(applicationContext).current
        val pcm = pcmFile ?: return
        if (!startCapture(pcm, settings.monitoring, settings.showPitch)) {
            RecordingController.markIdle()
            pcmFile = null
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        scope.launch {
            var lastSecondShown = -1L
            while (isActive && recorder.isRecording) {
                val elapsed = System.currentTimeMillis() - startedAt
                RecordingController.update(elapsed, recorder.currentAmplitude, recorder.currentPitch)
                val second = elapsed / 1000
                if (second != lastSecondShown) {
                    lastSecondShown = second
                    updateNotification(elapsed)
                }
                delay(100)
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startCapture(pcm: File, monitor: Boolean, detectPitch: Boolean): Boolean =
        recorder.start(pcm, monitor, detectPitch)

    private fun stopRecording() {
        if (!recorder.isRecording && pcmFile == null) {
            stopSelf()
            return
        }
        recorder.stop()
        RecordingController.markIdle()

        val pcm = pcmFile
        if (pcm != null && pcm.exists() && pcm.length() > 0) {
            val wav = File(store.recordingsDir, wavName)
            WavWriter.writeWav(pcm, wav, recorder.sampleRate, recorder.channels)
            pcm.delete()

            val byteRate = recorder.sampleRate * recorder.channels * recorder.bitsPerSample / 8
            val durationMs = if (byteRate > 0) wav.length().minus(44).coerceAtLeast(0) * 1000 / byteRate else 0
            val name = SettingsStore.get(applicationContext).nextRecordingName()
            store.add(wavName, name, durationMs, startedAt)
            RecordingController.notifyFinished()
        }
        pcmFile = null

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundNotification(elapsedMs: Long) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(elapsedMs), type)
    }

    private fun updateNotification(elapsedMs: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, buildNotification(elapsedMs))
    }

    private fun buildNotification(elapsedMs: Long): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, RecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_recording))
            .setContentText(formatElapsed(elapsedMs))
            .setSmallIcon(R.drawable.ic_stat_mic)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notif_stop), stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun formatElapsed(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        scope.cancel()
        if (recorder.isRecording) recorder.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.elendheim.recorder.action.START"
        const val ACTION_STOP = "com.elendheim.recorder.action.STOP"
        private const val CHANNEL_ID = "recording"
        private const val NOTIF_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, RecordingService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, RecordingService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
