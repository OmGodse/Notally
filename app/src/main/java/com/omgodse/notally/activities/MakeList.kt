package com.omgodse.notally.activities

import android.os.Build
import android.view.inputmethod.InputMethodManager
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


class MakeList : NotallyActivity(Type.LIST) {

    private lateinit var adapter: MakeListAdapter

    override fun setupToolbar(){
        super.setupToolbar()
        binding.Toolbar.menu.add(1,R.string.remove_checked_items, R.drawable.delete_all) { deleteCheckedItems() }
        binding.Toolbar.menu.add(1,R.string.check_all_items, R.drawable.checkbox_fill) { checkAllItems(true) }
        binding.Toolbar.menu.add(1,R.string.uncheck_all_items, R.drawable.checkbox) { checkAllItems(false) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.Toolbar.menu.setGroupDividerEnabled(true)
        }
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

        adapter = MakeListAdapter(model.textSize, elevation, model.items, Preferences.getInstance(application), object : ListItemListener {

            override fun delete(position: Int, force: Boolean) {
                if(force || position > 0) {
                    model.items.removeAt(position)
                    adapter.notifyItemRemoved(position)
                }
                if(!force) {
                    if(position > 0) {
                        this@MakeList.moveToNext(position - 2)
                    } else if(model.items.size > 1) {
                        this@MakeList.moveToNext(position)
                    }
                }

            }

            override fun moveToNext(position: Int) {
                this@MakeList.moveToNext(position)
            }

            override fun add(position: Int) {
                addListItem(position)
            }

            override fun textChanged(position: Int, text: String) {
                model.items[position].body = text
            }

            override fun checkedChanged(position: Int, checked: Boolean) {
                val item = model.items[position]
                if(!item.isChildItem) {
                    if(preferences.listItemSorting.value == ListItemSorting.autoSortByChecked) {
                        val lastChildPosition = checkWithAllChildren(position, checked)
                        var firstCheckedItemPosition =
                                model.items.indexOfFirst { it != item && it.checked && !it.isChildItem }
                            if (firstCheckedItemPosition == -1) {
                                firstCheckedItemPosition = model.items.size
                            }
                            moveItemRange(
                                firstCheckedItemPosition,
                                position,
                                lastChildPosition,
                                adapter
                            )
                    }
                } else {
                    item.checked = checked
                    adapter.notifyItemChanged(position)
                }
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

    /**
     * Checks ListItem and all its children.
     * @return position of the last child ListItem
     */
    private fun checkWithAllChildren(position: Int, checked: Boolean): Int {
        model.items[position].checked = checked
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
        insertPosition: Int,
        rangeStartPosition: Int,
        rangeEndPosition: Int,
        adapter: MakeListAdapter
    ) {
        model.items.addAll(
            insertPosition,
            model.items.subList(rangeStartPosition, rangeEndPosition)
        )
        val amountItems = rangeEndPosition - rangeStartPosition
        val removeStartPosition = rangeStartPosition + if(insertPosition > rangeStartPosition) 0 else amountItems
        val removeEndPosition = rangeEndPosition + if(insertPosition > rangeStartPosition) 0 else amountItems
        var counter = 0
        for (idx in removeStartPosition..< removeEndPosition) {
            model.items.removeAt(idx - counter)
            counter++
        }
        adapter.notifyItemRangeInserted(insertPosition, amountItems)
        adapter.notifyItemRangeRemoved(removeStartPosition, amountItems)
    }


    private fun addListItem(position: Int = model.items.size) {
        val isChildItem = model.items.isNotEmpty() && model.items.last().isChildItem
        val listItem = ListItem(String(), false, isChildItem)
        model.items.add(position, listItem)
        adapter.notifyItemInserted(position)
        binding.RecyclerView.post {
            val viewHolder = binding.RecyclerView.findViewHolderForAdapterPosition(position) as MakeListVH?
            viewHolder?.binding?.EditText?.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(viewHolder?.binding?.EditText, InputMethodManager.SHOW_IMPLICIT)
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

    private fun checkAllItems(checked: Boolean) {
        model.items.forEach { it.checked = checked }
        adapter.notifyDataSetChanged()
    }
}