package com.omgodse.notally.fragments

import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.omgodse.notally.databinding.MenuItemBinding

class Operation(val textId: Int, val drawableId: Int, val operation: () -> Unit)

fun Fragment.showMenu(vararg operations: Operation) {
    val context = requireContext()

    val linearLayout = LinearLayout(context)
    linearLayout.orientation = LinearLayout.VERTICAL
    linearLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

    val dialog = BottomSheetDialog(context)
    dialog.setContentView(linearLayout)

    for (operation in operations) {
        val item = MenuItemBinding.inflate(layoutInflater).root
        item.setText(operation.textId)
        item.setOnClickListener {
            dialog.dismiss()
            operation.operation.invoke()
        }
        item.setCompoundDrawablesRelativeWithIntrinsicBounds(operation.drawableId, 0, 0, 0)
        linearLayout.addView(item)
    }

    dialog.show()
}