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
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.omgodse.notally.R
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.databinding.FragmentNotesBinding
import com.omgodse.notally.helpers.OperationsParent
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.recyclerview.ItemDecoration
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.recyclerview.adapters.BaseNoteAdapter
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Label
import com.omgodse.notally.room.Type
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

        adapter = BaseNoteAdapter(settingsHelper, this)
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.RequestCodeExportFile && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                model.writeCurrentFileToUri(uri)
            }
        }
    }


    override fun onClick(position: Int) {
        adapter?.currentList?.get(position)?.let { baseNote ->
            when (baseNote.type) {
                Type.NOTE -> goToActivity(TakeNote::class.java, baseNote)
                Type.LIST -> goToActivity(MakeList::class.java, baseNote)
            }
        }
    }

    override fun onLongClick(position: Int) {
        adapter?.currentList?.get(position)?.let { baseNote -> showOperations(baseNote) }
    }


    override fun accessContext(): Context {
        return requireContext()
    }

    override fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) {
        model.insertLabel(label, onComplete)
    }


    private fun setupObserver() {
        getObservable()?.observe(viewLifecycleOwner, { list ->
            adapter?.submitList(list)
            binding?.RecyclerView?.isVisible = list.isNotEmpty()
        })
    }

    private fun setupRecyclerView() {
        val columns = if (settingsHelper.getView() == getString(R.string.gridKey)) {
            2
        } else 1

        val cardMargin = resources.getDimensionPixelSize(R.dimen.cardMargin)
        val itemDecoration = ItemDecoration(cardMargin, columns)
        binding?.RecyclerView?.addItemDecoration(itemDecoration)
        binding?.RecyclerView?.layoutManager = StaggeredGridLayoutManager(columns, RecyclerView.VERTICAL)
    }


    internal fun shareBaseNote(baseNote: BaseNote) {
        when (baseNote.type) {
            Type.NOTE -> shareNote(baseNote.title, baseNote.body.applySpans(baseNote.spans))
            Type.LIST -> shareNote(baseNote.title, baseNote.items)
        }
    }

    internal fun labelBaseNote(baseNote: BaseNote) {
        lifecycleScope.launch {
            val labels = model.getAllLabelsAsList()
            labelNote(labels, baseNote.labels) { updatedLabels ->
                model.updateBaseNoteLabels(updatedLabels, baseNote.id)
            }
        }
    }

    internal fun exportBaseNote(baseNote: BaseNote) {
        val pdf = Operation(R.string.pdf, R.drawable.pdf) { exportBaseNoteToPDF(baseNote) }
        val txt = Operation(R.string.txt, R.drawable.txt) { exportBaseNoteToTXT(baseNote) }
        val xml = Operation(R.string.xml, R.drawable.xml) { exportBaseNoteToXML(baseNote) }
        val json = Operation(R.string.json, R.drawable.json) { exportBaseNoteToJSON(baseNote) }
        val html = Operation(R.string.html, R.drawable.html) { exportBaseNoteToHTML(baseNote) }
        showMenu(pdf, txt, xml, json, html)
    }

    internal fun goToActivity(activity: Class<*>, baseNote: BaseNote? = null) {
        val intent = Intent(requireContext(), activity)
        intent.putExtra(Constants.SelectedBaseNote, baseNote)
        startActivity(intent)
    }


    private fun exportBaseNoteToPDF(baseNote: BaseNote) {
        model.getPDFFile(baseNote, settingsHelper.getShowDateCreated(), object : PostPDFGenerator.OnResult {

            override fun onSuccess(file: File) {
                showFileOptionsDialog(file, "application/pdf")
            }

            override fun onFailure(message: String?) {
                Toast.makeText(requireContext(), R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun exportBaseNoteToTXT(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getTXTFile(baseNote, settingsHelper.getShowDateCreated())
            showFileOptionsDialog(file, "text/plain")
        }
    }

    private fun exportBaseNoteToXML(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getXMLFile(baseNote)
            showFileOptionsDialog(file, "text/xml")
        }
    }

    private fun exportBaseNoteToJSON(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getJSONFile(baseNote)
            showFileOptionsDialog(file, "application/json")
        }
    }

    private fun exportBaseNoteToHTML(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getHTMLFile(baseNote, settingsHelper.getShowDateCreated())
            showFileOptionsDialog(file, "text/html")
        }
    }

    private fun showFileOptionsDialog(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)

        val share = Operation(R.string.share, R.drawable.share) { shareFile(uri, mimeType) }
        val viewFile = Operation(R.string.view_file, R.drawable.view) { viewFile(uri, mimeType) }
        val saveToDevice = Operation(R.string.save_to_device, R.drawable.save) { saveFileToDevice(file, mimeType) }
        showMenu(share, viewFile, saveToDevice)
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

        val chooser = Intent.createChooser(intent, getString(R.string.share_note))
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

    abstract fun getObservable(): LiveData<List<BaseNote>>?

    abstract fun showOperations(baseNote: BaseNote)
}