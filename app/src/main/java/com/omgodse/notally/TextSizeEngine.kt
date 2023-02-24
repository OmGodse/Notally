package com.omgodse.notally

import com.omgodse.notally.preferences.TextSize

object TextSizeEngine {

    fun getDisplayBodySize(textSize: String): Float {
        return when (textSize) {
            TextSize.small -> 12f
            TextSize.medium -> 14f
            TextSize.large -> 16f
            else -> throw IllegalArgumentException("Invalid value : $textSize")
        }
    }

    fun getDisplayTitleSize(textSize: String): Float {
        return when (textSize) {
            TextSize.small -> 14f
            TextSize.medium -> 16f
            TextSize.large -> 18f
            else -> throw IllegalArgumentException("Invalid value : $textSize")
        }
    }

    fun getEditBodySize(textSize: String): Float {
        return when (textSize) {
            TextSize.small -> 14f
            TextSize.medium -> 16f
            TextSize.large -> 18f
            else -> throw IllegalArgumentException("Invalid value : $textSize")
        }
    }

    fun getEditTitleSize(textSize: String): Float {
        return when (textSize) {
            TextSize.small -> 18f
            TextSize.medium -> 20f
            TextSize.large -> 22f
            else -> throw IllegalArgumentException("Invalid value : $textSize")
        }
    }
}