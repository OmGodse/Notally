package com.omgodse.notally.helpers

import android.content.Context
import androidx.preference.PreferenceManager
import com.omgodse.notally.R

class SettingsHelper(private val context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private fun getPreferenceValue(key: Int, defaultValue: Int): String {
        val actualKey = context.getString(key)
        val defaultValueString = context.getString(defaultValue)
        return preferences.getString(actualKey, defaultValueString).toString()
    }

    fun getViewPreference() = getPreferenceValue(R.string.viewKey, R.string.listKey)

    fun getNoteTypePreferences() = getPreferenceValue(R.string.cardTypeKey, R.string.elevatedKey)

    fun getMaxLinesPreference() = getPreferenceValue(R.string.maxLinesToDisplayInNoteKey, R.string.eight).toInt()

    fun getMaxItemsPreference() = getPreferenceValue(R.string.maxItemsToDisplayInListKey, R.string.four).toInt()

    fun getShowDateCreatedPreference() = preferences.getBoolean(context.getString(R.string.showDateCreatedKey), true)
}