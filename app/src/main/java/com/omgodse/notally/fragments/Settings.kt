package com.omgodse.notally.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
            startActivityForResult(intent, RequestCodeImportFile)
            return@setOnPreferenceClickListener true
        }

        bindPreferenceToLink(R.string.githubKey, "https://github.com/OmGodse/Notally")

        bindPreferenceToLink(R.string.patreonKey, "https://www.patreon.com/omgodse")

        bindPreferenceToLink(R.string.rateKey, "https://play.google.com/store/apps/details?id=com.omgodse.notally")

        findPreference<Preference>(R.string.librariesKey)?.setOnPreferenceClickListener {
            val builder = MaterialAlertDialogBuilder(mContext)
            builder.setTitle(R.string.libraries)
            builder.setItems(R.array.libraries) { dialog, which ->
                when (which) {
                    0 -> openLink("https://mvnrepository.com/artifact/androidx.room")
                    1 -> openLink("https://github.com/ocpsoft/prettytime")
                    2 -> openLink("https://github.com/material-components/material-components-android")
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
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
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


    private fun openLink(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun bindPreferenceToLink(keyId: Int, link: String) {
        findPreference<Preference>(keyId)?.setOnPreferenceClickListener {
            openLink(link)
            return@setOnPreferenceClickListener true
        }
    }

    private fun <T> findPreference(id: Int): T? = findPreference(mContext.getString(id))

    companion object {
        private const val RequestCodeImportFile = 20
    }
}