package com.omgodse.notally.viewmodels

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import androidx.core.text.getSpans
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omgodse.notally.Cache
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Color
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Label
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.room.NotallyDatabase
import com.omgodse.notally.room.SpanRepresentation
import com.omgodse.notally.room.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotallyModel(app: Application) : AndroidViewModel(app) {

    private val database = NotallyDatabase.getDatabase(app)
    private val labelDao = database.labelDao
    private val baseNoteDao = database.baseNoteDao

    val textSize = Preferences.getInstance(app).textSize.value

    var isNewNote = true
    var isFirstInstance = true

    var type = Type.NOTE

    var id = 0L
    var folder = Folder.NOTES
    var color = Color.DEFAULT

    var title = String()
    var pinned = false
    var timestamp = System.currentTimeMillis()

    val labels = ArrayList<String>()

    var body: Editable = SpannableStringBuilder()

    val items = ArrayList<ListItem>()

    fun setLabels(list: List<String>) {
        labels.clear()
        labels.addAll(list)
    }


    suspend fun setState(id: Long) {
        val cachedNote = Cache.list.find { baseNote -> baseNote.id == id }
        val baseNote = cachedNote ?: withContext(Dispatchers.IO) { baseNoteDao.get(id) }

        this.id = id
        folder = baseNote.folder
        color = baseNote.color

        title = baseNote.title
        pinned = baseNote.pinned
        timestamp = baseNote.timestamp

        setLabels(baseNote.labels)

        body = baseNote.body.applySpans(baseNote.spans)

        items.clear()
        items.addAll(baseNote.items)
    }

    suspend fun createBaseNote() = withContext(Dispatchers.IO) {
        id = baseNoteDao.insert(getBaseNote())
    }


    suspend fun delete() = withContext(Dispatchers.IO) {
        baseNoteDao.delete(id)
    }

    suspend fun saveNote() = withContext(Dispatchers.IO) {
        baseNoteDao.insert(getBaseNote())
    }


    fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            val success = try {
                withContext(Dispatchers.IO) { labelDao.insert(label) }
                true
            } catch (exception: SQLiteConstraintException) {
                false
            }
            onComplete(success)
        }
    }


    suspend fun getAllLabels() = withContext(Dispatchers.IO) { labelDao.getArrayOfAll() }


    private fun getBaseNote(): BaseNote {
        val spans = getFilteredSpans(body)
        val body = this.body.trimEnd().toString()
        val items = this.items.filter { item -> item.body.isNotEmpty() }
        return BaseNote(id, type, folder, color, title, pinned, timestamp, labels, body, spans, items)
    }

    private fun getFilteredSpans(spanned: Spanned): ArrayList<SpanRepresentation> {
        val representations = LinkedHashSet<SpanRepresentation>()
        spanned.getSpans<CharacterStyle>().forEach { span ->
            val end = spanned.getSpanEnd(span)
            val start = spanned.getSpanStart(span)
            val representation = SpanRepresentation(false, false, false, false, false, start, end)

            when (span) {
                is StyleSpan -> {
                    representation.bold = span.style == Typeface.BOLD
                    representation.italic = span.style == Typeface.ITALIC
                }

                is URLSpan -> representation.link = true
                is TypefaceSpan -> representation.monospace = span.family == "monospace"
                is StrikethroughSpan -> representation.strikethrough = true
            }

            if (representation.isNotUseless()) {
                representations.add(representation)
            }
        }
        return getFilteredRepresentations(ArrayList(representations))
    }

    private fun getFilteredRepresentations(representations: ArrayList<SpanRepresentation>): ArrayList<SpanRepresentation> {
        representations.forEachIndexed { index, representation ->
            val match = representations.find { spanRepresentation ->
                spanRepresentation.isEqualInSize(representation)
            }
            if (match != null && representations.indexOf(match) != index) {
                if (match.bold) {
                    representation.bold = true
                }
                if (match.link) {
                    representation.link = true
                }
                if (match.italic) {
                    representation.italic = true
                }
                if (match.monospace) {
                    representation.monospace = true
                }
                if (match.strikethrough) {
                    representation.strikethrough = true
                }
                val copy = ArrayList(representations)
                copy[index] = representation
                copy.remove(match)
                return getFilteredRepresentations(copy)
            }
        }
        return representations
    }
}