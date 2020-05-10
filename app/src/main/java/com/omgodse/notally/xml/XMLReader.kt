package com.omgodse.notally.xml

import com.omgodse.notally.miscellaneous.ListItem
import com.omgodse.notally.miscellaneous.SpanRepresentation
import com.omgodse.notally.miscellaneous.getAttributeValue
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File

class XMLReader(file: File) {

    private var isNote = true

    private var body = String()
    private var title = String()
    private var dateCreated = String()
    private val listItems = ArrayList<ListItem>()

    private val labels = HashSet<String>()
    private val spans = ArrayList<SpanRepresentation>()

    init {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(file.inputStream(), null)
        parseFile(parser)
    }

    private fun parseFile(parser: XmlPullParser) {
        var listItem = ListItem(String(), false)

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    XMLTags.List -> isNote = false
                    XMLTags.Title -> title = parser.nextText()
                    XMLTags.Body -> body = parser.nextText()
                    XMLTags.DateCreated -> dateCreated = parser.nextText()
                    XMLTags.Label -> labels.add(parser.nextText())
                    XMLTags.ListItemText -> listItem.body = parser.nextText()
                    XMLTags.ListItemChecked -> listItem.checked = parser.nextText()?.toBoolean() ?: false
                    XMLTags.Span -> parseSpan(parser)
                }
            } else if (parser.eventType == XmlPullParser.END_TAG) {
                if (parser.name == XMLTags.ListItem) {
                    listItems.add(listItem)
                    listItem = ListItem(String(), false)
                }
            }
        }
    }

    private fun parseSpan(parser: XmlPullParser) {
        val representation = SpanRepresentation(false, false, false, false, 0, 0)
        representation.start = parser.getAttributeValue(XMLTags.Start).toInt()
        representation.end = parser.getAttributeValue(XMLTags.End).toInt()
        representation.isBold = parser.getAttributeValue(XMLTags.Bold)?.toBoolean() ?: false
        representation.isItalic = parser.getAttributeValue(XMLTags.Italic)?.toBoolean() ?: false
        representation.isMonospace = parser.getAttributeValue(XMLTags.Monospace)?.toBoolean() ?: false
        representation.isStrikethrough = parser.getAttributeValue(XMLTags.Strike)?.toBoolean() ?: false
        spans.add(representation)
    }


    fun isNote() = isNote

    fun getBody() = body

    fun getTitle() = title

    fun getListItems() = listItems

    fun getDateCreated() = dateCreated

    fun getSpans() = spans

    fun getLabels() = labels
}