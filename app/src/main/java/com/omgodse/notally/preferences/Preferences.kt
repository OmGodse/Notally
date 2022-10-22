package com.omgodse.notally.preferences

import android.app.Application
import android.preference.PreferenceManager

class Preferences private constructor(app: Application) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(app)
    private val editor = preferences.edit()

    // Main thread (unfortunately)
    val view = BetterLiveData(getListPreferenceValue(View))
    val theme = BetterLiveData(getListPreferenceValue(Theme))
    val dateFormat = BetterLiveData(getListPreferenceValue(DateFormat))

    val maxItems = BetterLiveData(getSeekbarPreferenceValue(MaxItems))
    val maxLines = BetterLiveData(getSeekbarPreferenceValue(MaxLines))

    private fun getListPreferenceValue(info: ListInfo): String {
        return requireNotNull(preferences.getString(info.key, info.defaultValue))
    }

    private fun getSeekbarPreferenceValue(info: SeekbarInfo): Int {
        return requireNotNull(preferences.getInt(info.key, info.defaultValue))
    }


    fun savePreference(info: SeekbarInfo, value: Int) {
        editor.putInt(info.key, value)
        editor.commit()
        when (info) {
            MaxItems -> maxItems.postValue(getSeekbarPreferenceValue(MaxItems))
            MaxLines -> maxLines.postValue(getSeekbarPreferenceValue(MaxLines))
        }
    }

    fun savePreference(info: ListInfo, value: String) {
        editor.putString(info.key, value)
        editor.commit()
        when (info) {
            View -> view.postValue(getListPreferenceValue(View))
            Theme -> theme.postValue(getListPreferenceValue(Theme))
            DateFormat -> dateFormat.postValue(getListPreferenceValue(DateFormat))
        }
    }


    fun showDateCreated(): Boolean {
        return dateFormat.value != DateFormat.none
    }

    companion object {

        @Volatile
        private var instance: Preferences? = null

        fun getInstance(app: Application): Preferences {
            return instance ?: synchronized(this) {
                val instance = Preferences(app)
                this.instance = instance
                return instance
            }
        }
    }
}