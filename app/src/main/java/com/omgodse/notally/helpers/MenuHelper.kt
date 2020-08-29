package com.omgodse.notally.helpers

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Operation

class MenuHelper(private val context: Context) {

    private val bottomSheetDialog = BottomSheetDialog(context)
    private val linearLayout = LinearLayout(context)

    init {
        linearLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        linearLayout.orientation = LinearLayout.VERTICAL
        bottomSheetDialog.setContentView(linearLayout)
    }

    fun show() = bottomSheetDialog.show()

    fun addItem(@StringRes label: Int, @DrawableRes drawable: Int, onClick: () -> Unit) {
        val item = MaterialTextView(ContextThemeWrapper(context, R.style.Options))
        item.setText(label)
        item.setOnClickListener {
            bottomSheetDialog.dismiss()
            onClick()
        }
        item.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, 0, 0, 0)
        linearLayout.addView(item)
    }

    fun addItem(operation: Operation) = addItem(operation.textId, operation.drawableId, operation.operation)
}