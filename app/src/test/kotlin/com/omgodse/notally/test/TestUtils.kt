package com.omgodse.notally.test

import android.util.Log
import com.omgodse.notally.changehistory.ListAddChange
import com.omgodse.notally.changehistory.ListCheckedChange
import com.omgodse.notally.changehistory.ListIsChildChange
import com.omgodse.notally.changehistory.ListMoveChange
import com.omgodse.notally.preferences.BetterLiveData
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.ListManager
import com.omgodse.notally.room.ListItem
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.mockito.Mockito.`when`

fun createListItem(
    body: String,
    checked: Boolean = false,
    isChild: Boolean = false,
    uncheckedPosition: Int? = null,
    children: MutableList<ListItem> = mutableListOf(),
    id: Int = -1,
): ListItem {
    return ListItem(body, checked, isChild, uncheckedPosition, children, id)
}

fun mockPreferences(preferences: Preferences, sorting: String = ListItemSorting.noAutoSort) {
    `when`(preferences.listItemSorting).thenReturn(BetterLiveData(sorting))
}

fun mockAndroidLog() {
    mockkStatic(Log::class)
    every { Log.v(any(), any()) } returns 0
    every { Log.d(any(), any()) } returns 0
    every { Log.i(any(), any()) } returns 0
    every { Log.e(any(), any()) } returns 0
}

fun ListManager.simulateDrag(positionFrom: Int, positionTo: Int): Int? {
    val item = this.getItem(positionFrom).clone() as ListItem
    val itemCount = item.children.size + 1
    var newPosition: Int? = positionTo
    if (positionFrom < positionTo) {
        for (i in positionFrom..positionTo - itemCount) {
            newPosition = this.move(i, i + itemCount, false, false)
        }
    } else {
        for ((idx, i) in (positionFrom downTo positionTo + 1).withIndex()) {
            newPosition = this.move(i, i - 1, false, false)
        }
    }
    if (newPosition != null) {
        // The items have already been moved accordingly via move() calls
        this.updateChildrenAndPushMoveChange(
            positionFrom,
            positionTo,
            newPosition,
            item,
            true,
            true,
        )
    }
    return newPosition
}

fun List<ListItem>.printList(text: String? = null) {
    text?.let { print("--------------\n$it\n") }
    println("--------------")
    println(joinToString("\n"))
    println("--------------")
}

fun List<ListItem>.readableString() = joinToString("\n")

fun List<ListItem>.assertOrder(vararg itemBodies: String) {
    itemBodies.forEachIndexed { position, s ->
        assertEquals("${this.readableString()}\nAt position: $position", s, get(position).body)
    }
}

fun List<ListItem>.assertChecked(startingPosition: Int = 0, vararg checked: Boolean) {
    checked.forEachIndexed { index, expected ->
        val position = index + startingPosition
        assertEquals("checked at position: $position", expected, get(position).checked)
    }
}

fun ListItem.assertChildren(vararg childrenBodies: String) {
    if (childrenBodies.isNotEmpty()) {
        childrenBodies.forEachIndexed { index, s ->
            assertEquals("Child at position $index", s, children[index].body)
            assertTrue(children[index].isChild)
            assertTrue(children[index].children.isEmpty())
        }
    } else {
        assertTrue(
            "'${body}' expected empty children\t actual: ${
                children.joinToString(",") { "'${it.body}'" }
            } ",
            children.isEmpty(),
        )
    }
}

fun ListMoveChange.assert(from: Int, to: Int, after: Int, itemBeforeMove: String) {
    assertEquals("from", from, position)
    assertEquals("to", to, positionTo)
    assertEquals("after", after, positionAfter)
    assertEquals("itemBeforeMove", itemBeforeMove, this.itemBeforeMove.toString())
}

fun ListCheckedChange.assert(newValue: Boolean, position: Int, positionAfter: Int) {
    assertEquals("checked", newValue, this.newValue)
    assertEquals("position", position, this.position)
    assertEquals("positionAfter", positionAfter, this.positionAfter)
}

fun ListIsChildChange.assert(newValue: Boolean, position: Int, positionAfter: Int) {
    assertEquals("isChild", newValue, this.newValue)
    assertEquals("position", position, this.position)
    assertEquals("positionAfter", positionAfter, this.positionAfter)
}

fun ListAddChange.assert(position: Int, newItem: ListItem) {
    assertEquals("position", position, this.position)
    assertEquals("newItem", newItem, this.newItem)
}
