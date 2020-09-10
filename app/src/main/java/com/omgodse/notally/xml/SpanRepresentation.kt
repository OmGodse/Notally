package com.omgodse.notally.xml

data class SpanRepresentation(
    var isBold: Boolean,
    var isLink: Boolean,
    var isItalic: Boolean,
    var isMonospace: Boolean,
    var isStrikethrough: Boolean,
    var start: Int,
    var end: Int
) {

    fun isNotUseless(): Boolean {
        return isBold || isLink || isItalic || isMonospace || isStrikethrough
    }

    fun isEqualInSize(representation: SpanRepresentation): Boolean {
        return start == representation.start && end == representation.end
    }
}