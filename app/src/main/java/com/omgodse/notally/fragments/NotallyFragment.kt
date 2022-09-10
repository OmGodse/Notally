package com.omgodse.notally.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PostPDFGenerator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.MenuDialog
import com.omgodse.notally.R
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.databinding.DialogColorBinding
import com.omgodse.notally.databinding.FragmentNotesBinding
import com.omgodse.notally.helpers.OperationsParent
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.recyclerview.adapters.BaseNoteAdapter
import com.omgodse.notally.recyclerview.adapters.ColorAdapter
import com.omgodse.notally.room.*
import com.omgodse.notally.viewmodels.BaseNoteModel
import kotlinx.coroutines.launch
import java.io.File

abstract class NotallyFragment : Fragment(), OperationsParent, ItemListener {

    private lateinit var settingsHelper: SettingsHelper

    private var adapter: BaseNoteAdapter? = null
    private var binding: FragmentNotesBinding? = null

    internal val model: BaseNoteModel by activityViewModels()

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        adapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        settingsHelper = SettingsHelper(requireContext())

        adapter = BaseNoteAdapter(settingsHelper, model.formatter, this)
        adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (itemCount > 0) {
                    binding?.RecyclerView?.scrollToPosition(positionStart)
                }
            }
        })
        binding?.RecyclerView?.adapter = adapter
        binding?.RecyclerView?.setHasFixedSize(true)

        binding?.ImageView?.setImageResource(getBackground())

        setupRecyclerView()
        setupObserver()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentNotesBinding.inflate(inflater)
        return binding?.root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == Constants.RequestCodeExportFile && resultCode == Activity.RESULT_OK) {
            intent?.data?.let { uri ->
                model.writeCurrentFileToUri(uri)
            }
        }
    }


    override fun onClick(position: Int) {
        adapter?.currentList?.get(position)?.let { item ->
            if (item is BaseNote) {
                when (item.type) {
                    Type.NOTE -> goToActivity(TakeNote::class.java, item)
                    Type.LIST -> goToActivity(MakeList::class.java, item)
                }
            }
        }
    }

    override fun onLongClick(position: Int) {
        adapter?.currentList?.get(position)?.let { item ->
            if (item is BaseNote) {
                showOperations(item)
            }
        }
    }


    override fun accessContext(): Context {
        return requireContext()
    }

    override fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) {
        model.insertLabel(label, onComplete)
    }


    private fun setupObserver() {
        getObservable().observe(viewLifecycleOwner) { list ->
            adapter?.submitList(list)
            binding?.RecyclerView?.isVisible = list.isNotEmpty()
        }
    }

    private fun setupRecyclerView() {
        binding?.RecyclerView?.layoutManager = if (settingsHelper.getView() == SettingsHelper.View.grid) {
            StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
        } else LinearLayoutManager(requireContext())
    }


    private fun showOperations(baseNote: BaseNote) {
        val dialog = MenuDialog(requireContext())
        when (baseNote.folder) {
            Folder.NOTES -> {
                if (baseNote.pinned) {
                    dialog.add(R.string.unpin) { model.unpinBaseNote(baseNote.id) }
                } else dialog.add(R.string.pin) { model.pinBaseNote(baseNote.id) }
                dialog.add(R.string.share) { share(baseNote) }
                dialog.add(R.string.labels) { label(baseNote) }
                dialog.add(R.string.export) { export(baseNote) }
                dialog.add(R.string.delete) { model.moveBaseNoteToDeleted(baseNote.id) }
                dialog.add(R.string.archive) { model.moveBaseNoteToArchive(baseNote.id) }
                dialog.add(R.string.change_color) { color(baseNote) }
            }
            Folder.DELETED -> {
                dialog.add(R.string.restore) { model.restoreBaseNote(baseNote.id) }
                dialog.add(R.string.delete_forever) { delete(baseNote) }
            }
            Folder.ARCHIVED -> {
                dialog.add(R.string.unarchive) { model.restoreBaseNote(baseNote.id) }
            }
        }
        dialog.show()
    }

    internal fun goToActivity(activity: Class<*>, baseNote: BaseNote? = null) {
        val intent = Intent(requireContext(), activity)
        intent.putExtra(Constants.SelectedBaseNote, baseNote)
        startActivity(intent)
    }


    private fun share(baseNote: BaseNote) {
        val body = when (baseNote.type) {
            Type.NOTE -> baseNote.body.applySpans(baseNote.spans)
            Type.LIST -> Operations.getBody(baseNote.items)
        }
        Operations.shareNote(requireContext(), baseNote.title, body)
    }

    private fun label(baseNote: BaseNote) {
        lifecycleScope.launch {
            val labels = model.getAllLabelsAsList()
            labelNote(labels, baseNote.labels) { updatedLabels ->
                model.updateBaseNoteLabels(updatedLabels, baseNote.id)
            }
        }
    }

    private fun export(baseNote: BaseNote) {
        MenuDialog(requireContext())
            .add(R.string.pdf) { exportToPDF(baseNote) }
            .add(R.string.txt) { exportToTXT(baseNote) }
            .add(R.string.xml) { exportToXML(baseNote) }
            .add(R.string.json) { exportToJSON(baseNote) }
            .add(R.string.html) { exportToHTML(baseNote) }
            .show()
    }

    private fun delete(baseNote: BaseNote) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_note_forever)
            .setPositiveButton(R.string.delete) { dialog, which ->
                model.deleteBaseNoteForever(baseNote)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun color(baseNote: BaseNote) {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_color)
            .create()

        val colorAdapter = ColorAdapter(object : ItemListener {
            override fun onClick(position: Int) {
                dialog.dismiss()
                val color = Color.values()[position]
                model.colorBaseNote(baseNote.id, color)
            }

            override fun onLongClick(position: Int) {}
        })

        val dialogBinding = DialogColorBinding.inflate(layoutInflater)
        dialogBinding.RecyclerView.adapter = colorAdapter

        dialog.setView(dialogBinding.root)
        dialog.show()
    }


    private fun exportToPDF(baseNote: BaseNote) {
        model.getPDFFile(baseNote, settingsHelper.showDateCreated(), object : PostPDFGenerator.OnResult {

            override fun onSuccess(file: File) {
                showFileOptionsDialog(file, "application/pdf")
            }

            override fun onFailure(message: CharSequence?) {
                Toast.makeText(requireContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun exportToTXT(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getTXTFile(baseNote, settingsHelper.showDateCreated())
            showFileOptionsDialog(file, "text/plain")
        }
    }

    private fun exportToXML(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getXMLFile(baseNote)
            showFileOptionsDialog(file, "text/xml")
        }
    }

    private fun exportToJSON(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getJSONFile(baseNote)
            showFileOptionsDialog(file, "application/json")
        }
    }

    private fun exportToHTML(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getHTMLFile(baseNote, settingsHelper.showDateCreated())
            showFileOptionsDialog(file, "text/html")
        }
    }

    private fun showFileOptionsDialog(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)

        MenuDialog(requireContext())
            .add(R.string.share) { shareFile(uri, mimeType) }
            .add(R.string.view_file) { viewFile(uri, mimeType) }
            .add(R.string.save_to_device) { saveFileToDevice(file, mimeType) }
            .show()
    }


    private fun viewFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, mimeType)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        val chooser = Intent.createChooser(intent, getString(R.string.view_note))
        startActivity(chooser)
    }

    private fun shareFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_STREAM, uri)

        val chooser = Intent.createChooser(intent, null)
        startActivity(chooser)
    }

    private fun saveFileToDevice(file: File, mimeType: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = mimeType
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, file.nameWithoutExtension)

        model.currentFile = file
        startActivityForResult(intent, Constants.RequestCodeExportFile)
    }


    abstract fun getBackground(): Int

    abstract fun getObservable(): LiveData<List<Item>>
}