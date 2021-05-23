package com.omgodse.notally.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.databinding.FragmentNotesBinding
import com.omgodse.notally.helpers.MenuDialog
import com.omgodse.notally.helpers.MenuDialog.Operation
import com.omgodse.notally.helpers.OperationsParent
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.recyclerview.ItemDecoration
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.recyclerview.adapters.BaseNoteAdapter
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Label
import com.omgodse.notally.room.Type
import com.omgodse.notally.viewmodels.BaseNoteModel
import com.omgodse.post.PostPDFGenerator
import kotlinx.coroutines.launch
import java.io.File

abstract class NotallyFragment : Fragment(), OperationsParent, ItemListener {

    internal lateinit var mContext: Context
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
        settingsHelper = SettingsHelper(mContext)

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

        setupPadding()
        setupLayoutManager()
        setupItemDecoration()

        setupObserver()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentNotesBinding.inflate(layoutInflater)
        return binding?.root
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.RequestCodeExportFile && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
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
        adapter?.currentList?.get(position)?.let { baseNote ->
            val operations = getSupportedOperations(baseNote)
            if (operations.isNotEmpty()) {
                val menuHelper = MenuDialog(mContext)
                operations.forEach { menuHelper.addItem(it) }
                menuHelper.show()
            }
        }
    }


    override fun accessContext(): Context {
        return mContext
    }

    override fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) {
        model.insertLabel(label, onComplete)
    }


    private fun setupPadding() {
        val padding = mContext.resources.getDimensionPixelSize(R.dimen.recyclerViewPadding)
        if (settingsHelper.getCardType() == mContext.getString(R.string.elevatedKey)) {
            binding?.RecyclerView?.setPaddingRelative(padding, 0, padding, 0)
        }
    }

    private fun setupObserver() {
        getObservable()?.observe(viewLifecycleOwner, { list ->
            adapter?.submitList(list)

            if (list.isNotEmpty()) {
                binding?.RecyclerView?.visibility = View.VISIBLE
            } else binding?.RecyclerView?.visibility = View.GONE
        })
    }

    private fun setupLayoutManager() {
        binding?.RecyclerView?.layoutManager = if (settingsHelper.getView() == mContext.getString(R.string.gridKey)) {
            StaggeredGridLayoutManager(2, LinearLayout.VERTICAL)
        } else LinearLayoutManager(mContext)
    }

    private fun setupItemDecoration() {
        val cardMargin = mContext.resources.getDimensionPixelSize(R.dimen.cardMargin)
        if (settingsHelper.getCardType() == mContext.getString(R.string.elevatedKey)) {
            if (settingsHelper.getView() == mContext.getString(R.string.gridKey)) {
                binding?.RecyclerView?.addItemDecoration(ItemDecoration(cardMargin, 2))
            } else binding?.RecyclerView?.addItemDecoration(ItemDecoration(cardMargin, 1))
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

    internal fun confirmDeletion(baseNote: BaseNote) {
        MaterialAlertDialogBuilder(mContext)
            .setMessage(R.string.delete_note_forever)
            .setPositiveButton(R.string.delete) { dialog, which ->
                model.deleteBaseNoteForever(baseNote)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    internal fun showExportDialog(baseNote: BaseNote) {
        MenuDialog(mContext)
            .addItem(Operation(R.string.pdf, R.drawable.pdf) { exportBaseNoteToPDF(baseNote) })
            .addItem(Operation(R.string.html, R.drawable.html) { exportBaseNoteToHTML(baseNote) })
            .addItem(Operation(R.string.plain_text, R.drawable.plain_text) { exportBaseNoteToPlainText(baseNote) })
            .show()
    }

    internal fun goToActivity(activity: Class<*>, baseNote: BaseNote? = null) {
        val intent = Intent(mContext, activity)
        intent.putExtra(Constants.SelectedBaseNote, baseNote)
        intent.putExtra(Constants.PreviousFragment, findNavController().currentDestination?.id)
        startActivity(intent)
    }


    private fun exportBaseNoteToPDF(baseNote: BaseNote) {
        model.getPDFFile(baseNote, settingsHelper.getShowDateCreated(), object : PostPDFGenerator.OnResult {

            override fun onSuccess(file: File) {
                showFileOptionsDialog(file, "application/pdf")
            }

            override fun onFailure(message: String?) {
                Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun exportBaseNoteToHTML(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getHTMLFile(baseNote, settingsHelper.getShowDateCreated())
            showFileOptionsDialog(file, "text/html")
        }
    }

    private fun exportBaseNoteToPlainText(baseNote: BaseNote) {
        lifecycleScope.launch {
            val file = model.getPlainTextFile(baseNote, settingsHelper.getShowDateCreated())
            showFileOptionsDialog(file, "text/plain")
        }
    }

    private fun showFileOptionsDialog(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(mContext, "${mContext.packageName}.provider", file)

        MenuDialog(mContext)
            .addItem(Operation(R.string.view, R.drawable.view) { viewFile(uri, mimeType) })
            .addItem(Operation(R.string.share, R.drawable.share) { shareFile(uri, mimeType) })
            .addItem(Operation(R.string.save_to_device, R.drawable.save) { saveFileToDevice(file, mimeType) })
            .show()
    }


    private fun viewFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        val chooser = Intent.createChooser(intent, mContext.getString(R.string.view_note))
        startActivity(chooser)
    }

    private fun shareFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        val chooser = Intent.createChooser(intent, mContext.getString(R.string.share_note))
        startActivity(chooser)
    }

    private fun saveFileToDevice(file: File, mimeType: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = mimeType
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, file.nameWithoutExtension)
        }
        model.currentFile = file
        startActivityForResult(intent, Constants.RequestCodeExportFile)
    }


    abstract fun getBackground(): Int

    abstract fun getObservable(): LiveData<List<BaseNote>>?

    abstract fun getSupportedOperations(baseNote: BaseNote): ArrayList<Operation>
}