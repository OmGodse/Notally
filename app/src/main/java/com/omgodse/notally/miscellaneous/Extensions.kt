package com.omgodse.notally.miscellaneous

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.google.android.material.chip.ChipGroup
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.databinding.LabelBinding
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.room.SpanRepresentation
import java.util.*

fun List<ListItem>?.getBody() = buildString {
    this@getBody?.forEachIndexed { index, (body) ->
        appendLine("${(index + 1)}) $body")
    }
}


fun ChipGroup.bindLabels(labels: HashSet<String>) {
    if (labels.isEmpty()) {
        visibility = View.GONE
    } else {
        visibility = View.VISIBLE
        removeAllViews()
        for (label in labels) {
            val inflater = LayoutInflater.from(context)
            val displayLabel = LabelBinding.inflate(inflater).root
            displayLabel.text = label
            addView(displayLabel)
        }
    }
}

fun String.applySpans(representations: List<SpanRepresentation>): Editable {
    val editable = Editable.Factory.getInstance().newEditable(this)
    representations.forEach { (bold, link, italic, monospace, strikethrough, start, end) ->
        if (bold) {
            editable.setSpan(StyleSpan(Typeface.BOLD), start, end)
        }
        if (italic) {
            editable.setSpan(StyleSpan(Typeface.ITALIC), start, end)
        }
        if (link) {
            val url = getURL(start, end)
            editable.setSpan(URLSpan(url), start, end)
        }
        if (monospace) {
            editable.setSpan(TypefaceSpan("monospace"), start, end)
        }
        if (strikethrough) {
            editable.setSpan(StrikethroughSpan(), start, end)
        }
    }
    return editable
}


private fun String.getURL(start: Int, end: Int): String {
    return if (end <= length) {
        TakeNote.getURLFrom(substring(start, end))
    } else TakeNote.getURLFrom(substring(start, length))
}

/**
 * For some reason, this method crashes sometimes with an
 * IndexOutOfBoundsException that I've not been able to replicate.
 * When this happens, to prevent the entire app from crashing and becoming
 * unusable, the exception is suppressed.
 */
private fun Spannable.setSpan(span: Any, start: Int, end: Int) {
    try {
        if (end <= length) {
            setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else setSpan(span, start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    } catch (exception: Exception) {
        exception.printStackTrace()
    }
}


fun Context.getLocale(): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales[0]
    } else resources.configuration.locale
}

fun EditText.setOnNextAction(onNext: () -> Unit) {
    setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

    setOnKeyListener { v, keyCode, event ->
        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
            onNext()
            return@setOnKeyListener true
        } else return@setOnKeyListener false
    }

    setOnEditorActionListener { v, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_NEXT) {
            onNext()
            return@setOnEditorActionListener true
        } else return@setOnEditorActionListener false
    }
}