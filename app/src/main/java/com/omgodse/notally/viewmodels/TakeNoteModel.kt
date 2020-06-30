package com.omgodse.notally.viewmodels

import android.app.Application
import android.text.Editable
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.miscellaneous.getFilteredSpans
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.Note

class TakeNoteModel(app: Application) : NotallyModel(app) {

    var body = Editable.Factory.getInstance().newEditable(String())

    override fun saveNote() {
        file?.let {
            val note = Note(title, it.path, labels.value ?: HashSet(), timestamp.toString(), body.toString().trimEnd(), body.getFilteredSpans())
            note.writeToFile()
        }
    }

    override fun setStateFromFile() {
        file?.let { file ->
            if (file.exists()) {
                val baseNote = BaseNote.readFromFile(file) as Note
                title = baseNote.title
                timestamp = baseNote.timestamp.toLong()
                body = baseNote.body.applySpans(baseNote.spans)
                labels.value = baseNote.labels
            }
        }
    }
}