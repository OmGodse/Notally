package com.omgodse.notally.activities

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.omgodse.notally.R
import com.omgodse.notally.adapters.MakeListAdapter
import com.omgodse.notally.databinding.ActivityMakeListBinding
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.interfaces.LabelListener
import com.omgodse.notally.interfaces.ListItemListener
import com.omgodse.notally.miscellaneous.ItemTouchHelperCallback
import com.omgodse.notally.miscellaneous.ListItem
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.parents.NotallyActivity
import com.omgodse.notally.viewmodels.MakeListModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

class MakeList : NotallyActivity() {

    private lateinit var listAdapter: MakeListAdapter
    private lateinit var binding: ActivityMakeListBinding
    private val model: MakeListModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMakeListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTitle()
        setupListeners()
        setupRecyclerView()
        setupToolbar(binding.Toolbar)

        if (model.isNewNote) {
            binding.EnterTitle.requestFocus()
            if (model.items.isEmpty()) {
                addListItem()
            }
        }

        binding.AddItem.setOnClickListener {
            addListItem()
        }

        setStateFromModel()
    }

    override fun labelNote() {
        val notesHelper = NotesHelper(this)
        val labelListener = object : LabelListener {
            override fun onUpdateLabels(labels: HashSet<String>) {
                model.labels.value = labels
            }
        }
        notesHelper.labelNote(model.labels.value ?: HashSet(), labelListener)
    }

    override fun getViewModel() = model


    private fun addListItem() {
        val listItem = ListItem(String(), false)
        listAdapter.items.add(listItem)
        val position = listAdapter.items.size
        listAdapter.notifyItemInserted(position)
        binding.RecyclerView.post {
            val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(position - 1) as MakeListAdapter.ListHolder?
            viewHolder?.listItem?.requestFocus()
        }
    }

    private fun setupRecyclerView() {
        listAdapter = MakeListAdapter(this, model.items)
        val itemTouchHelperCallback = ItemTouchHelperCallback(listAdapter)
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.RecyclerView)

        listAdapter.listItemListener = object : ListItemListener {
            override fun onMoveToNext(position: Int) {
                moveToNext(position)
                model.saveNote()
            }

            override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                itemTouchHelper.startDrag(viewHolder)
                model.saveNote()
            }

            override fun onItemDeleted(position: Int) {
                model.items.removeAt(position)
                listAdapter.notifyItemRemoved(position)
                listAdapter.notifyItemRangeChanged(position, model.items.size)
                model.saveNote()
            }

            override fun onItemSwapped(fromPosition: Int, toPosition: Int) {
                Collections.swap(model.items, fromPosition, toPosition)
                listAdapter.notifyItemMoved(fromPosition, toPosition)
                model.saveNote()
            }

            override fun onItemTextChange(position: Int, newText: String) {
                listAdapter.items[position].body = newText
                model.saveNote()
            }

            override fun onItemCheckedChange(position: Int, checked: Boolean) {
                listAdapter.items[position].checked = checked
                model.saveNote()
            }
        }

        binding.RecyclerView.adapter = listAdapter
        binding.RecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun moveToNext(position: Int) {
        val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(position + 1) as MakeListAdapter.ListHolder?
        if (viewHolder != null) {
            viewHolder.listItem.requestFocus()
        } else addListItem()
    }


    private fun setStateFromModel() {
        binding.EnterTitle.setText(model.title)
        val formatter = SimpleDateFormat(DateFormat, getLocale())
        binding.DateCreated.text = formatter.format(model.timestamp)
        listAdapter.notifyDataSetChanged()
    }


    private fun setupTitle() {
        binding.EnterTitle.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

        binding.EnterTitle.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                moveToNext(-1)
                return@setOnKeyListener true
            } else return@setOnKeyListener false
        }

        binding.EnterTitle.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                moveToNext(-1)
                return@setOnEditorActionListener true
            } else return@setOnEditorActionListener false
        }
    }

    private fun setupListeners() {
        binding.EnterTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                model.title = text.toString().trim()
                model.saveNote()
            }
        })

        model.labels.observe(this, Observer { labels ->
            model.saveNote()
            binding.LabelGroup.removeAllViews()
            labels?.forEach { label ->
                val displayLabel = View.inflate(this, R.layout.chip_label, null) as MaterialButton
                displayLabel.text = label
                binding.LabelGroup.addView(displayLabel)
            }
        })
    }
}