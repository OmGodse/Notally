package com.omgodse.notally.helpers

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R

class MenuHelper(private val context: Context) {

    private val linearLayout = LinearLayout(context)
    private val bottomSheetDialog = BottomSheetDialog(context)

    init {
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        bottomSheetDialog.setContentView(linearLayout)
    }

    fun show() = bottomSheetDialog.show()

    fun addItem(operation: Operation) = apply {
        val item = MaterialTextView(ContextThemeWrapper(context, R.style.Option))
        item.setText(operation.textId)
        item.setOnClickListener {
            bottomSheetDialog.dismiss()
            operation.operation.invoke()
        }
        item.setCompoundDrawablesRelativeWithIntrinsicBounds(operation.drawableId, 0, 0, 0)
        linearLayout.addView(item)
    }

    data class Operation(val textId: Int, val drawableId: Int, val operation: () -> Unit)
}