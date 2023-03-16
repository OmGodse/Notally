package com.omgodse.notally

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.omgodse.notally.miscellaneous.Export
import com.omgodse.notally.preferences.AutoBackup
import com.omgodse.notally.preferences.Preferences
import java.text.SimpleDateFormat
import java.util.*

class AutoBackupWorker(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val app = context.applicationContext as Application
        val preferences = Preferences.getInstance(app)
        val backupPath = preferences.autoBackup.value

        if (backupPath != AutoBackup.emptyPath) {
            val uri = Uri.parse(backupPath)
            val folder = requireNotNull(DocumentFile.fromTreeUri(app, uri))

            if (folder.exists()) {
                val formatter = SimpleDateFormat("yyyyMMdd HHmmss '(Notally Backup)'", Locale.ENGLISH)
                val name = formatter.format(System.currentTimeMillis())
                val file = requireNotNull(folder.createFile("application/zip", name))
                val outputStream = requireNotNull(app.contentResolver.openOutputStream(file.uri))
                Export.backupToZip(app, outputStream)
            }
        }

        return Result.success()
    }
}