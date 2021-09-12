package android.print

import android.content.Context
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

/**
 * This class needs to be in android.print package to access the package private
 * methods of [PrintDocumentAdapter]
 */
object PostPDFGenerator {

    fun create(file: File, content: String, context: Context, onResult: OnResult) {
        val webView = WebView(context)
        webView.loadDataWithBaseURL(null, content, "text/html", "utf-8", null)
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                val adapter = webView.createPrintDocumentAdapter(file.nameWithoutExtension)
                print(file, adapter, onResult)
            }
        }
    }


    private fun print(file: File, adapter: PrintDocumentAdapter, onResult: OnResult) {
        val onLayoutResult = object : PrintDocumentAdapter.LayoutResultCallback() {

            override fun onLayoutFailed(error: CharSequence?) {
                onResult.onFailure(error?.toString())
            }

            override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                writeToFile(file, adapter, onResult)
            }
        }

        adapter.onLayout(null, getPrintAttributes(), null, onLayoutResult, null)
    }

    private fun writeToFile(file: File, adapter: PrintDocumentAdapter, onResult: OnResult) {
        val onWriteResult = object : PrintDocumentAdapter.WriteResultCallback() {

            override fun onWriteFailed(error: CharSequence?) {
                onResult.onFailure(error?.toString())
            }

            override fun onWriteFinished(pages: Array<out PageRange>?) {
                onResult.onSuccess(file)
            }
        }

        val pages = arrayOf(PageRange.ALL_PAGES)
        val fileDescriptor = getFileDescriptor(file)
        adapter.onWrite(pages, fileDescriptor, null, onWriteResult)
    }


    private fun getPrintAttributes(): PrintAttributes {
        return PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setResolution(PrintAttributes.Resolution("Standard", "Standard", 100, 100))
            .build()
    }

    private fun getFileDescriptor(file: File): ParcelFileDescriptor {
        if (!file.exists()) {
            file.createNewFile()
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
    }

    interface OnResult {

        fun onSuccess(file: File)

        fun onFailure(message: String?)
    }
}