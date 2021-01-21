package com.omgodse.notally.viewmodels

import android.app.Application
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.room.BaseNote

class MakeListModel(app: Application) : NotallyModel(app) {

    val items = ArrayList<ListItem>()

    override fun getBaseNote(): BaseNote {
        val filteredItems = items.filter { (body) -> body.isNotBlank() }
        return BaseNote.createList(id, folder, title, pinned, timestamp, labels, filteredItems)
    }

    override fun setStateFromBaseNote(baseNote: BaseNote) {
        super.setStateFromBaseNote(baseNote)
        items.clear()
        items.addAll(baseNote.items)
    }
}