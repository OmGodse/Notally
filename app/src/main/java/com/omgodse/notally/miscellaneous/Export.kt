package com.omgodse.notally.miscellaneous

import android.app.Application
import com.omgodse.notally.room.NotallyDatabase
import java.io.FileInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Export {

    fun backupToZip(app: Application, outputStream: OutputStream) {
        val database = NotallyDatabase.getDatabase(app)
        database.checkpoint()

        val source = app.getDatabasePath(NotallyDatabase.DatabaseName)

        val zipStream = ZipOutputStream(outputStream)
        val entry = ZipEntry(source.name)
        zipStream.putNextEntry(entry)

        val inputStream = FileInputStream(source)
        inputStream.copyTo(zipStream)
        inputStream.close()

        zipStream.closeEntry()
        zipStream.close()
    }
}