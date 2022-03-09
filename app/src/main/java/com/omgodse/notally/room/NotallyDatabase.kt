package com.omgodse.notally.room

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.omgodse.notally.room.dao.BaseNoteDao
import com.omgodse.notally.room.dao.CommonDao
import com.omgodse.notally.room.dao.LabelDao

@TypeConverters(Converters::class)
@Database(entities = [BaseNote::class, Label::class], version = 2, exportSchema = true)
abstract class NotallyDatabase : RoomDatabase() {

    abstract val labelDao: LabelDao
    abstract val commonDao: CommonDao
    abstract val baseNoteDao: BaseNoteDao

    companion object {

        private const val databaseName = "NotallyDatabase"

        val MIGRATION_1_2 = object : Migration(1,2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE BaseNote ADD COLUMN phoneItems TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var instance: NotallyDatabase? = null

        fun getDatabase(application: Application): NotallyDatabase {
            return instance ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(application, NotallyDatabase::class.java, databaseName)
                        .addMigrations(MIGRATION_1_2)
                        .build()
                this.instance = instance
                return instance
            }
        }
    }
}