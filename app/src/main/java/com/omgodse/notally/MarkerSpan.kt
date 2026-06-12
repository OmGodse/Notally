package com.omgodse.notally

import android.text.NoCopySpan
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance

class MarkerSpan(var color: Int) : NoCopySpan, UpdateAppearance, CharacterStyle() {

    override fun updateDrawState(paint: TextPaint) {
        paint.bgColor = color
    }
}