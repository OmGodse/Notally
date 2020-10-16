package com.omgodse.notally.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.omgodse.notally.R
import com.omgodse.notally.activities.MainActivity
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.databinding.FragmentNotesBinding
import com.omgodse.notally.helpers.ExportHelper
import com.omgodse.notally.helpers.MenuHelper
import com.omgodse.notally.helpers.OperationsHelper
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.recyclerview.ItemDecoration
import com.omgodse.notally.recyclerview.adapters.BaseNoteAdapter
import com.omgodse.notally.viewmodels.BaseNoteModel
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.List
import com.omgodse.notally.xml.Note
import java.io.File

abstract class NotallyFragment : Fragment() {

    internal lateinit var mContext: Context
    private lateinit var exportHelper: ExportHelper
    private lateinit var settingsHelper: SettingsHelper

    private var adapter: BaseNoteAdapter? = null
    internal var binding: FragmentNotesBinding? = null

    internal val model: BaseNoteModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        adapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        settingsHelper = SettingsHelper(mContext)
        exportHelper = ExportHelper(mContext, this)

        adapter = BaseNoteAdapter(mContext)
        adapter?.onNoteClicked = this::onBaseNoteClicked
        adapter?.onNoteLongClicked = this::onBaseNoteLongClicked
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.RequestCodeExportFile && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                exportHelper.writeFileToUri(uri)
            }
        } else if (resultCode == Constants.ResultCodeCreatedFile) {
            val filePath = data?.getStringExtra(Constants.FilePath)
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    val baseNote = BaseNote.readFromFile(file)
                    if (baseNote.isEmpty()) {
                        model.moveBaseNoteToDeleted(baseNote)
                        val rootView = (mContext as MainActivity).binding.CoordinatorLayout
                        Snackbar.make(rootView, baseNote.getEmptyMessage(), Snackbar.LENGTH_SHORT).show()
                    } else binding?.RecyclerView?.layoutManager?.scrollToPosition(0)
                }
            }
        }
    }


    private fun onBaseNoteClicked(position: Int) {
        when (val baseNote = adapter?.currentList?.get(position)) {
            is Note -> goToActivity(TakeNote::class.java, baseNote.filePath)
            is List -> goToActivity(MakeList::class.java, baseNote.filePath)
        }
    }

    private fun onBaseNoteLongClicked(position: Int) {
        adapter?.currentList?.get(position)?.let { baseNote ->
            val notesHelper = OperationsHelper(mContext)

            val operations = getSupportedOperations(notesHelper, baseNote)
            if (operations.isNotEmpty()) {
                val menuHelper = MenuHelper(mContext)
                operations.forEach { menuHelper.addItem(it) }
                menuHelper.show()
            }
        }
    }


    private fun setupPadding() {
        val padding = mContext.resources.getDimensionPixelSize(R.dimen.recyclerViewPadding)
        if (settingsHelper.getCardType() == mContext.getString(R.string.elevatedKey)) {
            binding?.RecyclerView?.setPaddingRelative(padding, 0, padding, 0)
        }
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

    private fun setupObserver() {
        getObservable()?.observe(viewLifecycleOwner, {
            adapter?.submitList(ArrayList(it))

            if (it.isNotEmpty()) {
                binding?.RecyclerView?.visibility = View.VISIBLE
            } else binding?.RecyclerView?.visibility = View.GONE
        })
    }


    internal fun labelBaseNote(baseNote: BaseNote) {
        val operationsHelper = OperationsHelper(mContext)
        operationsHelper.labelNote(baseNote.labels) { labels ->
            model.editNoteLabel(baseNote, labels)
        }
    }

    internal fun confirmDeletion(baseNote: BaseNote) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(mContext)
        alertDialogBuilder.setMessage(R.string.delete_note_forever)
        alertDialogBuilder.setPositiveButton(R.string.delete) { dialog, which ->
            model.deleteBaseNoteForever(baseNote)
        }
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)
        alertDialogBuilder.show()
    }

    internal fun showExportDialog(baseNote: BaseNote) {
        MenuHelper(mContext)
            .addItem(Operation(R.string.pdf, R.drawable.pdf) { exportHelper.exportBaseNoteToPDF(baseNote) })
            .addItem(Operation(R.string.html, R.drawable.html) { exportHelper.exportBaseNoteToHTML(baseNote) })
            .addItem(Operation(R.string.plain_text, R.drawable.plain_text) { exportHelper.exportBaseNoteToPlainText(baseNote) })
            .show()
    }

    internal fun goToActivity(activity: Class<*>, filePath: String? = null) {
        val intent = Intent(mContext, activity)
        intent.putExtra(Constants.FilePath, filePath)
        intent.putExtra(Constants.PreviousFragment, getFragmentID())
        startActivityForResult(intent, Constants.RequestCode)
    }


    abstract fun getFragmentID(): Int

    abstract fun getBackground(): Int

    abstract fun getObservable(): MutableLiveData<ArrayList<BaseNote>>?

    abstract fun getSupportedOperations(operationsHelper: OperationsHelper, baseNote: BaseNote): ArrayList<Operation>
}