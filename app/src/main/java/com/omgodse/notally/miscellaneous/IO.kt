package com.omgodse.notally.miscellaneous

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object IO {

    fun copyStreamToFile(input: InputStream, destination: File) {
        val output = FileOutputStream(destination)
        input.copyTo(output)
        input.close()
        output.close()
    }
}