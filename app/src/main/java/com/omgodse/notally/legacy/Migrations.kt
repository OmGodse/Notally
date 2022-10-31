package com.omgodse.notally.legacy

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Label
import java.io.File

// Backwards compatibility from v3.2 to v3.3
object Migrations {

    fun clearAllLabels(app: Application) {
        val preferences = getLabelsPreferences(app)
        preferences.edit().clear().commit()
    }

    fun clearAllFolders(app: Application) {
        getNotePath(app).listFiles()?.forEach { file -> file.delete() }
        getDeletedPath(app).listFiles()?.forEach { file -> file.delete() }
        getArchivedPath(app).listFiles()?.forEach { file -> file.delete() }
    }

    fun getPreviousLabels(app: Application): List<Label> {
        val preferences = getLabelsPreferences(app)
        val labels = requireNotNull(preferences.getStringSet("labelItems", emptySet()))
        return labels.map { value -> Label(value) }
    }

    fun getPreviousNotes(app: Application): List<BaseNote> {
        val list = ArrayList<BaseNote>()
        getNotePath(app).listFiles()?.mapTo(list) { file -> XMLUtils.readBaseNoteFromFile(file, Folder.NOTES) }
        getDeletedPath(app).listFiles()?.mapTo(list) { file -> XMLUtils.readBaseNoteFromFile(file, Folder.DELETED) }
        getArchivedPath(app).listFiles()?.mapTo(list) { file -> XMLUtils.readBaseNoteFromFile(file, Folder.ARCHIVED) }
        return list
    }


    private fun getNotePath(app: Application) = getFolder(app, "notes")

    private fun getDeletedPath(app: Application) = getFolder(app, "deleted")

    private fun getArchivedPath(app: Application) = getFolder(app, "archived")

    private fun getLabelsPreferences(app: Application): SharedPreferences {
        return app.getSharedPreferences("labelsPreferences", Context.MODE_PRIVATE)
    }


    private fun getFolder(app: Application, name: String): File {
        val folder = File(app.filesDir, name)
        if (!folder.exists()) {
            folder.mkdir()
        }
        return folder
    }
}