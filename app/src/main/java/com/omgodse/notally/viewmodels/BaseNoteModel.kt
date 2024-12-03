package com.omgodse.notally.viewmodels

import android.app.Application
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.print.PostPDFGenerator
import android.text.Html
import android.widget.Toast
import androidx.core.text.toHtml
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.omgodse.notally.ActionMode
import com.omgodse.notally.AttachmentDeleteService
import com.omgodse.notally.Cache
import com.omgodse.notally.Progress
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
import com.omgodse.notally.room.Attachment
import com.omgodse.notally.room.Audio
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Color
import com.omgodse.notally.room.Converters
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Header
import com.omgodse.notally.room.Image
import com.omgodse.notally.room.Item
import com.omgodse.notally.room.Label
import com.omgodse.notally.room.NotallyDatabase
import com.omgodse.notally.room.Type
import com.omgodse.notally.room.livedata.Content
import com.omgodse.notally.room.livedata.SearchResult
import com.omgodse.notally.widget.WidgetProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.UUID
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class BaseNoteModel(private val app: Application) : AndroidViewModel(app) {

    private val database = NotallyDatabase.getDatabase(app)
    private val labelDao = database.getLabelDao()
    private val commonDao = database.getCommonDao()
    private val baseNoteDao = database.getBaseNoteDao()

    private val labelCache = HashMap<String, Content>()

    var currentFile: File? = null

    val labels = labelDao.getAll()
    private val allNotes = baseNoteDao.getAll()
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

    val mediaRoot = IO.getExternalImagesDirectory(app)
    private val audioRoot = IO.getExternalAudioDirectory(app)

    val importingBackup = MutableLiveData<Progress>()
    val exportingBackup = MutableLiveData<Progress>()

    val actionMode = ActionMode()

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
        allNotes.observeForever { list ->
            Cache.list = list
        }
    }

    fun getNotesByLabel(label: String): Content {
        if (labelCache[label] == null) {
            labelCache[label] = Content(baseNoteDao.getBaseNotesByLabel(label), ::transform)
        }
        return requireNotNull(labelCache[label])
    }


    private fun transform(list: List<BaseNote>) = transform(list, pinned, others)


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
            exportingBackup.value = Progress(true, 0, 0, true)

            withContext(Dispatchers.IO) {
                val outputStream = requireNotNull(app.contentResolver.openOutputStream(uri))
                (outputStream as FileOutputStream).channel.truncate(0)

                val zipStream = ZipOutputStream(outputStream)

                database.checkpoint()
                Export.backupDatabase(app, zipStream)

                delay(1000)

                val images = baseNoteDao.getAllImages().flatMap { string -> Converters.jsonToImages(string) }
                val audios = baseNoteDao.getAllAudios().flatMap { string -> Converters.jsonToAudios(string) }
                val total = images.size + audios.size

                images.forEachIndexed { index, image ->
                    try {
                        Export.backupFile(zipStream, mediaRoot, "Images", image.name)
                    } catch (exception: Exception) {
                        Operations.log(app, exception)
                    } finally {
                        exportingBackup.postValue(Progress(true, index + 1, total, false))
                    }
                }
                audios.forEachIndexed { index, audio ->
                    try {
                        Export.backupFile(zipStream, audioRoot, "Audios", audio.name)
                    } catch (exception: Exception) {
                        Operations.log(app, exception)
                    } finally {
                        exportingBackup.postValue(Progress(true, images.size + index + 1, total, false))
                    }
                }

                zipStream.close()
            }

            exportingBackup.value = Progress(false, 0, 0, false)
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
     * We use a ZipFile instead of ZipInputStream because importing one image of 3 MB takes 1 second
     * on a phone with 6GB RAM. This is non negligible so we need to display the progress. However, with a stream
     * there is no way to know how many images are there, hence we can only display an indeterminate progress bar
     * which is almost as useless as displaying no progress bar.
     *
     * We only import the images referenced in notes. e.g If someone has added garbage to the
     * ZIP file, like a 100 MB image, ignore it.
     */
    private fun importZipBackup(uri: Uri) {
        val backupDir = getBackupDir()

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            finishImporting(backupDir)
            Toast.makeText(app, R.string.invalid_backup, Toast.LENGTH_LONG).show()
            Operations.log(app, throwable)
        }

        viewModelScope.launch(exceptionHandler) {
            importingBackup.value = Progress(true, 0, 0, true)

            withContext(Dispatchers.IO) {
                val stream = requireNotNull(app.contentResolver.openInputStream(uri))
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

                val labels = convertCursorToList(labelCursor) { cursor -> convertCursorToLabel(cursor) }
                val baseNotes = convertCursorToList(baseNoteCursor) { cursor -> convertCursorToBaseNote(cursor) }

                delay(1000)

                val total = baseNotes.fold(0) { acc, baseNote -> acc + baseNote.images.size + baseNote.audios.size }
                var current = 1

                // Don't let a single image bring down the entire backup
                baseNotes.forEach { baseNote ->
                    baseNote.images.forEach { image ->
                        try {
                            val entry = zipFile.getEntry("Images/${image.name}")
                            if (entry != null) {
                                val extension = image.name.substringAfterLast(".")
                                val name = "${UUID.randomUUID()}.$extension"
                                val file = File(mediaRoot, name)
                                image.name = name
                                val imageStream = zipFile.getInputStream(entry)
                                IO.copyStreamToFile(imageStream, file)
                            }
                        } catch (exception: Exception) {
                            Operations.log(app, exception)
                        } finally {
                            importingBackup.postValue(Progress(true, current, total, false))
                            current++
                        }
                    }
                    baseNote.audios.forEach { audio ->
                        try {
                            val entry = zipFile.getEntry("Audios/${audio.name}")
                            if (entry != null) {
                                val name = "${UUID.randomUUID()}.m4a"
                                val file = File(audioRoot, name)
                                audio.name = name
                                val audioStream = zipFile.getInputStream(entry)
                                IO.copyStreamToFile(audioStream, file)
                            }
                        } catch (exception: Exception) {
                            Operations.log(app, exception)
                        } finally {
                            importingBackup.postValue(Progress(true, current, total, false))
                            current++
                        }
                    }
                }

                commonDao.importBackup(baseNotes, labels)
            }

            finishImporting(backupDir)
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

        val imagesIndex = cursor.getColumnIndex("images")
        val images = if (imagesIndex != -1) {
            Converters.jsonToImages(cursor.getString(imagesIndex))
        } else emptyList()

        val audiosIndex = cursor.getColumnIndex("audios")
        val audios = if (audiosIndex != -1) {
            Converters.jsonToAudios(cursor.getString(audiosIndex))
        } else emptyList()

        return BaseNote(0, type, folder, color, title, pinned, timestamp, labels, body, spans, items, images, audios)
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

    private fun finishImporting(backupDir: File) {
        importingBackup.value = Progress(false, 0, 0, false)
        clear(backupDir)
    }


    private fun importXmlBackup(uri: Uri) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Operations.log(app, throwable)
            Toast.makeText(app, R.string.invalid_backup, Toast.LENGTH_LONG).show()
        }

        viewModelScope.launch(exceptionHandler) {
            withContext(Dispatchers.IO) {
                val stream = requireNotNull(app.contentResolver.openInputStream(uri))
                val backup = XMLUtils.readBackupFromStream(stream)
                commonDao.importBackup(backup.first, backup.second)
            }
            Toast.makeText(app, R.string.imported_backup, Toast.LENGTH_LONG).show()
        }
    }


    fun writeCurrentFileToUri(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val output = app.contentResolver.openOutputStream(uri) as FileOutputStream
                output.channel.truncate(0)
                val input = FileInputStream(requireNotNull(currentFile))
                input.copyTo(output)
                input.close()
                output.close()
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

        val date = DateFormat.getDateInstance(DateFormat.FULL).format(baseNote.timestamp)

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


    fun pinBaseNote(pinned: Boolean) {
        val id = actionMode.selectedIds.toLongArray()
        actionMode.close(false)
        executeAsync { baseNoteDao.updatePinned(id, pinned) }
    }

    fun colorBaseNote(color: Color) {
        val ids = actionMode.selectedIds.toLongArray()
        actionMode.close(true)
        executeAsync { baseNoteDao.updateColor(ids, color) }
    }

    fun copyBaseNote() {
        val ids = actionMode.selectedIds.toLongArray()
        actionMode.close(true)
        executeAsync { baseNoteDao.copy(ids) }
    }

    fun moveBaseNotes(folder: Folder) {
        val ids = actionMode.selectedIds.toLongArray()
        actionMode.close(false)
        executeAsync { baseNoteDao.move(ids, folder) }
    }

    fun updateBaseNoteLabels(labels: List<String>, id: Long) {
        actionMode.close(true)
        executeAsync { baseNoteDao.updateLabels(id, labels) }
    }


    fun deleteBaseNotes() {
        val ids = LongArray(actionMode.selectedNotes.size)
        val attachments = ArrayList<Attachment>()
        actionMode.selectedNotes.onEachIndexed { index, entry ->
            ids[index] = entry.key
            attachments.addAll(entry.value.images)
            attachments.addAll(entry.value.audios)
        }
        actionMode.close(false)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { baseNoteDao.delete(ids) }
            informOtherComponents(attachments, ids)
        }
    }

    fun deleteAllBaseNotes() {
        viewModelScope.launch {
            val ids: LongArray
            val images = ArrayList<Image>()
            val audios = ArrayList<Audio>()
            withContext(Dispatchers.IO) {
                ids = baseNoteDao.getDeletedNoteIds()
                val imageStrings = baseNoteDao.getDeletedNoteImages()
                val audioStrings = baseNoteDao.getDeletedNoteAudios()
                imageStrings.flatMapTo(images) { json -> Converters.jsonToImages(json) }
                audioStrings.flatMapTo(audios) { json -> Converters.jsonToAudios(json) }
                baseNoteDao.deleteFrom(Folder.DELETED)
            }
            val attachments = ArrayList<Attachment>(images.size + audios.size)
            attachments.addAll(images)
            attachments.addAll(audios)
            informOtherComponents(attachments, ids)
        }
    }

    private fun informOtherComponents(attachments: ArrayList<Attachment>, ids: LongArray) {
        if (attachments.isNotEmpty()) {
            AttachmentDeleteService.start(app, attachments)
        }
        if (ids.isNotEmpty()) {
            WidgetProvider.sendBroadcast(app, ids)
        }
    }


    suspend fun getAllLabels() = withContext(Dispatchers.IO) { labelDao.getArrayOfAll() }

    fun deleteLabel(value: String) = executeAsync { commonDao.deleteLabel(value) }

    fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) =
        executeAsyncWithCallback({ labelDao.insert(label) }, onComplete)

    fun updateLabel(oldValue: String, newValue: String, onComplete: (success: Boolean) -> Unit) =
        executeAsyncWithCallback({ commonDao.updateLabel(oldValue, newValue) }, onComplete)


    private fun getEmptyFolder(name: String): File {
        val folder = File(app.cacheDir, name)
        if (folder.exists()) {
            clear(folder)
        } else folder.mkdir()
        return folder
    }

    private fun clear(directory: File) {
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                file.delete()
            }
        }
    }

    private fun getBackupDir() = getEmptyFolder("backup")

    private fun getExportedPath() = getEmptyFolder("exported")


    private fun getJSON(baseNote: BaseNote): String {
        val jsonObject = JSONObject()
            .put("type", baseNote.type.name)
            .put("color", baseNote.color.name)
            .put("title", baseNote.title)
            .put("pinned", baseNote.pinned)
            .put("date-created", baseNote.timestamp)
            .put("labels", JSONArray(baseNote.labels))

        when (baseNote.type) {
            Type.NOTE -> {
                jsonObject.put("body", baseNote.body)
                jsonObject.put("spans", Converters.spansToJSONArray(baseNote.spans))
            }

            Type.LIST -> {
                jsonObject.put("items", Converters.itemsToJSONArray(baseNote.items))
            }
        }

        return jsonObject.toString(2)
    }

    private fun getHTML(baseNote: BaseNote, showDateCreated: Boolean) = buildString {
        val date = DateFormat.getDateInstance(DateFormat.FULL).format(baseNote.timestamp)
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

        fun transform(list: List<BaseNote>, pinned: Header, others: Header): List<Item> {
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
    }
}