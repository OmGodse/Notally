package com.omgodse.notally.miscellaneous

import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.room.SpanRepresentation

/**
 * For some reason, this method crashes sometimes with an
 * IndexOutOfBoundsException that I've not been able to replicate.
 * When this happens, to prevent the entire app from crashing and becoming
 * unusable, the exception is suppressed.
 */
fun String.applySpans(representations: List<SpanRepresentation>): Editable {
    val editable = Editable.Factory.getInstance().newEditable(this)
    representations.forEach { (bold, link, italic, monospace, strikethrough, start, end) ->
        try {
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
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
    return editable
}


private fun String.getURL(start: Int, end: Int): String {
    return if (end <= length) {
        TakeNote.getURLFrom(substring(start, end))
    } else TakeNote.getURLFrom(substring(start, length))
}

private fun Spannable.setSpan(span: Any, start: Int, end: Int) {
    if (end <= length) {
        setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else setSpan(span, start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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


fun Menu.add(title: Int, drawable: Int, onClick: (item: MenuItem) -> Unit): MenuItem {
    val menuItem = add(title)
    menuItem.setIcon(drawable)
    menuItem.setOnMenuItemClickListener { item ->
        onClick(item)
        return@setOnMenuItemClickListener false
    }
    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    return menuItem
}