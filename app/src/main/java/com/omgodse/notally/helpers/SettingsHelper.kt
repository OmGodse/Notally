package com.omgodse.notally.helpers

import android.content.Context
import androidx.preference.PreferenceManager
import com.omgodse.notally.R

class SettingsHelper(context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun getView() = preferences.getString(View.key, View.defaultValue)

    fun getMaxItems() = preferences.getInt(MaxItems.key, MaxItems.defaultValue)

    fun getMaxLines() = preferences.getInt(MaxLines.key, MaxLines.defaultValue)

    fun showDateCreated() = getDateFormat() != DateFormat.none

    fun getDateFormat() = preferences.getString(DateFormat.key, DateFormat.defaultValue)


    interface ListInfo {

        val title: Int

        val key: String
        val defaultValue: String

        fun getRawEntries(): Array<Int>

        fun getEntryValues(): Array<String>

        fun getEntries(context: Context): Array<String> {
            val rawEntries = getRawEntries()
            return Array(rawEntries.size) { index ->
                val id = rawEntries[index]
                context.getString(id)
            }
        }
    }

    interface SeekbarInfo {

        val title: Int

        val key: String
        val defaultValue: Int

        val min: Int
        val max: Int
    }


    object View : ListInfo {
        const val list = "list"
        const val grid = "grid"

        override val title = R.string.view
        override val key = "view"
        override val defaultValue = list

        override fun getRawEntries() = arrayOf(R.string.list, R.string.grid)

        override fun getEntryValues() = arrayOf(list, grid)
    }

    object Theme : ListInfo {
        const val dark = "dark"
        const val light = "light"
        const val followSystem = "followSystem"

        override val title = R.string.theme
        override val key = "theme"
        override val defaultValue = followSystem

        override fun getRawEntries() = arrayOf(R.string.dark, R.string.light, R.string.follow_system)

        override fun getEntryValues() = arrayOf(dark, light, followSystem)
    }

    object DateFormat : ListInfo {
        const val none = "none"
        const val relative = "relative"
        const val absolute = "absolute"

        override val title = R.string.date_format
        override val key = "dateFormat"
        override val defaultValue = relative

        override fun getRawEntries() = arrayOf(R.string.none, R.string.relative, R.string.absolute)

        override fun getEntryValues() = arrayOf(none, relative, absolute)
    }


    object MaxItems : SeekbarInfo {

        override val title = R.string.max_items_to_display

        override val key = "maxItemsToDisplayInList.v1"
        override val defaultValue = 4

        override val min = 1
        override val max = 10
    }

    object MaxLines : SeekbarInfo {

        override val title = R.string.max_lines_to_display

        override val key = "maxLinesToDisplayInNote.v1"
        override val defaultValue = 8

        override val min = 1
        override val max = 10
    }
}