package com.omgodse.notally.xml

import java.io.File
import java.io.InputStream

data class Backup(val baseNotes: ArrayList<BaseNote>,
                  val deletedBaseNotes: ArrayList<BaseNote>,
                  val archivedBaseNotes: ArrayList<BaseNote>,
                  val labels: HashSet<String>) {

    fun writeToFile(file: File) {
        XMLUtils.writeBackupToFile(this, file)
    }

    companion object {
        fun readFromStream(inputStream: InputStream): Backup {
            return XMLUtils.readBackupFromFile(inputStream)
        }
    }
}