package com.omgodse.notally

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.preferences.Theme

class NotallyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val preferences = Preferences.getInstance(this)
        preferences.theme.observeForever { theme ->
            when (theme) {
                Theme.dark -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                Theme.light -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                Theme.followSystem -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}