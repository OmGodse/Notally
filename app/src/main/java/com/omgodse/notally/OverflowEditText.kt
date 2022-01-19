package com.omgodse.notally

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/**
 * Implementation that fixes a bug in Lollipop where clicking on the overflow icon
 * in the custom text selection mode causes it to end.
 * For more information, see this -> https://issuetracker.google.com/issues/36937508
 */
class OverflowEditText(context: Context, attrs: AttributeSet) : AppCompatEditText(context, attrs) {

    var isActionModeOn = false

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!isActionModeOn) {
            super.onWindowFocusChanged(hasWindowFocus)
        }
    }
}