package com.omgodse.notally.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import com.omgodse.notally.R
import com.omgodse.notally.activities.RecordAudio
import com.omgodse.notally.audio.Status.PAUSED
import com.omgodse.notally.audio.Status.READY
import com.omgodse.notally.audio.Status.RECORDING
import com.omgodse.notally.miscellaneous.IO

@RequiresApi(24)
class AudioRecordService : Service() {

    var status = READY
    private var lastStart = 0L
    private var audioDuration = 0L

    private lateinit var recorder: MediaRecorder
    private lateinit var manager: NotificationManager
    private lateinit var builder: Notification.Builder

    override fun onCreate() {
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        builder = Notification.Builder(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "com.omgodse.audio"
            val channel = NotificationChannel(channelId, "Audio Recordings", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
            builder.setChannelId(channelId)
        }

        builder.setSmallIcon(R.drawable.record_audio)
        builder.setOnlyAlertOnce(true)

        /*
        Prevent user from dismissing notification in Android 13 (33) and above
        https://developer.android.com/guide/components/foreground-services#user-dismiss-notification
         */
        builder.setOngoing(true)

        /*
        On Android 12 (31) and above, the system waits 10 seconds before showing the notification.
        https://developer.android.com/guide/components/foreground-services#notification-immediate
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        val intent = Intent(this, RecordAudio::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)

        startForeground(2, buildNotification())

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else MediaRecorder()

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        val output = IO.getTempAudioFile(this)
        recorder.setOutputFile(output.path)
        recorder.prepare()
    }

    override fun onDestroy() {
        recorder.release()
    }


    override fun onBind(intent: Intent?) = LocalBinder(this)


    fun start() {
        recorder.start()
        status = RECORDING
        lastStart = SystemClock.elapsedRealtime()
        manager.notify(2, buildNotification())
    }

    fun resume() {
        recorder.resume()
        status = RECORDING
        lastStart = SystemClock.elapsedRealtime()
        manager.notify(2, buildNotification())
    }

    fun pause() {
        recorder.pause()
        status = PAUSED
        audioDuration += SystemClock.elapsedRealtime() - lastStart
        lastStart = 0L
        manager.notify(2, buildNotification())
    }

    fun stop() {
        recorder.stop()
        stopSelf()
    }

    fun getBase(): Long {
        return if (lastStart != 0L) {
            lastStart - audioDuration
        } else SystemClock.elapsedRealtime() - audioDuration
    }

    private fun buildNotification(): Notification {
        val title = when (status) {
            READY -> getString(R.string.ready_to_record)
            PAUSED -> getString(R.string.paused)
            RECORDING -> getString(R.string.recording)
        }
        builder.setContentTitle(title)
        builder.setContentText(getString(R.string.tap_for_more_options))
        return builder.build()
    }
}