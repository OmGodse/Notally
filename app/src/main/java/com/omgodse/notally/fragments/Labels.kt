package com.omgodse.notally.fragments

import android.content.Context
import android.os.Bundle
import android.text.InputType
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
import com.omgodse.notally.databinding.FragmentNotesBinding
import com.omgodse.notally.helpers.MenuHelper
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.adapters.LabelsAdapter
import com.omgodse.notally.xml.BaseNote
import java.io.File

class Labels : Fragment() {

    private lateinit var mContext: Context
    private lateinit var notesHelper: NotesHelper
    private lateinit var labelsAdapter: LabelsAdapter
    private var binding: FragmentNotesBinding? = null

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


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        notesHelper = NotesHelper(mContext)
        labelsAdapter = LabelsAdapter(mContext)

        labelsAdapter.onLabelClicked = this::onLabelClicked
        labelsAdapter.onLabelLongClicked = this::onLabelLongClicked

        binding?.RecyclerView?.adapter = labelsAdapter
        binding?.RecyclerView?.layoutManager = LinearLayoutManager(mContext)
        val itemDecoration = DividerItemDecoration(mContext, RecyclerView.VERTICAL)
        binding?.RecyclerView?.addItemDecoration(itemDecoration)

        setupFrameLayout()
        populateRecyclerView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        binding = FragmentNotesBinding.inflate(inflater)
        return binding?.root
    }


    private fun onLabelClicked(position: Int) {
        val label = labelsAdapter.currentList[position]
        val bundle = bundleOf(Constants.argLabelKey to label)
        findNavController().navigate(R.id.LabelsFragmentToDisplayLabel, bundle)
    }

    private fun onLabelLongClicked(position: Int) {
        val menuHelper = MenuHelper(mContext)

        menuHelper.addItem(R.string.edit, R.drawable.edit) { displayEditLabelDialog(position) }
        menuHelper.addItem(R.string.delete, R.drawable.delete) { confirmDeletion(position) }

        menuHelper.show()
    }


    private fun confirmVisibility(labels: List<String>) {
        if (labels.isNotEmpty()) {
            binding?.RecyclerView?.visibility = View.VISIBLE
        } else binding?.RecyclerView?.visibility = View.GONE
    }

    private fun setupFrameLayout() {
        binding?.ImageView?.setImageResource(R.drawable.colored_label)
    }

    private fun populateRecyclerView() {
        val labels = notesHelper.getSortedLabelsList()
        labelsAdapter.submitList(labels)
        confirmVisibility(labels)
    }


    private fun displayAddLabelDialog() {
        val priorLabels = notesHelper.getSortedLabelsList()

        val builder = MaterialAlertDialogBuilder(mContext)

        val view = View.inflate(context, R.layout.dialog_input, null)
        val editText: TextInputEditText = view.findViewById(android.R.id.edit)
        val textInputLayout: TextInputLayout = view.findViewById(R.id.TextInputLayout)

        editText.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        editText.filters = arrayOf()

        builder.setView(view)
        builder.setTitle(R.string.add_label)
        builder.setPositiveButton(R.string.save, null)
        builder.setNegativeButton(R.string.cancel, null)

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val label = editText.text.toString().trim()
                if (label.isNotEmpty()) {
                    if (!priorLabels.contains(label)) {
                        insertLabel(label)
                        dialog.dismiss()
                    } else {
                        textInputLayout.isErrorEnabled = true
                        textInputLayout.error = mContext.getString(R.string.label_exists)
                    }
                } else dialog.dismiss()
            }
        }

        dialog.show()
        editText.requestFocus()
    }

    private fun confirmDeletion(position: Int) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(mContext)
        alertDialogBuilder.setTitle(R.string.delete_label)
        alertDialogBuilder.setMessage(R.string.your_notes_associated)
        alertDialogBuilder.setPositiveButton(R.string.delete) { dialog, which ->
            deleteLabel(position)
        }
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)
        alertDialogBuilder.show()
    }

    private fun displayEditLabelDialog(position: Int) {
        val label = labelsAdapter.currentList[position]
        val priorLabels = notesHelper.getSortedLabelsList()

        val builder = MaterialAlertDialogBuilder(mContext)

        val view = View.inflate(context, R.layout.dialog_input, null)
        val editText: TextInputEditText = view.findViewById(android.R.id.edit)
        val textInputLayout: TextInputLayout = view.findViewById(R.id.TextInputLayout)

        editText.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        editText.filters = arrayOf()

        editText.setText(label)

        builder.setView(view)
        builder.setTitle(R.string.edit_label)
        builder.setPositiveButton(R.string.save, null)
        builder.setNegativeButton(R.string.cancel, null)

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton: Button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val enteredLabel = editText.text.toString().trim()
                when {
                    enteredLabel.isEmpty() -> dialog.dismiss()
                    enteredLabel == label -> dialog.dismiss()
                    !priorLabels.contains(enteredLabel) -> {
                        editLabel(position, enteredLabel)
                        dialog.dismiss()
                    }
                    priorLabels.contains(enteredLabel) -> {
                        textInputLayout.isErrorEnabled = true
                        textInputLayout.error = mContext.getString(R.string.label_exists)
                    }
                }
            }
        }

        dialog.show()
        editText.requestFocus()
    }


    private fun deleteLabel(position: Int) {
        val label = labelsAdapter.currentList[position]

        val priorLabels = notesHelper.getSortedLabelsList()
        val newLabels = HashSet(priorLabels)
        newLabels.remove(label)

        val sharedPreferences = mContext.getSharedPreferences(Constants.labelsPreferences, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putStringSet(Constants.labelItems, newLabels)
        editor.apply()

        labelsAdapter.submitList(notesHelper.getSortedLabelsList())
        confirmVisibility(notesHelper.getSortedLabelsList())

        val files = notesHelper.getNotePath().listFiles()
        val deletedFiles = notesHelper.getDeletedPath().listFiles()
        val archivedFiles = notesHelper.getArchivedPath().listFiles()

        files?.forEach { file ->
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

        labelsAdapter.submitList(notesHelper.getSortedLabelsList())
        confirmVisibility(notesHelper.getSortedLabelsList())

        binding?.RecyclerView?.layoutManager?.scrollToPosition(labelsAdapter.currentList.indexOf(label))
        val message = mContext.getString(R.string.created, label)

        val rootView = (mContext as MainActivity).binding.CoordinatorLayout
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun editLabel(position: Int, newLabel: String) {
        val priorLabels = notesHelper.getSortedLabelsList()

        val oldLabel = labelsAdapter.currentList[position]
        val newLabels = HashSet(priorLabels)
        newLabels.remove(oldLabel)
        newLabels.add(newLabel)

        val sharedPreferences = mContext.getSharedPreferences(Constants.labelsPreferences, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putStringSet(Constants.labelItems, newLabels)
        editor.apply()

        labelsAdapter.submitList(notesHelper.getSortedLabelsList())

        val files = notesHelper.getNotePath().listFiles()
        val deletedFiles = notesHelper.getDeletedPath().listFiles()
        val archivedFiles = notesHelper.getArchivedPath().listFiles()

        files?.forEach { file ->
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
        val baseNote = BaseNote.readFromFile(file)

        if (baseNote.labels.contains(label)) {
            baseNote.labels.remove(label)
            baseNote.writeToFile()
        }
    }

    private fun editLabelFromFile(oldLabel: String, newLabel: String, file: File) {
        val baseNote = BaseNote.readFromFile(file)

        if (baseNote.labels.contains(oldLabel)) {
            baseNote.labels.remove(oldLabel)
            baseNote.labels.add(newLabel)
            baseNote.writeToFile()
        }
    }
}