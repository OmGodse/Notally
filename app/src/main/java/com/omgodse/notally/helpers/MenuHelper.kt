package com.omgodse.notally.helpers

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.children
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.interfaces.DialogListener

class MenuHelper(private val context: Context) {

    private val bottomSheetDialog = BottomSheetDialog(context)
    private val linearLayout = View.inflate(context, R.layout.dialog_options, null) as LinearLayout

    init {
        bottomSheetDialog.setContentView(linearLayout)
    }

    fun show() {
        bottomSheetDialog.show()
    }

    fun setListener(dialogListener: DialogListener) {
        linearLayout.children.forEach { item ->
            item.setOnClickListener {
                bottomSheetDialog.dismiss()
                dialogListener.onDialogItemClicked((item as MaterialTextView).text.toString())
            }
        }
    }

    fun addItem(@StringRes label: Int, @DrawableRes drawable: Int) {
        val item = MaterialTextView(ContextThemeWrapper(context, R.style.Options_Button))
        item.setText(label)
        item.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, 0, 0, 0)
        linearLayout.addView(item)
    }
}