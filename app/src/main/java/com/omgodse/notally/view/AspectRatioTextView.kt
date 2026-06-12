package com.omgodse.notally.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class AspectRatioTextView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val height = (width * 0.75).toInt()
        if (height != measuredHeight) {
            setMeasuredDimension(width, height)
        }
    }
}