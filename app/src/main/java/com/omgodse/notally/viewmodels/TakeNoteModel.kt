package com.omgodse.notally.viewmodels

import android.graphics.Typeface
import android.text.Editable
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.omgodse.notally.miscellaneous.SpanRepresentation
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.xml.XMLReader
import com.omgodse.notally.xml.XMLTags
import com.omgodse.notally.xml.XMLWriter
import java.util.*
import kotlin.collections.HashSet

class TakeNoteModel : BaseModel() {

    var body = Editable.Factory.getInstance().newEditable(String())

    override fun saveNote() {
        file?.let {
            val xmlWriter = XMLWriter(XMLTags.Note, it)

            xmlWriter.start()
            xmlWriter.setTimestamp(timestamp.toString())
            xmlWriter.setTitle(title)
            xmlWriter.setBody(body.toString().trimEnd())
            xmlWriter.setSpans(getFilteredSpans())
            xmlWriter.setLabels(labels.value ?: HashSet())

            xmlWriter.end()
        }
    }

    override fun setStateFromFile() {
        file?.let { file ->
            if (file.exists()){
                val xmlReader = XMLReader(file)
                title = xmlReader.getTitle()
                timestamp = xmlReader.getTimestamp().toLong()
                body = xmlReader.getBody().applySpans(xmlReader.getSpans())
                labels.value = xmlReader.getLabels()
            }
        }
    }


    private fun getFilteredSpans(): ArrayList<SpanRepresentation> {
        val representations = LinkedHashSet<SpanRepresentation>()
        val spans = body.getSpans(0, body.length, Object::class.java)
        spans.forEach { span ->
            val end = body.getSpanEnd(span)
            val start = body.getSpanStart(span)
            val representation = SpanRepresentation(false, false, false, false, start, end)

            if (span is StyleSpan) {
                if (span.style == Typeface.BOLD) {
                    representation.isBold = true
                }
                else if (span.style == Typeface.ITALIC) {
                    representation.isItalic = true
                }
            }
            else if (span is TypefaceSpan) {
                if (span.family == "monospace") {
                    representation.isMonospace = true
                }
            }
            else if (span is StrikethroughSpan) {
                representation.isStrikethrough = true
            }

            if (representation.isNotUseless()) {
                representations.add(representation)
            }
        }
        return getFilteredRepresentations(ArrayList(representations))
    }

    private fun getFilteredRepresentations(representations: ArrayList<SpanRepresentation>): ArrayList<SpanRepresentation> {
        representations.forEachIndexed { index, representation ->
            val match = representations.find { spanRepresentation ->
                spanRepresentation.isEqualInSize(representation)
            }
            if (match != null && representations.indexOf(match) != index) {
                if (match.isBold) {
                    representation.isBold = true
                }
                if (match.isItalic) {
                    representation.isItalic = true
                }
                if (match.isMonospace) {
                    representation.isMonospace = true
                }
                if (match.isStrikethrough) {
                    representation.isStrikethrough = true
                }
                val copy = ArrayList(representations)
                copy[index] = representation
                copy.remove(match)
                return getFilteredRepresentations(copy)
            }
        }
        return representations
    }
}