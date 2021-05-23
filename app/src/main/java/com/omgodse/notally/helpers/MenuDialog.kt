package com.omgodse.notally.helpers

import android.content.Context
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.omgodse.notally.databinding.MenuItemBinding

class MenuDialog(context: Context) : BottomSheetDialog(context) {

    private val linearLayout = LinearLayout(context)

    init {
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setContentView(linearLayout)
    }

    fun addItem(operation: Operation) = apply {
        val item = MenuItemBinding.inflate(layoutInflater).root
        item.setText(operation.textId)
        item.setOnClickListener {
            dismiss()
            operation.operation.invoke()
        }
        item.setCompoundDrawablesRelativeWithIntrinsicBounds(operation.drawableId, 0, 0, 0)
        linearLayout.addView(item)
    }

    data class Operation(val textId: Int, val drawableId: Int, val operation: () -> Unit)
}