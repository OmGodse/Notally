package com.omgodse.notally.viewmodels

import android.app.Application
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.List
import com.omgodse.notally.xml.Note
import kotlinx.coroutines.launch
import java.io.File

class BaseNoteModel(app: Application) : AndroidViewModel(app) {

    val notes = MutableLiveData(ArrayList<BaseNote>())
    val archivedNotes = MutableLiveData(ArrayList<BaseNote>())
    val deletedNotes = MutableLiveData(ArrayList<BaseNote>())

    var keyword = String()
    val searchResults = MutableLiveData(ArrayList<BaseNote>())

    var label = String()
    val labelledNotes = MutableLiveData(ArrayList<BaseNote>())

    private val notesHelper = NotesHelper(app)

    private val observer: NotallyFileObserver
    private val deletedObserver: NotallyFileObserver
    private val archivedObserver: NotallyFileObserver

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            observer = NotallyFileObserver(notesHelper.getNotePath())
            deletedObserver = NotallyFileObserver(notesHelper.getDeletedPath())
            archivedObserver = NotallyFileObserver(notesHelper.getArchivedPath())
        }
        else {
            observer = NotallyFileObserver(notesHelper.getNotePath().path)
            deletedObserver = NotallyFileObserver(notesHelper.getDeletedPath().path)
            archivedObserver = NotallyFileObserver(notesHelper.getArchivedPath().path)
        }

        observer.onEventCallback = { event, path ->
            onEvent(event, path, notes, notesHelper.getNotePath())
            onEvent(event, path, labelledNotes, notesHelper.getNotePath())
            onEvent(event, path, searchResults, notesHelper.getNotePath())
        }

        deletedObserver.onEventCallback = { event, path ->
            onEvent(event, path, deletedNotes, notesHelper.getDeletedPath())
        }

        archivedObserver.onEventCallback = { event, path ->
            onEvent(event, path, archivedNotes, notesHelper.getArchivedPath())
        }

        viewModelScope.launch {
            notes.value = getBaseNotes()
            deletedNotes.value = getDeletedBaseNotes()
            archivedNotes.value = getArchivedBaseNotes()

            observer.startWatching()
            deletedObserver.startWatching()
            archivedObserver.startWatching()
        }
    }

    private fun onEvent(event: Int, filePath: String, liveData: MutableLiveData<ArrayList<BaseNote>>, rootDirectory: File) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val list = liveData.value
            list?.let {
                val file = File(rootDirectory, filePath)

                when (event) {
                    FileObserver.CREATE -> insertFileInList(file, list, liveData)
                    FileObserver.DELETE -> removeFileFromList(file, list, liveData)
                    FileObserver.MODIFY -> modifyFileInList(file, list, liveData)
                }
            }

        }
    }

    private fun insertFileInList(file: File, list: ArrayList<BaseNote>, liveData: MutableLiveData<ArrayList<BaseNote>>) {
        val baseNote = BaseNote.readFromFile(file)

        var index = list.binarySearch(baseNote, Comparator { o1, o2 ->
            o2.filePath.compareTo(o1.filePath)
        })
        if (index < 0) {
            index = index.inv()
        }

        list.add(index, baseNote)
        liveData.value = list
    }

    private fun removeFileFromList(file: File, list: ArrayList<BaseNote>, liveData: MutableLiveData<ArrayList<BaseNote>>) {
        val baseNote = list.find { note -> note.filePath == file.path }
        baseNote?.let {
            list.remove(baseNote)
            liveData.value = list
        }
    }

    private fun modifyFileInList(file: File, list: ArrayList<BaseNote>, liveData: MutableLiveData<ArrayList<BaseNote>>) {
        val baseNote = list.find { note -> note.filePath == file.path }
        val index = list.indexOf(baseNote)
        val editedNote = BaseNote.readFromFile(file)

        when (liveData) {
            labelledNotes -> {
                if (editedNote.labels.contains(label)) {
                    if (index != -1) {
                        list[index] = editedNote
                        liveData.value = list
                    }
                    else insertFileInList(file, list, liveData)
                }
                else {
                    list.remove(baseNote)
                    liveData.value = list
                }
            }
            searchResults -> {
                if (editedNote.matchesKeyword(keyword)) {
                    if (index != -1) {
                        list[index] = editedNote
                        liveData.value = list
                    }
                    else insertFileInList(file, list, liveData)
                }
                else {
                    list.remove(baseNote)
                    liveData.value = list
                }
            }
            else -> {
                list[index] = editedNote
                liveData.value = list
            }
        }
    }


    private fun getBaseNotes() : ArrayList<BaseNote> {
        val files = getSortedFiles(notesHelper.getNotePath())
        return convertFilesToNotes(files)
    }

    private fun getDeletedBaseNotes() : ArrayList<BaseNote> {
        val files = getSortedFiles(notesHelper.getDeletedPath())
        return convertFilesToNotes(files)
    }

    private fun getArchivedBaseNotes() : ArrayList<BaseNote> {
        val files = getSortedFiles(notesHelper.getArchivedPath())
        return convertFilesToNotes(files)
    }


    fun moveFileToArchive(filePath: String?) {
        filePath?.let {
            val file = File(filePath)
            notesHelper.moveFileToArchive(file)
        }
    }

    fun moveFileToDeleted(filePath: String?) {
        filePath?.let {
            val file = File(filePath)
            notesHelper.moveFileToDeleted(file)
        }
    }


    fun restoreFile(filePath: String?) {
        filePath?.let {
            val file = File(filePath)
            notesHelper.restoreFile(file)
        }
    }

    fun deleteFileForever(filePath: String?) {
        filePath?.let {
            val file = File(filePath)
            file.delete()
        }
    }


    fun fetchSearchResults() {
        if (keyword.isEmpty()) {
            searchResults.value = ArrayList()
            return
        }

        viewModelScope.launch {
            val baseNotes = getBaseNotes()
            val results = ArrayList<BaseNote>()

            baseNotes.forEach { baseNote ->
                if (baseNote.matchesKeyword(keyword)){
                    results.add(baseNote)
                }
            }

            searchResults.value = results
        }
    }

    fun fetchLabelledNotes() {
        if (label.isEmpty()){
            labelledNotes.value = ArrayList()
            return
        }

        viewModelScope.launch {
            val baseNotes = getBaseNotes()
            val results = ArrayList<BaseNote>()

            baseNotes.forEach { baseNote ->
                if (baseNote.labels.contains(label)){
                    results.add(baseNote)
                }
            }

            labelledNotes.value = results
        }
    }


    fun editNoteLabel(baseNote: BaseNote, labels: HashSet<String>) {
        val note = when(baseNote) {
            is Note -> baseNote.copy(labels = labels)
            is List -> baseNote.copy(labels = labels)
        }
        note.writeToFile()
    }


    private fun getSortedFiles(path: File) : ArrayList<File> {
        val files = ArrayList<File>()
        path.listFiles()?.toCollection(files)

        files.sortWith(Comparator { firstFile, secondFile ->
            secondFile.name.compareTo(firstFile.name)
        })

        return files
    }

    private fun convertFilesToNotes(files: ArrayList<File>) : ArrayList<BaseNote> {
        val baseNotes = ArrayList<BaseNote>()
        files.forEach { file ->
            val note = BaseNote.readFromFile(file)
            baseNotes.add(note)
        }
        return baseNotes
    }


    class NotallyFileObserver : FileObserver {

        var onEventCallback: ((event: Int, path: String) -> Unit)? = null

        @RequiresApi(Build.VERSION_CODES.Q)
        constructor(file: File) : super(file, mask)

        constructor(filePath: String) : super(filePath, mask)

        override fun onEvent(event: Int, path: String?) {
            /*
            This is a strange bug that occurs when the app is open in the background and one uses the launcher
            shortcuts to take a note. The observer initially returns the current path but then returns null
            and stops watching. To prevent this, it's restarted.
             */
            if (path == null) {
                stopWatching()
                startWatching()
            }
            else onEventCallback?.invoke(event, path)
        }

        companion object {
            const val mask = DELETE or CREATE or MODIFY
        }
    }
}