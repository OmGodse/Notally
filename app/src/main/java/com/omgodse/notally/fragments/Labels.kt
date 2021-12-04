package com.omgodse.notally.fragments

import android.os.Bundle
import android.text.InputType
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.setPadding
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
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.recyclerview.adapters.LabelAdapter
import com.omgodse.notally.room.Label
import com.omgodse.notally.viewmodels.BaseNoteModel

class Labels : Fragment(), ItemListener {

    private var adapter: LabelAdapter? = null
    private var binding: FragmentNotesBinding? = null

    private val model: BaseNoteModel by activityViewModels()

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        adapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = LabelAdapter(this)

        binding?.RecyclerView?.setHasFixedSize(true)
        binding?.RecyclerView?.adapter = adapter
        binding?.RecyclerView?.layoutManager = LinearLayoutManager(requireContext())
        val itemDecoration = DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        binding?.RecyclerView?.addItemDecoration(itemDecoration)

        binding?.RecyclerView?.setPadding(0)
        binding?.ImageView?.setImageResource(R.drawable.label)

        setupObserver()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        binding = FragmentNotesBinding.inflate(inflater)
        return binding?.root
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


    override fun onClick(position: Int) {
        adapter?.currentList?.get(position)?.let { value ->
            val bundle = bundleOf(Constants.SelectedLabel to value)
            findNavController().navigate(R.id.LabelsToDisplayLabel, bundle)
        }
    }

    override fun onLongClick(position: Int) {
        adapter?.currentList?.get(position)?.let { value ->
            val edit = Operation(R.string.edit, R.drawable.edit) { displayEditLabelDialog(value) }
            val delete = Operation(R.string.delete, R.drawable.delete) { confirmDeletion(value) }
            showMenu(edit, delete)
        }
    }


    private fun setupObserver() {
        model.labels.observe(viewLifecycleOwner, { labels ->
            adapter?.submitList(labels)
            binding?.RecyclerView?.isVisible = labels.isNotEmpty()
        })
    }


    private fun displayAddLabelDialog() {
        val dialogBinding = DialogInputBinding.inflate(layoutInflater)

        dialogBinding.edit.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        dialogBinding.edit.filters = arrayOf()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setTitle(R.string.add_label)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialogBinding.edit.requestFocus()
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val value = dialogBinding.edit.text.toString().trim()
                if (value.isNotEmpty()) {
                    val label = Label(value)
                    model.insertLabel(label) { success ->
                        if (success) {
                            dialog.dismiss()
                        } else dialogBinding.root.error = getString(R.string.label_exists)
                    }
                } else dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun confirmDeletion(value: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_label)
            .setMessage(R.string.your_notes_associated)
            .setPositiveButton(R.string.delete) { dialog, which ->
                model.deleteLabel(value)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun displayEditLabelDialog(oldValue: String) {
        val dialogBinding = DialogInputBinding.inflate(layoutInflater)

        dialogBinding.edit.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        dialogBinding.edit.filters = arrayOf()

        dialogBinding.edit.setText(oldValue)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setTitle(R.string.edit_label)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialogBinding.edit.requestFocus()
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val newValue = dialogBinding.edit.text.toString().trim()

                if (newValue.isNotEmpty()) {
                    model.updateLabel(oldValue, newValue) { success ->
                        if (success) {
                            dialog.dismiss()
                        } else dialogBinding.root.error = getString(R.string.label_exists)
                    }
                } else dialog.dismiss()
            }
        }

        dialog.show()
    }
}