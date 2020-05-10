package com.omgodse.notally.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.omgodse.notally.R
import com.omgodse.notally.activities.MainActivity
import com.omgodse.notally.adapters.LabelsAdapter
import com.omgodse.notally.databinding.FragmentNotesBinding
import com.omgodse.notally.helpers.MenuHelper
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.interfaces.DialogListener
import com.omgodse.notally.interfaces.NoteListener
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.xml.XMLReader
import com.omgodse.notally.xml.XMLTags
import com.omgodse.notally.xml.XMLWriter
import java.io.File
import java.io.FileWriter

class Labels : Fragment(), NoteListener {

    private lateinit var mContext: Context
    private lateinit var notesHelper: NotesHelper
    private lateinit var labelsAdapter: LabelsAdapter
    private lateinit var binding: FragmentNotesBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.AddLabel) {
            displayAddLabelDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.label, menu)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        notesHelper = NotesHelper(mContext)
        labelsAdapter = LabelsAdapter(mContext, ArrayList())
        labelsAdapter.noteListener = this

        binding.RecyclerView.adapter = labelsAdapter
        binding.RecyclerView.layoutManager = LinearLayoutManager(mContext)
        binding.RecyclerView.addItemDecoration(DividerItemDecoration(mContext, RecyclerView.VERTICAL))

        setupFrameLayout()
        populateRecyclerView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        binding = FragmentNotesBinding.inflate(inflater)
        return binding.root
    }


    override fun onNoteClicked(position: Int) {
        val label = labelsAdapter.items[position]
        val bundle = bundleOf(Constants.argLabelKey to label)
        findNavController().navigate(R.id.LabelsFragmentToDisplayLabel, bundle)
    }

    override fun onNoteLongClicked(position: Int) {
        val menuHelper = MenuHelper(mContext)

        menuHelper.addItem(R.string.edit, R.drawable.edit)
        menuHelper.addItem(R.string.delete, R.drawable.delete)

        menuHelper.setListener(object : DialogListener {
            override fun onDialogItemClicked(label: String) {
                when (label) {
                    mContext.getString(R.string.edit) -> displayEditLabelDialog(position)
                    mContext.getString(R.string.delete) -> confirmDeletion(position)
                }
            }
        })

        menuHelper.show()
    }


    private fun confirmVisibility() {
        if (labelsAdapter.itemCount > 0) {
            binding.RecyclerView.visibility = View.VISIBLE
        } else {
            binding.RecyclerView.visibility = View.GONE
            (mContext as MainActivity).binding.AppBarLayout.setExpanded(true, true)
        }
    }

    private fun setupFrameLayout() {
        binding.FrameLayout.background = mContext.getDrawable(R.drawable.layout_background_labels)
    }

    private fun populateRecyclerView() {
        val labels = notesHelper.getSortedLabelsList()
        labelsAdapter.items = ArrayList(labels)
        labelsAdapter.notifyDataSetChanged()
        confirmVisibility()
    }


    private fun displayAddLabelDialog() {
        val priorLabels = notesHelper.getSortedLabelsList()

        val dialogHelper = DialogHelper(mContext)
        dialogHelper.setTitle(R.string.add_label)
        dialogHelper.onPositiveButtonClicked(View.OnClickListener {
            val label = dialogHelper.getEnteredValue()
            if (label.isNotEmpty()) {
                if (!priorLabels.contains(label)) {
                    insertLabel(label)
                    dialogHelper.dismiss()
                } else dialogHelper.displayValueExistsError()
            } else dialogHelper.dismiss()
        })
        dialogHelper.showDialog()
    }

    private fun confirmDeletion(position: Int) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(mContext)
        alertDialogBuilder.setTitle(R.string.delete_label)
        alertDialogBuilder.setMessage(R.string.your_notes_associated)
        alertDialogBuilder.setPositiveButton(R.string.delete) { dialog, which ->
            deleteLabel(position)
            confirmVisibility()
        }
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)
        alertDialogBuilder.show()
    }

    private fun displayEditLabelDialog(position: Int) {
        val label = labelsAdapter.items[position]
        val priorLabels = notesHelper.getSortedLabelsList()

        val dialogHelper = DialogHelper(mContext)
        dialogHelper.setTitle(R.string.edit_label)
        dialogHelper.setInputText(label)
        dialogHelper.onPositiveButtonClicked(View.OnClickListener {
            val enteredValue = dialogHelper.getEnteredValue()
            when {
                enteredValue.isEmpty() -> dialogHelper.dismiss()
                enteredValue == label -> dialogHelper.dismiss()
                !priorLabels.contains(enteredValue) -> {
                    editLabel(position, enteredValue)
                    dialogHelper.dismiss()
                }
                priorLabels.contains(enteredValue) -> dialogHelper.displayValueExistsError()
            }
        })
        dialogHelper.showDialog()
    }


    private fun deleteLabel(position: Int) {
        val label = labelsAdapter.items[position]

        val priorLabels = notesHelper.getSortedLabelsList()
        val newLabels = HashSet(priorLabels)
        newLabels.remove(label)

        val sharedPreferences = mContext.getSharedPreferences(Constants.labelsPreferences, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putStringSet(Constants.labelItems, newLabels)
        editor.apply()

        labelsAdapter.items.removeAt(position)
        labelsAdapter.notifyItemRemoved(position)

        val notesFiles = notesHelper.getNotePath().listFiles()
        val deletedFiles = notesHelper.getDeletedPath().listFiles()
        val archivedFiles = notesHelper.getArchivedPath().listFiles()

        notesFiles?.forEach { file ->
            deleteLabelFromFile(label, file)
        }

        deletedFiles?.forEach { file ->
            deleteLabelFromFile(label, file)
        }

        archivedFiles?.forEach { file ->
            deleteLabelFromFile(label, file)
        }
    }

    private fun insertLabel(label: String) {
        val priorLabels = notesHelper.getSortedLabelsList()
        val newLabels = HashSet(priorLabels)
        newLabels.add(label)

        val sharedPreferences = mContext.getSharedPreferences(Constants.labelsPreferences, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putStringSet(Constants.labelItems, newLabels)
        editor.apply()

        labelsAdapter.items.add(label)
        labelsAdapter.items.sort()
        labelsAdapter.notifyDataSetChanged()
        binding.RecyclerView.layoutManager?.scrollToPosition(labelsAdapter.items.indexOf(label))
        val message = "${mContext.getString(R.string.created)} $label"

        val rootView = (mContext as MainActivity).binding.CoordinatorLayout
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
        confirmVisibility()
    }

    private fun editLabel(position: Int, newLabel: String) {
        val priorLabels = notesHelper.getSortedLabelsList()

        val oldLabel = labelsAdapter.items[position]
        val newLabels = HashSet(priorLabels)
        newLabels.remove(oldLabel)
        newLabels.add(newLabel)

        val sharedPreferences = mContext.getSharedPreferences(Constants.labelsPreferences, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putStringSet(Constants.labelItems, newLabels)
        editor.apply()

        labelsAdapter.items[position] = newLabel
        labelsAdapter.items.sort()
        labelsAdapter.notifyDataSetChanged()

        val notesFiles = notesHelper.getNotePath().listFiles()
        val deletedFiles = notesHelper.getDeletedPath().listFiles()
        val archivedFiles = notesHelper.getArchivedPath().listFiles()

        notesFiles?.forEach { file ->
            editLabelFromFile(oldLabel, newLabel, file)
        }

        deletedFiles?.forEach { file ->
            editLabelFromFile(oldLabel, newLabel, file)
        }

        archivedFiles?.forEach { file ->
            editLabelFromFile(oldLabel, newLabel, file)
        }
    }


    private fun deleteLabelFromFile(label: String, file: File) {
        val xmlReader = XMLReader(file)
        val labels = xmlReader.getLabels()

        if (labels.contains(label)) {
            labels.remove(label)

            val fileWriter = FileWriter(file)
            val xmlWriter: XMLWriter

            if (xmlReader.isNote()) {
                xmlWriter = XMLWriter(XMLTags.Note)
                xmlWriter.startNote()
                xmlWriter.setDateCreated(xmlReader.getDateCreated())
                xmlWriter.setTitle(xmlReader.getTitle())
                xmlWriter.setBody(xmlReader.getBody())
                xmlWriter.setSpans(xmlReader.getSpans())
                xmlWriter.setLabels(labels)
                xmlWriter.endNote()
            } else {
                xmlWriter = XMLWriter(XMLTags.List)
                xmlWriter.startNote()
                xmlWriter.setDateCreated(xmlReader.getDateCreated())
                xmlWriter.setTitle(xmlReader.getTitle())
                xmlWriter.setListItems(xmlReader.getListItems())
                xmlWriter.setLabels(labels)
                xmlWriter.endNote()
            }

            fileWriter.write(xmlWriter.getNote())
            fileWriter.close()
        }
    }

    private fun editLabelFromFile(oldLabel: String, newLabel: String, file: File) {
        val xmlReader = XMLReader(file)
        val labels = xmlReader.getLabels()

        if (labels.contains(oldLabel)) {
            labels.remove(oldLabel)
            labels.add(newLabel)

            val fileWriter = FileWriter(file)
            val xmlWriter: XMLWriter

            if (xmlReader.isNote()) {
                xmlWriter = XMLWriter(XMLTags.Note)
                xmlWriter.startNote()
                xmlWriter.setDateCreated(xmlReader.getDateCreated())
                xmlWriter.setTitle(xmlReader.getTitle())
                xmlWriter.setBody(xmlReader.getBody())
                xmlWriter.setSpans(xmlReader.getSpans())
                xmlWriter.setLabels(labels)
                xmlWriter.endNote()
            } else {
                xmlWriter = XMLWriter(XMLTags.List)
                xmlWriter.startNote()
                xmlWriter.setDateCreated(xmlReader.getDateCreated())
                xmlWriter.setTitle(xmlReader.getTitle())
                xmlWriter.setListItems(xmlReader.getListItems())
                xmlWriter.setLabels(labels)
                xmlWriter.endNote()
            }

            fileWriter.write(xmlWriter.getNote())
            fileWriter.close()
        }
    }

    private class DialogHelper(private val context: Context) {

        val dialog: AlertDialog
        val editText: TextInputEditText
        val textInputLayout: TextInputLayout

        init {
            val builder = MaterialAlertDialogBuilder(context)

            val view = View.inflate(context, R.layout.dialog_add_label, null)
            editText = view.findViewById(R.id.TextInputEditText)
            textInputLayout = view.findViewById(R.id.TextInputLayout)

            builder.setView(view)
            builder.setPositiveButton(R.string.save, null)
            builder.setNegativeButton(R.string.cancel, null)

            dialog = builder.create()
        }

        fun showDialog() {
            dialog.show()
            editText.requestFocus()
        }

        fun dismiss() = dialog.cancel()

        fun setTitle(titleId: Int) = dialog.setTitle(titleId)

        fun setInputText(text: String) = editText.setText(text)

        fun displayValueExistsError() {
            textInputLayout.isErrorEnabled = true
            textInputLayout.error = context.getString(R.string.label_exists)
        }

        fun getEnteredValue(): String {
            return editText.text.toString().trim()
        }

        fun onPositiveButtonClicked(positiveButtonClickListener: View.OnClickListener) {
            dialog.setOnShowListener {
                val positiveButton: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener(positiveButtonClickListener)
            }
        }
    }
}