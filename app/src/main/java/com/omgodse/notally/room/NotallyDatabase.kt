package com.omgodse.notally.room

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.omgodse.notally.room.dao.BaseNoteDao
import com.omgodse.notally.room.dao.CommonDao
import com.omgodse.notally.room.dao.LabelDao

@TypeConverters(Converters::class)
@Database(entities = [BaseNote::class, Label::class], version = 1)
abstract class NotallyDatabase : RoomDatabase() {

    abstract val labelDao: LabelDao
    abstract val commonDao: CommonDao
    abstract val baseNoteDao: BaseNoteDao

    companion object {

        private const val databaseName = "NotallyDatabase"

        @Volatile
        internal var instance: NotallyDatabase? = null

        fun getDatabase(application: Application): NotallyDatabase {
            val tempInstance = instance
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(application, NotallyDatabase::class.java, databaseName).build()
                Companion.instance = instance
                return instance
            }
        }
    }
}