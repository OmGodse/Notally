package com.omgodse.notally.recyclerview

import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.changehistory.ChangeHistory
import com.omgodse.notally.changehistory.ListAddChange
import com.omgodse.notally.changehistory.ListCheckedChange
import com.omgodse.notally.changehistory.ListDeleteChange
import com.omgodse.notally.changehistory.ListEditTextChange
import com.omgodse.notally.changehistory.ListIsChildChange
import com.omgodse.notally.changehistory.ListMoveChange
import com.omgodse.notally.miscellaneous.CheckedSorter
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.viewholder.MakeListVH
import com.omgodse.notally.room.ListItem

/**
 * Should be used for all changes to the items list. Notifies the RecyclerView.Adapter and pushes
 * according changes to the ChangeHistory
 */
class ListManager(
    private val items: MutableList<ListItem>,
    private val recyclerView: RecyclerView,
    private val changeHistory: ChangeHistory,
    private val preferences: Preferences,
    private val inputMethodManager: InputMethodManager,
) {

    internal lateinit var adapter: RecyclerView.Adapter<MakeListVH>

    fun add(
        position: Int = items.size,
        item: ListItem = defaultNewItem(position),
        pushChange: Boolean = true,
    ) {
        val itemBeforeInsert = item.clone() as ListItem
        for ((idx, newItem) in (item + item.children).withIndex()) {
            val insertPosition = position + idx
            items.addAndNotify(insertPosition, newItem, adapter)
            if (newItem.uncheckedPosition == null) {
                newItem.uncheckedPosition = insertPosition
            }
        }
        items.sortAndUpdateItems(adapter = adapter)
        items.updateAllChildren()
        if (pushChange) {
            changeHistory.push(ListAddChange(position, itemBeforeInsert, this))
        }
        recyclerView.post {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position) as MakeListVH?
            if (!item.checked && viewHolder != null) {
                viewHolder.focusEditText(inputMethodManager = inputMethodManager)
            }
        }
    }

    /**
     * Deletes item and its children at given position.
     *
     * @param force if false, deletion can be rejected, e.g. if trying to delete the first item
     * @param childrenToDelete can be used when a ListAddChange is undone to pass the item at the
     *   state before the insertion
     * @param allowFocusChange if true the UI will focus the last valid ListItem's EditText
     * @return the removed ListItem or null if deletion was rejected
     */
    fun delete(
        position: Int = items.lastIndex,
        force: Boolean = true,
        childrenToDelete: List<ListItem>? = null,
        pushChange: Boolean = true,
        allowFocusChange: Boolean = true,
    ): ListItem? {
        if (position < 0 || position > items.lastIndex) {
            return null
        }
        var item: ListItem? = null
        if (force || position > 0) {
            item = items.deleteItemAndNotify(position, childrenToDelete, adapter)
        }
        if (!force && allowFocusChange) {
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

    /** @return position of the moved item afterwards */
    fun move(
        positionFrom: Int,
        positionTo: Int,
        pushChange: Boolean = true,
        updateChildren: Boolean = true,
    ): Int? {
        val itemTo = items[positionTo]
        val itemFrom = items[positionFrom]
        val itemBeforeMove = itemFrom.clone() as ListItem
        // Disallow move unchecked item under any checked item (if auto-sort enabled)
        if (isAutoSortByCheckedEnabled() && itemTo.checked || itemTo.isChildOf(itemFrom)) {
            return null
        }

        val newPosition =
            items.moveItemRangeAndNotify(positionFrom, itemFrom.itemCount, positionTo, adapter)
                ?: return null

        finishMove(
            positionFrom,
            positionTo,
            newPosition,
            itemBeforeMove,
            updateChildren,
            pushChange,
        )
        return newPosition
    }

    fun finishMove(
        positionFrom: Int,
        positionTo: Int,
        newPosition: Int,
        itemBeforeMove: ListItem,
        updateChildren: Boolean,
        pushChange: Boolean,
    ) {
        if (updateChildren) {
            if (newPosition.isBeforeChildItemOfOtherParent) {
                items.setIsChildAndNotify(newPosition, true, true, adapter)
            } else if (newPosition == 0) {
                items.setIsChildAndNotify(newPosition, false, adapter = adapter)
            } else {
                items.updateAllChildren()
            }
        }
        if (pushChange) {
            changeHistory.push(
                ListMoveChange(positionFrom, positionTo, newPosition, itemBeforeMove, this)
            )
        }
    }

    fun revertMove(positionAfter: Int, positionFrom: Int, itemBeforeMove: ListItem) {
        val actualPositionTo =
            if (positionAfter < positionFrom) {
                positionFrom + itemBeforeMove.children.size
            } else {
                positionFrom
            }
        val positionBefore =
            move(positionAfter, actualPositionTo, pushChange = false, updateChildren = false)!!
        if (items[positionBefore].isChild != itemBeforeMove.isChild) {
            items.setIsChildAndNotify(positionBefore, itemBeforeMove.isChild, adapter = adapter)
        } else {
            items.updateAllChildren()
        }
    }

    fun changeText(
        editText: EditText,
        listener: TextWatcher,
        position: Int,
        textBefore: String,
        textAfter: String,
        pushChange: Boolean = true,
    ) {
        val item = items[position]
        item.body = textAfter
        if (pushChange) {
            changeHistory.push(
                ListEditTextChange(editText, position, textBefore, textAfter, listener, this)
            )
        }
    }

    fun changeChecked(position: Int, checked: Boolean, pushChange: Boolean = true): Int {
        val item = items[position]
        if (item.checked == checked) {
            return position
        }
        if (checked) {
            item.uncheckedPosition = position
        }
        if (item.isChild) {
            items.setCheckedAndNotify(position, checked, adapter)
            if (pushChange) {
                changeHistory.push(ListCheckedChange(checked, position, position, this))
            }
            return position
        }
        if (!checked && isAutoSortByCheckedEnabled()) {
            // TODO: atm needed for correct sorting. If an item is unchecked without decrementing
            //   uncheckedPosition it will be positioned 1 below its correct position
            item.uncheckedPosition = item.uncheckedPosition?.dec()
        }
        val (updatedItem, updatedList) = checkWithAllChildren(position, checked)
        items.sortAndUpdateItems(updatedList, false, adapter)

        val positionAfter = items.indexOf(updatedItem)
        if (pushChange) {
            changeHistory.push(ListCheckedChange(checked, position, positionAfter, this))
        }
        return positionAfter
    }

    fun changeCheckedForAll(checked: Boolean) {
        items.indices.forEach { items.setCheckedAndNotify(it, checked, adapter) }
        // TODO: Add to ChangeHistory - Have to make a snapshot of which items were checked before
    }

    fun changeIsChild(position: Int, isChild: Boolean, pushChange: Boolean = true) {
        items.setIsChildAndNotify(position, isChild, adapter = adapter)
        if (pushChange) {
            changeHistory.push(ListIsChildChange(isChild, position, this))
        }
    }

    fun moveFocusToNext(position: Int) {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position + 1) as MakeListVH?
        if (viewHolder != null) {
            if (viewHolder.binding.CheckBox.isChecked) {
                moveFocusToNext(position + 1)
            } else viewHolder.binding.EditText.requestFocus()
        } else add(pushChange = false)
    }

    fun getItem(position: Int): ListItem {
        return items[position]
    }

    fun deleteCheckedItems() {
        items.updateList(items.filter { !it.checked }.toMutableList(), adapter)
        // TODO: Add to ChangeHistory
    }

    fun initList() {
        items.forEachIndexed { index, item -> item.id = index }
        items.sortAndUpdateItems(initUncheckedPositions = true, adapter = adapter)
    }

    internal fun defaultNewItem(position: Int) =
        ListItem(
            "",
            false,
            items.isNotEmpty() &&
                ((position < items.size && items[position].isChild) ||
                    (position > 0 && items[position - 1].isChild)),
            null,
            mutableListOf(),
        )

    private fun isAutoSortByCheckedEnabled() =
        preferences.listItemSorting.value == ListItemSorting.autoSortByChecked

    /**
     * Checks item at position and its children (not in-place, returns cloned list)
     *
     * @return The updated ListItem + the updated List
     */
    private fun checkWithAllChildren(
        position: Int,
        checked: Boolean,
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

    private val Int.isBeforeChildItemOfOtherParent: Boolean
        get() {
            val item = items[this]
            return this > 0 &&
                item.isNextItemChild(this) &&
                !items[this + item.itemCount].isChildOf(this)
        }

    private fun ListItem.isNextItemChild(position: Int): Boolean {
        return (position < items.size - itemCount) && (items[position + this.itemCount].isChild)
    }

    private fun ListItem.isChildOf(otherPosition: Int): Boolean {
        return isChildOf(items[otherPosition])
    }

    private fun MutableList<ListItem>.sortAndUpdateItems(
        newList: MutableList<ListItem> = items,
        initUncheckedPositions: Boolean = false,
        adapter: RecyclerView.Adapter<*>,
    ) {
        val sortedList =
            SORTERS[preferences.listItemSorting.value]?.sort(newList, initUncheckedPositions)
        this.updateList(sortedList ?: newList.toMutableList(), adapter)
    }

    companion object {
        private val SORTERS = mapOf(ListItemSorting.autoSortByChecked to CheckedSorter())
    }
}
