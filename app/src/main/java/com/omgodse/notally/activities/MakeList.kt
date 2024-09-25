package com.omgodse.notally.activities

import android.os.Build
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.DiffUtil
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Change
import com.omgodse.notally.miscellaneous.ChangeHistory
import com.omgodse.notally.miscellaneous.add
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.ListItemCallback
import com.omgodse.notally.recyclerview.ListItemListener
import com.omgodse.notally.recyclerview.adapter.MakeListAdapter
import com.omgodse.notally.recyclerview.viewholder.MakeListVH
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.room.Type


class MakeList : NotallyActivity(Type.LIST) {

    private lateinit var adapter: MakeListAdapter

    override fun setupToolbar() {
        super.setupToolbar()
        binding.Toolbar.menu.add(
            1,
            R.string.remove_checked_items,
            R.drawable.delete_all,
            MenuItem.SHOW_AS_ACTION_IF_ROOM
        ) { deleteCheckedItems() }
        binding.Toolbar.menu.add(
            1,
            R.string.check_all_items,
            R.drawable.checkbox_fill,
            MenuItem.SHOW_AS_ACTION_IF_ROOM
        ) { checkAllItems(true) }
        binding.Toolbar.menu.add(
            1,
            R.string.uncheck_all_items,
            R.drawable.checkbox,
            MenuItem.SHOW_AS_ACTION_IF_ROOM
        ) { checkAllItems(false) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.Toolbar.menu.setGroupDividerEnabled(true)
        }
    }

    override fun initActionManager(undo: MenuItem, redo: MenuItem) {
        changeHistory = ChangeHistory {
            undo.isEnabled = changeHistory.canUndo()
            redo.isEnabled = changeHistory.canRedo()
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
            changeHistory.addChange(object: Change {
                override fun redo() {
                    addListItem()
                }

                override fun undo() {
                    deleteListItem(force = true)
                }

            })
        }
    }

    override fun setStateFromModel() {
        super.setStateFromModel()
        val elevation = resources.displayMetrics.density * 2
        adapter = MakeListAdapter(
            model.textSize,
            elevation,
            model.items,
            Preferences.getInstance(application),
            object : ListItemListener {
                override fun delete(position: Int, force: Boolean): Boolean {
                    return deleteListItem(position, force)
                }

                override fun moveToNext(position: Int) {
                    this@MakeList.moveToNext(position)
                }

                override fun add(position: Int, initialText: String, checked: Boolean, isChildItem: Boolean?, uncheckedPosition: Int) {
                    addListItem(position, initialText, checked, isChildItem, uncheckedPosition)
                }

                override fun textChanged(position: Int, text: String) {
                    val item = model.items[position]
                    item.body = text
                }

                override fun checkedChanged(
                    position: Int,
                    checked: Boolean
                ): Int {
                    val item = model.items[position]
                    if (item.checked == checked) {
                        return position
                    }
                    if (item.isChildItem) {
                        item.checked = checked
                        adapter.notifyItemChanged(position)
                        return position
                    }
                    checkWithAllChildren(position, checked)
                    updateList(model.sortedItems(preferences.listItemSorting.value))
                    return model.items.indexOf(item)
                }

                override fun isChildItemChanged(position: Int, isChildItem: Boolean) {
                    model.items[position].isChildItem = isChildItem
                }
            },
            changeHistory
        )
        binding.RecyclerView.adapter = adapter
        updateList(model.sortedItems(preferences.listItemSorting.value))
    }

    private fun updateList(newList: ArrayList<ListItem>) {
        val diffCallback = ListItemCallback(model.items, newList)
        val diffCourses = DiffUtil.calculateDiff(diffCallback)
        model.items.clear()
        model.items.addAll(newList)
        diffCourses.dispatchUpdatesTo(adapter)
    }

    private fun checkWithAllChildren(position: Int, checked: Boolean) {
        model.items[position].checked = checked
        var childPosition = position + 1
        while (childPosition < model.items.size) {
            val childItem = model.items[childPosition]
            if (childItem.isChildItem) {
                if (childItem.checked != checked) {
                    childItem.checked = checked
                }
            } else {
                break;
            }
            childPosition++;
        }
        adapter.notifyItemRangeChanged(position, childPosition - position)
    }

    private fun deleteListItem(position: Int = model.items.size - 1, force: Boolean): Boolean {
        // TODO: Delete all children as well?
        var isDeleted = false
        if (force || position > 0) {
            model.items.removeAt(position)
            adapter.notifyItemRemoved(position)
            isDeleted = true
        }
        if (!force) {
            if (position > 0) {
                this@MakeList.moveToNext(position - 2)
            } else if (model.items.size > 1) {
                this@MakeList.moveToNext(position)
            }
        }
        return isDeleted
    }


    private fun addListItem(
        position: Int = model.items.size,
        initialText: String = "",
        checked: Boolean = false,
        isChildItem: Boolean? = null,
        uncheckedPosition: Int = position
    ) {
        val actualIsChildItem = isChildItem ?: model.items.isNotEmpty() && model.items.last().isChildItem
        val listItem = ListItem(initialText, checked, actualIsChildItem, uncheckedPosition)
        model.items.add(position, listItem)
        adapter.notifyItemInserted(position)
        binding.RecyclerView.post {
            val viewHolder =
                binding.RecyclerView.findViewHolderForAdapterPosition(position) as MakeListVH?
            if (!checked) {
                val editText = viewHolder?.binding?.EditText
                editText?.requestFocus()
                editText?.setSelection(editText.text.length)
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(viewHolder?.binding?.EditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun moveToNext(currentPosition: Int) {
        val viewHolder =
            binding.RecyclerView.findViewHolderForAdapterPosition(currentPosition + 1) as MakeListVH?
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
        model.items.forEachIndexed { idx, item ->
            if(item.checked != checked) {
                item.checked = checked
                adapter.notifyItemChanged(idx)
            }
        }
    }
}