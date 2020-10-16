package com.omgodse.notally.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.core.text.toHtml
import androidx.fragment.app.Fragment
import com.omgodse.notally.R
import com.omgodse.notally.activities.NotallyActivity
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.viewmodels.BaseNoteModel
import com.omgodse.notally.xml.Backup
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.List
import com.omgodse.notally.xml.Note
import com.omgodse.post.PostPDFGenerator
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat

class ExportHelper(private val context: Context, private val fragment: Fragment) {

    private var currentFile: File? = null
    private val settingsHelper = SettingsHelper(context)

    fun exportBackup() {
        val backupFile = File(getExportedPath(), "Notally Backup.xml")

        val baseNotes = BaseNoteModel.getNotePath(context).listFiles().convertFilesToNotes()
        val deletedBaseNotes = BaseNoteModel.getDeletedPath(context).listFiles().convertFilesToNotes()
        val archivedBaseNotes = BaseNoteModel.getArchivedPath(context).listFiles().convertFilesToNotes()

        val labels = BaseNoteModel.getSortedLabels(context).toHashSet()

        val backup = Backup(baseNotes, deletedBaseNotes, archivedBaseNotes, labels)
        backup.writeToFile(backupFile)

        saveFileToDevice(backupFile, "text/xml")
    }

    fun importBackup(inputStream: InputStream) {
        val backup = Backup.readFromStream(inputStream)

        backup.baseNotes.forEach { saveImportedNote(BaseNoteModel.getNotePath(context), it) }

        backup.deletedBaseNotes.forEach { saveImportedNote(BaseNoteModel.getDeletedPath(context), it) }

        backup.archivedBaseNotes.forEach { saveImportedNote(BaseNoteModel.getArchivedPath(context), it) }

        val previousLabels = BaseNoteModel.getSortedLabels(context)
        previousLabels.addAll(backup.labels)
        BaseNoteModel.saveLabels(context, previousLabels.toHashSet())
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
        return if (this == null) {
            ArrayList()
        } else {
            val baseNotes = ArrayList<BaseNote>()
            forEach {
                val note = BaseNote.readFromFile(it)
                baseNotes.add(note)
            }
            baseNotes
        }
    }


    fun exportBaseNoteToPDF(baseNote: BaseNote) {
        val fileName = getFileName(baseNote)
        val pdfFile = File(getExportedPath(), "$fileName.pdf")

        val html = getHTML(baseNote)

        PostPDFGenerator.Builder()
            .setFile(pdfFile)
            .setContent(html)
            .setContext(context.applicationContext)
            .setOnResult(object : PostPDFGenerator.OnResult {
                override fun onSuccess(file: File) {
                    showFileOptionsDialog(file, "application/pdf")
                }

                override fun onFailure(message: String?) {
                    Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                }
            })
            .build()
            .create()
    }

    fun exportBaseNoteToHTML(baseNote: BaseNote) {
        val fileName = getFileName(baseNote)
        val htmlFile = File(getExportedPath(), "$fileName.html")

        val html = getHTML(baseNote)
        htmlFile.writeText(html)

        showFileOptionsDialog(htmlFile, "text/html")
    }

    fun exportBaseNoteToPlainText(baseNote: BaseNote) {
        val fileName = getFileName(baseNote)
        val textFile = File(getExportedPath(), "$fileName.txt")

        val content = getPlainText(baseNote)
        textFile.writeText(content)

        showFileOptionsDialog(textFile, "text/plain")
    }


    fun writeFileToUri(destinationUri: Uri) {
        val outputStream = context.contentResolver.openOutputStream(destinationUri)
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
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.view_note))
        context.startActivity(chooser)
    }

    private fun shareFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.share_note))
        context.startActivity(chooser)
    }

    private fun saveFileToDevice(file: File, mimeType: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, file.nameWithoutExtension)
        }
        currentFile = file
        fragment.startActivityForResult(intent, Constants.RequestCodeExportFile)
    }

    private fun showFileOptionsDialog(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        MenuHelper(context)
            .addItem(Operation(R.string.view, R.drawable.view) { viewFile(uri, mimeType) })
            .addItem(Operation(R.string.share, R.drawable.share) { shareFile(uri, mimeType) })
            .addItem(Operation(R.string.save_to_device, R.drawable.save) { saveFileToDevice(file, mimeType) })
            .show()
    }


    private fun getExportedPath(): File {
        val filePath = File(context.cacheDir, "exported")
        if (!filePath.exists()) {
            filePath.mkdir()
        }
        filePath.listFiles()?.forEach { it.delete() }
        return filePath
    }

    private fun getFileName(baseNote: BaseNote): String {
        val title = baseNote.title
        val body = when (baseNote) {
            is Note -> baseNote.body
            is List -> OperationsHelper.getBodyFromItems(baseNote.items)
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

    private fun getHTML(baseNote: BaseNote) = buildString {
        val formatter = SimpleDateFormat(NotallyActivity.DateFormat, context.getLocale())
        val date = formatter.format(baseNote.timestamp.toLong())

        append("<html><head><meta charset=\"UTF-8\" /></head><body>")
        append("<h2>${baseNote.title}</h2>")

        if (settingsHelper.getShowDateCreated()) {
            append("<p>$date</p>")
        }

        when (baseNote) {
            is Note -> {
                val body = baseNote.body.applySpans(baseNote.spans).toHtml(HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                append("<p>$body</p>")
            }
            is List -> {
                append("<ol>")
                baseNote.items.forEach {
                    append("<li>${it.body}</li>")
                }
                append("</ol>")
            }
        }
        append("</body></html")
    }

    private fun getPlainText(baseNote: BaseNote) = buildString {
        val formatter = SimpleDateFormat(NotallyActivity.DateFormat, context.getLocale())
        val date = formatter.format(baseNote.timestamp.toLong())

        val body = when (baseNote) {
            is Note -> baseNote.body
            is List -> OperationsHelper.getBodyFromItems(baseNote.items)
        }

        if (baseNote.title.isNotEmpty()) {
            append("${baseNote.title}\n\n")
        }
        if (settingsHelper.getShowDateCreated()) {
            append("$date\n\n")
        }
        append(body)
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