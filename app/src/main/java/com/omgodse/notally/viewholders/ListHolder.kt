package com.omgodse.notally.viewholders

import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.omgodse.notally.R
import com.omgodse.notally.interfaces.ListItemListener

class ListHolder(view: View, listItemListener: ListItemListener?) : RecyclerView.ViewHolder(view) {

    val listItem: TextInputEditText = view.findViewById(R.id.ListItem)
    val checkBox: MaterialCheckBox = view.findViewById(R.id.CheckBox)
    private val dragHandle: ImageView = view.findViewById(R.id.DragHandle)

    init {
        if (listItemListener != null) {
            listItem.setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    listItemListener.onMoveToNext(adapterPosition)
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }

            listItem.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    listItemListener.onMoveToNext(adapterPosition)
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    listItem.paintFlags = listItem.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    listItem.isEnabled = false
                } else {
                    listItem.paintFlags = listItem.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    listItem.isEnabled = true
                }
                listItemListener.onItemCheckedChange(adapterPosition, isChecked)
            }

            listItem.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                    listItemListener.onItemTextChange(adapterPosition, text.toString())
                }
            })

            dragHandle.setOnTouchListener { v, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    listItemListener.onStartDrag(this)
                }
                false
            }
        }
    }
}