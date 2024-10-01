package com.omgodse.notally.changehistory

import android.text.TextWatcher
import android.widget.EditText

class EditTextChange(
    private val editText: EditText,
    textBefore: String,
    textAfter: String,
    private val listener: TextWatcher,
    private val updateModel: (newValue: String) -> Unit,
) : ValueChange<String>(textAfter, textBefore) {

    private val cursorPosition = editText.selectionStart

    override fun update(value: String, isUndo: Boolean) {
        updateModel.invoke(value)
        editText.removeTextChangedListener(listener)
        editText.setText(value)
        editText.requestFocus()
        editText.setSelection(Math.max(0, cursorPosition - (if (isUndo) 1 else 0)))
        editText.addTextChangedListener(listener)
    }
}
