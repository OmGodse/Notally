package com.omgodse.notally.room.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.room.*
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Folder

@Dao
interface BaseNoteDao {

    @Delete
    suspend fun delete(baseNote: BaseNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(baseNote: BaseNote): Long

    @Insert
    suspend fun insert(baseNotes: List<BaseNote>)

    @Update
    suspend fun update(baseNotes: List<BaseNote>)


    @Query("DELETE FROM BaseNote WHERE folder = :folder")
    suspend fun deleteFrom(folder: Folder)


    @Query("SELECT * FROM BaseNote WHERE folder = :folder ORDER BY pinned DESC, timestamp DESC")
    fun getFrom(folder: Folder): LiveData<List<BaseNote>>

    @Query("SELECT * FROM BaseNote WHERE folder = :folder ORDER BY pinned DESC, timestamp DESC")
    suspend fun getListFrom(folder: Folder): List<BaseNote>


    @Query("UPDATE BaseNote SET folder = :folder WHERE id = :id")
    suspend fun move(id: Long, folder: Folder)


    @Query("UPDATE BaseNote SET pinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean)

    @Query("UPDATE BaseNote SET labels = :labels WHERE id = :id")
    suspend fun updateLabels(id: Long, labels: HashSet<String>)


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


    suspend fun getListOfBaseNotesByLabel(label: String): List<BaseNote> {
        val result = getListOfBaseNotesByLabelImpl(label)
        return result.filter { baseNote -> baseNote.labels.contains(label) }
    }

    @Query("SELECT * FROM BaseNote WHERE labels LIKE '%' || :label || '%'")
    suspend fun getListOfBaseNotesByLabelImpl(label: String): List<BaseNote>


    fun getBaseNotesByKeyword(keyword: String): LiveData<List<BaseNote>> {
        val result = getBaseNotesByKeyword(keyword, Folder.NOTES)
        return Transformations.map(result) { list -> list.filter { baseNote -> matchesKeyword(baseNote, keyword) } }
    }

    @Query("SELECT * FROM BaseNote WHERE folder = :folder AND (title LIKE '%' || :keyword || '%' OR body LIKE '%' || :keyword || '%' OR items LIKE '%' || :keyword || '%' OR labels LIKE '%' || :keyword || '%') ORDER BY pinned DESC, timestamp DESC")
    fun getBaseNotesByKeyword(keyword: String, folder: Folder): LiveData<List<BaseNote>>


    private fun matchesKeyword(baseNote: BaseNote, keyword: String): Boolean {
        if (baseNote.title.contains(keyword, true)) {
            return true
        }
        if (baseNote.body.contains(keyword, true)) {
            return true
        }
        for (label in baseNote.labels) {
            if (label.contains(keyword, true)) {
                return true
            }
        }
        for (item in baseNote.items) {
            if (item.body.contains(keyword, true)) {
                return true
            }
        }
        return false
    }
}