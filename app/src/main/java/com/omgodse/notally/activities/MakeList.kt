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
import com.omgodse.notally.preferences.ListItemSorting
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
            changeHistory.addChange(object : Change {
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

                override fun add(
                    position: Int,
                    initialText: String,
                    checked: Boolean,
                    isChildItem: Boolean?,
                    uncheckedPosition: Int
                ) {
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
                    val (updatedItem, updateList) = checkWithAllChildren(position, checked)
                    sortAndUpdateItems(updateList)
                    return model.items.indexOf(updatedItem)
                }

                override fun isChildItemChanged(position: Int, isChildItem: Boolean) {
                    model.items[position].isChildItem = isChildItem
                }
            },
            changeHistory
        )
        binding.RecyclerView.adapter = adapter
        sortAndUpdateItems()
    }

    private fun sortAndUpdateItems(newList: List<ListItem> = model.items) {
        updateList(sortedItems(newList, preferences.listItemSorting.value))
    }

    private fun sortedItems(list: List<ListItem>, sorting: String): List<ListItem> {
        // Make sure every unchecked item has uncheckedPosition set
        list.forEachIndexed { idx, it ->
            if (!it.checked && it.uncheckedPosition == -1) it.uncheckedPosition = idx
        }
        if (sorting == ListItemSorting.autoSortByChecked) {
            val sortedParents = list.mapIndexedNotNull { idx, item ->
                if (item.isChildItem) {
                    null
                } else if (idx < list.lastIndex) {
                    val lastChildIdx = list.subList(idx + 1, list.size)
                        .indexOfFirst { !it.isChildItem } + idx
                    val children = list.subList(idx, lastChildIdx + 1)
                    children
                } else {
                    mutableListOf(item)
                }
            }.sortedWith(Comparator { i1, i2 ->
                val parent1 = i1[0]
                val parent2 = i2[0]
                if (parent1.checked && !parent2.checked) {
                    return@Comparator 1
                }
                if (!parent1.checked && parent2.checked) {
                    return@Comparator -1
                }
                return@Comparator parent1.uncheckedPosition.compareTo(parent2.uncheckedPosition)

            })
            val sortedItems =  sortedParents.flatten().toMutableList()
            sortedItems.forEachIndexed { index, item -> if(!item.checked) item.uncheckedPosition = index }
            return sortedItems
        }
        return list.toMutableList()
    }

    private fun updateList(newList: List<ListItem>) {
        val diffCallback = ListItemCallback(model.items, newList)
        val diffCourses = DiffUtil.calculateDiff(diffCallback)
        model.items.clear()
        model.items.addAll(newList)
        diffCourses.dispatchUpdatesTo(adapter)
    }

    /**
     * Checks item at position and its children (not in-place, returns cloned list)
     *
     * @return The updated ListItem + the updated ListItem
     */
    private fun checkWithAllChildren(position: Int, checked: Boolean): Pair<ListItem, List<ListItem>> {
        val items = model.items.toMutableList()
        val item = items[position].clone() as ListItem
        items[position] = item
        item.checked = checked
        var childPosition = position + 1
        while (childPosition < items.size) {
            val childItem = items[childPosition].clone() as ListItem
            items[childPosition] = childItem
            if (childItem.isChildItem) {
                if (childItem.checked != checked) {
                    childItem.checked = checked
                }
            } else {
                break;
            }
            childPosition++;
        }
        return Pair(item, items)
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
        val actualIsChildItem =
            isChildItem ?: model.items.isNotEmpty() && model.items.last().isChildItem
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
        updateList(model.items.filter { !it.checked }.toMutableList())
    }

    private fun checkAllItems(checked: Boolean) {
        model.items.forEachIndexed { idx, item ->
            if (item.checked != checked) {
                item.checked = checked
                adapter.notifyItemChanged(idx)
            }
        }
    }
}