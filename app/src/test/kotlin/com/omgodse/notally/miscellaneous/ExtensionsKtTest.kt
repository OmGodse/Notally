package com.omgodse.notally.miscellaneous

import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.test.createListItem
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class ExtensionsKtTest {
    private lateinit var list: MutableList<ListItem>
    private lateinit var adapter: RecyclerView.Adapter<*>

    @Before
    fun setup() {
        // Mock the adapter using Mockito
        adapter = mock(RecyclerView.Adapter::class.java)

        // Create a sample list of items
        list = mutableListOf(
            createListItem("Item 1", checked = true),
            createListItem("Item 2", checked = true),
            createListItem("Item 3", checked = true),
            createListItem("Item 4", checked = true),
            createListItem("Item 5", checked = true),
            createListItem("Item 6", checked = true)
        )
    }

    @Test
    fun testNoMoveWhenFromIndexEqualsToIndex() {
        list.moveRangeAndNotify(fromIndex = 2, itemCount = 2, toIndex = 2, adapter = adapter)

        // Verify no changes to the list and no adapter notifications
        assertEquals(listOf(
            createListItem("Item 1", checked = true),
            createListItem("Item 2", checked = true),
            createListItem("Item 3", checked = true),
            createListItem("Item 4", checked = true),
            createListItem("Item 5", checked = true),
            createListItem("Item 6", checked = true)
        ), list)
        verify(adapter, never()).notifyItemMoved(anyInt(), anyInt())
    }

    @Test
    fun testNoMoveWhenItemCountIsZero() {
        list.moveRangeAndNotify(fromIndex = 1, itemCount = 0, toIndex = 3, adapter = adapter)

        // Verify no changes to the list and no adapter notifications
        assertEquals(listOf(
            createListItem("Item 1", checked = true),
            createListItem("Item 2", checked = true),
            createListItem("Item 3", checked = true),
            createListItem("Item 4", checked = true),
            createListItem("Item 5", checked = true),
            createListItem("Item 6", checked = true)
        ), list)
        verify(adapter, never()).notifyItemMoved(anyInt(), anyInt())
    }

    @Test
    fun testMoveItemsForward() {
        list.moveRangeAndNotify(fromIndex = 1, itemCount = 2, toIndex = 4, adapter = adapter)

        assertEquals(listOf(
            createListItem("Item 1", checked = true),
            createListItem("Item 4", checked = true),
            createListItem("Item 5", checked = true),
            createListItem("Item 2", checked = true),
            createListItem("Item 3", checked = true),
            createListItem("Item 6", checked = true)
        ), list)

        verify(adapter).notifyItemMoved(1, 3)
        verify(adapter).notifyItemMoved(2, 4)
    }

    @Test
    fun testMoveItemsBackward() {
        list.moveRangeAndNotify(fromIndex = 3, itemCount = 2, toIndex = 1, adapter = adapter)

        // After moving items 4 and 5 backward to after 1:
        assertEquals(listOf(
            createListItem("Item 1", checked = true),
            createListItem("Item 4", checked = true),
            createListItem("Item 5", checked = true),
            createListItem("Item 2", checked = true),
            createListItem("Item 3", checked = true),
            createListItem("Item 6", checked = true)
        ), list)

        verify(adapter).notifyItemMoved(3, 1)
        verify(adapter).notifyItemMoved(4, 2)
    }

    @Test
    fun testUncheckedItemGetsUpdated() {
        list[1].checked = false

        list.moveRangeAndNotify(fromIndex = 1, itemCount = 2, toIndex = 4, adapter = adapter)

        assertEquals(listOf(
            createListItem("Item 1", checked = true),
            createListItem("Item 4", checked = true),
            createListItem("Item 5", checked = true),
            createListItem("Item 2", checked = false, uncheckedPosition = 3),
            createListItem("Item 3", checked = true),
            createListItem("Item 6", checked = true)
        ), list)

        verify(adapter).notifyItemMoved(2, 4)
        verify(adapter).notifyItemMoved(1, 3)
    }


}