package com.omgodse.notally.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.MenuDialog
import com.omgodse.notally.R
import com.omgodse.notally.databinding.FragmentSettingsBinding
import com.omgodse.notally.databinding.PreferenceListBinding
import com.omgodse.notally.databinding.PreferenceSeekbarBinding
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.preferences.*
import com.omgodse.notally.viewmodels.BaseNoteModel

class Settings : Fragment() {

    private var binding: FragmentSettingsBinding? = null

    private val model: BaseNoteModel by activityViewModels()

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.preferences.view.observe(viewLifecycleOwner) { value ->
            binding?.View?.setup(View, value)
        }

        model.preferences.theme.observe(viewLifecycleOwner) { value ->
            binding?.Theme?.setup(Theme, value)
        }

        model.preferences.dateFormat.observe(viewLifecycleOwner) { value ->
            binding?.DateFormat?.setup(DateFormat, value)
        }


        binding?.MaxItems?.setup(MaxItems, model.preferences.maxItems.value)

        binding?.MaxLines?.setup(MaxLines, model.preferences.maxLines.value)


        binding?.ImportBackup?.setOnClickListener {
            importBackup()
        }

        binding?.ExportBackup?.setOnClickListener {
            exportBackup()
        }

        binding?.GitHub?.setOnClickListener {
            openLink("https://github.com/OmGodse/Notally")
        }

        binding?.Libraries?.setOnClickListener {
            displayLibraries()
        }

        binding?.Rate?.setOnClickListener {
            openLink("https://play.google.com/store/apps/details?id=com.omgodse.notally")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSettingsBinding.inflate(inflater)
        return binding?.root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            intent?.data?.let { uri ->
                when (requestCode) {
                    RequestCodeImportXml -> model.importXmlBackup(uri)
                    RequestCodeImportZip -> model.importZipBackup(uri)
                    Constants.RequestCodeExportFile -> model.exportBackup(uri)
                }
            }
        }
    }


    private fun exportBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = "application/zip"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, "Notally Backup")
        startActivityForResult(intent, Constants.RequestCodeExportFile)
    }

    private fun importBackup() {
        MenuDialog(requireContext())
            .add(R.string.zip) { launchImportActivity("application/zip", RequestCodeImportZip) }
            .add(R.string.xml) { launchImportActivity("text/xml", RequestCodeImportXml) }
            .show()
    }

    private fun launchImportActivity(type: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = type
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(intent, requestCode)
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


    private fun PreferenceListBinding.setup(info: ListInfo, value: String) {
        Title.setText(info.title)

        val entries = info.getEntries(requireContext())
        val entryValues = info.getEntryValues()

        val checked = entryValues.indexOf(value)
        val displayValue = entries[checked]

        Value.text = displayValue

        root.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(info.title)
                .setSingleChoiceItems(entries, checked) { dialog, which ->
                    dialog.cancel()
                    val newValue = entryValues[which]
                    model.savePreference(info, newValue)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun PreferenceSeekbarBinding.setup(info: SeekbarInfo, initialValue: Int) {
        Title.setText(info.title)

        Slider.valueTo = info.max.toFloat()
        Slider.valueFrom = info.min.toFloat()

        Slider.value = initialValue.toFloat()

        Slider.addOnChangeListener { slider, value, fromUser ->
            model.savePreference(info, value.toInt())
        }
    }


    private fun openLink(link: String) {
        val uri = Uri.parse(link)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    companion object {
        private const val RequestCodeImportXml = 20
        private const val RequestCodeImportZip = 21
    }
}