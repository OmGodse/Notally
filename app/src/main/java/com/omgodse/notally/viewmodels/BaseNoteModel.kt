package com.omgodse.notally.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.print.PostPDFGenerator
import android.text.Html
import android.widget.Toast
import androidx.core.text.toHtml
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.omgodse.notally.R
import com.omgodse.notally.legacy.Migrations
import com.omgodse.notally.legacy.XMLUtils
import com.omgodse.notally.miscellaneous.Export
import com.omgodse.notally.miscellaneous.IO
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.preferences.AutoBackup
import com.omgodse.notally.preferences.ListInfo
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.preferences.SeekbarInfo
import com.omgodse.notally.room.*
import com.omgodse.notally.room.livedata.Content
import com.omgodse.notally.room.livedata.SearchResult
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipFile

class BaseNoteModel(private val app: Application) : AndroidViewModel(app) {

    private val database = NotallyDatabase.getDatabase(app)
    private val labelDao = database.labelDao
    private val commonDao = database.commonDao
    private val baseNoteDao = database.baseNoteDao

    private val labelCache = HashMap<String, Content>()
    val formatter = getDateFormatter(app)

    var currentFile: File? = null

    val labels = labelDao.getAll()
    val baseNotes = Content(baseNoteDao.getFrom(Folder.NOTES), ::transform)
    val deletedNotes = Content(baseNoteDao.getFrom(Folder.DELETED), ::transform)
    val archivedNotes = Content(baseNoteDao.getFrom(Folder.ARCHIVED), ::transform)

    var folder = Folder.NOTES
        set(value) {
            if (field != value) {
                field = value
                searchResults.fetch(keyword, folder)
            }
        }
    var keyword = String()
        set(value) {
            if (field != value) {
                field = value
                searchResults.fetch(keyword, folder)
            }
        }

    val searchResults = SearchResult(viewModelScope, baseNoteDao, ::transform)

    private val pinned = Header(app.getString(R.string.pinned))
    private val others = Header(app.getString(R.string.others))

    val preferences = Preferences.getInstance(app)

    private val backupExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Operations.log(app, throwable)
        Toast.makeText(app, R.string.invalid_backup, Toast.LENGTH_LONG).show()
    }

    init {
        viewModelScope.launch {
            val previousNotes = Migrations.getPreviousNotes(app)
            val previousLabels = Migrations.getPreviousLabels(app)
            if (previousNotes.isNotEmpty() || previousLabels.isNotEmpty()) {
                database.withTransaction {
                    labelDao.insert(previousLabels)
                    baseNoteDao.insert(previousNotes)
                    Migrations.clearAllLabels(app)
                    Migrations.clearAllFolders(app)
                }
            }
        }
    }

    fun getNotesByLabel(label: String): Content {
        if (labelCache[label] == null) {
            labelCache[label] = Content(baseNoteDao.getBaseNotesByLabel(label), ::transform)
        }
        return requireNotNull(labelCache[label])
    }


    private fun transform(list: List<BaseNote>): List<Item> {
        if (list.isEmpty()) {
            return list
        } else {
            val firstNote = list[0]
            return if (firstNote.pinned) {
                val newList = ArrayList<Item>(list.size + 2)
                newList.add(pinned)

                val firstUnpinnedNote = list.indexOfFirst { baseNote -> !baseNote.pinned }
                list.forEachIndexed { index, baseNote ->
                    if (index == firstUnpinnedNote) {
                        newList.add(others)
                    }
                    newList.add(baseNote)
                }
                newList
            } else list
        }
    }


    fun savePreference(info: SeekbarInfo, value: Int) = executeAsync { preferences.savePreference(info, value) }

    fun savePreference(info: ListInfo, value: String) = executeAsync { preferences.savePreference(info, value) }


    fun disableAutoBackup() {
        clearPersistedUriPermissions()
        executeAsync { preferences.savePreference(AutoBackup, AutoBackup.emptyPath) }
    }

    fun setAutoBackupPath(uri: Uri) {
        clearPersistedUriPermissions()
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        app.contentResolver.takePersistableUriPermission(uri, flags)
        executeAsync { preferences.savePreference(AutoBackup, uri.toString()) }
    }

    /**
     * Release previously persisted permissions, if any
     * There is a hard limit of 128 before Android 11, 512 after
     * Check -> https://commonsware.com/blog/2020/06/13/count-your-saf-uri-permission-grants.html
     */
    private fun clearPersistedUriPermissions() {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        app.contentResolver.persistedUriPermissions.forEach { permission ->
            app.contentResolver.releasePersistableUriPermission(permission.uri, flags)
        }
    }


    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                (app.contentResolver.openOutputStream(uri) as? FileOutputStream)?.use { stream ->
                    stream.channel.truncate(0)
                    Export.backupToZip(app, stream)
                }
            }
            Toast.makeText(app, R.string.saved_to_device, Toast.LENGTH_LONG).show()
        }
    }

    fun importBackup(uri: Uri) {
        when (app.contentResolver.getType(uri)) {
            "text/xml" -> importXmlBackup(uri)
            "application/zip" -> importZipBackup(uri)
        }
    }


    /**
     * Lots of things can go wrong, instead of trying to account for all of them,
     * display a generic message and allow the user to send a report
     */
    private fun importZipBackup(uri: Uri) {
        viewModelScope.launch(backupExceptionHandler) {
            val stream = app.contentResolver.openInputStream(uri)
            requireNotNull(stream) { "inputStream opened by contentResolver is null" }

            withContext(Dispatchers.IO) {
                val backupDir = getBackupPath()
                val destination = File(backupDir, "TEMP.zip")
                IO.copyStreamToFile(stream, destination)

                val zipFile = ZipFile(destination)
                val databaseEntry = zipFile.getEntry(NotallyDatabase.DatabaseName)

                val databaseFile = File(backupDir, NotallyDatabase.DatabaseName)
                val inputStream = zipFile.getInputStream(databaseEntry)
                IO.copyStreamToFile(inputStream, databaseFile)

                val database = SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY)

                val labelCursor = database.query("Label", null, null, null, null, null, null)
                val baseNoteCursor = database.query("BaseNote", null, null, null, null, null, null)

                val labels = convertCursorToList(labelCursor, ::convertCursorToLabel)
                val baseNotes = convertCursorToList(baseNoteCursor, ::convertCursorToBaseNote)

                commonDao.importBackup(baseNotes, labels)
            }
            Toast.makeText(app, R.string.imported_backup, Toast.LENGTH_LONG).show()
        }
    }

    private fun convertCursorToLabel(cursor: Cursor): Label {
        val value = cursor.getString(cursor.getColumnIndexOrThrow("value"))
        return Label(value)
    }

    private fun convertCursorToBaseNote(cursor: Cursor): BaseNote {
        val typeTmp = cursor.getString(cursor.getColumnIndexOrThrow("type"))
        val folderTmp = cursor.getString(cursor.getColumnIndexOrThrow("folder"))
        val colorTmp = cursor.getString(cursor.getColumnIndexOrThrow("color"))
        val title = cursor.getString(cursor.getColumnIndexOrThrow("title"))
        val pinnedTmp = cursor.getInt(cursor.getColumnIndexOrThrow("pinned"))
        val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
        val labelsTmp = cursor.getString(cursor.getColumnIndexOrThrow("labels"))
        val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))
        val spansTmp = cursor.getString(cursor.getColumnIndexOrThrow("spans"))
        val itemsTmp = cursor.getString(cursor.getColumnIndexOrThrow("items"))

        val pinned = when (pinnedTmp) {
            0 -> false
            1 -> true
            else -> throw IllegalArgumentException("pinned must be 0 or 1")
        }

        val type = Type.valueOf(typeTmp)
        val folder = Folder.valueOf(folderTmp)
        val color = Color.valueOf(colorTmp)

        val labels = Converters.jsonToLabels(labelsTmp)
        val spans = Converters.jsonToSpans(spansTmp)
        val items = Converters.jsonToItems(itemsTmp)

        return BaseNote(0, type, folder, color, title, pinned, timestamp, labels, body, spans, items, emptyList())
    }

    private fun <T> convertCursorToList(cursor: Cursor, convert: (cursor: Cursor) -> T): ArrayList<T> {
        val list = ArrayList<T>(cursor.count)
        while (cursor.moveToNext()) {
            val item = convert(cursor)
            list.add(item)
        }
        cursor.close()
        return list
    }


    private fun importXmlBackup(uri: Uri) {
        viewModelScope.launch(backupExceptionHandler) {
            withContext(Dispatchers.IO) {
                val stream = app.contentResolver.openInputStream(uri)
                requireNotNull(stream) { "inputStream opened by contentResolver is null" }
                val backup = XMLUtils.readBackupFromStream(stream)
                commonDao.importBackup(backup.first, backup.second)
            }
            Toast.makeText(app, R.string.imported_backup, Toast.LENGTH_LONG).show()
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


    suspend fun getJSONFile(baseNote: BaseNote) = withContext(Dispatchers.IO) {
        val file = File(getExportedPath(), "Untitled.json")
        val json = getJSON(baseNote)
        file.writeText(json)
        file
    }

    suspend fun getTXTFile(baseNote: BaseNote) = withContext(Dispatchers.IO) {
        val file = File(getExportedPath(), "Untitled.txt")
        val writer = file.bufferedWriter()

        val date = formatter.format(baseNote.timestamp)

        val body = when (baseNote.type) {
            Type.NOTE -> baseNote.body
            Type.LIST -> Operations.getBody(baseNote.items)
        }

        if (baseNote.title.isNotEmpty()) {
            writer.append("${baseNote.title}\n\n")
        }
        if (preferences.showDateCreated()) {
            writer.append("$date\n\n")
        }
        writer.append(body)
        writer.close()

        file
    }

    suspend fun getHTMLFile(baseNote: BaseNote) = withContext(Dispatchers.IO) {
        val file = File(getExportedPath(), "Untitled.html")
        val html = getHTML(baseNote, preferences.showDateCreated())
        file.writeText(html)
        file
    }

    fun getPDFFile(baseNote: BaseNote, onResult: PostPDFGenerator.OnResult) {
        val file = File(getExportedPath(), "Untitled.pdf")
        val html = getHTML(baseNote, preferences.showDateCreated())
        PostPDFGenerator.create(file, html, app, onResult)
    }


    fun colorBaseNote(id: Long, color: Color) = executeAsync { baseNoteDao.updateColor(id, color) }


    fun pinBaseNote(id: Long) = executeAsync { baseNoteDao.updatePinned(id, true) }

    fun unpinBaseNote(id: Long) = executeAsync { baseNoteDao.updatePinned(id, false) }


    fun deleteAllBaseNotes() = executeAsync { baseNoteDao.deleteFrom(Folder.DELETED) }

    fun restoreBaseNote(id: Long) = executeAsync { baseNoteDao.move(id, Folder.NOTES) }

    fun moveBaseNoteToDeleted(id: Long) = executeAsync { baseNoteDao.move(id, Folder.DELETED) }

    fun moveBaseNoteToArchive(id: Long) = executeAsync { baseNoteDao.move(id, Folder.ARCHIVED) }

    fun deleteBaseNoteForever(id: Long) = executeAsync { baseNoteDao.delete(id) }

    fun updateBaseNoteLabels(labels: List<String>, id: Long) = executeAsync { baseNoteDao.updateLabels(id, labels) }


    suspend fun getAllLabels() = withContext(Dispatchers.IO) { labelDao.getArrayOfAll() }

    fun deleteLabel(value: String) = executeAsync { commonDao.deleteLabel(value) }

    fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) =
        executeAsyncWithCallback({ labelDao.insert(label) }, onComplete)

    fun updateLabel(oldValue: String, newValue: String, onComplete: (success: Boolean) -> Unit) =
        executeAsyncWithCallback({ commonDao.updateLabel(oldValue, newValue) }, onComplete)


    private fun getEmptyFolder(name: String): File {
        val folder = File(app.cacheDir, name)
        if (folder.exists()) {
            val files = folder.listFiles()
            if (files != null) {
                for (file in files) {
                    file.delete()
                }
            }
        } else folder.mkdir()
        return folder
    }

    private fun getBackupPath() = getEmptyFolder("backup")

    private fun getExportedPath() = getEmptyFolder("exported")


    private fun getJSON(baseNote: BaseNote): String {
        val labels = JSONArray(baseNote.labels)

        val jsonObject = JSONObject()
            .put("type", baseNote.type.name)
            .put("color", baseNote.color.name)
            .put("title", baseNote.title)
            .put("pinned", baseNote.pinned)
            .put("date-created", baseNote.timestamp)
            .put("labels", labels)

        when (baseNote.type) {
            Type.NOTE -> {
                jsonObject.put("body", baseNote.body)
                jsonObject.put("spans", Converters.spansToJson(baseNote.spans))
            }
            Type.LIST -> {
                jsonObject.put("items", Converters.itemsToJson(baseNote.items))
            }
        }

        return jsonObject.toString(2)
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
                append("<ol style=\"list-style: none; padding: 0;\">")
                baseNote.items.forEach { item ->
                    val body = Html.escapeHtml(item.body)
                    val checked = if (item.checked) "checked" else ""
                    append("<li><input type=\"checkbox\" $checked>$body</li>")
                }
                append("</ol>")
            }
        }
        append("</body></html>")
    }


    private fun executeAsync(function: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { function() }
    }

    companion object {

        fun getDateFormatter(context: Context): SimpleDateFormat {
            val locale = context.resources.configuration.locale
            val pattern = when (locale.language) {
                Locale.CHINESE.language,
                Locale.JAPANESE.language -> "yyyy年 MMM d日 (EEE)"
                else -> "EEE d MMM yyyy"
            }
            return SimpleDateFormat(pattern, locale)
        }
    }
}