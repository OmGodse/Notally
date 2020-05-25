package com.omgodse.notally.xml

import com.omgodse.notally.miscellaneous.Backup
import com.omgodse.notally.miscellaneous.Note
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class BackupReader(inputStream: InputStream) {

    private val backup: Backup

    init {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(inputStream, null)
        backup = parseBackup(parser)
    }

    private fun parseBackup(parser: XmlPullParser): Backup {
        var notes = ArrayList<Note>()
        var deletedNotes = ArrayList<Note>()
        var archivedNotes = ArrayList<Note>()
        val labels = HashSet<String>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    XMLTags.Notes -> notes = parseList(parser, XMLTags.Notes)
                    XMLTags.DeletedNotes -> deletedNotes = parseList(parser, XMLTags.DeletedNotes)
                    XMLTags.ArchivedNotes -> archivedNotes = parseList(parser, XMLTags.ArchivedNotes)
                    XMLTags.Label -> labels.add(parser.nextText())
                }
            }
        }

        return Backup(notes, deletedNotes, archivedNotes, labels)
    }

    private fun parseList(parser: XmlPullParser, rootTag: String): ArrayList<Note> {
        val notes = ArrayList<Note>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                val note = XMLReader.parseNote(parser, parser.name)
                notes.add(note)
            } else if (parser.eventType == XmlPullParser.END_TAG) {
                if (parser.name == rootTag) {
                    break
                }
            }
        }

        return notes
    }

    fun getBackup() = backup
}