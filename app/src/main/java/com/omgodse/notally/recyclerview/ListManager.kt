package com.omgodse.notally.recyclerview

import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.miscellaneous.CheckedSorter
import com.omgodse.notally.miscellaneous.addAndNotify
import com.omgodse.notally.miscellaneous.isChildOf
import com.omgodse.notally.miscellaneous.moveRangeAndNotify
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.viewholder.MakeListVH
import com.omgodse.notally.room.ListItem

class ListManager(
    private val items: MutableList<ListItem>,
    private val recyclerView: RecyclerView,
    private val preferences: Preferences,
    private val inputMethodManager: InputMethodManager
) {

    internal lateinit var adapter: RecyclerView.Adapter<MakeListVH>

    internal fun add(
        position: Int = items.size,
        initialText: String = "",
        checked: Boolean = false,
        isChild: Boolean? = null,
        uncheckedPosition: Int? = position,
        children: MutableList<ListItem> = mutableListOf()
    ) {
        val listItem =
            ListItem(
                initialText,
                checked,
                isChild ?: (items.isNotEmpty() && items.last().isChild),
                uncheckedPosition,
                children
            )
        items.addAndNotify(position, listItem, adapter)
        for ((idx, item) in children.withIndex()) {
            items.addAndNotify(position + idx + 1, item, adapter)
        }
        recyclerView.post {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as MakeListVH?
            if (!checked && viewHolder != null) {
                val editText = viewHolder.binding.EditText
                editText.requestFocus()
                editText.setSelection(editText.text.length)
                inputMethodManager.showSoftInput(
                    viewHolder.binding.EditText,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
    }

    internal fun add(position: Int, item: ListItem) {
        add(
            position,
            item.body,
            item.checked,
            item.isChild,
            item.uncheckedPosition,
            item.children
        )
    }

    internal fun delete(position: Int = items.size - 1, force: Boolean): ListItem? {
        var listItem: ListItem? = null
        if (force || position > 0) {
            listItem = items.removeAt(position)
            listItem.children.indices.forEach { items.removeAt(position) }
            adapter.notifyItemRangeRemoved(position, listItem.children.size + 1)
        }
        if (!force) {
            if (position > 0) {
                this.moveFocusToNext(position - 2)
            } else if (items.size > 1) {
                this.moveFocusToNext(position)
            }
        }
        return listItem
    }

    internal fun swap(positionFrom: Int, positionTo: Int): Boolean {
        if (positionFrom < 0 || positionTo < 0)
            return false
        val itemTo = items[positionTo]
        val itemFrom = items[positionFrom]
        // Disallow dragging into item's own children or unchecked item under any checked item (if auto-sort enabled)
        if (itemTo.isChildOf(itemFrom)
            || preferences.listItemSorting.value == ListItemSorting.autoSortByChecked && itemTo.checked
        ) {
            return false
        }
        items.moveRangeAndNotify(
            positionFrom,
            itemFrom.children.size + 1,
            positionTo,
            adapter
        )

        return true
    }

    internal fun move(positionFrom: Int, positionTo: Int, byDrag: Boolean): Boolean {
        val itemTo = items[positionTo]
        var itemFrom = items[positionFrom]

        if (byDrag) {
            // have already been swapped
            itemFrom = itemTo
        } else {
            items.moveRangeAndNotify(
                positionFrom,
                itemFrom.children.size + 1,
                positionTo,
                adapter
            )
        }
        val isChildBefore = itemFrom.isChild

        if ((positionTo < positionFrom && (!byDrag && itemTo.isChild || byDrag && isBeforeChildItem(
                positionTo
            )))
            || (positionTo > positionFrom && isBeforeChildItem(positionTo))
        ) {
            val movedPosition = if (byDrag) positionTo else positionFrom
            for (position in movedPosition..movedPosition + itemFrom.children.size) {
                updateChildren(position, true)
                adapter.notifyItemChanged(position)
            }
        }
        if (positionTo == 0 && itemFrom.isChild) {
            itemFrom.isChild = false
            adapter.notifyItemChanged(positionTo)
        }

        return isChildBefore
    }

    internal fun revertMove(
        positionFrom: Int,
        positionTo: Int,
        isChildBefore: Boolean?
    ) {
        val itemTo = items[positionTo]

        val actualPositionFrom = positionFrom + itemTo.children.size
        items.moveRangeAndNotify(
            positionTo,
            itemTo.children.size + 1,
            actualPositionFrom,
            adapter
        )

        if (isChildBefore != null) {
            items[positionFrom].isChild = isChildBefore
            adapter.notifyItemChanged(positionFrom)
        }

        itemTo.children.forEachIndexed { index, item ->
            item.isChild = true
            adapter.notifyItemChanged(positionTo + index)
        }

        return
    }

    internal fun moveFocusToNext(position: Int) {
        this.moveToNextInternal(position)
    }

    internal fun changeText(position: Int, text: String) {
        val item = items[position]
        item.body = text
    }

    internal fun changeChecked(
        position: Int,
        checked: Boolean
    ): Int {
        val item = items[position]
        if (item.checked == checked) {
            return position
        }
        if (checked) {
            item.uncheckedPosition = position
        }
        if (item.isChild) {
            item.checked = checked
            adapter.notifyItemChanged(position)
            return position
        }
        if (!checked) {
            item.uncheckedPosition = item.uncheckedPosition?.dec()
        }
        val (updatedItem, updateList) = checkWithAllChildren(position, checked)
        sortAndUpdateItems(updateList)
        return items.indexOf(updatedItem)
    }

    internal fun changeIsChild(position: Int, isChild: Boolean) {
        updateChildren(position, isChild)
    }

    internal fun checkAllItems(checked: Boolean) {
        items.forEachIndexed { idx, item ->
            if (item.checked != checked) {
                item.checked = checked
                adapter.notifyItemChanged(idx)
            }
        }
    }

    internal fun deleteCheckedItems() {
        updateList(items.filter { !it.checked }.toMutableList())
    }

    internal fun sortAndUpdateItems(newList: List<ListItem> = items) {
        updateList(sortedItems(newList, preferences.listItemSorting.value))
    }

    private fun updateList(newList: List<ListItem>) {
        val diffCallback = ListItemCallback(items, newList)
        val diffCourses = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newList)
        diffCourses.dispatchUpdatesTo(adapter)

        diffCourses.dispatchUpdatesTo(object: ListUpdateCallback{
            override fun onInserted(position: Int, count: Int) {
                println("onInserted pos: $position count: $count")
            }

            override fun onRemoved(position: Int, count: Int) {
                println("onRemoved pos: $position count: $count")
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                println("onMoved fromPosition: $fromPosition toPosition: $toPosition")
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                println("onChanged position: $position count: $count")
            }

        })
    }


    private fun updateChildren(position: Int, isChild: Boolean) {
        val item = items[position]
        item.isChild = isChild
        val parentAndIndex = findParentItem(position) ?: return
        if (isChild) {
            parentAndIndex.first.children.add(parentAndIndex.second, item)
        } else {
            parentAndIndex.first.children.remove(item)
        }
    }

    private fun findParentItem(childPosition: Int): Pair<ListItem, Int>? {
        (childPosition - 1 downTo 0).forEachIndexed { index, position ->
            if (!items[position].isChild) {
                return Pair(items[position], index)
            }
        }
        return null
    }

    private fun isBeforeChildItem(positionTo: Int): Boolean {
        return positionTo < items.lastIndex && items[positionTo + 1].isChild
    }

    private fun sortedItems(list: List<ListItem>, sorting: String): List<ListItem> {
        return SORTERS[sorting]?.sort(list) ?: list.toMutableList()
    }

    /**
     * Checks item at position and its children (not in-place, returns cloned list)
     *
     * @return The updated ListItem + the updated ListItem
     */
    private fun checkWithAllChildren(
        position: Int,
        checked: Boolean
    ): Pair<ListItem, List<ListItem>> {
        val items = items.toMutableList()
        val item = items[position].clone() as ListItem
        items[position] = item
        item.checked = checked
        var childPosition = position + 1
        while (childPosition < items.size) {
            val childItem = items[childPosition].clone() as ListItem
            items[childPosition] = childItem
            if (childItem.isChild) {
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

    private fun moveToNextInternal(currentPosition: Int) {
        val viewHolder =
            recyclerView.findViewHolderForAdapterPosition(currentPosition + 1) as MakeListVH?
        if (viewHolder != null) {
            if (viewHolder.binding.CheckBox.isChecked) {
                moveToNextInternal(currentPosition + 1)
            } else viewHolder.binding.EditText.requestFocus()
        } else add()
    }

    companion object {
        private val SORTERS = mapOf(ListItemSorting.autoSortByChecked to CheckedSorter())
    }
}
