package com.omgodse.notally.preferences

import android.content.Context
import com.omgodse.notally.R
import com.omgodse.notally.viewmodels.BaseNoteModel
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

sealed interface ListInfo {

    val title: Int

    val key: String
    val defaultValue: String

    fun getEntryValues(): Array<String>

    fun getEntries(context: Context): Array<String>

    fun convertToValues(ids: Array<Int>, context: Context): Array<String> {
        return Array(ids.size) { index ->
            val id = ids[index]
            context.getString(id)
        }
    }
}

object View : ListInfo {
    const val list = "list"
    const val grid = "grid"

    override val title = R.string.view
    override val key = "view"
    override val defaultValue = list

    override fun getEntryValues() = arrayOf(list, grid)

    override fun getEntries(context: Context): Array<String> {
        val ids = arrayOf(R.string.list, R.string.grid)
        return convertToValues(ids, context)
    }
}

object Theme : ListInfo {
    const val dark = "dark"
    const val light = "light"
    const val followSystem = "followSystem"

    override val title = R.string.theme
    override val key = "theme"
    override val defaultValue = followSystem

    override fun getEntryValues() = arrayOf(dark, light, followSystem)

    override fun getEntries(context: Context): Array<String> {
        val ids = arrayOf(R.string.dark, R.string.light, R.string.follow_system)
        return convertToValues(ids, context)
    }
}

object DateFormat : ListInfo {
    const val none = "none"
    const val relative = "relative"
    const val absolute = "absolute"

    override val title = R.string.date_format
    override val key = "dateFormat"
    override val defaultValue = relative

    override fun getEntryValues() = arrayOf(none, relative, absolute)

    override fun getEntries(context: Context): Array<String> {
        val none = context.getString(R.string.none)
        val date = Date(System.currentTimeMillis() - 86400000)
        val relative = PrettyTime().format(date)
        val absolute = BaseNoteModel.getDateFormatter(context).format(date)
        return arrayOf(none, relative, absolute)
    }
}

object TextSize : ListInfo {
    const val small = "small"
    const val medium = "medium"
    const val large = "large"

    override val title = R.string.text_size
    override val key = "textSize"
    override val defaultValue = medium

    override fun getEntryValues() = arrayOf(small, medium, large)

    override fun getEntries(context: Context): Array<String> {
        val ids = arrayOf(R.string.small, R.string.medium, R.string.large)
        return convertToValues(ids, context)
    }
}