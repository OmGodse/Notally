package com.omgodse.notally.test

import android.util.Log
import com.omgodse.notally.preferences.BetterLiveData
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.ListManager
import com.omgodse.notally.room.ListItem
import io.mockk.every
import io.mockk.mockkStatic
import org.mockito.Mockito.`when`

fun createListItem(
    body: String,
    checked: Boolean = false,
    isChild: Boolean = false,
    uncheckedPosition: Int? = null,
    children: MutableList<ListItem> = mutableListOf()
): ListItem {
    return ListItem(body, checked, isChild, uncheckedPosition, children)
}

fun mockPreferences(preferences: Preferences, sorting: String = ListItemSorting.autoSortByChecked) {
    `when`(preferences.listItemSorting).thenReturn(BetterLiveData(sorting))
}

fun mockAndroidLog() {
    mockkStatic(Log::class)
    every { Log.v(any(), any()) } returns 0
    every { Log.d(any(), any()) } returns 0
    every { Log.i(any(), any()) } returns 0
    every { Log.e(any(), any()) } returns 0
}

fun mockDrag(positionFrom: Int, positionTo: Int, listManager: ListManager) {
    val item = listManager.getItem(positionFrom)
    val itemCount = item.children.size + 1
    listManager.swap(positionFrom, item.children.size + 2)
    for ((idx, i) in ((positionFrom + 1)..positionTo - itemCount).withIndex()) {
        listManager.swap(i, i + itemCount)
    }
    listManager.endDrag(positionFrom, positionTo, item.isChild)
}

fun printList(items: List<ListItem>) {
    println("--------------")
    println(items.map {
        "${if (it.isChild) " >" else ""}${if (it.checked) "x" else ""} ${it.body}${
            if (it.children.isNotEmpty()) "(${
                it.children.map { it.body }.joinToString(",")
            })" else ""
        }"
    }.joinToString("\n"))
    println("--------------")
}