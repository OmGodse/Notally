package com.omgodse.notally.activities

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.add
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.ListItemListener
import com.omgodse.notally.recyclerview.adapter.MakeListAdapter
import com.omgodse.notally.recyclerview.viewholder.MakeListVH
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.room.Type

class MakeList() : NotallyActivity(Type.LIST) {

    private lateinit var adapter: MakeListAdapter

    private lateinit var preferences: Preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = Preferences.getInstance(application)
    }

    override fun setupToolbar(){
        super.setupToolbar()
        binding.Toolbar.menu.add(R.string.remove_checked_items, R.drawable.delete_all) { deleteCheckedItems() }
    }

    override fun configureUI() {
        binding.EnterTitle.setOnNextAction {
            moveToNext(-1)
        }

        if (model.isNewNote) {
            if (model.items.isEmpty()) {
                addListItem()
            }
        }
    }

    override fun setupListeners() {
        super.setupListeners()
        binding.AddItem.setOnClickListener {
            addListItem()
        }
    }

    override fun setStateFromModel() {
        super.setStateFromModel()
        val elevation = resources.displayMetrics.density * 2

        adapter = MakeListAdapter(model.textSize, elevation, model.items, object : ListItemListener {

            override fun delete(position: Int) {
                model.items.removeAt(position)
                adapter.notifyItemRemoved(position)
            }

            override fun moveToNext(position: Int) {
                this@MakeList.moveToNext(position)
            }

            override fun textChanged(position: Int, text: String) {
                model.items[position].body = text
            }

            override fun checkedChanged(position: Int, checked: Boolean) {
                val item = model.items[position]
                if(!item.isChildItem) {
                    val lastChildPosition = checkAllChildItems(position, checked)
                    if(preferences.listItemSorting.value == ListItemSorting.autoSortByChecked) {
                            val newList = model.items.clone() as ArrayList<ListItem>
                            var firstCheckedItemPosition =
                                newList.indexOfFirst { it != item && it.checked && !it.isChildItem }
                            if (firstCheckedItemPosition == -1) {
                                firstCheckedItemPosition = newList.size
                            }
                                moveItemRange(
                                    newList,
                                    firstCheckedItemPosition,
                                    position,
                                    lastChildPosition
                                )
                            updateList(newList)
                    }
                }
                item.checked = checked
                    adapter.notifyItemChanged(position)

            }

            override fun isChildItemChanged(position: Int, isChildItem: Boolean) {
                model.items[position].isChildItem = isChildItem
            }
        })

        binding.RecyclerView.adapter = adapter
    }

    private fun updateList(newList: ArrayList<ListItem>) {
        val diffCallback = ListItemCallback(model.items, newList)
        val diffCourses = DiffUtil.calculateDiff(diffCallback)
        model.items.clear()
        model.items.addAll(newList)
        diffCourses.dispatchUpdatesTo(adapter)
    }

    private fun checkAllChildItems(position: Int, checked: Boolean): Int {
        var childPosition = position + 1
        while (childPosition < model.items.size) {
            val childItem = model.items[childPosition]
            if (childItem.isChildItem) {
                if (childItem.checked != checked) {
                    childItem.checked = checked
                    adapter.notifyItemChanged(childPosition)
                }
            } else {
                break;
            }
            childPosition++;
        }
        return childPosition
    }

    private fun moveItemRange(
        newList: ArrayList<ListItem>,
        insertPosition: Int,
        rangeStartPosition: Int,
        rangeEndPosition: Int
    ) {
        newList.addAll(
            insertPosition,
            newList.subList(rangeStartPosition, rangeEndPosition)
        )
        val amountItems = rangeEndPosition - rangeStartPosition
        val removeStartPosition = rangeStartPosition + if(insertPosition > rangeStartPosition) 0 else amountItems
        val removeEndPosition = rangeEndPosition + if(insertPosition > rangeStartPosition) 0 else amountItems
        var counter = 0
        for (idx in removeStartPosition..< removeEndPosition) {
            newList.removeAt(idx - counter)
            counter++
        }
    }


    private fun addListItem() {
        val position = model.items.size
        val isChildItem = model.items.isNotEmpty() && model.items.last().isChildItem
        val listItem = ListItem(String(), false, isChildItem)
        model.items.add(listItem)
        adapter.notifyItemInserted(position)
        binding.RecyclerView.post {
            val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(position) as MakeListVH?
            viewHolder?.binding?.EditText?.requestFocus()
        }
    }

    private fun moveToNext(currentPosition: Int) {
        val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(currentPosition + 1) as MakeListVH?
        if (viewHolder != null) {
            if (viewHolder.binding.CheckBox.isChecked) {
                moveToNext(currentPosition + 1)
            } else viewHolder.binding.EditText.requestFocus()
        } else addListItem()
    }

    private fun deleteCheckedItems() {
        val newList = model.items.clone() as ArrayList<ListItem>
        newList.removeAll { it.checked }
        updateList(newList)
    }
}