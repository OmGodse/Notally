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

    internal fun swap(positionFrom: Int, positionTo: Int): Boolean {
        if (positionFrom < 0 || positionTo < 0 || positionFrom == positionTo)
            return false
        val itemTo = items[positionTo]
        val itemFrom = items[positionFrom]
        // Disallow dragging into item's own children or unchecked item under any checked item (if auto-sort enabled)
        if (itemTo.isChildOf(itemFrom) || isAutoSortByCheckedEnabled() && itemTo.checked) {
            return false
        }
        items.moveRangeAndNotify(
            positionFrom,
            itemFrom.itemCount,
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
        if (isAutoSortByCheckedEnabled() && itemTo.checked || itemTo.isChildOf(itemFrom)) {
            return
        }

        items.moveRangeAndNotify(
            positionFrom,
            itemFrom.itemCount,
            positionTo,
            adapter
        )

        updateChildrenAfterMove(itemFrom, positionTo, positionFrom)
        if (pushChange) {
            changeHistory.push(
                ListMoveChange(
                    positionFrom,
                    positionTo,
                    itemFrom.isChild,
                    itemFrom.children.isNotEmpty(),
                    this
                )
            )
        }
    }

    internal fun revertMove(
        positionFrom: Int,
        positionTo: Int,
        isChildBefore: Boolean?,
        hadChildren: Boolean?
    ) {
        val itemTo = items[positionTo]

        val actualPositionFrom = positionFrom + itemTo.children.size
        var actualPositionTo = positionTo
        var itemCount = itemTo.itemCount
        if (isChildBefore != null && hadChildren == true) {
            val (parent, parentPosition, _) = positionTo.findParentItem(positionTo == items.lastIndex)
            if (parent != null) {
                actualPositionTo = parentPosition!!
                itemCount = parent.itemCount
            }
        }
        items.moveRangeAndNotify(
            actualPositionTo,
            itemCount,
            actualPositionFrom,
            adapter
        )

        if (isChildBefore != null && items[actualPositionFrom].isChild != isChildBefore) {
            actualPositionFrom.updateIsChild(isChildBefore)
        }

        if (isChildBefore == true) {
            findParentAndUpdateChildren(actualPositionTo, actualPositionTo == items.lastIndex)
            findParentAndUpdateChildren(actualPositionFrom)
        }
    }

    internal fun endDrag(
        positionFrom: Int,
        positionTo: Int,
        draggedItemIsChild: Boolean,
        pushChange: Boolean = true
    ) {
        var actualPositionFrom = positionTo
        var actualPositionTo = if (isMoveUp(positionFrom, positionTo)) {
            positionTo + 1
        } else {
            positionTo - 1
        }

        if (!draggedItemIsChild) {
            val parentPosition = positionTo.parentPosition()
            actualPositionFrom = parentPosition ?: positionTo
            actualPositionTo = if (parentPosition == null) {
                actualPositionFrom
            } else {
                Math.max(0, actualPositionFrom - 1)
            }
        }

        val itemFrom = items[actualPositionFrom]
        val itemTo = items[actualPositionTo]

        // Disallow move unchecked item under any checked item (if auto-sort enabled)
        if (isAutoSortByCheckedEnabled() && itemTo.checked) {
            return
        }

        updateChildrenAfterMove(itemFrom, positionTo, positionFrom)
        if (draggedItemIsChild) {
            findParentAndUpdateChildren(positionFrom, positionTo == 0)
            findParentAndUpdateChildren(positionTo, positionTo == items.lastIndex)
        }
        if (pushChange) {
            changeHistory.push(
                ListMoveChange(
                    positionFrom,
                    positionTo,
                    draggedItemIsChild,
                    itemFrom.children.isNotEmpty(),
                    this
                )
            )
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

    private fun findParentAndUpdateChildren(position: Int, allowFindSelf: Boolean = false) {
        val (parent, parentPosition, _) = position.findParentItem(allowFindSelf)
        parentPosition?.let {
            if (parent != items[position]) {
                parentPosition.updateIsChild(false)
            } else {
                position.updateIsChild(false)
            }
        }
    }

    private fun isAutoSortByCheckedEnabled() =
        preferences.listItemSorting.value == ListItemSorting.autoSortByChecked

    private fun updateChildrenAfterMove(
        itemFrom: ListItem,
        positionTo: Int,
        positionFrom: Int,
    ) {
        if (isBeforeChildItem(positionTo)) {
            for (position in positionTo..positionTo + itemFrom.children.size) {
                position.updateIsChild(true)
            }
        }
        if (positionTo == 0 && itemFrom.isChild) {
            positionTo.updateIsChild(false)
        }
    }

    private fun isMoveUp(positionFrom: Int, positionTo: Int): Boolean {
        return positionTo < positionFrom
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

    private fun Int.updateIsChild(isChild: Boolean) {
        val item = items[this]
        val isValueChanged = isChild != item.isChild
        item.isChild = isChild
        val (parent, _, childIndex) = this.findParentItem()
        if (isChild) {
            parent?.children?.add(childIndex!!, item)
            parent?.children?.addAll(childIndex!! + 1, item.children)
            item.children.clear()
        } else {
            val children = this.findChildren()
            parent?.children?.remove(item)
            parent?.children?.removeAll(children)
            item.children.clear()
            item.children.addAll(children)
        }
        if (isValueChanged) {
            adapter.notifyItemChanged(this)
        }
    }

    private fun Int.findChildren(): MutableList<ListItem> {
        val children = mutableListOf<ListItem>()
        for (position in this + 1..items.lastIndex) {
            if (items[position].isChild) {
                children.add(items[position])
            } else {
                return children
            }
        }
        return children
    }

    private fun Int.findParentItem(allowFindSelf: Boolean = false): Triple<ListItem?, Int?, Int?> {
        val startPosition = if (allowFindSelf) {
            this
        } else {
            this - 1
        }
        for ((childIndex, position) in (startPosition downTo 0).withIndex()) {
            if (!items[position].isChild) {
                return Triple(items[position], position, childIndex)
            }
        }
        return Triple(null, null, null)
    }

    private fun isBeforeChildItem(positionTo: Int): Boolean {
        return positionTo < items.lastIndex && items[positionTo + 1].isChild
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
    ) {
        if (fromIndex == toIndex || itemCount <= 0) return

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

    companion object {
        private val SORTERS = mapOf(ListItemSorting.autoSortByChecked to CheckedSorter())
    }
}


