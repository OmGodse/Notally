package com.omgodse.notally.miscellaneous

data class SpanRepresentation(var isBold: Boolean, var isItalic: Boolean, var isMonospace: Boolean, var isStrikethrough: Boolean, var start: Int, var end: Int) {
    fun isNotUseless(): Boolean {
        return isBold || isItalic || isMonospace || isStrikethrough
    }

    fun isEqualInSize(representation: SpanRepresentation) : Boolean {
        return start == representation.start && end == representation.end
    }
}