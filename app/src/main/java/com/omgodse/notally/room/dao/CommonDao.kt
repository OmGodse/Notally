package com.omgodse.notally.room.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.room.*
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Label
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
     * will also be returned. To prevent this, we use Kotlin's flow and
     * filter the result accordingly.
     */
    fun getBaseNotesByLabel(label: String): LiveData<List<BaseNote>> {
        val result = getBaseNotesByLabel(label, Folder.NOTES.name)
        val filtered = result.map { list -> list.filter { baseNote -> baseNote.labels.contains(label) } }
        return filtered.asLiveData()
    }

    @Query("SELECT * FROM BaseNote WHERE folder = :folderName AND labels LIKE '%' || :label || '%' ORDER BY pinned DESC, timestamp DESC")
    fun getBaseNotesByLabel(label: String, folderName: String): Flow<List<BaseNote>>

    @Query("SELECT * FROM BaseNote WHERE labels LIKE '%' || :label || '%' ORDER BY pinned DESC, timestamp DESC")
    suspend fun getBaseNotesByLabelAsList(label: String): List<BaseNote>
}