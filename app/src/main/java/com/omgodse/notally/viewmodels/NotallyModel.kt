package com.omgodse.notally.viewmodels

import android.app.Application
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class NotallyModel(app: Application, private val type: Type) : AndroidViewModel(app) {

    private val database = NotallyDatabase.getDatabase(app)
    private val labelDao = database.labelDao
    private val baseNoteDao = database.baseNoteDao

    var id = 0L

    var isNewNote = true
    var isFirstInstance = true

    var folder = Folder.NOTES
    var color = Color.DEFAULT

    var title = String()
    var pinned = false

    var timestamp = Date().time
    var labels = HashSet<String>()

    var body = Editable.Factory.getInstance().newEditable(String())
    val items = ArrayList<ListItem>()

    fun setStateFromBaseNote(baseNote: BaseNote) {
        id = baseNote.id
        folder = baseNote.folder
        color = baseNote.color

        title = baseNote.title
        pinned = baseNote.pinned
        timestamp = baseNote.timestamp
        labels = baseNote.labels

        body = baseNote.body.applySpans(baseNote.spans)

        items.clear()
        items.addAll(baseNote.items)
    }


    fun saveNote(onComplete: () -> Unit) {
        viewModelScope.launch {
            id = withContext(Dispatchers.IO) { baseNoteDao.insert(getBaseNote()) }
            onComplete()
        }
    }

    fun deleteBaseNoteForever(onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { baseNoteDao.delete(getBaseNote()) }
            onComplete()
        }
    }

    fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) =
        executeAsyncWithCallback({ labelDao.insert(label) }, onComplete)


    fun restoreBaseNote() {
        folder = Folder.NOTES
    }

    fun moveBaseNoteToArchive() {
        folder = Folder.ARCHIVED
    }

    fun moveBaseNoteToDeleted() {
        folder = Folder.DELETED
    }


    suspend fun getAllLabelsAsList() = withContext(Dispatchers.IO) { labelDao.getListOfAll() }


    private fun getBaseNote(): BaseNote {
        val spans = getFilteredSpans(body)
        val trimmedBody = body.toString().trimEnd()
        val filteredItems = items.filter { (body) -> body.isNotBlank() }
        return BaseNote(id, type, folder, color, title, pinned, timestamp, labels, trimmedBody, spans, filteredItems)
    }

    private fun getFilteredSpans(spannable: Spannable): ArrayList<SpanRepresentation> {
        val representations = LinkedHashSet<SpanRepresentation>()
        val spans = spannable.getSpans(0, spannable.length, Any::class.java)
        spans.forEach { span ->
            val end = spannable.getSpanEnd(span)
            val start = spannable.getSpanStart(span)
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


    class Factory(private val app: Application, private val type: Type) : ViewModelProvider.AndroidViewModelFactory(app) {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return NotallyModel(app, type) as T
        }
    }
}