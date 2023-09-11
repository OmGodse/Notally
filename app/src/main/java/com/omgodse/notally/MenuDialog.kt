package com.omgodse.notally

import android.content.Context
import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.omgodse.notally.databinding.MenuItemBinding

class MenuDialog(context: Context) : BottomSheetDialog(context) {

    private val linearLayout: LinearLayout

    init {
        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        linearLayout = LinearLayout(context)
        linearLayout.layoutParams = params
        linearLayout.orientation = LinearLayout.VERTICAL

        val scrollView = NestedScrollView(context)
        scrollView.layoutParams = params

        scrollView.addView(linearLayout)
        setContentView(scrollView)
    }

    fun add(title: String, drawable: Int = 0, onClick: () -> Unit): MenuDialog {
        val item = MenuItemBinding.inflate(layoutInflater, linearLayout, true).root
        item.text = title
        item.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, 0, 0, 0)
        item.setOnClickListener {
            dismiss()
            onClick()
        }
        return this
    }

    fun add(title: Int, drawable: Int = 0, onClick: () -> Unit) = add(context.getString(title), drawable, onClick)
}