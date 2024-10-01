package com.omgodse.notally.recyclerview

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.room.ListItem

fun MutableList<ListItem>.moveItemRangeAndNotify(
    fromIndex: Int,
    itemCount: Int,
    toIndex: Int,
    adapter: RecyclerView.Adapter<*>,
): Int? {
    if (fromIndex == toIndex || itemCount <= 0) return null

    val itemsToMove = subList(fromIndex, fromIndex + itemCount).toList()
    removeAll(itemsToMove)
    val insertIndex = if (fromIndex < toIndex) toIndex - itemCount + 1 else toIndex
    addAll(insertIndex, itemsToMove)
    updateUncheckedPositions()
    val movedIndexes =
        if (fromIndex < toIndex) {
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

fun MutableList<ListItem>.addAndNotify(
    position: Int,
    item: ListItem,
    adapter: RecyclerView.Adapter<*>,
) {
    if (item.checked && item.uncheckedPosition == null) {
        item.uncheckedPosition = position
    }
    add(position, item)
    adapter.notifyItemInserted(position)
}

fun MutableList<ListItem>.deleteItemAndNotify(
    position: Int,
    childrenToDelete: List<ListItem>? = null,
    adapter: RecyclerView.Adapter<*>,
): ListItem {
    val item = this.removeAt(position)
    if (childrenToDelete == null) {
        item.children.indices.forEach { this.removeAt(position) }
    } else {
        childrenToDelete.indices.forEach { this.removeAt(position) }
    }
    adapter.notifyItemRangeRemoved(
        position,
        if (childrenToDelete == null) item.itemCount else 1 + childrenToDelete.size,
    )
    return item
}

fun MutableList<ListItem>.updateList(newList: List<ListItem>, adapter: RecyclerView.Adapter<*>) {
    val diffCallback = ListItemCallback(this, newList)
    val diffCourses = DiffUtil.calculateDiff(diffCallback)
    this.clear()
    this.addAll(newList)
    diffCourses.dispatchUpdatesTo(adapter)
}

fun List<ListItem>.updateAllChildren() {
    var parent: ListItem? = null
    this.forEach { item ->
        if (item.isChild) {
            item.children.clear()
            parent!!.children.add(item)
        } else {
            parent = item
            parent!!.children.clear()
        }
    }
}

fun MutableList<ListItem>.updateUncheckedPositions() {
    forEachIndexed { index, item -> if (!item.checked) item.uncheckedPosition = index }
}

fun List<ListItem>.setIsChildAndNotify(
    position: Int,
    isChild: Boolean,
    forceOnChildren: Boolean = false,
    adapter: RecyclerView.Adapter<*>,
) {
    val item = this[position]
    val isValueChanged = isChild != item.isChild
    item.isChild = isChild
    if (forceOnChildren) {
        item.children.forEachIndexed { childIndex, it ->
            if (it.isChild != isChild) {
                it.isChild = isChild
                adapter.notifyItemChanged(position + childIndex + 1)
            }
        }
    }
    this.updateAllChildren() // TODO: optimize performance by only updating position
    if (isValueChanged) {
        adapter.notifyItemChanged(position)
    }
}

fun List<ListItem>.setCheckedAndNotify(
    position: Int,
    checked: Boolean,
    adapter: RecyclerView.Adapter<*>,
) {
    if (this[position].checked != checked) {
        this[position].checked = checked
        adapter.notifyItemChanged(position)
    }
}

operator fun ListItem.plus(list: MutableList<ListItem>): List<ListItem> {
    return mutableListOf(this) + list
}
