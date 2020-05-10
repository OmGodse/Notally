package com.omgodse.notally.xml

import android.util.Xml
import com.omgodse.notally.miscellaneous.ListItem
import com.omgodse.notally.miscellaneous.SpanRepresentation
import java.io.StringWriter

class XMLWriter(private val tag: String) {

    private var stringWriter = StringWriter()
    private var xmlSerializer = Xml.newSerializer()

    fun startNote() {
        xmlSerializer.setOutput(stringWriter)
        xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        xmlSerializer.startDocument("UTF-8", true)
        xmlSerializer.startTag(null, tag)
    }

    fun setDateCreated(date: String) {
        xmlSerializer.startTag(null, XMLTags.DateCreated)
        xmlSerializer.text(date)
        xmlSerializer.endTag(null, XMLTags.DateCreated)
    }

    fun setTitle(title: String) {
        xmlSerializer.startTag(null, XMLTags.Title)
        xmlSerializer.text(title)
        xmlSerializer.endTag(null, XMLTags.Title)
    }

    fun setBody(body: String) {
        xmlSerializer.startTag(null, XMLTags.Body)
        xmlSerializer.text(body)
        xmlSerializer.endTag(null, XMLTags.Body)
    }

    fun setListItems(items: List<ListItem>) {
        items.forEach { listItem ->
            xmlSerializer.startTag(null, XMLTags.ListItem)

            xmlSerializer.startTag(null, XMLTags.ListItemText)
            xmlSerializer.text(listItem.body)
            xmlSerializer.endTag(null, XMLTags.ListItemText)

            xmlSerializer.startTag(null, XMLTags.ListItemChecked)
            xmlSerializer.text(listItem.checked.toString())
            xmlSerializer.endTag(null, XMLTags.ListItemChecked)

            xmlSerializer.endTag(null, XMLTags.ListItem)
        }
    }

    fun setLabels(labels: HashSet<String>?) {
        labels?.forEach { label ->
            xmlSerializer.startTag(null, XMLTags.Label)
            xmlSerializer.text(label)
            xmlSerializer.endTag(null, XMLTags.Label)
        }
    }

    fun setSpans(spans: ArrayList<SpanRepresentation>) {
        spans.forEach { spanRepresentation ->
            xmlSerializer.startTag(null, XMLTags.Span)

            xmlSerializer.attribute(null, XMLTags.Start, spanRepresentation.start.toString())
            xmlSerializer.attribute(null, XMLTags.End, spanRepresentation.end.toString())

            if (spanRepresentation.isBold) {
                xmlSerializer.attribute(null, XMLTags.Bold, spanRepresentation.isBold.toString())
            }
            if (spanRepresentation.isItalic) {
                xmlSerializer.attribute(null, XMLTags.Italic, spanRepresentation.isItalic.toString())
            }
            if (spanRepresentation.isMonospace) {
                xmlSerializer.attribute(null, XMLTags.Monospace, spanRepresentation.isMonospace.toString())
            }
            if (spanRepresentation.isStrikethrough) {
                xmlSerializer.attribute(null, XMLTags.Strike, spanRepresentation.isStrikethrough.toString())
            }

            xmlSerializer.endTag(null, XMLTags.Span)
        }
    }

    fun endNote() {
        xmlSerializer.endTag(null, tag)
        xmlSerializer.endDocument()
    }

    fun getNote(): String {
        return stringWriter.toString()
    }
}