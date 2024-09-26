package com.omgodse.notally.miscellaneous

import com.omgodse.notally.room.ListItem

interface ListItemSorterStrategy {

    fun sort(list: List<ListItem>): MutableList<ListItem>
}