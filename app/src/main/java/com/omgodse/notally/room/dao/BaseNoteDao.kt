package com.omgodse.notally.room.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.room.*
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Folder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

/**
 * Class containing operations involving only
 * the 'NewBaseNote' table
 */
@Dao
interface BaseNoteDao {

    @Delete
    suspend fun deleteBaseNote(baseNote: BaseNote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaseNote(baseNote: BaseNote): Long

    @Insert
    suspend fun insertBaseNotes(baseNotes: List<BaseNote>)


    @Query("DELETE FROM BaseNote WHERE folder = :folderName")
    suspend fun deleteAllBaseNotesFromFolder(folderName: String)


    fun getAllBaseNotes() = getAllBaseNotes(Folder.NOTES.name)

    fun getAllDeletedNotes() = getAllBaseNotes(Folder.DELETED.name)

    fun getAllArchivedNotes() = getAllBaseNotes(Folder.ARCHIVED.name)

    @Query("SELECT * FROM BaseNote WHERE folder = :folderName ORDER BY pinned DESC, timestamp DESC")
    fun getAllBaseNotes(folderName: String): LiveData<List<BaseNote>>


    suspend fun getAllBaseNotesAsList() = getAllBaseNotesAsList(Folder.NOTES.name)

    suspend fun getAllDeletedNotesAsList() = getAllBaseNotesAsList(Folder.DELETED.name)

    suspend fun getAllArchivedNotesAsList() = getAllBaseNotesAsList(Folder.ARCHIVED.name)

    @Query("SELECT * FROM BaseNote WHERE folder = :folderName ORDER BY pinned DESC, timestamp DESC")
    suspend fun getAllBaseNotesAsList(folderName: String): List<BaseNote>


    suspend fun restoreBaseNote(id: Long) = moveBaseNote(Folder.NOTES.name, id)

    suspend fun moveBaseNoteToDeleted(id: Long) = moveBaseNote(Folder.DELETED.name, id)

    suspend fun moveBaseNoteToArchive(id: Long) = moveBaseNote(Folder.ARCHIVED.name, id)

    @Query("UPDATE BaseNote SET folder = :folderName WHERE id = :id")
    suspend fun moveBaseNote(folderName: String, id: Long)


    suspend fun updateBaseNoteLabels(labels: HashSet<String>, id: Long) {
        val json = JSONArray(labels).toString()
        updateBaseNoteLabels(json, id)
    }

    @Query("UPDATE BaseNote SET labels = :labels WHERE id = :id")
    suspend fun updateBaseNoteLabels(labels: String, id: Long)


    fun getBaseNotesByKeyword(keyword: String): LiveData<List<BaseNote>> {
        val result = getBaseNotesByKeyword(keyword, Folder.NOTES.name)
        val filtered = result.map { list -> list.filter { baseNote -> baseNote.matchesKeyword(keyword) } }
        return filtered.asLiveData()
    }

    @Query("SELECT * FROM BaseNote WHERE folder = :folderName AND (title LIKE '%' || :keyword || '%' OR body LIKE '%' || :keyword || '%' OR items LIKE '%' || :keyword || '%' OR labels LIKE '%' || :keyword || '%') ORDER BY pinned DESC, timestamp DESC")
    fun getBaseNotesByKeyword(keyword: String, folderName: String): Flow<List<BaseNote>>
}