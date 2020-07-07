package com.omgodse.notally.viewmodels

import android.app.Application
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.List
import com.omgodse.notally.xml.ListItem

class MakeListModel(app: Application) : NotallyModel(app) {

    val items = ArrayList<ListItem>()

    override fun saveNote() {
        val listItems = items.filter { item -> item.body.isNotBlank() }
        file?.let {
            val list = List(title, it.path, labels.value ?: HashSet(), timestamp.toString(), listItems)
            if (it.exists()) {
                val savedList = BaseNote.readFromFile(it)
                if (savedList != list) {
                    list.writeToFile()
                }
            } else list.writeToFile()
        }
    }

    override fun setStateFromFile() {
        file?.let { file ->
            if (file.exists()) {
                val baseNote = BaseNote.readFromFile(file) as List
                title = baseNote.title
                timestamp = baseNote.timestamp.toLong()

                items.clear()
                items.addAll(baseNote.items)

                labels.value = baseNote.labels
            }
        }
    }
}