package com.omgodse.notally

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.omgodse.notally.helpers.SettingsHelper.Theme

class NotallyApplication : Application(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate() {
        super.onCreate()

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(preferences)

        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?) {
        if (key == getThemeKey()) {
            setTheme(preferences)
        }
    }


    private fun setTheme(preferences: SharedPreferences) {
        val key = getThemeKey()
        val default = Theme.defaultValue
        when (preferences.getString(key, default)) {
            Theme.dark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Theme.light -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Theme.followSystem -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun getThemeKey() = Theme.key
}