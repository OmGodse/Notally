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
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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

fun Spannable.getFilteredSpans(): ArrayList<SpanRepresentation> {
    val representations = LinkedHashSet<SpanRepresentation>()
    val spans = getSpans(0, length, Object::class.java)
    spans.forEach { span ->
        val end = getSpanEnd(span)
        val start = getSpanStart(span)
        val representation = SpanRepresentation(false, false, false, false, start, end)

        if (span is StyleSpan) {
            if (span.style == Typeface.BOLD) {
                representation.isBold = true
            }
            else if (span.style == Typeface.ITALIC) {
                representation.isItalic = true
            }
        }
        else if (span is TypefaceSpan) {
            if (span.family == "monospace") {
                representation.isMonospace = true
            }
        }
        else if (span is StrikethroughSpan) {
            representation.isStrikethrough = true
        }

        if (representation.isNotUseless()) {
            representations.add(representation)
        }
    }
    return getFilteredRepresentations(ArrayList(representations))
}

private fun getFilteredRepresentations(representations: ArrayList<SpanRepresentation>): ArrayList<SpanRepresentation> {
    representations.forEachIndexed { index, representation ->
        val match = representations.find { spanRepresentation ->
            spanRepresentation.isEqualInSize(representation)
        }
        if (match != null && representations.indexOf(match) != index) {
            if (match.isBold) {
                representation.isBold = true
            }
            if (match.isItalic) {
                representation.isItalic = true
            }
            if (match.isMonospace) {
                representation.isMonospace = true
            }
            if (match.isStrikethrough) {
                representation.isStrikethrough = true
            }
            val copy = ArrayList(representations)
            copy[index] = representation
            copy.remove(match)
            return getFilteredRepresentations(copy)
        }
    }
    return representations
}