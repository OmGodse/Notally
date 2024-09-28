package com.omgodse.notally.room

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.omgodse.notally.room.dao.BaseNoteDao
import com.omgodse.notally.room.dao.CommonDao
import com.omgodse.notally.room.dao.LabelDao

@TypeConverters(Converters::class)
@Database(entities = [BaseNote::class, Label::class], version = 4)
abstract class NotallyDatabase : RoomDatabase() {

    abstract fun getLabelDao(): LabelDao
    abstract fun getCommonDao(): CommonDao
    abstract fun getBaseNoteDao(): BaseNoteDao

    fun checkpoint() {
        getBaseNoteDao().query(SimpleSQLiteQuery("pragma wal_checkpoint(FULL)"))
    }

    companion object {

        const val DatabaseName = "NotallyDatabase"

        @Volatile
        private var instance: NotallyDatabase? = null

        fun getDatabase(app: Application): NotallyDatabase {
            return instance ?: synchronized(this) {
                val instance = Room.databaseBuilder(app, NotallyDatabase::class.java, DatabaseName)
                    .addMigrations(Migration2, Migration3, Migration4)
                    .build()
                this.instance = instance
                return instance
            }
        }

        object Migration2 : Migration(1, 2) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `color` TEXT NOT NULL DEFAULT 'DEFAULT'")
            }
        }

        object Migration3 : Migration(2, 3) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `images` TEXT NOT NULL DEFAULT `[]`")
            }
        }

        object Migration4 : Migration(3, 4) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `audios` TEXT NOT NULL DEFAULT `[]`")
            }
        }
    }
}