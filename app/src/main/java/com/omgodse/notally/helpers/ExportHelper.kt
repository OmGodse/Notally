package com.omgodse.notally.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.core.text.toHtml
import androidx.fragment.app.Fragment
import com.itextpdf.text.Document
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.tool.xml.XMLWorkerHelper
import com.omgodse.notally.R
import com.omgodse.notally.interfaces.DialogListener
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.parents.NotallyActivity
import com.omgodse.notally.xml.XMLReader
import org.jsoup.Jsoup
import java.io.File
import java.io.FileWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import org.jsoup.nodes.Document as JsoupDocument

class ExportHelper(private val context: Context, private val fragment: Fragment) {

    private var currentFile: File? = null

    fun exportFileToPDF(file: File) {
        val fileName = getFileName(file)

        val pdfFile = File(getExportedPath(), "$fileName.pdf")
        val document = Document(PageSize.A4)
        val writer = PdfWriter.getInstance(document, pdfFile.outputStream())
        val htmlDocument = getHTML(file)
        document.open()
        XMLWorkerHelper.getInstance().parseXHtml(writer, document, htmlDocument.html().byteInputStream())
        document.close()
        writer.close()

        showFileOptionsDialog(pdfFile, "application/pdf")
    }

    fun exportFileToHTML(file: File) {
        val fileName = getFileName(file)
        val htmlFile = File(getExportedPath(), "$fileName.html")

        val fileWriter = FileWriter(htmlFile)
        fileWriter.write(getHTML(file).html())
        fileWriter.close()

        showFileOptionsDialog(htmlFile, "text/html")
    }

    fun exportFileToPlainText(file: File) {
        val xmlReader = XMLReader(file)
        val title = xmlReader.getTitle()
        val body = if (xmlReader.isNote()) {
            xmlReader.getBody()
        } else {
            val listItems = xmlReader.getListItems()
            val notesHelper = NotesHelper(context)
            notesHelper.getBodyFromItems(listItems)
        }

        val formatter = SimpleDateFormat(NotallyActivity.DateFormat, Locale.US)
        val date = formatter.format(xmlReader.getDateCreated().toLong())

        val fileName = getFileName(file)

        val textFile = File(getExportedPath(), "$fileName.txt")

        val fileWriter = FileWriter(textFile)

        val buffer = StringBuffer()
        if (title.isNotEmpty()){
            buffer.append(title)
            buffer.append("\n")
            buffer.append("\n")
        }
        buffer.append(date)
        buffer.append("\n")
        buffer.append("\n")

        buffer.append(body)

        fileWriter.write(buffer.toString())
        fileWriter.close()

        showFileOptionsDialog(textFile, "text/plain")
    }


    fun writeFileToStream(destinationURI: Uri) {
        val outputStream = context.contentResolver.openOutputStream(destinationURI)
        val byteArray = currentFile?.readBytes()
        if (byteArray != null){
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

        menuHelper.addItem(R.string.view, R.drawable.view)
        menuHelper.addItem(R.string.share, R.drawable.share)
        menuHelper.addItem(R.string.save_to_device, R.drawable.save)

        menuHelper.setListener(object : DialogListener {
            override fun onDialogItemClicked(label: String) {
                when (label) {
                    context.getString(R.string.view) -> viewFile(uri, mimeType)
                    context.getString(R.string.share) -> shareFile(uri, mimeType)
                    context.getString(R.string.save_to_device) -> saveFileToDevice(uri, file, mimeType)
                }
            }
        })

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
        val title = xmlReader.getTitle()
        val body = if (xmlReader.isNote()) {
            xmlReader.getBody()
        } else {
            val listItems = xmlReader.getListItems()
            val stringWriter = StringWriter()
            listItems.forEach { listItem ->
                stringWriter.append("${listItem.body} ")
            }
            stringWriter.toString()
        }

        return if (title.isEmpty()) {
            val words = body.split(" ")
            if (words.size > 1) {
                "${words[0]} ${words[1]}"
            } else words[0]
        } else title
    }

    private fun getHTML(file: File) : JsoupDocument {
        val xmlReader = XMLReader(file)

        val title = xmlReader.getTitle()
        val body = xmlReader.getBody()
        val items = xmlReader.getListItems()
        val spans = xmlReader.getSpans()
        val htmlBody = body.applySpans(spans).toHtml(HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)

        val formatter = SimpleDateFormat(NotallyActivity.DateFormat, Locale.US)
        val date = formatter.format(xmlReader.getDateCreated().toLong())

        val htmlBuffer = StringBuffer()
        htmlBuffer.append("<h2>$title</h2>")
        htmlBuffer.append("<p style=\"color: #7f7f7f;\">$date</p>")

        if (xmlReader.isNote()) {
            htmlBuffer.append("<p>$htmlBody</p>")
        }
        else {
            htmlBuffer.append("<ol>")
            items.forEach { item ->
                htmlBuffer.append("<li>${item.body}</li>")
            }
            htmlBuffer.append("</ol>")
        }
        val document = Jsoup.parseBodyFragment(htmlBuffer.toString())
        document.outputSettings().syntax(JsoupDocument.OutputSettings.Syntax.xml)
        return document
    }
}