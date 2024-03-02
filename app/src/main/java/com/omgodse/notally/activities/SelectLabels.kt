package com.omgodse.notally.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityLabelBinding
import com.omgodse.notally.databinding.DialogInputBinding
import com.omgodse.notally.miscellaneous.add
import com.omgodse.notally.recyclerview.adapter.SelectableLabelAdapter
import com.omgodse.notally.room.Label
import com.omgodse.notally.viewmodels.LabelModel

class SelectLabels : AppCompatActivity() {

    private val model: LabelModel by viewModels()
    private lateinit var binding: ActivityLabelBinding

    private lateinit var selectedLabels: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLabelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val savedList = savedInstanceState?.getStringArrayList(SELECTED_LABELS)
        val passedList = requireNotNull(intent.getStringArrayListExtra(SELECTED_LABELS))
        selectedLabels = savedList ?: passedList

        val result = Intent()
        result.putExtra(SELECTED_LABELS, selectedLabels)
        setResult(RESULT_OK, result)

        setupToolbar()
        setupRecyclerView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(SELECTED_LABELS, selectedLabels)
    }


    private fun setupToolbar() {
        binding.Toolbar.setNavigationOnClickListener { finish() }
        binding.Toolbar.menu.add(R.string.add_label, R.drawable.add) { addLabel() }
    }

    private fun addLabel() {
        val binding = DialogInputBinding.inflate(layoutInflater)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_label)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { dialog, _ ->
                val value = binding.EditText.text.toString().trim()
                if (value.isNotEmpty()) {
                    val label = Label(value)
                    model.insertLabel(label) { success ->
                        if (success) {
                            dialog.dismiss()
                        } else Toast.makeText(this, R.string.label_exists, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()

        binding.EditText.requestFocus()
    }

    private fun setupRecyclerView() {
        val adapter = SelectableLabelAdapter(selectedLabels)
        adapter.onChecked = { position, checked ->
            if (position != -1) {
                val label = adapter.currentList[position]
                if (checked) {
                    if (!selectedLabels.contains(label)) {
                        selectedLabels.add(label)
                    }
                } else selectedLabels.remove(label)
            }
        }

        binding.RecyclerView.setHasFixedSize(true)
        binding.RecyclerView.adapter = adapter
        binding.RecyclerView.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))

        model.labels.observe(this) { labels ->
            adapter.submitList(labels)
            if (labels.isEmpty()) {
                binding.EmptyState.visibility = View.VISIBLE
            } else binding.EmptyState.visibility = View.INVISIBLE
        }
    }

    companion object {
        const val SELECTED_LABELS = "SELECTED_LABELS"
    }
}