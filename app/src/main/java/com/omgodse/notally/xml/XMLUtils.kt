package com.omgodse.notally.xml

import android.util.Xml
import com.omgodse.notally.room.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

object XMLUtils {

    fun readBaseNoteFromFile(file: File, folder: Folder): BaseNote {
        val inputStream = FileInputStream(file)
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(inputStream, null)
        parser.next()
        return parseBaseNote(parser, parser.name, folder)
    }


    fun readBackupFromStream(inputStream: InputStream): Backup {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(inputStream, null)

        var baseNotes = listOf<BaseNote>()
        var deletedNotes = listOf<BaseNote>()
        var archivedNotes = listOf<BaseNote>()
        val labels = HashSet<String>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    XMLTags.Notes -> baseNotes = parseList(parser, XMLTags.Notes, Folder.NOTES)
                    XMLTags.DeletedNotes -> deletedNotes = parseList(parser, XMLTags.DeletedNotes, Folder.DELETED)
                    XMLTags.ArchivedNotes -> archivedNotes = parseList(parser, XMLTags.ArchivedNotes, Folder.ARCHIVED)
                    XMLTags.Label -> labels.add(parser.nextText())
                }
            }
        }

        return Backup(baseNotes, deletedNotes, archivedNotes, labels)
    }

    private fun parseList(parser: XmlPullParser, rootTag: String, folder: Folder): List<BaseNote> {
        val list = ArrayList<BaseNote>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                val note = parseBaseNote(parser, parser.name, folder)
                list.add(note)
            } else if (parser.eventType == XmlPullParser.END_TAG) {
                if (parser.name == rootTag) {
                    break
                }
            }
        }

        return list
    }

    private fun parseBaseNote(parser: XmlPullParser, rootTag: String, folder: Folder): BaseNote {
        var body = String()
        var title = String()
        var timestamp = 0L
        var pinned = false
        val items = ArrayList<ListItem>()
        val phoneItems = ArrayList<PhoneItem>()


        val labels = HashSet<String>()
        val spans = ArrayList<SpanRepresentation>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    XMLTags.Title -> title = parser.nextText()
                    XMLTags.Body -> body = parser.nextText()
                    XMLTags.DateCreated -> timestamp = parser.nextText().toLong()
                    XMLTags.Pinned -> pinned = parser.nextText().toBoolean()
                    XMLTags.Label -> labels.add(parser.nextText())
                    XMLTags.ListItem -> items.add(parseItem(parser))
                    XMLTags.PhoneItem -> phoneItems.add(parsePhoneItem(parser))
                    XMLTags.Span -> spans.add(parseSpan(parser))
                }
            } else if (parser.eventType == XmlPullParser.END_TAG) {
                if (parser.name == rootTag) {
                    break
                }
            }
        }

        return when (rootTag) {
            XMLTags.Note -> {
                BaseNote.createNote(0, folder, title, pinned, timestamp, labels, body, spans)
            }
            XMLTags.List -> {
                BaseNote.createList(0, folder, title, pinned, timestamp, labels, items)
            }
            else -> {
                BaseNote.createPhoneNoList(0, folder, title, pinned, timestamp, labels, phoneItems)
            }
        }
    }


    fun writeBackupToStream(backup: Backup, stream: OutputStream) {
        val xmlSerializer = Xml.newSerializer()

        xmlSerializer.setOutput(stream, null)
        xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        xmlSerializer.startDocument("UTF-8", true)

        xmlSerializer.startTag(null, XMLTags.ExportedNotes)

        appendBackupList(XMLTags.Notes, xmlSerializer, backup.baseNotes)
        appendBackupList(XMLTags.ArchivedNotes, xmlSerializer, backup.archivedNotes)
        appendBackupList(XMLTags.DeletedNotes, xmlSerializer, backup.deletedNotes)

        backup.labels.forEach { label -> xmlSerializer.writeTagContent(XMLTags.Label, label) }

        xmlSerializer.endTag(null, XMLTags.ExportedNotes)

        xmlSerializer.endDocument()
    }

    fun writeBaseNoteToStream(baseNote: BaseNote, stream: OutputStream) {
        val xmlSerializer = Xml.newSerializer()

        xmlSerializer.setOutput(stream, null)
        xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        xmlSerializer.startDocument("UTF-8", true)

        when (baseNote.type) {
            Type.NOTE -> appendNote(baseNote, xmlSerializer)
            Type.LIST -> appendList(baseNote, xmlSerializer)
            Type.PHONE -> appendPhoneList(baseNote, xmlSerializer)
        }

        xmlSerializer.endDocument()
    }


    private fun appendNote(note: BaseNote, xmlSerializer: XmlSerializer) {
        xmlSerializer.startTag(null, XMLTags.Note)

        xmlSerializer.writeTagContent(XMLTags.DateCreated, note.timestamp.toString())
        xmlSerializer.writeTagContent(XMLTags.Pinned, note.pinned.toString())
        xmlSerializer.writeTagContent(XMLTags.Title, note.title)
        xmlSerializer.writeTagContent(XMLTags.Body, note.body)

        note.labels.forEach { label -> xmlSerializer.writeTagContent(XMLTags.Label, label) }

        note.spans.forEach { (bold, link, italic, monospace, strikethrough, start, end) ->
            xmlSerializer.startTag(null, XMLTags.Span)

            xmlSerializer.attribute(XMLTags.Start, start.toString())
            xmlSerializer.attribute(XMLTags.End, end.toString())

            if (bold) {
                xmlSerializer.attribute(XMLTags.Bold, bold.toString())
            }
            if (link) {
                xmlSerializer.attribute(XMLTags.Link, link.toString())
            }
            if (italic) {
                xmlSerializer.attribute(XMLTags.Italic, italic.toString())
            }
            if (monospace) {
                xmlSerializer.attribute(XMLTags.Monospace, monospace.toString())
            }
            if (strikethrough) {
                xmlSerializer.attribute(XMLTags.Strike, strikethrough.toString())
            }

            xmlSerializer.endTag(null, XMLTags.Span)
        }

        xmlSerializer.endTag(null, XMLTags.Note)
    }

    private fun appendList(list: BaseNote, xmlSerializer: XmlSerializer) {
        xmlSerializer.startTag(null, XMLTags.List)

        xmlSerializer.writeTagContent(XMLTags.DateCreated, list.timestamp.toString())
        xmlSerializer.writeTagContent(XMLTags.Pinned, list.pinned.toString())
        xmlSerializer.writeTagContent(XMLTags.Title, list.title)

        list.items.forEach { (body, checked) ->
            xmlSerializer.startTag(null, XMLTags.ListItem)

            xmlSerializer.writeTagContent(XMLTags.ListItemText, body)
            xmlSerializer.writeTagContent(XMLTags.ListItemChecked, checked.toString())

            xmlSerializer.endTag(null, XMLTags.ListItem)
        }

        list.labels.forEach { label -> xmlSerializer.writeTagContent(XMLTags.Label, label) }

        xmlSerializer.endTag(null, XMLTags.List)
    }

    private fun appendPhoneList(list: BaseNote, xmlSerializer: XmlSerializer) {
        xmlSerializer.startTag(null, XMLTags.Phone)

        xmlSerializer.writeTagContent(XMLTags.DateCreated, list.timestamp.toString())
        xmlSerializer.writeTagContent(XMLTags.Pinned, list.pinned.toString())
        xmlSerializer.writeTagContent(XMLTags.Title, list.title)

        list.phoneItems.forEach { (contactName, contactNo) ->
            xmlSerializer.startTag(null, XMLTags.PhoneItem)

            xmlSerializer.writeTagContent(XMLTags.PhoneItemContact, contactName)
            xmlSerializer.writeTagContent(XMLTags.PhoneItemNumber, contactNo)

            xmlSerializer.endTag(null, XMLTags.PhoneItem)
        }

        list.labels.forEach { label -> xmlSerializer.writeTagContent(XMLTags.Label, label) }

        xmlSerializer.endTag(null, XMLTags.Phone)
    }


    private fun appendBackupList(rootTag: String, xmlSerializer: XmlSerializer, list: List<BaseNote>) {
        if (list.isNotEmpty()) {
            xmlSerializer.startTag(null, rootTag)

            list.forEach { baseNote ->
                when (baseNote.type) {
                    Type.NOTE -> appendNote(baseNote, xmlSerializer)
                    Type.LIST -> appendList(baseNote, xmlSerializer)
                    Type.PHONE -> appendPhoneList(baseNote, xmlSerializer)
                }
            }

            xmlSerializer.endTag(null, rootTag)
        }
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

    private fun parsePhoneItem(parser: XmlPullParser): PhoneItem {
        val phoneItem = PhoneItem()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    XMLTags.PhoneItemContact -> phoneItem.contactName = parser.nextText()
                    XMLTags.PhoneItemNumber -> phoneItem.contactNo = parser.nextText()
                }
            } else if (parser.eventType == XmlPullParser.END_TAG) {
                if (parser.name == XMLTags.PhoneItem) {
                    break
                }
            }
        }

        return phoneItem
    }

    private fun parseSpan(parser: XmlPullParser): SpanRepresentation {
        val representation = SpanRepresentation(false, false, false, false, false, 0, 0)
        representation.start = parser.getAttributeValue(XMLTags.Start).toInt()
        representation.end = parser.getAttributeValue(XMLTags.End).toInt()
        representation.bold = parser.getAttributeValue(XMLTags.Bold)?.toBoolean() ?: false
        representation.link = parser.getAttributeValue(XMLTags.Link)?.toBoolean() ?: false
        representation.italic = parser.getAttributeValue(XMLTags.Italic)?.toBoolean() ?: false
        representation.monospace = parser.getAttributeValue(XMLTags.Monospace)?.toBoolean() ?: false
        representation.strikethrough = parser.getAttributeValue(XMLTags.Strike)?.toBoolean() ?: false
        return representation
    }


    private fun XmlSerializer.attribute(name: String, value: String) {
        attribute(null, name, value)
    }

    private fun XmlSerializer.writeTagContent(tag: String, content: String) {
        startTag(null, tag)
        text(content)
        endTag(null, tag)
    }

    private fun XmlPullParser.getAttributeValue(attribute: String) = getAttributeValue(null, attribute)
}