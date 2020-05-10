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
import com.omgodse.notally.adapters.ListAdapter
import com.omgodse.notally.databinding.ActivityMakeListBinding
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.interfaces.LabelListener
import com.omgodse.notally.interfaces.ListItemListener
import com.omgodse.notally.miscellaneous.ItemTouchHelperCallback
import com.omgodse.notally.miscellaneous.ListItem
import com.omgodse.notally.parents.NotallyActivity
import com.omgodse.notally.viewholders.ListHolder
import com.omgodse.notally.viewmodels.MakeListViewModel
import com.omgodse.notally.xml.XMLReader
import com.omgodse.notally.xml.XMLTags
import com.omgodse.notally.xml.XMLWriter
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

class MakeList : NotallyActivity() {

    private lateinit var listAdapter: ListAdapter
    private lateinit var binding: ActivityMakeListBinding
    private val model: MakeListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMakeListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTitle()
        setupListeners()
        setupRecyclerView()
        setupToolbar(binding.Toolbar)

        if (model.isFirstInstance){
            if (!isNew){
                setupEditMode()
            }
            else {
                val formatter = SimpleDateFormat(DateFormat, Locale.US)
                binding.DateCreated.text = formatter.format(Date())
                addListItem()
            }
            model.isFirstInstance = false
        }

        binding.AddItem.setOnClickListener {
            addListItem()
        }

        setStateFromModel()
    }


    override fun saveNote() {
        val listItems = model.items.filter { listItem -> listItem.body.isNotEmpty() }

        if (model.title.isEmpty() && listItems.isEmpty()) {
            return
        }

        val timestamp = if (isNew) {
            Date().time.toString()
        } else XMLReader(file).getDateCreated()

        val fileWriter = FileWriter(file)
        val xmlWriter = XMLWriter(XMLTags.List)
        xmlWriter.startNote()
        xmlWriter.setDateCreated(timestamp)
        xmlWriter.setTitle(model.title.trim())
        xmlWriter.setListItems(listItems)
        xmlWriter.setLabels(model.labels.value ?: HashSet())
        xmlWriter.endNote()

        fileWriter.write(xmlWriter.getNote())
        fileWriter.close()
    }

    override fun shareNote() {
        val notesHelper = NotesHelper(this)
        notesHelper.shareNote(model.title, listAdapter.items)
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


    private fun addListItem() {
        val listItem = ListItem(String(), false)
        listAdapter.items.add(listItem)
        val position = listAdapter.items.size
        listAdapter.notifyItemInserted(position)
        binding.RecyclerView.post {
            val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(position - 1) as ListHolder?
            viewHolder?.listItem?.requestFocus()
        }
    }

    private fun setupRecyclerView() {
        listAdapter = ListAdapter(this, model.items)
        val itemTouchHelperCallback = ItemTouchHelperCallback(listAdapter)
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.RecyclerView)

        listAdapter.listItemListener = object : ListItemListener {
            override fun onMoveToNext(position: Int) {
                moveToNext(position)
            }

            override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                itemTouchHelper.startDrag(viewHolder)
            }

            override fun onItemTextChange(position: Int, newText: String) {
                listAdapter.items[position].body = newText
            }

            override fun onItemCheckedChange(position: Int, checked: Boolean) {
                listAdapter.items[position].checked = checked
            }
        }

        binding.RecyclerView.adapter = listAdapter
        binding.RecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun moveToNext(position: Int) {
        val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(position + 1) as ListHolder?
        if (viewHolder != null) {
            viewHolder.listItem.requestFocus()
        } else addListItem()
    }


    private fun setupEditMode() {
        val xmlReader = XMLReader(file)

        val title = xmlReader.getTitle()
        val labels = xmlReader.getLabels()
        val items = xmlReader.getListItems()

        val timestamp = xmlReader.getDateCreated()
        val formatter = SimpleDateFormat(DateFormat, Locale.US)
        binding.DateCreated.text = formatter.format(Date(timestamp.toLong()))

        model.title = title
        model.items.clear()
        model.items.addAll(items)
        model.labels.value = labels
    }

    private fun setStateFromModel() {
        binding.EnterTitle.setText(model.title)
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
            }
        })

        model.labels.observe(this, Observer { labels ->
            binding.LabelGroup.removeAllViews()
            labels?.forEach { label ->
                val displayLabel = View.inflate(this, R.layout.chip_label, null) as MaterialButton
                displayLabel.text = label
                binding.LabelGroup.addView(displayLabel)
            }
        })
    }
}