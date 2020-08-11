package com.omgodse.notally

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.Keep
import com.google.android.material.textfield.TextInputEditText

/*
Custom implementation that fixes a bug in Lollipop wherein clicking on the overflow icon
in the custom text selection mode causes the mode to end.
For more information, see this -> https://issuetracker.google.com/issues/36937508
 */

@Keep
class NotallyEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : TextInputEditText(context, attrs, defStyleAttr) {

    var isActionModeOn = false

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!isActionModeOn) {
            super.onWindowFocusChanged(hasWindowFocus)
        }
    }
}