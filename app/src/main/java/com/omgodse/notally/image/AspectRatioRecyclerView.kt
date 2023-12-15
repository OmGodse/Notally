package com.omgodse.notally.image

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class AspectRatioRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val height = (measuredWidth * 0.75).toInt()
        if (height != measuredHeight) {
            setMeasuredDimension(measuredWidth, height)
        }
    }
}