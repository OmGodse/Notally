package com.omgodse.notally.recyclerview

import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.changehistory.ChangeHistory
import com.omgodse.notally.changehistory.ListAddChange
import com.omgodse.notally.changehistory.ListBooleanChange
import com.omgodse.notally.changehistory.ListDeleteChange
import com.omgodse.notally.changehistory.ListEditTextChange
import com.omgodse.notally.changehistory.ListMoveChange
import com.omgodse.notally.miscellaneous.CheckedSorter
import com.omgodse.notally.miscellaneous.addAndNotify
import com.omgodse.notally.miscellaneous.isChildOf
import com.omgodse.notally.miscellaneous.moveRangeAndNotify
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.viewholder.MakeListVH
import com.omgodse.notally.room.ListItem

/**
 * Should be use for all changes to the items list..
 * Notifies the RecyclerView.Adapter and pushes according changes to the ChangeHistory
 */
class ListManager(
    private val items: MutableList<ListItem>,
    private val recyclerView: RecyclerView,
    private val changeHistory: ChangeHistory,
    private val preferences: Preferences,
    private val inputMethodManager: InputMethodManager
) {

    internal lateinit var adapter: RecyclerView.Adapter<MakeListVH>

    internal fun add(
        position: Int = items.size,
        item: ListItem = ListItem(
            "",
            false,
            items.isNotEmpty() && (position < items.size && items[position].isChild) || (position == items.size && items[position - 1].isChild),
            null,
            mutableListOf()
        ),
        pushChange: Boolean = true
    ) {
        items.addAndNotify(position, item, adapter)
        for ((idx, child) in item.children.withIndex()) {
            items.addAndNotify(position + idx + 1, child, adapter)
        }
        updateChildren(position, item.isChild)
        if (pushChange) {
            changeHistory.push(ListAddChange(position, this))
        }
        recyclerView.post {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as MakeListVH?
            if (!item.checked && viewHolder != null) {
                viewHolder.focusEditText(inputMethodManager = inputMethodManager)
            }
        }
    }

    internal fun delete(
        position: Int = items.size - 1,
        force: Boolean = true,
        pushChange: Boolean = true
    ): ListItem? {
        if (position < 0 || position > items.lastIndex) {
            return null
        }
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
        if (listItem != null && pushChange) {
            changeHistory.push(ListDeleteChange(position, listItem, this))
        }
        return listItem
    }

    internal fun swap(positionFrom: Int, positionTo: Int): Boolean {
        if (positionFrom < 0 || positionTo < 0 || positionFrom == positionTo)
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

    internal fun move(
        positionFrom: Int,
        positionTo: Int,
        pushChange: Boolean = true
    ) {
        val itemTo = items[positionTo]
        val itemFrom = items[positionFrom]

        // Disallow move unchecked item under any checked item (if auto-sort enabled)
        if (preferences.listItemSorting.value == ListItemSorting.autoSortByChecked && itemTo.checked) {
            return
        }

        // Disallow moving into item's own children
        if (itemTo.isChildOf(itemFrom)) {
            return
        }
        items.moveRangeAndNotify(
            positionFrom,
            itemFrom.children.size + 1,
            positionTo,
            adapter
        )

        updateChildrenAfterMove(itemFrom, positionTo, positionFrom)
        if (pushChange) {
            changeHistory.push(ListMoveChange(positionFrom, positionTo, itemFrom.isChild, this))
        }
    }

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
        if (pushChange) {
            changeHistory.push(ListMoveChange(positionFrom, positionTo, isChildBefore, this))
        }
    }

    internal fun revertMove(
        positionFrom: Int,
        positionTo: Int,
        isChildBefore: Boolean?
    ) {
        val itemTo = items[positionTo]

        var actualPositionFrom = positionFrom + itemTo.children.size
        var actualPositionTo = positionTo
        var itemCount = 1
        if (isChildBefore != null && !isChildBefore) {
            val parentAndIndex = findParentItem(positionTo + 1)
            parentAndIndex.second?.let {
                actualPositionTo -= it
            }
            parentAndIndex.first?.let {
                itemCount += it.children.size
            }
        }
        items.moveRangeAndNotify(
            actualPositionTo,
            itemCount,
            actualPositionFrom,
            adapter
        )

        if (isChildBefore != null && items[actualPositionFrom].isChild != isChildBefore) {
            updateChildren(actualPositionFrom, isChildBefore)
            adapter.notifyItemChanged(actualPositionFrom)
        }

        if (isChildBefore == true) {
            findParentAndUpdateChildren(actualPositionTo)
            findParentAndUpdateChildren(actualPositionFrom)
        }

        return
    }

    private fun findParentAndUpdateChildren(position: Int) {
        val (_, newParentIndex) = findParentItem(position)
        newParentIndex?.let {
            updateChildren(position - (newParentIndex + 1), false)
        }
    }

    private fun updateChildrenAfterMove(
        itemFrom: ListItem,
        positionTo: Int,
        positionFrom: Int,
    ) {
        if ((positionTo < positionFrom && isBeforeChildItem(positionTo))
            || (positionTo > positionFrom && isBeforeChildItem(positionTo))
        ) {
            for (position in positionTo..positionTo + itemFrom.children.size) {
                updateChildren(position, true)
                adapter.notifyItemChanged(position)
            }
        }
        if (positionTo == 0 && itemFrom.isChild) {
            itemFrom.isChild = false
            adapter.notifyItemChanged(positionTo)
        }

    }

    internal fun moveFocusToNext(position: Int) {
        this.moveToNextInternal(position)
    }

    internal fun changeText(
        editText: EditText,
        listener: TextWatcher,
        position: Int,
        textBefore: String,
        textAfter: String,
        pushChange: Boolean = true
    ) {
        val item = items[position]
        item.body = textAfter
        if (pushChange) {
            changeHistory.push(
                ListEditTextChange(
                    editText,
                    position,
                    textBefore,
                    textAfter,
                    listener,
                    this
                )
            )
        }
    }

    internal fun changeChecked(
        position: Int,
        checked: Boolean,
        pushChange: Boolean = true
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
            if (pushChange) {
                pushChangeCheckChange(position, position, checked)
            }
            return position
        }
        if (!checked) {
            item.uncheckedPosition = item.uncheckedPosition?.dec()
        }
        val (updatedItem, updateList) = checkWithAllChildren(position, checked)
        sortAndUpdateItems(updateList)

        val positionAfter = items.indexOf(updatedItem)
        if (pushChange) {
            pushChangeCheckChange(position, positionAfter, checked)
        }
        return positionAfter
    }

    private fun pushChangeCheckChange(position: Int, positionAfter: Int, checked: Boolean) {
        changeHistory.push(object : ListBooleanChange(checked, position, positionAfter) {
            override fun update(position: Int, value: Boolean, isUndo: Boolean) {
                changeChecked(position, value, pushChange = false)
            }

            override fun toString(): String {
                return "CheckedChange pos: $position positionAfter: $positionAfter isChecked: $checked"
            }

        })
    }

    internal fun changeIsChild(position: Int, isChild: Boolean, pushChange: Boolean = true) {
        updateChildren(position, isChild)
        adapter.notifyItemChanged(position)
        if (pushChange) {
            changeHistory.push(object : ListBooleanChange(isChild, position) {
                override fun update(position: Int, value: Boolean, isUndo: Boolean) {
                    changeIsChild(position, value, pushChange = false)
                }

                override fun toString(): String {
                    return "IsChildChange position: $position isChild: $isChild"
                }

            })
        }
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
        // TODO: can be optimized, perhaps add id field to ListItem, otherwise DiffUtil adds
        //  adapter.notifyItemRangeRemoved() and adapter.notifyItemRangeInserted instead of
        //  adapter.notifyItemChanged or adapter.notifyItemMoved
        val diffCallback = ListItemCallback(items, newList)
        val diffCourses = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newList)
        diffCourses.dispatchUpdatesTo(adapter)

        diffCourses.dispatchUpdatesTo(object : ListUpdateCallback {
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
        val parentAndIndex = findParentItem(position)
        if (isChild) {
            val parentChildren = parentAndIndex.first?.children
            parentChildren?.add(parentAndIndex.second!!, item)
            parentChildren?.addAll(item.children)
            item.children.clear()
        } else {
            parentAndIndex.first?.children?.remove(item)
            val children = findChildren(position)
            item.children.clear()
            item.children.addAll(children)
            parentAndIndex.first?.children?.removeAll(children)
        }
    }

    private fun findChildren(position: Int): MutableList<ListItem> {
        val children = mutableListOf<ListItem>()
        (position + 1..items.lastIndex).forEachIndexed { index, position ->
            if (items[position].isChild) {
                children.add(items[position])
            } else {
                return children
            }
        }
        return children
    }

    private fun findParentItem(position: Int): Pair<ListItem?, Int?> {
        (position - 1 downTo 0).forEachIndexed { index, position ->
            if (!items[position].isChild) {
                return Pair(items[position], index)
            }
        }
        return Pair(null, null)
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
        } else add(pushChange = false)
    }

    fun getItem(position: Int): ListItem {
        return items[position]
    }

    companion object {
        private val SORTERS = mapOf(ListItemSorting.autoSortByChecked to CheckedSorter())
    }
}
