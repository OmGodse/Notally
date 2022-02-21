package com.omgodse.notally.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.preference.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.viewmodels.BaseNoteModel

class Settings : PreferenceFragmentCompat() {

    private val model: BaseNoteModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        val appearance = getPreferenceCategory(context, R.string.appearance)
        val contentDensity = getPreferenceCategory(context, R.string.content_density)
        val backup = getPreferenceCategory(context, R.string.backup)
        val about = getPreferenceCategory(context, R.string.about)

        screen.addPreference(appearance)
        screen.addPreference(contentDensity)
        screen.addPreference(backup)
        screen.addPreference(about)

        appearance.addPreference(getListPreference(context, SettingsHelper.View))
        appearance.addPreference(getListPreference(context, SettingsHelper.Theme))
        appearance.addPreference(getListPreference(context, SettingsHelper.DateFormat))

        contentDensity.addPreference(getSeekbarPreference(context, SettingsHelper.MaxItems))
        contentDensity.addPreference(getSeekbarPreference(context, SettingsHelper.MaxLines))

        val importBackup = getPreference(context, R.string.import_backup)
        val exportBackup = getPreference(context, R.string.export_backup)

        backup.addPreference(importBackup)
        backup.addPreference(exportBackup)

        val libraries = getPreference(context, R.string.libraries)
        val rate = getPreference(context, R.string.rate)
        val github = getPreference(context, R.string.github)

        about.addPreference(github)
        about.addPreference(libraries)
        about.addPreference(rate)

        disableIconSpace(screen)

        preferenceScreen = screen

        exportBackup.setOnPreferenceClickListener {
            exportBackup()
            return@setOnPreferenceClickListener true
        }

        importBackup.setOnPreferenceClickListener {
            importBackup()
            return@setOnPreferenceClickListener true
        }

        github.setOnPreferenceClickListener {
            openLink("https://github.com/OmGodse/Notally")
            return@setOnPreferenceClickListener true
        }

        libraries.setOnPreferenceClickListener {
            displayLibraries()
            return@setOnPreferenceClickListener true
        }

        rate.setOnPreferenceClickListener {
            openLink("https://play.google.com/store/apps/details?id=com.omgodse.notally")
            return@setOnPreferenceClickListener true
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            intent?.data?.let { uri ->
                when (requestCode) {
                    RequestCodeImportFile -> model.importBackup(uri)
                    Constants.RequestCodeExportFile -> model.exportBackup(uri)
                }
            }
        }
    }


    private fun disableIconSpace(group: PreferenceGroup) {
        for (index in 0 until group.preferenceCount) {
            val preference = group.getPreference(index)
            preference.isIconSpaceReserved = false
            if (preference is PreferenceGroup) {
                disableIconSpace(preference)
            }
        }
    }


    private fun exportBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = "text/xml"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, "Notally Backup")
        startActivityForResult(intent, Constants.RequestCodeExportFile)
    }

    private fun importBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "text/xml"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, RequestCodeImportFile)
    }

    private fun displayLibraries() {
        val libraries = arrayOf("Room", "Pretty Time", "Material Components for Android")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.libraries)
            .setItems(libraries) { dialog, which ->
                when (which) {
                    0 -> openLink("https://developer.android.com/jetpack/androidx/releases/room")
                    1 -> openLink("https://github.com/ocpsoft/prettytime")
                    2 -> openLink("https://github.com/material-components/material-components-android")
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    private fun openLink(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    companion object {

        private const val RequestCodeImportFile = 20

        private fun getPreference(context: Context, title: Int): Preference {
            val preference = Preference(context)
            preference.setTitle(title)
            return preference
        }

        private fun getPreferenceCategory(context: Context, title: Int): PreferenceCategory {
            val category = PreferenceCategory(context)
            category.setTitle(title)
            return category
        }


        private fun getListPreference(context: Context, info: SettingsHelper.ListInfo): ListPreference {
            val preference = ListPreference(context)
            preference.setTitle(info.title)
            preference.setDialogTitle(info.title)

            preference.key = info.key
            preference.entries = info.getEntries(context)
            preference.entryValues = info.getEntryValues()
            preference.setDefaultValue(info.defaultValue)
            preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()

            return preference
        }

        private fun getSeekbarPreference(context: Context, info: SettingsHelper.SeekbarInfo): SeekBarPreference {
            val preference = SeekBarPreference(context)
            preference.setTitle(info.title)

            preference.key = info.key
            preference.setDefaultValue(info.defaultValue)

            preference.min = info.min
            preference.max = info.max
            preference.showSeekBarValue = true

            return preference
        }
    }
}