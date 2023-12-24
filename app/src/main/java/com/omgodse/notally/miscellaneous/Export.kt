package com.omgodse.notally.miscellaneous

import android.app.Application
import com.omgodse.notally.room.Image
import com.omgodse.notally.room.NotallyDatabase
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Export {

    fun backupDatabase(app: Application, zipStream: ZipOutputStream) {
        val entry = ZipEntry(NotallyDatabase.DatabaseName)
        zipStream.putNextEntry(entry)

        val file = app.getDatabasePath(NotallyDatabase.DatabaseName)
        val inputStream = FileInputStream(file)
        inputStream.copyTo(zipStream)
        inputStream.close()

        zipStream.closeEntry()
    }

    fun backupImage(zipStream: ZipOutputStream, mediaRoot: File?, image: Image) {
        val file = if (mediaRoot != null) File(mediaRoot, image.name) else null
        if (file != null && file.exists()) {
            val entry = ZipEntry("Images/${image.name}")
            zipStream.putNextEntry(entry)

            val inputStream = FileInputStream(file)
            inputStream.copyTo(zipStream)
            inputStream.close()

            zipStream.closeEntry()
        }
    }
}