package com.omgodse.notally.miscellaneous

import android.content.res.Resources
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.RemoteViews
import android.widget.TextView
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.changehistory.ChangeHistory
import com.omgodse.notally.changehistory.EditTextChange
import com.omgodse.notally.recyclerview.ListManager
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.room.SpanRepresentation
import org.ocpsoft.prettytime.PrettyTime
import java.util.Date
import kotlin.math.roundToInt

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
    return add(Menu.NONE, title, drawable, MenuItem.SHOW_AS_ACTION_IF_ROOM, onClick)
}

fun Menu.add(
    title: Int,
    drawable: Int,
    showAsAction: Int,
    onClick: (item: MenuItem) -> Unit
): MenuItem {
    return add(Menu.NONE, title, drawable, showAsAction, onClick)
}

fun Menu.add(
    groupId: Int,
    title: Int,
    drawable: Int,
    showAsAction: Int,
    onClick: (item: MenuItem) -> Unit
): MenuItem {
    val menuItem = add(groupId, Menu.NONE, Menu.NONE, title)
    menuItem.setIcon(drawable)
    menuItem.setOnMenuItemClickListener { item ->
        onClick(item)
        return@setOnMenuItemClickListener false
    }
    menuItem.setShowAsAction(showAsAction)
    return menuItem
}

fun TextView.displayFormattedTimestamp(timestamp: Long, dateFormat: String) {
    if (dateFormat != com.omgodse.notally.preferences.DateFormat.none) {
        visibility = View.VISIBLE
        text = formatTimestamp(timestamp, dateFormat)
    } else visibility = View.GONE
}

fun RemoteViews.displayFormattedTimestamp(id: Int, timestamp: Long, dateFormat: String) {
    if (dateFormat != com.omgodse.notally.preferences.DateFormat.none) {
        setViewVisibility(id, View.VISIBLE)
        setTextViewText(id, formatTimestamp(timestamp, dateFormat))
    } else setViewVisibility(id, View.GONE)
}

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).roundToInt()

/**
 * Creates a TextWatcher for an EditText that is part of a list.
 * Everytime the text is changed, a Change is added to the ChangeHistory.
 *
 * @param positionGetter Function to determine the current position of the EditText in the list
 * (e.g. the current adapterPosition when using RecyclerViewer.Adapter)
 * @param updateModel Function to update the model. Is called on any text changes and on undo/redo.
 */
fun EditText.createListTextWatcherWithHistory(
    listManager: ListManager,
    positionGetter: () -> Int,
) = object : TextWatcher {
    private lateinit var currentTextBefore: String

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        currentTextBefore = s.toString()
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        listManager.changeText(
            this@createListTextWatcherWithHistory,
            this,
            positionGetter.invoke(),
            currentTextBefore,
            requireNotNull(s).toString()
        )

    }
}

fun EditText.createTextWatcherWithHistory(
    changeHistory: ChangeHistory,
    updateModel: (text: String) -> Unit
) =
    object : TextWatcher {
        private lateinit var currentTextBefore: String

        override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
            currentTextBefore = s.toString()
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            val textBefore = currentTextBefore
            val textAfter = requireNotNull(s).toString()
            updateModel.invoke(textAfter)

            changeHistory.push(
                EditTextChange(
                    this@createTextWatcherWithHistory,
                    textAfter,
                    textBefore,
                    this,
                    updateModel
                )
            )
        }
    }

fun MutableList<ListItem>.updateUncheckedPositions() {
    forEachIndexed { index, item -> if (!item.checked) item.uncheckedPosition = index }
}


private fun formatTimestamp(timestamp: Long, dateFormat: String): String {
    val date = Date(timestamp)
    return when (dateFormat) {
        com.omgodse.notally.preferences.DateFormat.relative -> PrettyTime().format(date)
        else -> java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(date)
    }
}
