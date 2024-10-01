package com.omgodse.notally.miscellaneous

import com.omgodse.notally.room.ListItem

class CheckedSorter : ListItemSorterStrategy {
    override fun sort(
        list: MutableList<ListItem>,
        initUncheckedPositions: Boolean
    ): MutableList<ListItem> {
        if (initUncheckedPositions) {
            list.updateUncheckedPositions()
        }
        // Sorted by parents
        val sortedGroups = list.mapIndexedNotNull { idx, item ->
            if (item.isChild) {
                null
            } else if (idx < list.lastIndex) {
                val itemsBelow = list.subList(idx + 1, list.size)
                var nextParentIdx = itemsBelow.indexOfFirst { !it.isChild }
                if (nextParentIdx == -1) {
                    // there is only children below it
                    nextParentIdx = list.lastIndex
                } else {
                    nextParentIdx += idx
                }
                val items = list.subList(idx, nextParentIdx + 1)
                if (items.size > 1) {
                    items[0].children =
                        list.subList(idx + 1, nextParentIdx + 1).toMutableList()
                }
                items
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
            return@Comparator parent1.uncheckedPosition!!.compareTo(parent2.uncheckedPosition!!)

        })
        val sortedItems = sortedGroups.flatten().toMutableList()
        sortedItems.updateUncheckedPositions()
        return sortedItems
    }
}