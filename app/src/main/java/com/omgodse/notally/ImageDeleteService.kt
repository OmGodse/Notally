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
import com.omgodse.notally.room.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ImageDeleteService : Service() {

    private val scope = MainScope()
    private val channel = Channel<ArrayList<Image>>()

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
                val mediaRoot = IO.getExternalImagesDirectory(application)
                do {
                    val images = channel.receive()
                    images.forEachIndexed { index, image ->
                        val file = if (mediaRoot != null) File(mediaRoot, image.name) else null
                        if (file != null && file.exists()) {
                            file.delete()
                        }
                        builder.setContentText(getString(R.string.count, index + 1, images.size))
                        builder.setProgress(images.size, index + 1, false)
                        manager.notify(1, builder.build())
                    }
                } while (!channel.isEmpty)
                channel.close()
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val list = requireNotNull(intent.getParcelableArrayListExtra<Image>(EXTRA_IMAGES))
            scope.launch {
                withContext(Dispatchers.IO) {
                    channel.send(list)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_IMAGES = "com.omgodse.notally.EXTRA_IMAGES"

        fun start(app: Application, list: ArrayList<Image>) {
            val intent = Intent(app, ImageDeleteService::class.java)
            intent.putParcelableArrayListExtra(EXTRA_IMAGES, list)
            ContextCompat.startForegroundService(app, intent)
        }
    }
}