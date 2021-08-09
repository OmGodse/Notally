package com.omgodse.notally.xml

import com.omgodse.notally.room.BaseNote
import java.io.InputStream
import java.io.OutputStream

class Backup(
    val baseNotes: List<BaseNote>,
    val deletedNotes: List<BaseNote>,
    val archivedNotes: List<BaseNote>,
    val labels: HashSet<String>
) {

    fun writeToStream(stream: OutputStream) {
        XMLUtils.writeBackupToStream(this, stream)
    }

    companion object {
        fun readFromStream(inputStream: InputStream): Backup {
            return XMLUtils.readBackupFromStream(inputStream)
        }
    }
}