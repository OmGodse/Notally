package com.omgodse.notally.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.omgodse.notally.room.Label

/**
 * Class containing operations involving only
 * the 'Label' table
 */
@Dao
interface LabelDao {

    @Insert
    suspend fun insertLabel(label: Label)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLabels(labels: List<Label>)

    @Query("SELECT value FROM Label")
    suspend fun getAllLabelsAsList(): List<String>

    @Query("SELECT * FROM Label ORDER BY value")
    fun getAllLabels(): LiveData<List<Label>>
}