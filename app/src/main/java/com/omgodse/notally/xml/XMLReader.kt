package com.omgodse.notally.xml

import com.omgodse.notally.miscellaneous.ListItem
import com.omgodse.notally.miscellaneous.Note
import com.omgodse.notally.miscellaneous.SpanRepresentation
import com.omgodse.notally.miscellaneous.getAttributeValue
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File

class XMLReader(file: File) {

    private val note: Note

    init {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(file.inputStream(), null)
        parser.next()

        note = parseNote(parser, parser.name)
    }

    fun isNote() = note.isNote

    fun getBody() = note.body

    fun getTitle() = note.title

    fun getListItems() = note.items

    fun getTimestamp() = note.timestamp

    fun getSpans() = note.spans

    fun getLabels() = note.labels

    companion object {
        fun parseNote(parser: XmlPullParser, rootTag: String): Note {
            var isNote = rootTag == XMLTags.Note

            var body = String()
            var title = String()
            var timestamp = String()
            val listItems = ArrayList<ListItem>()

            val labels = HashSet<String>()
            val spans = ArrayList<SpanRepresentation>()

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        XMLTags.List -> isNote = false
                        XMLTags.Title -> title = parser.nextText()
                        XMLTags.Body -> body = parser.nextText()
                        XMLTags.DateCreated -> timestamp = parser.nextText()
                        XMLTags.Label -> labels.add(parser.nextText())
                        XMLTags.ListItem -> listItems.add(parseItem(parser))
                        XMLTags.Span -> spans.add(parseSpan(parser))
                    }
                } else if (parser.eventType == XmlPullParser.END_TAG) {
                    if (parser.name == rootTag) {
                        break
                    }
                }
            }

            return Note(isNote, title, timestamp, body, listItems, spans, labels, String())
        }

        private fun parseItem(parser: XmlPullParser): ListItem {
            val listItem = ListItem(String(), false)

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        XMLTags.ListItemText -> listItem.body = parser.nextText()
                        XMLTags.ListItemChecked -> listItem.checked = parser.nextText()?.toBoolean() ?: false
                    }
                } else if (parser.eventType == XmlPullParser.END_TAG) {
                    if (parser.name == XMLTags.ListItem) {
                        break
                    }
                }
            }

            return listItem
        }

        private fun parseSpan(parser: XmlPullParser): SpanRepresentation {
            val representation = SpanRepresentation(false, false, false, false, 0, 0)
            representation.start = parser.getAttributeValue(XMLTags.Start).toInt()
            representation.end = parser.getAttributeValue(XMLTags.End).toInt()
            representation.isBold = parser.getAttributeValue(XMLTags.Bold)?.toBoolean() ?: false
            representation.isItalic = parser.getAttributeValue(XMLTags.Italic)?.toBoolean() ?: false
            representation.isMonospace = parser.getAttributeValue(XMLTags.Monospace)?.toBoolean() ?: false
            representation.isStrikethrough = parser.getAttributeValue(XMLTags.Strike)?.toBoolean() ?: false
            return representation
        }
    }
}