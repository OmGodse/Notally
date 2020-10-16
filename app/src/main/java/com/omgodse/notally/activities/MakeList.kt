package com.omgodse.notally.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityMakeListBinding
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.recyclerview.ListItemListener
import com.omgodse.notally.recyclerview.adapters.MakeListAdapter
import com.omgodse.notally.recyclerview.viewholders.MakeListViewHolder
import com.omgodse.notally.viewmodels.MakeListModel
import com.omgodse.notally.xml.ListItem
import java.text.SimpleDateFormat
import java.util.*

class MakeList : NotallyActivity() {

    private lateinit var adapter: MakeListAdapter
    private lateinit var binding: ActivityMakeListBinding
    override val model: MakeListModel by viewModels()

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


    override fun shareNote() = operationsHelper.shareNote(model.title, model.items)


    private fun setupListeners() {
        binding.EnterTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                model.title = text.toString().trim()
            }
        })

        model.labels.observe(this, {
            binding.LabelGroup.removeAllViews()
            it?.forEach { label ->
                val displayLabel = View.inflate(this, R.layout.label, null) as MaterialButton
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
            val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(position - 1) as MakeListViewHolder?
            viewHolder?.requestFocus()
        }
    }

    private fun setupRecyclerView() {
        adapter = MakeListAdapter(this, model.items)

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val drag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipe = ItemTouchHelper.START or ItemTouchHelper.END
                return makeMovementFlags(drag, swipe)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                Collections.swap(model.items, viewHolder.adapterPosition, target.adapterPosition)
                adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                model.items.removeAt(viewHolder.adapterPosition)
                adapter.notifyItemRemoved(viewHolder.adapterPosition)
                adapter.notifyItemRangeChanged(viewHolder.adapterPosition, model.items.size)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.RecyclerView)

        adapter.listItemListener = object : ListItemListener {
            override fun onMoveToNext(position: Int) {
                moveToNext(position)
            }

            override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                itemTouchHelper.startDrag(viewHolder)
            }

            override fun onItemTextChange(position: Int, newText: String) {
                adapter.items[position].body = newText
            }

            override fun onItemCheckedChange(position: Int, checked: Boolean) {
                adapter.items[position].checked = checked
            }
        }

        binding.RecyclerView.adapter = adapter
        binding.RecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun moveToNext(position: Int) {
        val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(position + 1) as MakeListViewHolder?
        if (viewHolder != null) {
            viewHolder.requestFocus()
        } else addListItem()
    }
}