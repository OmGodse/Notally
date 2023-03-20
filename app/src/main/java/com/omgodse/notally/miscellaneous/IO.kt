package com.omgodse.notally.miscellaneous

import android.app.Application
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object IO {

    fun getImagesDirectory(app: Application): File {
        val media = app.externalMediaDirs[0]
        val images = File(media, "Notally Images")
        images.mkdir()
        return images
    }

    fun copyStreamToFile(input: InputStream, destination: File) {
        val output = FileOutputStream(destination)
        input.copyTo(output)
        input.close()
        output.close()
    }
}