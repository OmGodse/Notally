package android.print

import android.os.ParcelFileDescriptor
import com.omgodse.post.PostPDFGenerator
import java.io.File

internal class PostPDFPrinter(
    private val file: File,
    private val printDocumentAdapter: PrintDocumentAdapter,
    private val printAttributes: PrintAttributes,
    private val onResult: PostPDFGenerator.OnResult
) {

    fun print() {
        val onLayoutResult = object : PrintDocumentAdapter.LayoutResultCallback() {

            override fun onLayoutFailed(error: CharSequence?) {
                onResult.onFailure(error?.toString())
            }

            override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                writeToFile()
            }

        }

        printDocumentAdapter.onLayout(null, printAttributes, null, onLayoutResult, null)
    }

    private fun writeToFile() {
        val onWriteResult = object : PrintDocumentAdapter.WriteResultCallback() {

            override fun onWriteFailed(error: CharSequence?) {
                onResult.onFailure(error?.toString())
            }

            override fun onWriteFinished(pages: Array<out PageRange>?) {
                onResult.onSuccess(file)
            }

        }

        printDocumentAdapter.onWrite(arrayOf(PageRange.ALL_PAGES), getFileDescriptor(), null, onWriteResult)
    }

    private fun getFileDescriptor(): ParcelFileDescriptor {
        if (!file.exists()) {
            file.createNewFile()
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
    }
}