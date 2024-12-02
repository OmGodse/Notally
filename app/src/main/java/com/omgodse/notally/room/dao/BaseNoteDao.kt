package com.omgodse.notally.room.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.omgodse.notally.room.Audio
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Color
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Image
import com.omgodse.notally.room.LabelsInBaseNote
import com.omgodse.notally.room.ListItem

@Dao
interface BaseNoteDao {

    @RawQuery
    fun query(query: SupportSQLiteQuery): Int


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(baseNote: BaseNote): Long

    @Insert
    suspend fun insert(baseNotes: List<BaseNote>)

    @Update(entity = BaseNote::class)
    suspend fun update(labelsInBaseNotes: List<LabelsInBaseNote>)


    @Query(
        "INSERT INTO BaseNote (type, folder, color, title, pinned, timestamp, labels, body, spans, items, images, audios)\n" +
                "SELECT type, folder, color, title, pinned, timestamp, labels, body, spans, items, images, audios\n" +
                "FROM BaseNote WHERE id IN (:ids)"
    )
    suspend fun copy(ids: LongArray)


    @Query("DELETE FROM BaseNote WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM BaseNote WHERE id IN (:ids)")
    suspend fun delete(ids: LongArray)

    @Query("DELETE FROM BaseNote WHERE folder = :folder")
    suspend fun deleteFrom(folder: Folder)


    @Query("SELECT * FROM BaseNote WHERE folder = :folder ORDER BY pinned DESC, timestamp DESC")
    fun getFrom(folder: Folder): LiveData<List<BaseNote>>

    @Query("SELECT * FROM BaseNote WHERE folder = 'NOTES' ORDER BY pinned DESC, timestamp DESC")
    suspend fun getAllNotes(): List<BaseNote>

    @Query("SELECT * FROM BaseNote")
    fun getAll(): LiveData<List<BaseNote>>

    @Query("SELECT * FROM BaseNote WHERE id = :id")
    fun get(id: Long): BaseNote?

    @Query("SELECT images FROM BaseNote WHERE id = :id")
    fun getImages(id: Long): String


    @Query("SELECT images FROM BaseNote")
    fun getAllImages(): List<String>

    @Query("SELECT audios FROM BaseNote")
    fun getAllAudios(): List<String>


    @Query("SELECT id FROM BaseNote WHERE folder = 'DELETED'")
    suspend fun getDeletedNoteIds(): LongArray

    @Query("SELECT images FROM BaseNote WHERE folder = 'DELETED'")
    suspend fun getDeletedNoteImages(): List<String>

    @Query("SELECT audios FROM BaseNote WHERE folder = 'DELETED'")
    suspend fun getDeletedNoteAudios(): List<String>


    @Query("UPDATE BaseNote SET folder = :folder WHERE id IN (:ids)")
    suspend fun move(ids: LongArray, folder: Folder)


    @Query("UPDATE BaseNote SET color = :color WHERE id IN (:ids)")
    suspend fun updateColor(ids: LongArray, color: Color)

    @Query("UPDATE BaseNote SET pinned = :pinned WHERE id IN (:ids)")
    suspend fun updatePinned(ids: LongArray, pinned: Boolean)

    @Query("UPDATE BaseNote SET labels = :labels WHERE id = :id")
    suspend fun updateLabels(id: Long, labels: List<String>)

    @Query("UPDATE BaseNote SET items = :items WHERE id = :id")
    suspend fun updateItems(id: Long, items: List<ListItem>)

    @Query("UPDATE BaseNote SET images = :images WHERE id = :id")
    suspend fun updateImages(id: Long, images: List<Image>)

    @Query("UPDATE BaseNote SET audios = :audios WHERE id = :id")
    suspend fun updateAudios(id: Long, audios: List<Audio>)


    /**
     * Both id and position can be invalid.
     *
     * Example of id being invalid - User adds a widget,
     * then goes to Settings and clears app data. Now the
     * widget refers to a list which doesn't exist.
     *
     * Example of position being invalid - User adds a widget,
     * goes to Settings, clears app data and then imports a backup.
     * Even if the backup contains the same list and it is inserted
     * with the same id, it may not be of the safe size.
     *
     * In this case, an exception will be thrown. It is the caller's
     * responsibility to handle it.
     */
    suspend fun updateChecked(id: Long, position: Int, checked: Boolean) {
        val items = requireNotNull(get(id)).items
        items[position].checked = checked
        updateItems(id, items)
    }


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


    fun getBaseNotesByKeyword(keyword: String, folder: Folder): LiveData<List<BaseNote>> {
        val result = getBaseNotesByKeywordImpl(keyword, folder)
        return Transformations.map(result) { list -> list.filter { baseNote -> matchesKeyword(baseNote, keyword) } }
    }

    @Query("SELECT * FROM BaseNote WHERE folder = :folder AND (title LIKE '%' || :keyword || '%' OR body LIKE '%' || :keyword || '%' OR items LIKE '%' || :keyword || '%' OR labels LIKE '%' || :keyword || '%') ORDER BY pinned DESC, timestamp DESC")
    fun getBaseNotesByKeywordImpl(keyword: String, folder: Folder): LiveData<List<BaseNote>>


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