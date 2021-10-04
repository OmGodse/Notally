package com.omgodse.notally.room

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONException
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

        fun fromJSONObject(jsonObject: JSONObject): SpanRepresentation {
            val bold = jsonObject.getSafeBoolean(boldTag)
            val link = jsonObject.getSafeBoolean(linkTag)
            val italic = jsonObject.getSafeBoolean(italicTag)
            val monospace = jsonObject.getSafeBoolean(monospaceTag)
            val strikethrough = jsonObject.getSafeBoolean(strikethroughTag)
            val start = jsonObject.getInt(startTag)
            val end = jsonObject.getInt(endTag)
            return SpanRepresentation(bold, link, italic, monospace, strikethrough, start, end)
        }

        private fun JSONObject.getSafeBoolean(name: String): Boolean {
            return try {
                getBoolean(name)
            } catch (exception: JSONException) {
                false
            }
        }
    }
}