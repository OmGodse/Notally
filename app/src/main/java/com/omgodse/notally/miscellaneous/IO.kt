package com.omgodse.notally.miscellaneous

import android.app.Application
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files

object IO {

    private fun createDirectory(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.createDirectory(file.toPath())
        } else file.mkdir()
    }

    fun getExternalImagesDirectory(app: Application): File? {
        var file: File? = null

        try {
            val mediaDir = app.externalMediaDirs.firstOrNull()
            if (mediaDir != null) {
                file = File(mediaDir, "Images")
                if (file.exists()) {
                    if (!file.isDirectory) {
                        file.delete()
                        createDirectory(file)
                    }
                } else createDirectory(file)
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }

        return file
    }


    fun copyStreamToFile(input: InputStream, destination: File) {
        val output = FileOutputStream(destination)
        input.copyTo(output)
        input.close()
        output.close()
    }

    fun renameFile(file: File, name: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val source = file.toPath()
            val destination = source.resolveSibling(name)
            Files.move(source, destination)
            true // If move failed, an exception would have been thrown
        } else {
            val destination = file.resolveSibling(name)
            file.renameTo(destination)
        }
    }
}