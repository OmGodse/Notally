package com.omgodse.notally.helpers

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Operation

class MenuHelper(private val context: Context) {

    private val bottomSheetDialog = BottomSheetDialog(context)
    private val linearLayout = View.inflate(context, R.layout.dialog_options, null) as LinearLayout

    init {
        bottomSheetDialog.setContentView(linearLayout)
    }

    fun show() = bottomSheetDialog.show()

    fun addItem(@StringRes label: Int, @DrawableRes drawable: Int?, onClick: (() -> Unit)?) {
        val item = MaterialTextView(ContextThemeWrapper(context, R.style.Options_Button))
        item.setText(label)
        item.setOnClickListener {
            bottomSheetDialog.dismiss()
            if (onClick != null) {
                onClick()
            }
        }
        if (drawable != null) {
            item.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, 0, 0, 0)
        }
        linearLayout.addView(item)
    }

    fun addItem(label: String) {
        val item = MaterialTextView(ContextThemeWrapper(context, R.style.Modified_Detail))
        item.text = label
        linearLayout.addView(item)
    }

    fun addItem(operation: Operation) = addItem(operation.textId, operation.drawableId, operation.operation)
}