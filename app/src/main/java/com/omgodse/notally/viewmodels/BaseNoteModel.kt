package com.omgodse.notally.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.List
import com.omgodse.notally.xml.Note
import kotlinx.coroutines.launch
import java.io.File

class BaseNoteModel(private val app: Application) : AndroidViewModel(app) {

    val labels = MutableLiveData(ArrayList<String>())
    val notes = MutableLiveData(ArrayList<BaseNote>())
    val deletedNotes = MutableLiveData(ArrayList<BaseNote>())
    val archivedNotes = MutableLiveData(ArrayList<BaseNote>())

    var keyword = String()
        set(value) {
            field = value
            fetchSearchResults()
        }
    val searchResults = MutableLiveData(ArrayList<BaseNote>())

    private val labelsMap = HashMap<String, MutableLiveData<ArrayList<BaseNote>>>()

    private val labelsObserver: SharedPreferences.OnSharedPreferenceChangeListener

    private val notesObserver: NotallyFileObserver
    private val deletedObserver: NotallyFileObserver
    private val archivedObserver: NotallyFileObserver

    private val notesPath = getNotePath(app)
    private val deletedPath = getDeletedPath(app)
    private val archivedPath = getArchivedPath(app)

    private val handler = Handler(Looper.getMainLooper())

    init {
        labelsObserver = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
            labels.value = getSortedLabels(app)
        }

        notesObserver = NotallyFileObserver(notesPath) { event, path ->
            onEvent(event, path, notes, notesPath)
            onEvent(event, path, searchResults, notesPath)
        }
        deletedObserver = NotallyFileObserver(deletedPath) { event, path ->
            onEvent(event, path, deletedNotes, deletedPath)
        }
        archivedObserver = NotallyFileObserver(archivedPath) { event, path ->
            onEvent(event, path, archivedNotes, archivedPath)
        }

        viewModelScope.launch {
            notes.value = getBaseNotes(notesPath, true)
            deletedNotes.value = getBaseNotes(deletedPath)
            archivedNotes.value = getBaseNotes(archivedPath)
            labels.value = getSortedLabels(app)

            notesObserver.startWatching()
            deletedObserver.startWatching()
            archivedObserver.startWatching()
            getLabelsPreferences(app).registerOnSharedPreferenceChangeListener(labelsObserver)
        }
    }

    private fun onEvent(event: Int, filePath: String, liveData: MutableLiveData<ArrayList<BaseNote>>, rootDirectory: File) {
        println("Event : $event : $filePath")
        handler.post {
            liveData.value?.let {
                val file = File(rootDirectory, filePath)

                when (event) {
                    FileObserver.CREATE -> insertFileInList(file, it, liveData)
                    FileObserver.MODIFY -> modifyFileInList(file, it, liveData)
                    FileObserver.DELETE -> removeFileFromList(file, it, liveData)
                }
            }
        }
    }

    private fun insertFileInList(file: File, list: ArrayList<BaseNote>, liveData: MutableLiveData<ArrayList<BaseNote>>) {
        val baseNote = BaseNote.readFromFile(file)
        list.addSorted(baseNote)
        liveData.value = list
    }

    private fun removeFileFromList(file: File, list: ArrayList<BaseNote>, liveData: MutableLiveData<ArrayList<BaseNote>>) {
        val baseNote = list.find { note -> note.filePath == file.path }
        baseNote?.let {
            list.remove(baseNote)
            liveData.value = list

            if (liveData == notes) {
                removeBaseNoteFromMap(it)
            }
        }
    }

    private fun modifyFileInList(file: File, list: ArrayList<BaseNote>, liveData: MutableLiveData<ArrayList<BaseNote>>) {
        val baseNote = list.find { note -> note.filePath == file.path }
        val index = list.indexOf(baseNote)
        val editedNote = BaseNote.readFromFile(file)

        when (liveData) {
            notes -> {
                list[index] = editedNote
                liveData.value = list

                removeBaseNoteFromMap(baseNote)
                putBaseNoteInMap(editedNote)
            }
            searchResults -> {
                if (editedNote.matchesKeyword(keyword)) {
                    if (index != -1) {
                        list[index] = editedNote
                        liveData.value = list
                    } else insertFileInList(file, list, liveData)
                } else {
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


    private fun deleteLabelFromBaseNote(label: String, baseNote: BaseNote) {
        baseNote.labels.remove(label)
        baseNote.writeToFile()
    }

    private fun editLabelFromBaseNote(oldLabel: String, newLabel: String, baseNote: BaseNote) {
        baseNote.labels.remove(oldLabel)
        baseNote.labels.add(newLabel)
        baseNote.writeToFile()
    }


    private fun putBaseNoteInMap(baseNote: BaseNote) {
        baseNote.labels.forEach {
            if (labelsMap[it] == null) {
                labelsMap[it] = MutableLiveData()
            }
            val list = labelsMap[it]?.value ?: ArrayList()

            list.addSorted(baseNote)
            labelsMap[it]?.value = list
        }
    }

    private fun removeBaseNoteFromMap(baseNote: BaseNote?) {
        baseNote?.labels?.forEach {
            val labelsList = labelsMap[it]?.value
            labelsList?.remove(baseNote)
            labelsMap[it]?.value = labelsList
        }
    }


    private fun fetchSearchResults() {
        if (keyword.isEmpty()) {
            searchResults.value = arrayListOf()
        } else viewModelScope.launch {
            val results = ArrayList<BaseNote>()

            notes.value?.forEach { baseNote ->
                if (baseNote.matchesKeyword(keyword)) {
                    results.add(baseNote)
                }
            }

            searchResults.value = results
        }
    }


    fun deleteLabel(label: String) {
        labels.value?.let {
            it.remove(label)
            saveLabels(app, it.toSet())

            labelsMap[label]?.value?.forEach { baseNote -> deleteLabelFromBaseNote(label, baseNote) }
        }
    }

    fun insertLabel(label: String, onResult: (success: Boolean) -> Unit) {
        labels.value?.let {
            if (it.contains(label)) {
                onResult.invoke(false)
            } else {
                it.add(label)
                saveLabels(app, it.toSet())
                onResult.invoke(true)
            }
        }
    }

    fun editLabel(oldLabel: String, newLabel: String, onResult: (success: Boolean) -> Unit) {
        labels.value?.let {
            if (it.contains(newLabel)) {
                onResult.invoke(false)
            } else {
                it.add(newLabel)
                it.remove(oldLabel)
                saveLabels(app, it.toSet())

                val oldList = labelsMap[oldLabel]?.value
                oldList?.forEach { baseNote -> editLabelFromBaseNote(oldLabel, newLabel, baseNote) }
                labelsMap[newLabel] = MutableLiveData(oldList)

                onResult.invoke(true)
            }
        }
    }


    fun moveBaseNoteToArchive(baseNote: BaseNote) = moveBaseNoteToArchive(baseNote, app)

    fun moveBaseNoteToDeleted(baseNote: BaseNote) = moveBaseNoteToDeleted(baseNote, app)


    fun restoreBaseNote(baseNote: BaseNote) = restoreBaseNote(baseNote, app)

    fun deleteBaseNoteForever(baseNote: BaseNote) {
        val file = File(baseNote.filePath)
        file.delete()
    }


    fun getLabelledNotes(label: String) = labelsMap[label]

    fun editNoteLabel(baseNote: BaseNote, labels: HashSet<String>) {
        val note = when (baseNote) {
            is Note -> baseNote.copy(labels = labels)
            is List -> baseNote.copy(labels = labels)
        }
        note.writeToFile()
    }


    private fun getBaseNotes(path: File, map: Boolean = false): ArrayList<BaseNote> {
        val baseNotes = ArrayList<BaseNote>()

        path.listFiles()?.forEach {
            val baseNote = BaseNote.readFromFile(it)
            baseNotes.addSorted(baseNote)

            if (map) {
                putBaseNoteInMap(baseNote)
            }
        }

        return baseNotes
    }

    private fun ArrayList<BaseNote>.addSorted(element: BaseNote) {
        var index = binarySearch(element, { o1, o2 -> o2.filePath.compareTo(o1.filePath) })

        if (index < 0) {
            index = index.inv()
        }

        add(index, element)
    }


    private class NotallyFileObserver(file: File, val onEventCallback: (event: Int, path: String) -> Unit) : FileObserver(file.path, mask) {

        override fun onEvent(event: Int, path: String?) {
            /*
            This is a strange bug that occurs when the app is open in the background and one uses the launcher
            shortcuts to take a note. The observer initially returns the current path but then returns null
            and stops watching. To prevent this, it's restarted.
             */
            if (path == null) {
                stopWatching()
                startWatching()
            } else onEventCallback.invoke(event, path)
        }

        companion object {
            private const val mask = DELETE or CREATE or MODIFY
        }
    }

    companion object {

        // TODO: 06-10-2020 Move these functions to external repository
        fun getNotePath(context: Context) = getFolder(context, "notes")

        fun getDeletedPath(context: Context) = getFolder(context, "deleted")

        fun getArchivedPath(context: Context) = getFolder(context, "archived")

        private fun getFolder(context: Context, name: String): File {
            val folder = File(context.filesDir, name)
            if (!folder.exists()) {
                folder.mkdir()
            }
            return folder
        }


        fun restoreBaseNote(baseNote: BaseNote, context: Context) = moveBaseNote(baseNote, getNotePath(context))

        fun moveBaseNoteToDeleted(baseNote: BaseNote, context: Context) = moveBaseNote(baseNote, getDeletedPath(context))

        fun moveBaseNoteToArchive(baseNote: BaseNote, context: Context) = moveBaseNote(baseNote, getArchivedPath(context))

        private fun moveBaseNote(baseNote: BaseNote, destination: File) {
            val currentFile = File(baseNote.filePath)
            val destinationPath = File(destination, currentFile.name).path

            when (baseNote) {
                is Note -> baseNote.copy(filePath = destinationPath).writeToFile()
                is List -> baseNote.copy(filePath = destinationPath).writeToFile()
            }

            currentFile.delete()
        }


        fun saveLabels(context: Context, labels: Set<String>) {
            val editor = getLabelsPreferences(context).edit()
            editor.putStringSet("labelItems", labels)
            editor.apply()
        }

        fun getSortedLabels(context: Context): ArrayList<String> {
            val labels = getLabelsPreferences(context).getLabels()
            val arrayList = ArrayList(labels)
            arrayList.sort()
            return arrayList
        }

        private fun SharedPreferences.getLabels() = getStringSet("labelItems", HashSet()) as HashSet<String>

        private fun getLabelsPreferences(context: Context) = context.getSharedPreferences("labelsPreferences", Context.MODE_PRIVATE)
    }
}