package com.omgodse.notally.miscellaneous

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.Editable
import android.text.Spannable
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.omgodse.notally.adapters.NoteAdapter
import org.xmlpull.v1.XmlPullParser
import java.util.*
import kotlin.collections.ArrayList

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

fun XmlPullParser.getAttributeValue(attribute: String) = getAttributeValue(null, attribute)

fun NoteAdapter.submitCorrectList(list: ArrayList<Note>) {
    submitList(ArrayList(list))
}

fun Context.getLocale() : Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales[0]
    } else resources.configuration.locale
}