package com.omgodse.notally

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

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
        val themeKey = getThemeKey()
        val default = getString(R.string.followSystemKey)
        when (preferences.getString(themeKey, default)) {
            getString(R.string.darkKey) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            getString(R.string.lightKey) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            getString(R.string.followSystemKey) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun getThemeKey() = getString(R.string.themeKey)
}