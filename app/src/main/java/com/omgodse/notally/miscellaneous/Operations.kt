package com.omgodse.notally.miscellaneous

import android.content.Context
import android.content.Intent
import com.omgodse.notally.R

object Operations {

    const val extraCharSequence = "com.omgodse.notally.extra.charSequence"

    fun shareNote(context: Context, title: String, body: CharSequence) {
        val text = body.toString()
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(extraCharSequence, body)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.putExtra(Intent.EXTRA_SUBJECT, title)
        val label = context.getString(R.string.share_note)
        val chooser = Intent.createChooser(intent, label)
        context.startActivity(chooser)
    }
}