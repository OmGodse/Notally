package com.omgodse.notally.recyclerview

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ItemDecoration(private val margin: Int, private val columns: Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)

        outRect.left = margin / 2
        outRect.right = margin / 2

        if (position < columns) {
            outRect.top = margin
        }
        outRect.bottom = margin
    }
}