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
import android.view.inputmethod.EditorInfo
import com.google.android.material.textfield.TextInputEditText
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.xml.SpanRepresentation
import java.util.*

fun String.applySpans(representations: ArrayList<SpanRepresentation>): Editable {
    val editable = Editable.Factory.getInstance().newEditable(this)
    representations.forEach { representation ->
        if (representation.isBold) {
            editable.setSpan(StyleSpan(Typeface.BOLD), representation.start, representation.end)
        }
        if (representation.isItalic) {
            editable.setSpan(StyleSpan(Typeface.ITALIC), representation.start, representation.end)
        }
        if (representation.isLink) {
            val url = TakeNote.getURLFrom(substring(representation.start, representation.end))
            editable.setSpan(URLSpan(url), representation.start, representation.end)
        }
        if (representation.isMonospace) {
            editable.setSpan(TypefaceSpan("monospace"), representation.start, representation.end)
        }
        if (representation.isStrikethrough) {
            editable.setSpan(StrikethroughSpan(), representation.start, representation.end)
        }
    }
    return editable
}

private fun Spannable.setSpan(span: Any, start: Int, end: Int) {
    if (end <= length) {
        setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else setSpan(span, start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

fun Context.getLocale(): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales[0]
    } else resources.configuration.locale
}

fun TextInputEditText.setOnNextAction(onNext: () -> Unit) {
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