package com.omgodse.post

import android.content.Context
import android.print.PostPDFPrinter
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

class PostPDFGenerator private constructor(private val file: File,
                                           private val baseURL: String?,
                                           private val content: String,
                                           private val context: Context,
                                           private val encoding: String,
                                           private val mimeType: String,
                                           private val printAttributes: PrintAttributes,
                                           private val onResult: OnResult) {

    fun create() {
        val webView = WebView(context)
        webView.loadDataWithBaseURL(baseURL, content, mimeType, encoding, null)
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                val printDocumentAdapter = webView.createPrintDocumentAdapter(file.nameWithoutExtension)
                generatePDF(printDocumentAdapter)
            }
        }
    }

    private fun generatePDF(printDocumentAdapter: PrintDocumentAdapter) {
        val postPDFPrinter = PostPDFPrinter(file, printDocumentAdapter, printAttributes, onResult)
        postPDFPrinter.print()
    }

    class Builder {

        private var file: File? = null
        private var baseURL: String? = null
        private var content: String? = null
        private var context: Context? = null

        private var encoding = "utf-8"
        private var mimeType = "text/html"

        private var printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setResolution(PrintAttributes.Resolution("Standard", "Standard", 100, 100))
            .build()

        private var onResult: OnResult? = null

        fun setFile(file: File?) = apply { this.file = file }

        fun setContent(content: String) = apply { this.content = content }

        fun setContext(context: Context) = apply { this.context = context }

        fun setOnResult(onResult: OnResult) = apply { this.onResult = onResult }


        fun build(): PostPDFGenerator {
            if (file == null) {
                throw Exception("File must not be null")
            }
            if (content == null) {
                throw Exception("Content must not be null")
            }
            if (context == null) {
                throw Exception("Context must not be null")
            }
            if (onResult == null) {
                throw Exception("Provide a callback for onSuccess()")
            }

            return PostPDFGenerator(file!!, baseURL, content!!, context!!, encoding, mimeType, printAttributes, onResult!!)
        }
    }

    interface OnResult {

        fun onSuccess(file: File)

        fun onFailure(message: String?)
    }
}