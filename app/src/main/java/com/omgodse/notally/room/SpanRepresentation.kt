package com.omgodse.notally.room

import android.os.Parcelable
import com.omgodse.notally.room.json.SafeJSONObject
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class SpanRepresentation(
    var bold: Boolean,
    var link: Boolean,
    var italic: Boolean,
    var monospace: Boolean,
    var strikethrough: Boolean,
    var start: Int,
    var end: Int
) : Parcelable {

    fun isNotUseless(): Boolean {
        return bold || link || italic || monospace || strikethrough
    }

    fun isEqualInSize(representation: SpanRepresentation): Boolean {
        return start == representation.start && end == representation.end
    }

    fun toJSONObject(): JSONObject {
        return JSONObject()
            .put(boldTag, bold)
            .put(linkTag, link)
            .put(italicTag, italic)
            .put(monospaceTag, monospace)
            .put(strikethroughTag, strikethrough)
            .put(startTag, start)
            .put(endTag, end)
    }

    companion object {

        private const val boldTag = "bold"
        private const val linkTag = "link"
        private const val italicTag = "italic"
        private const val monospaceTag = "monospace"
        private const val strikethroughTag = "strikethrough"
        private const val startTag = "start"
        private const val endTag = "end"

        fun fromJSONObject(unsafeObject: JSONObject): SpanRepresentation {
            val safeObject = SafeJSONObject(unsafeObject.toString())
            val bold = safeObject.getBoolean(boldTag)
            val link = safeObject.getBoolean(linkTag)
            val italic = safeObject.getBoolean(italicTag)
            val monospace = safeObject.getBoolean(monospaceTag)
            val strikethrough = safeObject.getBoolean(strikethroughTag)
            val start = safeObject.getInt(startTag)
            val end = safeObject.getInt(endTag)
            return SpanRepresentation(bold, link, italic, monospace, strikethrough, start, end)
        }
    }
}