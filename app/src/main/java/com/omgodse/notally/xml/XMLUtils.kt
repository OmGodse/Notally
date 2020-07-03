package com.omgodse.notally.xml

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.InputStream

class XMLUtils {

    companion object {
        fun readBaseNoteFromFile(file: File): BaseNote {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(file.inputStream(), null)
            parser.next()
            return when (val baseNote = parseBaseNote(parser, parser.name)) {
                is Note -> baseNote.copy(filePath = file.path)
                is List -> baseNote.copy(filePath = file.path)
            }
        }


        fun writeNoteToFile(note: Note) {
            val file = File(note.filePath)
            val xmlSerializer = Xml.newSerializer()

            xmlSerializer.setOutput(file.outputStream(), null)
            xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            xmlSerializer.startDocument("UTF-8", true)

            appendNote(note, xmlSerializer)

            xmlSerializer.endDocument()
        }

        fun writeListToFile(list: List) {
            val file = File(list.filePath)
            val xmlSerializer = Xml.newSerializer()

            xmlSerializer.setOutput(file.outputStream(), null)
            xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            xmlSerializer.startDocument("UTF-8", true)

            appendList(list, xmlSerializer)

            xmlSerializer.endDocument()
        }


        fun readBackupFromFile(inputStream: InputStream): Backup {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(inputStream, null)

            var baseNotes = ArrayList<BaseNote>()
            var deletedNotes = ArrayList<BaseNote>()
            var archivedNotes = ArrayList<BaseNote>()
            val labels = HashSet<String>()

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        XMLTags.Notes -> baseNotes = parseList(parser, XMLTags.Notes)
                        XMLTags.DeletedNotes -> deletedNotes = parseList(parser, XMLTags.DeletedNotes)
                        XMLTags.ArchivedNotes -> archivedNotes = parseList(parser, XMLTags.ArchivedNotes)
                        XMLTags.Label -> labels.add(parser.nextText())
                    }
                }
            }

            return Backup(baseNotes, deletedNotes, archivedNotes, labels)
        }


        fun writeBackupToFile(backup: Backup, file: File) {
            val xmlSerializer = Xml.newSerializer()

            xmlSerializer.setOutput(file.outputStream(), null)
            xmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
            xmlSerializer.startDocument("UTF-8", true)

            xmlSerializer.startTag(null, XMLTags.ExportedNotes)

            appendBackupList(XMLTags.Notes, xmlSerializer, backup.baseNotes)
            appendBackupList(XMLTags.ArchivedNotes, xmlSerializer, backup.archivedBaseNotes)
            appendBackupList(XMLTags.DeletedNotes, xmlSerializer, backup.deletedBaseNotes)

            backup.labels.forEach { label ->
                xmlSerializer.writeTagContent(XMLTags.Label, label)
            }

            xmlSerializer.endTag(null, XMLTags.ExportedNotes)

            xmlSerializer.endDocument()
        }

        private fun appendNote(note: Note, xmlSerializer: XmlSerializer) {
            xmlSerializer.startTag(null, XMLTags.Note)

            xmlSerializer.writeTagContent(XMLTags.DateCreated, note.timestamp)
            xmlSerializer.writeTagContent(XMLTags.TimeModified, note.timeModified)
            xmlSerializer.writeTagContent(XMLTags.Title, note.title)
            xmlSerializer.writeTagContent(XMLTags.Body, note.body)

            note.labels.forEach { label -> xmlSerializer.writeTagContent(XMLTags.Label, label) }

            note.spans.forEach { spanRepresentation ->
                xmlSerializer.startTag(null, XMLTags.Span)

                xmlSerializer.attribute(XMLTags.Start, spanRepresentation.start.toString())
                xmlSerializer.attribute(XMLTags.End, spanRepresentation.end.toString())

                if (spanRepresentation.isBold) {
                    xmlSerializer.attribute(XMLTags.Bold, spanRepresentation.isBold.toString())
                }
                if (spanRepresentation.isItalic) {
                    xmlSerializer.attribute(XMLTags.Italic, spanRepresentation.isItalic.toString())
                }
                if (spanRepresentation.isMonospace) {
                    xmlSerializer.attribute(XMLTags.Monospace, spanRepresentation.isMonospace.toString())
                }
                if (spanRepresentation.isStrikethrough) {
                    xmlSerializer.attribute(XMLTags.Strike, spanRepresentation.isStrikethrough.toString())
                }

                xmlSerializer.endTag(null, XMLTags.Span)
            }

            xmlSerializer.endTag(null, XMLTags.Note)
        }

        private fun appendList(list: List, xmlSerializer: XmlSerializer) {
            xmlSerializer.startTag(null, XMLTags.List)

            xmlSerializer.writeTagContent(XMLTags.DateCreated, list.timestamp)
            xmlSerializer.writeTagContent(XMLTags.TimeModified, list.timeModified)
            xmlSerializer.writeTagContent(XMLTags.Title, list.title)

            list.items.forEach { listItem ->
                xmlSerializer.startTag(null, XMLTags.ListItem)

                xmlSerializer.writeTagContent(XMLTags.ListItemText, listItem.body)
                xmlSerializer.writeTagContent(XMLTags.ListItemChecked, listItem.checked.toString())

                xmlSerializer.endTag(null, XMLTags.ListItem)
            }

            list.labels.forEach { label -> xmlSerializer.writeTagContent(XMLTags.Label, label) }

            xmlSerializer.endTag(null, XMLTags.List)
        }

        private fun appendBackupList(rootTag: String, xmlSerializer: XmlSerializer, list: ArrayList<BaseNote>) {
            if (list.isNotEmpty()) {
                xmlSerializer.startTag(null, rootTag)

                list.forEach { baseNote ->
                    when (baseNote) {
                        is Note -> appendNote(baseNote, xmlSerializer)
                        is List -> appendList(baseNote, xmlSerializer)
                    }
                }

                xmlSerializer.endTag(null, rootTag)
            }
        }


        private fun parseList(parser: XmlPullParser, rootTag: String): ArrayList<BaseNote> {
            val notes = ArrayList<BaseNote>()

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    val note = parseBaseNote(parser, parser.name)
                    notes.add(note)
                } else if (parser.eventType == XmlPullParser.END_TAG) {
                    if (parser.name == rootTag) {
                        break
                    }
                }
            }

            return notes
        }

        private fun parseBaseNote(parser: XmlPullParser, rootTag: String): BaseNote {
            var body = String()
            var title = String()
            var timestamp = String()
            var timeModified = String()
            val listItems = ArrayList<ListItem>()

            val labels = HashSet<String>()
            val spans = ArrayList<SpanRepresentation>()

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        XMLTags.Title -> title = parser.nextText()
                        XMLTags.Body -> body = parser.nextText()
                        XMLTags.DateCreated -> timestamp = parser.nextText()
                        XMLTags.TimeModified -> timeModified = parser.nextText()
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

            return if (rootTag == XMLTags.Note) {
                Note(title, String(), labels, timestamp, timeModified, body, spans)
            } else List(title, String(), labels, timestamp, timeModified, listItems)
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
}