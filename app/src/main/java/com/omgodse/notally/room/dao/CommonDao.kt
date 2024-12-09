package com.omgodse.notally.room.dao

import androidx.room.Dao
import androidx.room.Transaction
import androidx.room.Update
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.IdLabels
import com.omgodse.notally.room.Label
import com.omgodse.notally.room.NotallyDatabase

@Dao
abstract class CommonDao(private val database: NotallyDatabase) {

    @Update(entity = BaseNote::class)
    abstract suspend fun update(list: List<IdLabels>)

    @Transaction
    open suspend fun deleteLabel(value: String) {
        val labelsInBaseNotes = database.getBaseNoteDao().getListOfBaseNotesByLabel(value).map { baseNote ->
            val labels = ArrayList(baseNote.labels)
            labels.remove(value)
            IdLabels(baseNote.id, labels)
        }
        update(labelsInBaseNotes)
        database.getLabelDao().delete(value)
    }

    @Transaction
    open suspend fun updateLabel(oldValue: String, newValue: String) {
        val labelsInBaseNotes = database.getBaseNoteDao().getListOfBaseNotesByLabel(oldValue).map { baseNote ->
            val labels = ArrayList(baseNote.labels)
            labels.remove(oldValue)
            labels.add(newValue)
            IdLabels(baseNote.id, labels)
        }
        update(labelsInBaseNotes)
        database.getLabelDao().update(oldValue, newValue)
    }

    @Transaction
    open suspend fun importBackup(baseNotes: List<BaseNote>, labels: List<Label>) {
        database.getBaseNoteDao().insert(baseNotes)
        database.getLabelDao().insert(labels)
    }
}