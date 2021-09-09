package com.omgodse.notally.room

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.omgodse.notally.R
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(indices = [Index(value = ["id", "folder", "pinned", "timestamp", "labels"])])
data class BaseNote(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val type: Type,
    val folder: Folder,
    val title: String,
    val pinned: Boolean,
    val timestamp: Long,
    val labels: HashSet<String>,
    val body: String,
    val spans: List<SpanRepresentation>,
    val items: List<ListItem>,
) : Parcelable {

    fun isEmpty(): Boolean {
        return when (type) {
            Type.NOTE -> title.isBlank() && body.isBlank()
            Type.LIST -> title.isBlank() && items.isEmpty()
        }
    }

    fun getEmptyMessage(): Int {
        return when (type) {
            Type.NOTE -> R.string.empty_note
            Type.LIST -> R.string.empty_list
        }
    }

    fun matchesKeyword(keyword: String): Boolean {
        if (title.contains(keyword, true)) {
            return true
        }
        if (body.contains(keyword, true)) {
            return true
        }
        for (label in labels) {
            if (label.contains(keyword, true)) {
                return true
            }
        }
        for (item in items) {
            if (item.body.contains(keyword, true)) {
                return true
            }
        }
        return false
    }

    companion object {

        fun createNote(id: Long, folder: Folder, title: String, pinned: Boolean, timestamp: Long, labels: HashSet<String>, body: String, spans: List<SpanRepresentation>): BaseNote {
            return BaseNote(id, Type.NOTE, folder, title, pinned, timestamp, labels, body, spans, emptyList())
        }

        fun createList(id: Long, folder: Folder, title: String, pinned: Boolean, timestamp: Long, labels: HashSet<String>, items: List<ListItem>): BaseNote {
            return BaseNote(id, Type.LIST, folder, title, pinned, timestamp, labels, String(), emptyList(), items)
        }
    }
}