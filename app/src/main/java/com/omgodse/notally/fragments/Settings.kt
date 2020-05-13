package com.omgodse.notally.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R

class Settings : PreferenceFragmentCompat() {

    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themePreference: ListPreference? = findPreference(mContext.getString(R.string.themeKey))

        val maxItemsPref: EditTextPreference? = findPreference(mContext.getString(R.string.maxItemsToDisplayInListKey))
        val maxLinesPref: EditTextPreference? = findPreference(mContext.getString(R.string.maxLinesToDisplayInNoteKey))

        val ratePref: Preference? = findPreference(mContext.getString(R.string.rateKey))
        val githubPref: Preference? = findPreference(mContext.getString(R.string.githubKey))
        val librariesPref: Preference? = findPreference(mContext.getString(R.string.librariesKey))

        githubPref?.setOnPreferenceClickListener {
            openLink(Github)
            return@setOnPreferenceClickListener true
        }

        ratePref?.setOnPreferenceClickListener {
            openLink(PlayStore)
            return@setOnPreferenceClickListener true
        }

        librariesPref?.setOnPreferenceClickListener {
            val libraries = arrayOf("iText", "Jsoup", "Pretty Time", "Material Components for Android")
            val builder = MaterialAlertDialogBuilder(mContext)
            builder.setTitle(R.string.libraries)
            builder.setItems(libraries) { dialog, which ->
                when (which) {
                    0 -> openLink(iText)
                    1 -> openLink(Jsoup)
                    2 -> openLink(PrettyTime)
                    3 -> openLink(MaterialComponents)
                }
            }
            builder.setNegativeButton(R.string.cancel, null)
            builder.show()
            return@setOnPreferenceClickListener true
        }

        maxItemsPref?.setOnPreferenceChangeListener { preference, newValue ->
            return@setOnPreferenceChangeListener newValue.toString().isNotEmpty()
        }

        maxLinesPref?.setOnPreferenceChangeListener { preference, newValue ->
            return@setOnPreferenceChangeListener newValue.toString().isNotEmpty()
        }

        themePreference?.setOnPreferenceChangeListener { preference, newValue ->
            when (newValue) {
                getString(R.string.darkKey) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                getString(R.string.lightKey) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                getString(R.string.followSystemKey) -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            return@setOnPreferenceChangeListener true
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    private fun openLink(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    companion object {
        private const val Github = "https://github.com/OmGodse/Notally"
        private const val PlayStore = "https://play.google.com/store/apps/details?id=com.omgodse.notally"
        private const val iText = "https://github.com/itext/itextpdf"
        private const val Jsoup = "https://github.com/jhy/jsoup/"
        private const val PrettyTime = "https://github.com/ocpsoft/prettytime"
        private const val MaterialComponents = "https://github.com/material-components/material-components-android"
    }
}