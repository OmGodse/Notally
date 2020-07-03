package com.omgodse.notally.xml

import com.omgodse.notally.R
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import java.io.File
import kotlin.collections.List

sealed class BaseNote(open val title: String,
                      open val filePath: String,
                      open val labels: HashSet<String>,
                      open val timestamp: String,
                      open val timeModified: String
) {

    abstract fun isEmpty(): Boolean

    abstract fun getEmptyMessage(): Int

    abstract fun matchesKeyword(keyword: String): Boolean


    abstract fun writeToFile()

    abstract fun getAssociatedActivity() : Class<*>

    companion object  {
        fun readFromFile(file: File): BaseNote {
            return XMLUtils.readBaseNoteFromFile(file)
        }
    }
}

data class Note(override val title: String,
                override val filePath: String,
                override val labels: HashSet<String>,
                override val timestamp: String,
                override val timeModified: String,
                val body: String,
                val spans: ArrayList<SpanRepresentation>) : BaseNote(title, filePath, labels, timestamp, timeModified) {

    override fun isEmpty(): Boolean {
        return title.isBlank() && body.isBlank()
    }

    override fun getEmptyMessage() = R.string.discarded_empty_note

    override fun matchesKeyword(keyword: String): Boolean {
        if (title.contains(keyword, true)) {
            return true
        }
        if (body.contains(keyword, true)) {
            return true
        }
        labels.forEach { label ->
            if (label.contains(keyword, true)) {
                return true
            }
        }
        return false
    }


    override fun writeToFile() {
        XMLUtils.writeNoteToFile(this)
    }

    override fun getAssociatedActivity() = TakeNote::class.java
}

data class List(override val title: String,
                override val filePath: String,
                override val labels: HashSet<String>,
                override val timestamp: String,
                override val timeModified: String,
                val items: List<ListItem>) : BaseNote(title, filePath, labels, timestamp, timeModified) {

    override fun isEmpty(): Boolean {
        return title.isBlank() && items.isEmpty()
    }

    override fun getEmptyMessage() = R.string.discarded_empty_list

    override fun matchesKeyword(keyword: String): Boolean {
        if (title.contains(keyword, true)) {
            return true
        }
        items.forEach { item ->
            if (item.body.contains(keyword, true)) {
                return true
            }
        }
        labels.forEach { label ->
            if (label.contains(keyword, true)) {
                return true
            }
        }
        return false
    }


    override fun writeToFile() {
        XMLUtils.writeListToFile(this)
    }

    override fun getAssociatedActivity() = MakeList::class.java
}