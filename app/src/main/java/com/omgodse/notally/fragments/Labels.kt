package com.omgodse.notally.fragments

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.databinding.DialogInputBinding
import com.omgodse.notally.databinding.FragmentNotesBinding
import com.omgodse.notally.helpers.MenuHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.recyclerview.adapters.LabelsAdapter
import com.omgodse.notally.viewmodels.BaseNoteModel

class Labels : Fragment() {

    private lateinit var mContext: Context
    private var labelsAdapter: LabelsAdapter? = null
    private var binding: FragmentNotesBinding? = null

    private val model: BaseNoteModel by activityViewModels()

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
        labelsAdapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        labelsAdapter = LabelsAdapter(mContext)

        labelsAdapter?.onLabelClicked = this::onLabelClicked
        labelsAdapter?.onLabelLongClicked = this::onLabelLongClicked

        binding?.RecyclerView?.adapter = labelsAdapter
        binding?.RecyclerView?.layoutManager = LinearLayoutManager(mContext)
        val itemDecoration = DividerItemDecoration(mContext, RecyclerView.VERTICAL)
        binding?.RecyclerView?.addItemDecoration(itemDecoration)

        binding?.ImageView?.setImageResource(R.drawable.label)

        populateRecyclerView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        binding = FragmentNotesBinding.inflate(inflater)
        return binding?.root
    }


    private fun onLabelClicked(position: Int) {
        labelsAdapter?.currentList?.get(position)?.let {
            val bundle = Bundle()
            bundle.putString(Constants.argLabelKey, it)
            findNavController().navigate(R.id.LabelsFragmentToDisplayLabel, bundle)
        }
    }

    private fun onLabelLongClicked(position: Int) {
        labelsAdapter?.currentList?.get(position)?.let {
            MenuHelper(mContext)
                .addItem(Operation(R.string.edit, R.drawable.edit) { displayEditLabelDialog(it) })
                .addItem(Operation(R.string.delete, R.drawable.delete) { confirmDeletion(it) })
                .show()
        }
    }


    private fun populateRecyclerView() {
        model.labels.observe(viewLifecycleOwner, {
            labelsAdapter?.submitList(ArrayList(it))

            if (it.isNotEmpty()) {
                binding?.RecyclerView?.visibility = View.VISIBLE
            } else binding?.RecyclerView?.visibility = View.GONE
        })
    }


    private fun displayAddLabelDialog() {
        val builder = MaterialAlertDialogBuilder(mContext)
        val dialogBinding = DialogInputBinding.inflate(LayoutInflater.from(context))

        dialogBinding.edit.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        dialogBinding.edit.filters = arrayOf()

        builder.setView(dialogBinding.root)
        builder.setTitle(R.string.add_label)
        builder.setPositiveButton(R.string.save, null)
        builder.setNegativeButton(R.string.cancel, null)

        val dialog = builder.create()

        dialog.setOnShowListener {
            dialogBinding.edit.requestFocus()
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val label = dialogBinding.edit.text.toString().trim()
                if (label.isNotEmpty()) {
                    model.insertLabel(label) { success ->
                        if (success) {
                            dialog.dismiss()
                        } else dialogBinding.TextInputLayout.error = mContext.getString(R.string.label_exists)
                    }
                } else dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun confirmDeletion(label: String) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(mContext)
        alertDialogBuilder.setTitle(R.string.delete_label)
        alertDialogBuilder.setMessage(R.string.your_notes_associated)
        alertDialogBuilder.setPositiveButton(R.string.delete) { dialog, which ->
            model.deleteLabel(label)
        }
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)
        alertDialogBuilder.show()
    }

    private fun displayEditLabelDialog(oldLabel: String) {
        val builder = MaterialAlertDialogBuilder(mContext)
        val dialogBinding = DialogInputBinding.inflate(LayoutInflater.from(context))

        dialogBinding.edit.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        dialogBinding.edit.filters = arrayOf()

        dialogBinding.edit.setText(oldLabel)

        builder.setView(dialogBinding.root)
        builder.setTitle(R.string.edit_label)
        builder.setPositiveButton(R.string.save, null)
        builder.setNegativeButton(R.string.cancel, null)

        val dialog = builder.create()

        dialog.setOnShowListener {
            dialogBinding.edit.requestFocus()
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val enteredLabel = dialogBinding.edit.text.toString().trim()

                if (enteredLabel.isNotEmpty() && enteredLabel != oldLabel) {
                    model.editLabel(oldLabel, enteredLabel) { success ->
                        if (success) {
                            dialog.dismiss()
                        } else dialogBinding.TextInputLayout.error = mContext.getString(R.string.label_exists)
                    }
                } else dialog.dismiss()
            }
        }

        dialog.show()
    }
}