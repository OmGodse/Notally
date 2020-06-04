package com.omgodse.notally.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.core.text.toHtml
import androidx.fragment.app.Fragment
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.tool.xml.XMLWorkerFontProvider
import com.itextpdf.tool.xml.XMLWorkerHelper
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Note
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.parents.NotallyActivity
import com.omgodse.notally.xml.BackupReader
import com.omgodse.notally.xml.XMLReader
import com.omgodse.notally.xml.XMLTags
import com.omgodse.notally.xml.XMLWriter
import org.jsoup.Jsoup
import java.io.File
import java.io.InputStream
import java.io.StringWriter
import java.text.SimpleDateFormat
import org.jsoup.nodes.Document as JsoupDocument

class ExportHelper(private val context: Context, private val fragment: Fragment) {

    private var currentFile: File? = null
    private val notesHelper = NotesHelper(context)
    private val settingsHelper = SettingsHelper(context)

    fun exportBackup() {
        val backupFile = File(getExportedPath(), "Notally Backup.xml")

        val notes = notesHelper.getNotePath().listFiles()
        val deletedNotes = notesHelper.getDeletedPath().listFiles()
        val archivedNotes = notesHelper.getArchivedPath().listFiles()

        val labels = notesHelper.getSortedLabelsList()

        val xmlWriter = XMLWriter(XMLTags.ExportedNotes, backupFile)
        xmlWriter.start()

        if (notes?.isNotEmpty() == true) {
            xmlWriter.startTag(XMLTags.Notes)

            notes.forEach { file -> appendFileToWriter(file, xmlWriter) }

            xmlWriter.endTag(XMLTags.Notes)
        }

        if (deletedNotes?.isNotEmpty() == true) {
            xmlWriter.startTag(XMLTags.DeletedNotes)

            deletedNotes.forEach { file -> appendFileToWriter(file, xmlWriter) }

            xmlWriter.endTag(XMLTags.DeletedNotes)
        }

        if (archivedNotes?.isNotEmpty() == true) {
            xmlWriter.startTag(XMLTags.ArchivedNotes)

            archivedNotes.forEach { file -> appendFileToWriter(file, xmlWriter) }

            xmlWriter.endTag(XMLTags.ArchivedNotes)
        }

        xmlWriter.setLabels(labels.toHashSet())

        xmlWriter.end()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", backupFile)

        saveFileToDevice(uri, backupFile, "text/xml")
    }

    fun importBackup(inputStream: InputStream) {
        val backupReader = BackupReader(inputStream)
        val backup = backupReader.getBackup()

        backup.notes.forEach { note ->
            saveImportedNote(notesHelper.getNotePath(), note)
        }

        backup.deletedNotes.forEach { note ->
            saveImportedNote(notesHelper.getDeletedPath(), note)
        }

        backup.archivedNotes.forEach { note ->
            saveImportedNote(notesHelper.getArchivedPath(), note)
        }

        val preferences = context.getSharedPreferences(Constants.labelsPreferences, Context.MODE_PRIVATE)
        val previousLabels = preferences.getStringSet(Constants.labelItems, HashSet<String>())
        previousLabels?.addAll(backup.labels)
        val editor = preferences.edit()
        editor.putStringSet(Constants.labelItems, previousLabels)
        editor.apply()
    }

    private fun saveImportedNote(path: File, note: Note) {
        val fileName = getFileName(path, note.timestamp)
        val file = File (path, fileName)

        val xmlWriter: XMLWriter
        if (note.isNote) {
            xmlWriter = XMLWriter(XMLTags.Note, file)
            xmlWriter.start()
            xmlWriter.setTitle(note.title)
            xmlWriter.setTimestamp(note.timestamp)
            xmlWriter.setBody(note.body)
            xmlWriter.setSpans(note.spans)
        }
        else {
            xmlWriter = XMLWriter(XMLTags.List, file)
            xmlWriter.start()
            xmlWriter.setTitle(note.title)
            xmlWriter.setTimestamp(note.timestamp)
            xmlWriter.setListItems(note.items)
        }
        xmlWriter.setLabels(note.labels)
        xmlWriter.end()
    }

    private fun appendFileToWriter(file: File, xmlWriter: XMLWriter) {
        val note = NotesHelper.convertFileToNote(file)
        if (note.isNote) {
            xmlWriter.startTag(XMLTags.Note)
            xmlWriter.setTitle(note.title)
            xmlWriter.setTimestamp(note.timestamp)
            xmlWriter.setBody(note.body)
            xmlWriter.setSpans(note.spans)
            xmlWriter.setLabels(note.labels)
            xmlWriter.endTag(XMLTags.Note)
        } else {
            xmlWriter.startTag(XMLTags.List)
            xmlWriter.setTitle(note.title)
            xmlWriter.setTimestamp(note.timestamp)
            xmlWriter.setListItems(note.items)
            xmlWriter.setLabels(note.labels)
            xmlWriter.endTag(XMLTags.List)
        }
    }


    fun exportFileToPDF(file: File) {
        val fileName = getFileName(file)

        val pdfFile = File(getExportedPath(), "$fileName.pdf")
        val document = Document(PageSize.A4)
        val writer = PdfWriter.getInstance(document, pdfFile.outputStream())
        val htmlDocument = getHTML(file)
        val stylesheet = "<style>body { font-family : sans-serif ; } h2 { letter-spacing : 0.5px; } tt { font-family : monospace ;}</style>"
        htmlDocument.head().append(stylesheet)
        document.open()

        val fontProvider = XMLWorkerFontProvider(XMLWorkerFontProvider.DONTLOOKFORFONTS)
        fontProvider.register("assets/roboto.ttf", "sans-serif")
        fontProvider.register("assets/roboto_mono.ttf", "monospace")

        XMLWorkerHelper.getInstance().parseXHtml(writer, document, htmlDocument.html().byteInputStream(), Charsets.UTF_8, fontProvider)
        document.close()
        writer.close()

        showFileOptionsDialog(pdfFile, "application/pdf")
    }

    fun exportFileToHTML(file: File) {
        val fileName = getFileName(file)
        val htmlFile = File(getExportedPath(), "$fileName.html")

        val html = getHTML(file).html()
        htmlFile.writeText(html)

        showFileOptionsDialog(htmlFile, "text/html")
    }

    fun exportFileToPlainText(file: File) {
        val xmlReader = XMLReader(file)
        val title = xmlReader.getTitle()
        val body = if (xmlReader.isNote()) {
            xmlReader.getBody()
        } else {
            notesHelper.getBodyFromItems(xmlReader.getListItems())
        }

        val formatter = SimpleDateFormat(NotallyActivity.DateFormat, context.getLocale())
        val date = formatter.format(xmlReader.getTimestamp().toLong())

        val fileName = getFileName(file)

        val textFile = File(getExportedPath(), "$fileName.txt")
        val content = buildString {
            if (title.isNotEmpty()){
                append(title)
                append("\n")
                append("\n")
            }
            if (settingsHelper.getShowDateCreatedPreference()){
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
        val xmlReader = XMLReader(file)
        val body = if (xmlReader.isNote()) {
            xmlReader.getBody()
        } else {
            val stringWriter = StringWriter()
            xmlReader.getListItems().forEach { listItem ->
                stringWriter.append("${listItem.body} ")
            }
            stringWriter.toString()
        }

        val fileName =  if (xmlReader.getTitle().isEmpty()) {
            val words = body.split(" ")
            if (words.size > 1) {
                "${words[0]} ${words[1]}"
            } else words[0]
        } else xmlReader.getTitle()

        return fileName.replace("/", "")
    }

    private fun getHTML(file: File): JsoupDocument {
        val xmlReader = XMLReader(file)

        val htmlBody = xmlReader.getBody().applySpans(xmlReader.getSpans()).toHtml(HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

        val formatter = SimpleDateFormat(NotallyActivity.DateFormat, context.getLocale())
        val date = formatter.format(xmlReader.getTimestamp().toLong())

        val html = buildString {
            append("<h2>${xmlReader.getTitle()}</h2>")

            if (settingsHelper.getShowDateCreatedPreference()) {
                append("<p>$date</p>")
            }

            if (xmlReader.isNote()) {
                append("<p>$htmlBody</p>")
            }
            else {
                append("<ol>")
                xmlReader.getListItems().forEach { item ->
                    append("<li>${item.body}</li>")
                }
                append("</ol>")
            }
        }

        val document = Jsoup.parseBodyFragment(html)
        document.charset(Charsets.UTF_8)
        document.outputSettings().syntax(JsoupDocument.OutputSettings.Syntax.xml)
        return document
    }

    private fun getFileName(folder: File, name: String, index: Int = 0) : String {
        val fileName = if (index == 0){
            "$name.xml"
        }
        else "$name ($index) .xml"
        val file = File (folder, fileName)
        return if (file.exists()) {
            getFileName(folder, name, index + 1)
        } else fileName
    }
}