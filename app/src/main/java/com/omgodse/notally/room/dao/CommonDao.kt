package com.omgodse.notally.room.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.room.*
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Label

/**
 * Class containing operations common to all tables
 */
@Dao
interface CommonDao {

    @Delete
    suspend fun deleteLabelInternal(label: Label)

    @Update
    suspend fun updateBaseNotes(baseNotes: List<BaseNote>)


    @Transaction
    suspend fun deleteLabel(label: Label) {
        val baseNotesWithLabel = getBaseNotesByLabelAsList(label.value)
        val modified = baseNotesWithLabel.map { baseNote ->
            baseNote.labels.remove(label.value)
            baseNote
        }
        updateBaseNotes(modified)
        deleteLabelInternal(label)
    }

    @Transaction
    suspend fun updateLabel(oldValue: String, newValue: String) {
        val baseNotesWithLabel = getBaseNotesByLabelAsList(oldValue)
        val modified = baseNotesWithLabel.map { baseNote ->
            baseNote.labels.remove(oldValue)
            baseNote.labels.add(newValue)
            baseNote
        }
        updateBaseNotes(modified)
        updateLabelInternal(oldValue, newValue)
    }

    @Query("UPDATE LABEL SET value = :newValue WHERE value = :oldValue")
    suspend fun updateLabelInternal(oldValue: String, newValue: String)


    /**
     * Since we store the labels as a JSON Array, it is not possible
     * to perform operations on it. Thus, we use the 'Like' query
     * which can return false positives sometimes.
     *
     * Take for example, a request for all base notes having the label
     * 'Important' The base notes which instead have the label 'Unimportant'
     * will also be returned. To prevent this, we use [Transformations.map] and
     * filter the result accordingly.
     */
    fun getBaseNotesByLabel(label: String): LiveData<List<BaseNote>> {
        val result = getBaseNotesByLabel(label, Folder.NOTES)
        return Transformations.map(result) { list -> list.filter { baseNote -> baseNote.labels.contains(label) } }
    }

    @Query("SELECT * FROM BaseNote WHERE folder = :folder AND labels LIKE '%' || :label || '%' ORDER BY pinned DESC, timestamp DESC")
    fun getBaseNotesByLabel(label: String, folder: Folder): LiveData<List<BaseNote>>


    @Query("SELECT * FROM BaseNote WHERE labels LIKE '%' || :label || '%'")
    suspend fun getBaseNotesByLabelAsList(label: String): List<BaseNote>
}