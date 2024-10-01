package com.omgodse.notally.recyclerview

import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.changehistory.ChangeHistory
import com.omgodse.notally.changehistory.ListAddChange
import com.omgodse.notally.changehistory.ListCheckedChange
import com.omgodse.notally.changehistory.ListDeleteChange
import com.omgodse.notally.changehistory.ListEditTextChange
import com.omgodse.notally.changehistory.ListIsChildChange
import com.omgodse.notally.changehistory.ListMoveChange
import com.omgodse.notally.miscellaneous.CheckedSorter
import com.omgodse.notally.miscellaneous.updateUncheckedPositions
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.viewholder.MakeListVH
import com.omgodse.notally.room.ListItem

/**
 * Should be used for all changes to the items list.
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
        item: ListItem = defaultNewItem(position),
        pushChange: Boolean = true
    ) {
        items.addAndNotify(position, item, adapter)
        for ((idx, child) in item.children.withIndex()) {
            items.addAndNotify(position + idx + 1, child, adapter)
        }
        position.updateIsChild(item.isChild)
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
        position: Int = items.lastIndex,
        force: Boolean = true,
        pushChange: Boolean = true
    ): ListItem? {
        if (position < 0 || position > items.lastIndex) {
            return null
        }
        var item: ListItem? = null
        if (force || position > 0) {
            item = deleteItemAndNotify(position)
        }
        if (!force) {
            if (position > 0) {
                this.moveFocusToNext(position - 2)
            } else if (items.size > 1) {
                this.moveFocusToNext(position)
            }
        }
        if (item != null && pushChange) {
            changeHistory.push(ListDeleteChange(position, item, this))
        }
        return item
    }

    internal fun updateChildrenAndPushMoveChange(
        positionFrom: Int,
        positionTo: Int,
        newPosition: Int,
        itemBeforeMove: ListItem,
        updateChildren: Boolean,
        pushChange: Boolean
    ) {
        if (updateChildren) {
            updateChildrenAfterMove(newPosition)
        }
        if (pushChange) {
            changeHistory.push(
                ListMoveChange(
                    positionFrom,
                    positionTo,
                    newPosition,
                    itemBeforeMove,
                    this
                )
            )
        }
    }

    /**
     * @return position of the moved item afterwards
     */
    internal fun move(
        positionFrom: Int,
        positionTo: Int,
        pushChange: Boolean = true,
        updateChildren: Boolean = true
    ): Int? {
        val itemTo = items[positionTo]
        val itemFrom = items[positionFrom]
        val itemBeforeMove = itemFrom.clone() as ListItem
        // Disallow move unchecked item under any checked item (if auto-sort enabled)
        if (isAutoSortByCheckedEnabled() && itemTo.checked || itemTo.isChildOf(itemFrom)) {
            return null
        }

        val newPosition = items.moveRangeAndNotify(
            positionFrom,
            itemFrom.itemCount,
            positionTo,
            adapter
        )

        if (newPosition == null) {
            return null
        }

        updateChildrenAndPushMoveChange(
            positionFrom,
            positionTo,
            newPosition,
            itemBeforeMove,
            updateChildren,
            pushChange
        )
        return newPosition
    }

    internal fun revertMove(
        positionAfter: Int,
        positionFrom: Int,
        itemBeforeMove: ListItem
    ) {
        val actualPositionTo = if (positionAfter < positionFrom) {
            positionFrom + itemBeforeMove.children.size
        } else {
            positionFrom
        }
        val positionBefore =
            move(positionAfter, actualPositionTo, pushChange = false, updateChildren = false)!!
        if (items[positionBefore].isChild != itemBeforeMove.isChild) {
            positionBefore.updateIsChild(itemBeforeMove.isChild)
        } else {
            updateAllChildren()
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
            // TODO: ?
            item.uncheckedPosition = item.uncheckedPosition?.dec()
        }
        val (updatedItem, updatedList) = checkWithAllChildren(position, checked)
        sortAndUpdateItems(updatedList, false)

        val positionAfter = items.indexOf(updatedItem)
        if (pushChange) {
            pushChangeCheckChange(position, positionAfter, checked)
        }
        return positionAfter
    }

    internal fun getItem(position: Int): ListItem {
        return items[position]
    }

    private fun updateAllChildren() {
        var parent: ListItem? = null
        items.forEach { item ->
            if (item.isChild) {
                item.children.clear()
                parent!!.children.add(item)
            } else {
                position.updateIsChild(false)
            }
        }
    }

    private fun isAutoSortByCheckedEnabled() =
        preferences.listItemSorting.value == ListItemSorting.autoSortByChecked

    private fun updateChildrenAfterMove(
        position: Int,
    ) {
        if (isBeforeChildItemOfOtherParent(position)) {
            position.updateIsChild(true, forceOnChildren = true)
        } else if (position == 0) {
            position.updateIsChild(false)
        } else {
            updateAllChildren()
        }
    }

    private fun pushChangeCheckChange(position: Int, positionAfter: Int, checked: Boolean) {
        changeHistory.push(ListCheckedChange(checked, position, positionAfter, this))
    }

    private fun Int.parentPosition(allowFindSelf: Boolean = false): Int? {
        return this.findParentItem(allowFindSelf).second
    }

    private fun deleteItemAndNotify(position: Int): ListItem {
        val item = items.removeAt(position)
        item.children.indices.forEach { items.removeAt(position) }
        adapter.notifyItemRangeRemoved(position, item.itemCount)
        return item
    }

    internal fun changeIsChild(position: Int, isChild: Boolean, pushChange: Boolean = true) {
        position.updateIsChild(isChild)
        if (pushChange) {
            changeHistory.push(ListIsChildChange(isChild, position, this))
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

    internal fun initList() {
        items.forEachIndexed { index, item -> item.id = index }
        sortAndUpdateItems(initUncheckedPositions = true)
    }


    private fun sortAndUpdateItems(
        newList: MutableList<ListItem> = items,
        initUncheckedPositions: Boolean = false
    ) {
        updateList(sortedItems(newList, preferences.listItemSorting.value, initUncheckedPositions))
    }

    private fun updateList(newList: List<ListItem>) {
        val diffCallback = ListItemCallback(items, newList)
        val diffCourses = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newList)
        diffCourses.dispatchUpdatesTo(adapter)
    }

    private fun Int.updateIsChild(isChild: Boolean, forceOnChildren: Boolean = false) {
        val item = items[this]
        val isValueChanged = isChild != item.isChild
        item.isChild = isChild
        if (forceOnChildren) {
            item.children.forEachIndexed { childIndex, it ->
                if (it.isChild != isChild) {
                    it.isChild = isChild
                    adapter.notifyItemChanged(this + childIndex + 1)
                }
            }
        }
        updateAllChildren() // TODO: optimize performance by only updating position
        if (isValueChanged) {
            adapter.notifyItemChanged(this)
        }
    }

    private fun isBeforeChildItemOfOtherParent(position: Int): Boolean {
        val item = items[position]
        return position > 0 && item.isNextItemChild(position) && !items[position + item.itemCount].isChildOf(
            position
        )
    }

    private fun ListItem.isNextItemChild(position: Int): Boolean {
        return (position < items.size - itemCount) && (items[position + this.itemCount].isChild)
    }

    private fun sortedItems(
        list: MutableList<ListItem>,
        sorting: String,
        initUncheckedPositions: Boolean
    ): List<ListItem> {
        return SORTERS[sorting]?.sort(list, initUncheckedPositions) ?: list.toMutableList()
    }

    /**
     * Checks item at position and its children (not in-place, returns cloned list)
     *
     * @return The updated ListItem + the updated List
     */
    private fun checkWithAllChildren(
        position: Int,
        checked: Boolean
    ): Pair<ListItem, MutableList<ListItem>> {
        val items = items.toMutableList()
        val item = items[position].clone() as ListItem
        items[position] = item
        item.checked = checked
        for ((index, childItem) in item.children.withIndex()) {
            val updatedChildItem = childItem.clone() as ListItem
            updatedChildItem.checked = checked
            items[position + index + 1] = updatedChildItem
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

    private fun defaultNewItem(position: Int) = ListItem(
        "",
        false,
        items.isNotEmpty() &&
                ((position < items.size && items[position].isChild)
                        || (position == items.size && items[position - 1].isChild)),
        null,
        mutableListOf()
    )

    private val ListItem.itemCount: Int
        get() = children.size + 1

    internal fun MutableList<ListItem>.moveRangeAndNotify(
        fromIndex: Int,
        itemCount: Int,
        toIndex: Int,
        adapter: RecyclerView.Adapter<*>
    ): Int? {
        if (fromIndex == toIndex || itemCount <= 0) return null

        val itemsToMove = subList(fromIndex, fromIndex + itemCount).toList()
        removeAll(itemsToMove)
        val insertIndex = if (fromIndex < toIndex) toIndex - itemCount + 1 else toIndex
        addAll(insertIndex, itemsToMove)
        updateUncheckedPositions()
        val movedIndexes = if (fromIndex < toIndex) {
            itemCount - 1 downTo 0
        } else {
            0 until itemCount
        }
        for (idx in movedIndexes) {
            val newPosition =
                if (fromIndex < toIndex) {
                    toIndex + idx - (itemCount - 1)
                } else {
                    toIndex + idx
                }
            adapter.notifyItemMoved(fromIndex + idx, newPosition)
        }
        return insertIndex
    }

    private fun MutableList<ListItem>.addAndNotify(
        position: Int,
        item: ListItem,
        adapter: RecyclerView.Adapter<*>
    ) {
        if (item.checked && item.uncheckedPosition == null) {
            item.uncheckedPosition = position
        }
        add(position, item)
        adapter.notifyItemInserted(position)
    }

    private fun ListItem.isChildOf(other: ListItem): Boolean {
        return !other.isChild && other.children.contains(this)
    }

    private fun ListItem.isChildOf(otherPosition: Int): Boolean {
        return isChildOf(items[otherPosition])
    }

    companion object {
        private val SORTERS = mapOf(ListItemSorting.autoSortByChecked to CheckedSorter())
    }
}


