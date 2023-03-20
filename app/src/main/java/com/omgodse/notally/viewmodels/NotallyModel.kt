package com.omgodse.notally.viewmodels

import android.app.Application
import android.content.ClipData
import android.database.sqlite.SQLiteConstraintException
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.text.Editable
import android.text.Spanned
import android.text.style.*
import android.widget.Toast
import androidx.core.text.getSpans
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.IO
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.preferences.BetterLiveData
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class NotallyModel(private val app: Application) : AndroidViewModel(app) {

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

    var body = Editable.Factory.getInstance().newEditable(String())

    val items = ArrayList<ListItem>()

    val images = BetterLiveData<List<Image>>(emptyList())
    val labels = BetterLiveData<List<String>>(emptyList())

    val imageDir = IO.getImagesDirectory(app)

    fun addImageFromUri(uri: Uri) {
        val temp = File(imageDir, "TEMP")

        viewModelScope.launch {
            val input = app.contentResolver.openInputStream(uri)
            requireNotNull(input) { "inputStream opened by contentResolver is null" }

            val mimeType = withContext(Dispatchers.IO) {
                IO.copyStreamToFile(input, temp)

                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(temp.path, options)
                options.outMimeType
            }

            if (mimeType != null) {
                val extension = getExtensionForMimeType(mimeType)
                if (extension != null) {
                    val name = "${UUID.randomUUID()}.$extension"
                    val target = File(imageDir, name)
                    temp.renameTo(target)
                    val image = Image(name, mimeType)
                    val list = ArrayList(images.value)
                    list.add(image)
                    images.value = list
                } else {
                    temp.delete()
                    Toast.makeText(app, R.string.image_format_not_supported, Toast.LENGTH_LONG).show()
                }
            } else {
                temp.delete()
                Toast.makeText(app, R.string.invalid_image, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun addImagesFromClipData(data: ClipData) {}

    private fun getExtensionForMimeType(type: String): String? {
        return when (type) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            else -> null
        }
    }


    fun setStateFromBaseNote(baseNote: BaseNote) {
        id = baseNote.id
        folder = baseNote.folder
        color = baseNote.color

        title = baseNote.title
        pinned = baseNote.pinned
        timestamp = baseNote.timestamp

        labels.value = baseNote.labels

        body = baseNote.body.applySpans(baseNote.spans)

        items.clear()
        items.addAll(baseNote.items)

        images.value = baseNote.images
    }


    fun saveNote(onComplete: () -> Unit) {
        viewModelScope.launch {
            id = withContext(Dispatchers.IO) { baseNoteDao.insert(getBaseNote()) }
            onComplete()
        }
    }

    fun deleteForever(onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                baseNoteDao.delete(id)
                for (image in images.value) {
                    val file = File(imageDir, image.name)
                    file.delete()
                }
            }
            onComplete()
        }
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
        return BaseNote(
            id, type, folder, color, title, pinned, timestamp, labels.value, body, spans, items, images.value
        )
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