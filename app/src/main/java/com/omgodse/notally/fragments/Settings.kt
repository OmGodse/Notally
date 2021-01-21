package com.omgodse.notally.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.viewmodels.BaseNoteModel

class Settings : PreferenceFragmentCompat() {

    private lateinit var mContext: Context
    private val model: BaseNoteModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findPreference<Preference>(R.string.exportNotesToAFileKey)?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.type = "text/xml"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_TITLE, "Notally Backup")
            startActivityForResult(intent, Constants.RequestCodeExportFile)
            return@setOnPreferenceClickListener true
        }

        findPreference<Preference>(R.string.importNotesFromAFileKey)?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "text/xml"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, Constants.RequestCodeImportFile)
            return@setOnPreferenceClickListener true
        }

        findPreference<Preference>(R.string.githubKey)?.setOnPreferenceClickListener {
            openLink(Github)
            return@setOnPreferenceClickListener true
        }

        findPreference<Preference>(R.string.patreonKey)?.setOnPreferenceClickListener {
            openLink(Patreon)
            return@setOnPreferenceClickListener true
        }

        findPreference<Preference>(R.string.rateKey)?.setOnPreferenceClickListener {
            openLink(PlayStore)
            return@setOnPreferenceClickListener true
        }

        findPreference<Preference>(R.string.librariesKey)?.setOnPreferenceClickListener {
            val builder = MaterialAlertDialogBuilder(mContext)
            builder.setTitle(R.string.libraries)
            builder.setItems(R.array.libraries) { dialog, which ->
                when (which) {
                    0 -> openLink(Room)
                    1 -> openLink(PrettyTime)
                    2 -> openLink(MaterialComponents)
                }
            }
            builder.setNegativeButton(R.string.cancel, null)
            builder.show()
            return@setOnPreferenceClickListener true
        }

        findPreference<Preference>(R.string.maxItemsToDisplayInListKey)?.setOnPreferenceChangeListener { preference, newValue ->
            return@setOnPreferenceChangeListener newValue.toString().isNotEmpty()
        }

        findPreference<Preference>(R.string.maxLinesToDisplayInNoteKey)?.setOnPreferenceChangeListener { preference, newValue ->
            return@setOnPreferenceChangeListener newValue.toString().isNotEmpty()
        }

        findPreference<Preference>(R.string.themeKey)?.setOnPreferenceChangeListener { preference, newValue ->
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.RequestCodeExportFile && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                model.exportBackup(uri)
            }
        }
        if (requestCode == Constants.RequestCodeImportFile && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                model.importBackup(uri)
            }
        }
    }


    private fun openLink(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun <T> findPreference(id: Int): T? = findPreference(mContext.getString(id))

    companion object {
        private const val Patreon = "https://www.patreon.com/omgodse"
        private const val Github = "https://github.com/OmGodse/Notally"
        private const val PrettyTime = "https://github.com/ocpsoft/prettytime"
        private const val Room = "https://mvnrepository.com/artifact/androidx.room"
        private const val PlayStore = "https://play.google.com/store/apps/details?id=com.omgodse.notally"
        private const val MaterialComponents = "https://github.com/material-components/material-components-android"
    }
}