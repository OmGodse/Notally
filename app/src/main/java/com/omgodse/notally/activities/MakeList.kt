package com.omgodse.notally.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
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
import com.omgodse.notally.interfaces.ListItemListener
import com.omgodse.notally.miscellaneous.ItemTouchHelperCallback
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.viewmodels.MakeListModel
import com.omgodse.notally.xml.ListItem
import java.text.SimpleDateFormat
import java.util.*

class MakeList : NotallyActivity() {

    private lateinit var adapter: MakeListAdapter
    private lateinit var binding: ActivityMakeListBinding
    private val model: MakeListModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMakeListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.EnterTitle.setOnNextAction {
            moveToNext(-1)
        }

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


    override fun shareNote() {
        val notesHelper = NotesHelper(this)
        notesHelper.shareNote(model.title, model.items)
    }

    override fun getViewModel() = model


    private fun setupListeners() {
        binding.EnterTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                if (model.title !=text.toString()) {
                    model.timeModified = Date().time
                }
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

    private fun setStateFromModel() {
        binding.EnterTitle.setText(model.title)
        val formatter = SimpleDateFormat(DateFormat, getLocale())
        binding.DateCreated.text = formatter.format(model.timestamp)
        adapter.notifyDataSetChanged()
    }


    private fun addListItem() {
        val listItem = ListItem(String(), false)
        adapter.items.add(listItem)
        val position = adapter.items.size
        adapter.notifyItemInserted(position)
        binding.RecyclerView.post {
            val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(position - 1) as MakeListAdapter.ViewHolder?
            viewHolder?.listItem?.requestFocus()
        }
    }

    private fun setupRecyclerView() {
        adapter = MakeListAdapter(this, model.items, model)
        val itemTouchHelperCallback = ItemTouchHelperCallback(adapter)
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.RecyclerView)

        adapter.listItemListener = object : ListItemListener {
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
                adapter.notifyItemRemoved(position)
                adapter.notifyItemRangeChanged(position, model.items.size)
                model.saveNote()
            }

            override fun onItemSwapped(fromPosition: Int, toPosition: Int) {
                Collections.swap(model.items, fromPosition, toPosition)
                adapter.notifyItemMoved(fromPosition, toPosition)
                model.timeModified = Date().time
                model.saveNote()
            }

            override fun onItemTextChange(position: Int, newText: String) {
                if(adapter.items[position].body!=newText){
                    model.timeModified = Date().time
                }
                adapter.items[position].body = newText
                model.saveNote()
            }

            override fun onItemCheckedChange(position: Int, checked: Boolean) {
                adapter.items[position].checked = checked
                model.saveNote()
            }
        }

        binding.RecyclerView.adapter = adapter
        binding.RecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun moveToNext(position: Int) {
        val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(position + 1) as MakeListAdapter.ViewHolder?
        if (viewHolder != null) {
            viewHolder.listItem.requestFocus()
        } else addListItem()
    }
}