package com.omgodse.notally.fragments

import android.view.ViewGroup.LayoutParams
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.omgodse.notally.databinding.MenuItemBinding

class Operation(val textId: Int, val drawableId: Int = 0, val operation: () -> Unit)

fun Fragment.showMenu(vararg operations: Operation) {
    val context = requireContext()

    val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

    val scrollView = NestedScrollView(context)
    val linearLayout = LinearLayout(context)
    linearLayout.orientation = LinearLayout.VERTICAL
    scrollView.addView(linearLayout, params)

    val dialog = BottomSheetDialog(context)
    dialog.setContentView(scrollView, params)

    for (operation in operations) {
        val item = MenuItemBinding.inflate(layoutInflater).root
        item.setText(operation.textId)
        item.setOnClickListener {
            dialog.dismiss()
            operation.operation()
        }
        item.setCompoundDrawablesRelativeWithIntrinsicBounds(operation.drawableId, 0, 0, 0)
        linearLayout.addView(item)
    }

    dialog.show()
}