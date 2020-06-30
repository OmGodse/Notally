package com.omgodse.notally.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.core.text.toHtml
import androidx.fragment.app.Fragment
import com.omgodse.notally.R
import com.omgodse.notally.activities.NotallyActivity
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.xml.Backup
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.List
import com.omgodse.notally.xml.Note
import com.uttampanchasara.pdfgenerator.CreatePdf
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat

class ExportHelper(private val context: Context, private val fragment: Fragment) {

    private var currentFile: File? = null
    private val notesHelper = NotesHelper(context)
    private val settingsHelper = SettingsHelper(context)

    fun exportBackup() {
        val backupFile = File(getExportedPath(), "Notally Backup.xml")

        val baseNotes = notesHelper.getNotePath().listFiles().convertFilesToNotes()
        val deletedBaseNotes = notesHelper.getDeletedPath().listFiles().convertFilesToNotes()
        val archivedBaseNotes = notesHelper.getArchivedPath().listFiles().convertFilesToNotes()

        val labels = notesHelper.getSortedLabelsList().toHashSet()

        val backup = Backup(baseNotes, deletedBaseNotes, archivedBaseNotes, labels)
        backup.writeToFile(backupFile)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", backupFile)
        saveFileToDevice(uri, backupFile, "text/xml")
    }

    fun importBackup(inputStream: InputStream) {
        val backup = Backup.readFromStream(inputStream)

        backup.baseNotes.forEach { baseNote ->
            saveImportedNote(notesHelper.getNotePath(), baseNote)
        }

        backup.deletedBaseNotes.forEach { baseNote ->
            saveImportedNote(notesHelper.getDeletedPath(), baseNote)
        }

        backup.archivedBaseNotes.forEach { baseNote ->
            saveImportedNote(notesHelper.getArchivedPath(), baseNote)
        }

        val preferences = context.getSharedPreferences(Constants.labelsPreferences, Context.MODE_PRIVATE)
        val previousLabels = preferences.getStringSet(Constants.labelItems, HashSet<String>())
        previousLabels?.addAll(backup.labels)
        val editor = preferences.edit()
        editor.putStringSet(Constants.labelItems, previousLabels)
        editor.apply()
    }

    private fun saveImportedNote(path: File, baseNote: BaseNote) {
        val fileName = getFileName(path, baseNote.timestamp)
        val file = File(path, fileName)

        when (baseNote) {
            is Note -> baseNote.copy(filePath = file.path).writeToFile()
            is List -> baseNote.copy(filePath = file.path).writeToFile()
        }
    }

    private fun Array<File>?.convertFilesToNotes(): ArrayList<BaseNote> {
        return if (this == null){
            ArrayList()
        } else {
            val baseNotes = ArrayList<BaseNote>()
            forEach { file ->
                val note = BaseNote.readFromFile(file)
                baseNotes.add(note)
            }
            baseNotes
        }
    }


    fun exportFileToPDF(file: File) {
        val fileName = getFileName(file)

        val html = getHTML(file)

        CreatePdf(context)
            .setPdfName(fileName)
            .openPrintDialog(false)
            .setContentBaseUrl(null)
            .setPageSize(PrintAttributes.MediaSize.ISO_A4)
            .setContent(html)
            .setFilePath(getExportedPath().path)
            .setCallbackListener(object : CreatePdf.PdfCallbackListener {
                override fun onFailure(errorMsg: String) {
                    Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                }

                override fun onSuccess(filePath: String) {
                    showFileOptionsDialog(File(filePath), "application/pdf")
                }
            })
            .create()
    }

    fun exportFileToHTML(file: File) {
        val fileName = getFileName(file)
        val htmlFile = File(getExportedPath(), "$fileName.html")

        val html = getHTML(file)
        htmlFile.writeText(html)

        showFileOptionsDialog(htmlFile, "text/html")
    }

    fun exportFileToPlainText(file: File) {
        val baseNote = BaseNote.readFromFile(file)

        val title = baseNote.title
        val body = when (baseNote) {
            is Note -> baseNote.body
            is List -> notesHelper.getBodyFromItems(baseNote.items)
        }

        val formatter = SimpleDateFormat(NotallyActivity.DateFormat, context.getLocale())
        val date = formatter.format(baseNote.timestamp.toLong())

        val fileName = getFileName(file)

        val textFile = File(getExportedPath(), "$fileName.txt")
        val content = buildString {
            if (title.isNotEmpty()) {
                append(title)
                append("\n")
                append("\n")
            }
            if (settingsHelper.getShowDateCreatedPreference()) {
                append(date)
                append("\n")
                append("\n")
            }
            append(body)
        }

        textFile.writeText(content)

        showFileOptionsDialog(textFile, "text/plain")
    }


    fun writeFileToStream(destinationURI: Uri) {
        val outputStream = context.contentResolver.openOutputStream(destinationURI)
        val byteArray = currentFile?.readBytes()
        if (byteArray != null) {
            outputStream?.write(byteArray)
        }
        outputStream?.close()
        currentFile = null
        Toast.makeText(context, R.string.saved_to_device, Toast.LENGTH_LONG).show()
    }


    private fun viewFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.view_note))
        context.startActivity(chooser)
    }

    private fun shareFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.share_note))
        context.startActivity(chooser)
    }

    private fun saveFileToDevice(uri: Uri, file: File, mimeType: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, file.nameWithoutExtension)
            type = mimeType
        }
        currentFile = file
        fragment.startActivityForResult(intent, Constants.RequestCodeExportFile)
    }

    private fun showFileOptionsDialog(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val menuHelper = MenuHelper(context)

        menuHelper.addItem(R.string.view, R.drawable.view) { viewFile(uri, mimeType) }
        menuHelper.addItem(R.string.share, R.drawable.share) { shareFile(uri, mimeType) }
        menuHelper.addItem(R.string.save_to_device, R.drawable.save) { saveFileToDevice(uri, file, mimeType) }

        menuHelper.show()
    }


    private fun getExportedPath(): File {
        val filePath = File(context.cacheDir, "exported")
        if (!filePath.exists()) {
            filePath.mkdir()
        }
        filePath.listFiles()?.forEach { file ->
            file.delete()
        }
        return filePath
    }

    private fun getFileName(file: File): String {
        val baseNote = BaseNote.readFromFile(file)

        val title = baseNote.title
        val body = when (baseNote) {
            is Note -> baseNote.body
            is List -> notesHelper.getBodyFromItems(baseNote.items)
        }
        val fileName = if (title.isEmpty()) {
            val words = body.split(" ")
            if (words.size > 1) {
                "${words[0]} ${words[1]}"
            } else words[0]
        } else title
        return fileName.take(64).replace("/", "")
    }

    private fun getHTML(file: File): String {
        val baseNote = BaseNote.readFromFile(file)

        val formatter = SimpleDateFormat(NotallyActivity.DateFormat, context.getLocale())
        val date = formatter.format(baseNote.timestamp.toLong())

        val html = buildString {
            append("<html><head><meta charset=\"UTF-8\" /></head><body>")
            append("<h2>${baseNote.title}</h2>")

            if (settingsHelper.getShowDateCreatedPreference()) {
                append("<p>$date</p>")
            }

            when (baseNote) {
                is Note -> {
                    val body = baseNote.body.applySpans(baseNote.spans).toHtml(HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                    append("<p>$body</p>")
                }
                is List -> {
                    append("<ol>")
                    baseNote.items.forEach { item ->
                        append("<li>${item.body}</li>")
                    }
                    append("</ol>")
                }
            }
            append("</body></html")
        }

        return html
    }

    private fun getFileName(folder: File, name: String, index: Int = 0): String {
        val fileName = if (index == 0) {
            "$name.xml"
        } else "$name ($index) .xml"
        val file = File(folder, fileName)
        return if (file.exists()) {
            getFileName(folder, name, index + 1)
        } else fileName
    }
}