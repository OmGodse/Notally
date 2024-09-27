package com.omgodse.notally.changehistory

import android.text.TextWatcher
import android.widget.EditText
import com.omgodse.notally.recyclerview.ListManager

open class ListEditTextChange(
    private val editText: EditText,
    position: Int,
    private val textBefore: String,
    private val textAfter: String,
    private val listener: TextWatcher,
    private val listManager: ListManager,
) : ListValueChange<String>(textAfter, textBefore, position) {
    private val cursorPosition = editText.selectionStart

    override fun update(position: Int, value: String, isUndo: Boolean) {
        listManager.changeText(position, value)
        editText.removeTextChangedListener(listener)
        editText.setText(value)
        editText.requestFocus()
        editText.setSelection(Math.max(0, cursorPosition - (if (isUndo) 1 else 0)))
        editText.addTextChangedListener(listener)
    }

    override fun toString(): String {
        return "CheckedText at $position from: $textBefore to: $textAfter"
    }

}