package com.omgodse.notally.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.method.DigitsKeyListener
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.viewmodels.BaseNoteModel

class Settings : PreferenceFragmentCompat() {

    private val model: BaseNoteModel by activityViewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        findPreference<Preference>(R.string.exportBackupKey)?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.type = "text/xml"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_TITLE, "Notally Backup")
            startActivityForResult(intent, Constants.RequestCodeExportFile)
            return@setOnPreferenceClickListener true
        }

        findPreference<Preference>(R.string.importBackupKey)?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "text/xml"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intent, RequestCodeImportFile)
            return@setOnPreferenceClickListener true
        }

        val libraries = arrayOf("Room", "Pretty Time", "Material Components for Android")
        findPreference<Preference>(R.string.librariesKey)?.setOnPreferenceClickListener {
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
            return@setOnPreferenceClickListener true
        }

        val bind = { editText: EditText ->
            val maxLength = InputFilter.LengthFilter(1)
            editText.filters = arrayOf(maxLength)
            editText.inputType = EditorInfo.TYPE_CLASS_NUMBER
            editText.keyListener = DigitsKeyListener.getInstance("123456789")
        }

        val maxItems = findPreference<EditTextPreference>(R.string.maxItemsToDisplayInListKey)
        val maxLines = findPreference<EditTextPreference>(R.string.maxLinesToDisplayInNoteKey)

        maxItems?.setOnBindEditTextListener(bind)
        maxLines?.setOnBindEditTextListener(bind)

        val onChange = Preference.OnPreferenceChangeListener { preference, newValue -> newValue.toString().isNotEmpty() }

        maxItems?.onPreferenceChangeListener = onChange
        maxLines?.onPreferenceChangeListener = onChange
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

    private fun <T> findPreference(id: Int): T? = findPreference(getString(id))

    companion object {
        private const val RequestCodeImportFile = 20
    }
}