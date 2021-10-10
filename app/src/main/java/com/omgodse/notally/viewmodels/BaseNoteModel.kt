package com.omgodse.notally.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import android.print.PostPDFGenerator
import android.text.Html
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.text.toHtml
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.miscellaneous.getBody
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.room.*
import com.omgodse.notally.room.livedata.Content
import com.omgodse.notally.room.livedata.SearchResult
import com.omgodse.notally.xml.Backup
import com.omgodse.notally.xml.XMLTags
import com.omgodse.notally.xml.XMLUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BaseNoteModel(private val app: Application) : AndroidViewModel(app) {

    private val database = NotallyDatabase.getDatabase(app)
    private val labelDao = database.labelDao
    private val commonDao = database.commonDao
    private val baseNoteDao = database.baseNoteDao

    private val labelCache = HashMap<String, Content<BaseNote>>()
    private val formatter = getDateFormatter(app.getLocale())

    var currentFile: File? = null

    val labels = Content(labelDao.getAllLabels())
    val baseNotes = Content(baseNoteDao.getAllBaseNotes(Folder.NOTES))
    val deletedNotes = Content(baseNoteDao.getAllBaseNotes(Folder.DELETED))
    val archivedNotes = Content(baseNoteDao.getAllBaseNotes(Folder.ARCHIVED))

    var keyword = String()
        set(value) {
            if (field != value) {
                field = value
                searchResults.fetch(value)
            }
        }

    val searchResults = SearchResult(viewModelScope, baseNoteDao)

    init {
        viewModelScope.launch {
            val previousNotes = getPreviousNotes()
            val previousLabels = getPreviousLabels()
            val delete: (file: File) -> Unit = { file: File -> file.delete() }
            if (previousNotes.isNotEmpty() || previousLabels.isNotEmpty()) {
                database.withTransaction {
                    labelDao.insertLabels(previousLabels)
                    baseNoteDao.insertBaseNotes(previousNotes)
                    getNotePath().listFiles()?.forEach(delete)
                    getDeletedPath().listFiles()?.forEach(delete)
                    getArchivedPath().listFiles()?.forEach(delete)
                    getLabelsPreferences().edit(true) { clear() }
                }
            }
        }
    }

    fun getNotesByLabel(label: String): Content<BaseNote> {
        if (labelCache[label] == null) {
            labelCache[label] = Content(commonDao.getBaseNotesByLabel(label))
        }
        return requireNotNull(labelCache[label])
    }


    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val labels = labelDao.getAllLabelsAsList().toHashSet()
                val baseNotes = baseNoteDao.getAllBaseNotesAsList(Folder.NOTES)
                val deletedNotes = baseNoteDao.getAllBaseNotesAsList(Folder.DELETED)
                val archivedNotes = baseNoteDao.getAllBaseNotesAsList(Folder.ARCHIVED)

                val backup = Backup(baseNotes, deletedNotes, archivedNotes, labels)

                (app.contentResolver.openOutputStream(uri) as? FileOutputStream)?.use { stream ->
                    stream.channel.truncate(0)
                    XMLUtils.writeBackupToStream(backup, stream)
                }
            }
            Toast.makeText(app, R.string.saved_to_device, Toast.LENGTH_LONG).show()
        }
    }

    fun importBackup(uri: Uri) {
        executeAsync {
            app.contentResolver.openInputStream(uri)?.use { stream ->
                val backup = XMLUtils.readBackupFromStream(stream)

                val list = ArrayList(backup.baseNotes)
                list.addAll(backup.deletedNotes)
                list.addAll(backup.archivedNotes)

                val labels = backup.labels.map { label -> Label(label) }

                baseNoteDao.insertBaseNotes(list)
                labelDao.insertLabels(labels)
            }
        }
    }

    fun writeCurrentFileToUri(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                (app.contentResolver.openOutputStream(uri) as? FileOutputStream)?.use { stream ->
                    stream.channel.truncate(0)
                    stream.write(requireNotNull(currentFile).readBytes())
                }
            }
            Toast.makeText(app, R.string.saved_to_device, Toast.LENGTH_LONG).show()
        }
    }


    suspend fun getXMLFile(baseNote: BaseNote) = withContext(Dispatchers.IO) {
        val fileName = getFileName(baseNote)
        val file = File(getExportedPath(), "$fileName.xml")
        val outputStream = FileOutputStream(file)
        XMLUtils.writeBaseNoteToStream(baseNote, outputStream)
        outputStream.close()
        file
    }

    suspend fun getJSONFile(baseNote: BaseNote) = withContext(Dispatchers.IO) {
        val fileName = getFileName(baseNote)
        val file = File(getExportedPath(), "$fileName.json")
        val json = getJSON(baseNote)
        file.writeText(json)
        file
    }

    suspend fun getTXTFile(baseNote: BaseNote, showDateCreated: Boolean) = withContext(Dispatchers.IO) {
        val fileName = getFileName(baseNote)
        val file = File(getExportedPath(), "$fileName.txt")
        val content = getTXT(baseNote, showDateCreated)
        file.writeText(content)
        file
    }

    suspend fun getHTMLFile(baseNote: BaseNote, showDateCreated: Boolean) = withContext(Dispatchers.IO) {
        val fileName = getFileName(baseNote)
        val file = File(getExportedPath(), "$fileName.html")
        val content = getHTML(baseNote, showDateCreated)
        file.writeText(content)
        file
    }

    fun getPDFFile(baseNote: BaseNote, showDateCreated: Boolean, onResult: PostPDFGenerator.OnResult) {
        val fileName = getFileName(baseNote)
        val pdfFile = File(getExportedPath(), "$fileName.pdf")

        val html = getHTML(baseNote, showDateCreated)

        PostPDFGenerator.create(pdfFile, html, app, onResult)
    }


    fun pinBaseNote(id: Long) = executeAsync { baseNoteDao.updateBaseNotePinned(id, true) }

    fun unpinBaseNote(id: Long) = executeAsync { baseNoteDao.updateBaseNotePinned(id, false) }


    fun restoreBaseNote(id: Long) = executeAsync { baseNoteDao.moveBaseNote(id, Folder.NOTES) }

    fun moveBaseNoteToDeleted(id: Long) = executeAsync { baseNoteDao.moveBaseNote(id, Folder.DELETED) }

    fun moveBaseNoteToArchive(id: Long) = executeAsync { baseNoteDao.moveBaseNote(id, Folder.ARCHIVED) }

    fun deleteAllBaseNotes() = executeAsync { baseNoteDao.deleteAllBaseNotes(Folder.DELETED) }

    fun deleteBaseNoteForever(baseNote: BaseNote) = executeAsync { baseNoteDao.deleteBaseNote(baseNote) }

    fun updateBaseNoteLabels(labels: HashSet<String>, id: Long) =
        executeAsync { baseNoteDao.updateBaseNoteLabels(id, labels) }


    suspend fun getAllLabelsAsList() = withContext(Dispatchers.IO) { labelDao.getAllLabelsAsList() }

    fun deleteLabel(label: Label) = executeAsync { commonDao.deleteLabel(label) }

    fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) =
        executeAsyncWithCallback({ labelDao.insertLabel(label) }, onComplete)

    fun updateLabel(oldValue: String, newValue: String, onComplete: (success: Boolean) -> Unit) =
        executeAsyncWithCallback({ commonDao.updateLabel(oldValue, newValue) }, onComplete)


    private fun getExportedPath(): File {
        val filePath = File(app.cacheDir, "exported")
        if (!filePath.exists()) {
            filePath.mkdir()
        }
        filePath.listFiles()?.forEach { file -> file.delete() }
        return filePath
    }

    private fun getFileName(baseNote: BaseNote): String {
        val title = baseNote.title
        val body = when (baseNote.type) {
            Type.NOTE -> baseNote.body
            Type.LIST -> baseNote.items.getBody()
        }
        val fileName = if (title.isEmpty()) {
            val words = body.split(" ").take(2)
            buildString {
                words.forEach {
                    append(it)
                    append(" ")
                }
            }
        } else title
        return fileName.take(64).replace("/", "")
    }


    private fun getJSON(baseNote: BaseNote): String {
        val labels = JSONArray(baseNote.labels)

        val jsonObject = JSONObject()
            .put("type", baseNote.type.name)
            .put(XMLTags.Title, baseNote.title)
            .put(XMLTags.Pinned, baseNote.pinned)
            .put(XMLTags.DateCreated, baseNote.timestamp)
            .put("labels", labels)

        when (baseNote.type) {
            Type.NOTE -> {
                val spans = JSONArray(baseNote.spans.map { representation -> representation.toJSONObject() })
                jsonObject.put(XMLTags.Body, baseNote.body)
                jsonObject.put("spans", spans)
            }
            Type.LIST -> {
                val items = JSONArray(baseNote.items.map { item -> item.toJSONObject() })
                jsonObject.put("items", items)
            }
        }

        return jsonObject.toString(2)
    }

    private fun getTXT(baseNote: BaseNote, showDateCreated: Boolean) = buildString {
        val date = formatter.format(baseNote.timestamp)

        val body = when (baseNote.type) {
            Type.NOTE -> baseNote.body
            Type.LIST -> baseNote.items.getBody()
        }

        if (baseNote.title.isNotEmpty()) {
            append("${baseNote.title}\n\n")
        }
        if (showDateCreated) {
            append("$date\n\n")
        }
        append(body)
    }

    private fun getHTML(baseNote: BaseNote, showDateCreated: Boolean) = buildString {
        val date = formatter.format(baseNote.timestamp)
        val title = Html.escapeHtml(baseNote.title)

        append("<!DOCTYPE html>")
        append("<html><head>")
        append("<meta charset=\"UTF-8\"><title>$title</title>")
        append("</head><body>")
        append("<h2>$title</h2>")

        if (showDateCreated) {
            append("<p>$date</p>")
        }

        when (baseNote.type) {
            Type.NOTE -> {
                val body = baseNote.body.applySpans(baseNote.spans).toHtml()
                append(body)
            }
            Type.LIST -> {
                append("<ol>")
                baseNote.items.forEach { (body) ->
                    append("<li>${Html.escapeHtml(body)}</li>")
                }
                append("</ol>")
            }
        }
        append("</body></html>")
    }


    private fun getPreviousNotes(): List<BaseNote> {
        val previousNotes = ArrayList<BaseNote>()
        getNotePath().listFiles()?.mapTo(previousNotes, { file -> XMLUtils.readBaseNoteFromFile(file, Folder.NOTES) })
        getDeletedPath().listFiles()?.mapTo(previousNotes, { file -> XMLUtils.readBaseNoteFromFile(file, Folder.DELETED) })
        getArchivedPath().listFiles()?.mapTo(previousNotes, { file -> XMLUtils.readBaseNoteFromFile(file, Folder.ARCHIVED) })
        return previousNotes
    }

    private fun getPreviousLabels(): List<Label> {
        val labels = getLabelsPreferences().getStringSet("labelItems", emptySet()) ?: emptySet()
        return labels.map { value -> Label(value) }
    }


    private fun getNotePath() = getFolder("notes")

    private fun getDeletedPath() = getFolder("deleted")

    private fun getArchivedPath() = getFolder("archived")

    private fun getFolder(name: String): File {
        val folder = File(app.filesDir, name)
        if (!folder.exists()) {
            folder.mkdir()
        }
        return folder
    }

    private fun getLabelsPreferences() = app.getSharedPreferences("labelsPreferences", Context.MODE_PRIVATE)


    private fun executeAsync(function: suspend () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { function() }
        }
    }

    companion object {

        fun getDateFormatter(locale: Locale): SimpleDateFormat {
            val pattern = if (locale.language == Locale.JAPANESE.language || locale.language == Locale.CHINESE.language) {
                "yyyy年 MMM d日 (EEE)"
            } else "EEE d MMM yyyy"
            return SimpleDateFormat(pattern, locale)
        }
    }
}