package com.omgodse.notally.miscellaneous

import android.app.Application
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files

object IO {

    fun getImagesDirectory(app: Application): File {
        val images = File(app.filesDir, "Notally Images")
        images.mkdir()
        return images
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