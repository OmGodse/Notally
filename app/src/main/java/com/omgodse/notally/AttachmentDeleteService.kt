package com.omgodse.notally

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.omgodse.notally.miscellaneous.IO
import com.omgodse.notally.room.Attachment
import com.omgodse.notally.room.Audio
import com.omgodse.notally.room.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AttachmentDeleteService : Service() {

    private val scope = MainScope()
    private val channel = Channel<ArrayList<Attachment>>()

    override fun onCreate() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = Notification.Builder(application)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "com.omgodse.fileUpdates"
            val channel = NotificationChannel(channelId, "Backups and Images", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
            builder.setChannelId(channelId)
        }

        builder.setContentTitle(getString(R.string.deleting_images))
        builder.setSmallIcon(R.drawable.notification_delete)
        builder.setProgress(0, 0, true)
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
        startForeground(1, builder.build())

        scope.launch {
            withContext(Dispatchers.IO) {
                val imageRoot = IO.getExternalImagesDirectory(application)
                val audioRoot = IO.getExternalAudioDirectory(application)
                do {
                    val attachments = channel.receive()
                    attachments.forEachIndexed { index, attachment ->
                        val file = when (attachment) {
                            is Audio -> if (audioRoot != null) File(audioRoot, attachment.name) else null
                            is Image -> if (imageRoot != null) File(imageRoot, attachment.name) else null
                        }
                        if (file != null && file.exists()) {
                            file.delete()
                        }
                        builder.setContentText(getString(R.string.count, index + 1, attachments.size))
                        builder.setProgress(attachments.size, index + 1, false)
                        manager.notify(1, builder.build())
                    }
                } while (!channel.isEmpty)
                channel.close()
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            val list = requireNotNull(intent).getParcelableArrayListExtra<Attachment>(EXTRA_ATTACHMENTS)
            withContext(Dispatchers.IO) {
                channel.send(requireNotNull(list))
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_ATTACHMENTS = "com.omgodse.notally.EXTRA_ATTACHMENTS"

        fun start(app: Application, list: ArrayList<out Attachment>) {
            val intent = Intent(app, AttachmentDeleteService::class.java)
            intent.putParcelableArrayListExtra(EXTRA_ATTACHMENTS, list)
            ContextCompat.startForegroundService(app, intent)
        }
    }
}