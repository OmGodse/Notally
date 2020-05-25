package com.omgodse.notally.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.omgodse.notally.R
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.Note
import com.omgodse.notally.xml.XMLTags
import com.omgodse.notally.xml.XMLWriter
import kotlinx.coroutines.launch
import java.io.File

class NoteModel(private val app: Application) : AndroidViewModel(app) {

    private val notes = ArrayList<Note>()
    val observableNotes = MutableLiveData(ArrayList<Note>())

    private val archivedNotes = ArrayList<Note>()
    val observableArchivedNotes = MutableLiveData(ArrayList<Note>())

    private val deletedNotes = ArrayList<Note>()
    val observableDeletedNotes = MutableLiveData(ArrayList<Note>())

    var keyword = String()
    private val searchResults = ArrayList<Note>()
    val observableSearchResults = MutableLiveData(ArrayList<Note>())

    var label = String()
    private val labelledNotes = ArrayList<Note>()
    val observableLabelledNotes = MutableLiveData(ArrayList<Note>())

    private val notesHelper = NotesHelper(app)
    private val settingsHelper = SettingsHelper(app)

    init {
        fetchNotes()
        fetchDeletedNotes()
        fetchArchivedNotes()
    }

    fun handleNoteEdited(filePath: String?, payload: String) {
        filePath?.let {
            val list = getRelevantList(payload)
            val liveData = getRelevantLiveData(payload)

            val file = File(filePath)
            val note = list.find { note -> note.filePath == filePath }
            val position = list.indexOf(note)
            val editedNote = NotesHelper.convertFileToNote(file)

            list[position] = editedNote
            liveData.value = list
        }
    }

    fun handleNoteCreated(filePath: String?, payload: String) {
        filePath?.let {
            val list = getRelevantList(payload)
            val liveData = getRelevantLiveData(payload)

            val file = File(filePath)
            if (file.exists()) {
                val note = NotesHelper.convertFileToNote(file)

                if (settingsHelper.getSortingPreferences() == app.getString(R.string.newestFirstKey)) {
                    list.add(0, note)
                } else list.add(note)

                liveData.value = list
            }
        }
    }


    fun handleNoteDeleted(filePath: String?, payload: String) {
        filePath?.let {
            val list = getRelevantList(payload)
            val liveData = getRelevantLiveData(payload)

            val file = File(filePath)
            val note = list.find { note -> note.filePath == filePath }

            if (notesHelper.moveFileToDeleted(file)){
                list.remove(note)
                liveData.value = list
            }
        }
    }

    fun handleNoteArchived(filePath: String?, payload: String) {
        filePath?.let {
            val list = getRelevantList(payload)
            val liveData = getRelevantLiveData(payload)

            val file = File(filePath)
            val note = list.find { note -> note.filePath == filePath }

            if (notesHelper.moveFileToArchive(file)) {
                list.remove(note)
                liveData.value = list
            }
        }
    }


    fun handleNoteRestored(filePath: String?, payload: String) {
        filePath?.let {
            val list = getRelevantList(payload)
            val liveData = getRelevantLiveData(payload)

            val file = File(filePath)
            val note = list.find { note -> note.filePath == filePath }

            if (notesHelper.restoreFile(file)) {
                list.remove(note)
                liveData.value = list
            }
        }
    }

    fun handleNoteDeletedForever(filePath: String?, payload: String) {
        filePath?.let {
            val list = getRelevantList(payload)
            val liveData = getRelevantLiveData(payload)

            val file = File(filePath)
            val note = list.find { note -> note.filePath == filePath }

            if (file.delete()) {
                list.remove(note)
                liveData.value = list
            }
        }
    }


    fun fetchRelevantNotes(payload: String) {
        when (payload) {
            NOTES -> fetchNotes()
            DELETED_NOTES -> fetchDeletedNotes()
            ARCHIVED_NOTES -> fetchArchivedNotes()
            SEARCH_RESULTS -> fetchSearchResults()
            LABELLED_NOTES -> fetchLabelledNotes()
        }
    }

    private fun getRelevantList(payload: String) : ArrayList<Note> {
        return when (payload) {
            NOTES -> notes
            ARCHIVED_NOTES -> archivedNotes
            DELETED_NOTES -> deletedNotes
            SEARCH_RESULTS -> searchResults
            LABELLED_NOTES -> labelledNotes
            else -> notes
        }
    }

    private fun getRelevantLiveData(payload: String) : MutableLiveData<ArrayList<Note>> {
        return when (payload) {
            NOTES -> observableNotes
            ARCHIVED_NOTES -> observableArchivedNotes
            DELETED_NOTES -> observableDeletedNotes
            SEARCH_RESULTS -> observableSearchResults
            LABELLED_NOTES -> observableLabelledNotes
            else -> observableNotes
        }
    }


    private fun fetchNotes() {
        viewModelScope.launch {
            val files = getSortedFiles(notesHelper.getNotePath())

            notes.clear()
            notes.addAll(convertFilesToNotes(files))

            observableNotes.value = notes
        }
    }

    private fun fetchDeletedNotes() {
        viewModelScope.launch {
            val files = getSortedFiles(notesHelper.getDeletedPath())

            deletedNotes.clear()
            deletedNotes.addAll(convertFilesToNotes(files))

            observableDeletedNotes.value = deletedNotes
        }
    }

    private fun fetchArchivedNotes() {
        viewModelScope.launch {
            val files = getSortedFiles(notesHelper.getArchivedPath())

            archivedNotes.clear()
            archivedNotes.addAll(convertFilesToNotes(files))

            observableArchivedNotes.value = archivedNotes
        }
    }

    private fun fetchSearchResults() {
        searchResults.clear()

        if (keyword.isEmpty()){
            observableSearchResults.value = searchResults
            return
        }

        viewModelScope.launch {
            val files = getSortedFiles(notesHelper.getNotePath())
            val notes = convertFilesToNotes(files)

            notes.forEach { note ->
                if (isNoteMatch(note)){
                    searchResults.add(note)
                }
            }

            observableSearchResults.value = searchResults
        }
    }

    private fun fetchLabelledNotes() {
        labelledNotes.clear()

        if (label.isEmpty()){
            observableLabelledNotes.value = labelledNotes
            return
        }

        viewModelScope.launch {
            val files = getSortedFiles(notesHelper.getNotePath())
            val notes = convertFilesToNotes(files)

            notes.forEach { note ->
                if (note.labels.contains(label)){
                    labelledNotes.add(note)
                }
            }

            observableLabelledNotes.value = labelledNotes
        }
    }


    fun isNoteMatch(note: Note): Boolean {
        note.labels.forEach { label ->
            if (label.contains(keyword, true)) {
                return true
            }
        }

        note.items.forEach { item ->
            if (item.body.contains(keyword, true)) {
                return true
            }
        }

        return if (note.body.contains(keyword, true)) {
            true
        } else note.title.contains(keyword, true)
    }

    fun editNoteLabel(note: Note, labels: HashSet<String>, payload: String) {
        val file = File(note.filePath)

        val xmlWriter: XMLWriter
        if (note.isNote) {
            xmlWriter = XMLWriter(XMLTags.Note, file)
            xmlWriter.start()
            xmlWriter.setTimestamp(note.timestamp)
            xmlWriter.setTitle(note.title)
            xmlWriter.setBody(note.body)
            xmlWriter.setSpans(note.spans)
        } else {
            xmlWriter = XMLWriter(XMLTags.List, file)
            xmlWriter.start()
            xmlWriter.setTimestamp(note.timestamp)
            xmlWriter.setTitle(note.title)
            xmlWriter.setListItems(note.items)
        }

        xmlWriter.setLabels(labels)

        xmlWriter.end()

        handleNoteEdited(note.filePath, payload)
    }


    private fun getSortedFiles(path: File) : ArrayList<File> {
        val files = ArrayList<File>()
        path.listFiles()?.toCollection(files)

        files.sortWith(Comparator { firstFile, secondFile ->
            firstFile.name.compareTo(secondFile.name)
        })

        if (settingsHelper.getSortingPreferences() == app.getString(R.string.newestFirstKey)) {
            files.reverse()
        }

        return files
    }

    private fun convertFilesToNotes(files: ArrayList<File>) : ArrayList<Note> {
        val notes = ArrayList<Note>()
        files.forEach { file ->
            val note = NotesHelper.convertFileToNote(file)
            notes.add(note)
        }
        return notes
    }

    companion object {
        const val NOTES = "NOTES"
        const val DELETED_NOTES = "DELETED_NOTES"
        const val ARCHIVED_NOTES = "ARCHIVED_NOTES"
        const val SEARCH_RESULTS = "SEARCH_RESULTS"
        const val LABELLED_NOTES = "LABELLED_NOTES"
    }
}