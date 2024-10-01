package com.omgodse.notally.recyclerview

import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.changehistory.ChangeHistory
import com.omgodse.notally.changehistory.ListAddChange
import com.omgodse.notally.changehistory.ListBooleanChange
import com.omgodse.notally.changehistory.ListDeleteChange
import com.omgodse.notally.changehistory.ListMoveChange
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.viewholder.MakeListVH
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.test.createListItem
import com.omgodse.notally.test.mockDrag
import com.omgodse.notally.test.mockPreferences
import com.omgodse.notally.test.printList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.kotlin.verify

class ListManagerTest {

    // Mocked dependencies
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerView.Adapter<*>
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var changeHistory: ChangeHistory
    private lateinit var makeListVH: MakeListVH
    private lateinit var preferences: Preferences

    private lateinit var items: MutableList<ListItem>

    private lateinit var listManager: ListManager

    @Before
    fun setUp() {
        recyclerView = mock(RecyclerView::class.java)
        adapter = mock(RecyclerView.Adapter::class.java)
        inputMethodManager = mock(InputMethodManager::class.java)
        changeHistory = mock(ChangeHistory::class.java)
        makeListVH = mock(MakeListVH::class.java)
        preferences = mock(Preferences::class.java)
        items = mutableListOf()
        listManager =
            ListManager(items, recyclerView, changeHistory, preferences, inputMethodManager)
        listManager.adapter = adapter as RecyclerView.Adapter<MakeListVH>
        // Prepare view holder
        `when`(recyclerView.findViewHolderForAdapterPosition(anyInt())).thenReturn(makeListVH)

    }

    //region add
    @Test
    fun `add item to empty list at default position`() {
        // Arrange
        val itemToAdd = ListItem("Test Item", false, false, null, mutableListOf())

        // Act
        listManager.add(item = itemToAdd)

        // Assert
        assertEquals(1, items.size)
        assertEquals(itemToAdd.body, items[0].body)
        verify(adapter).notifyItemInserted(0)
    }

    @Test
    fun `add item to non-empty list`() {
        items.add(ListItem("First Item", false, false, null, mutableListOf()))

        val itemToAdd = ListItem("Test Item", false, false, null, mutableListOf())

        // Act
        listManager.add(item = itemToAdd)

        // Assert
        assertEquals(2, items.size)
        assertEquals(itemToAdd.body, items[1].body)
        verify(adapter).notifyItemInserted(1)
    }

    @Test
    fun `add default item before child item`() {
        val childItem1 = ListItem("Child Item1", false, true, null, mutableListOf())
        val parentItem = ListItem("Parent Item", false, false, null, mutableListOf(childItem1))
        items.add(parentItem)
        items.add(childItem1)
        // Act
        listManager.add()

        // Assert
        assertEquals(3, items.size)
        val childItem2 = items[2]
        assertEquals("", childItem2.body)
        assertEquals(true, childItem2.isChild)
        assertEquals(1, parentItem.children.indexOf(childItem2))
        verify(adapter).notifyItemInserted(2)
    }

    @Test
    fun `add default item after child item`() {
        val childItem1 = ListItem("Child Item1", false, true, null, mutableListOf())
        val parentItem = ListItem("Parent Item", false, false, null, mutableListOf(childItem1))
        items.add(parentItem)
        items.add(childItem1)
        // Act
        listManager.add(1)

        // Assert
        assertEquals(3, items.size)
        val childItem2 = items[1]
        assertEquals("", childItem2.body)
        assertEquals(true, childItem2.isChild)
        assertEquals(2, parentItem.children.size)
        assertEquals(0, parentItem.children.indexOf(childItem2))
        verify(adapter).notifyItemInserted(1)
    }

    @Test
    fun `add checked item with correct uncheckedPosition`() {
        // Arrange
        val itemToAdd = ListItem("Test Item", true, false, null, mutableListOf())

        // Act
        listManager.add(item = itemToAdd)

        // Assert
        assertEquals(1, items.size)
        assertEquals(itemToAdd.body, items[0].body)
        assertEquals(itemToAdd.checked, true)
        assertEquals(itemToAdd.uncheckedPosition, 0)
        verify(adapter).notifyItemInserted(0)
    }

    @Test
    fun `add item with children`() {
        // Arrange
        val childItem1 = ListItem("Child Item1", false, true, null, mutableListOf())
        val childItem2 = ListItem("Child Item2", false, true, null, mutableListOf())
        val parentItem =
            ListItem("Parent Item", false, false, null, mutableListOf(childItem1, childItem2))

        // Act
        listManager.add(item = parentItem)

        // Assert
        assertEquals(3, items.size)
        assertEquals(parentItem.body, items[0].body)
        assertEquals(childItem1.body, items[1].body)
        assertEquals(childItem2.body, items[2].body)
        assertEquals(2, parentItem.children.size)
        assertEquals(0, parentItem.children.indexOf(childItem1))
        assertEquals(1, parentItem.children.indexOf(childItem2))
        verify(adapter).notifyItemInserted(0)
        verify(adapter).notifyItemInserted(1)
        verify(adapter).notifyItemInserted(2)
    }

    @Test
    fun `add item pushes change if pushChange is true`() {
        // Arrange
        val itemToAdd = ListItem("Test Item", false, false, null, mutableListOf())

        // Act
        listManager.add(item = itemToAdd, pushChange = true)

        // Assert
        verify(changeHistory).push(MockitoHelper.anyObject<ListAddChange>())
    }

    @Test
    fun `add item does not push change if pushChange is false`() {
        // Arrange
        val itemToAdd = ListItem("Test Item", false, false, null, mutableListOf())

        // Act
        listManager.add(item = itemToAdd, pushChange = false)

        // Assert
        verify(changeHistory, never()).push(MockitoHelper.anyObject<ListAddChange>())
    }
    //endregion

    //region delete
    @Test
    fun `delete first item from list unforced`() {
        // Arrange
        val itemToDelete = ListItem("Test Item", false, false, null, mutableListOf())
        items.add(itemToDelete)

        // Act
        val deletedItem = listManager.delete(position = 0, force = false)

        // Assert
        assertNull(deletedItem)
        assertEquals(1, items.size)
        verify(adapter, never()).notifyItemRangeRemoved(0, 1)
    }

    @Test
    fun `delete first item from list forced`() {
        // Arrange
        val itemToDelete = ListItem("Test Item", false, false, null, mutableListOf())
        items.add(itemToDelete)

        // Act
        val deletedItem = listManager.delete(position = 0, force = true)

        // Assert
        assertEquals(itemToDelete, deletedItem)
        assertTrue(items.isEmpty())
        verify(adapter).notifyItemRangeRemoved(0, 1)
    }

    @Test
    fun `delete item with children from list`() {
        // Arrange
        val childItem = ListItem("Child Item", false, true, null, mutableListOf())
        val parentItem = ListItem("Parent Item", false, false, null, mutableListOf(childItem))
        items.add(parentItem)
        items.add(childItem)

        // Act
        val deletedItem = listManager.delete(position = 0, force = true)

        // Assert
        assertEquals(parentItem, deletedItem)
        assertTrue(items.isEmpty())
        verify(adapter).notifyItemRangeRemoved(0, 2) // 1 parent + 1 child
    }

    @Test
    fun `delete item from empty list`() {
        // Arrange

        // Act
        val deletedItem = listManager.delete(position = 0, force = false)

        // Assert
        assertNull(deletedItem)
        verify(adapter, never()).notifyItemRangeRemoved(anyInt(), anyInt())
    }

    @Test
    fun `delete item with force option`() {
        // Arrange
        val itemToDelete = ListItem("Test Item", false, false, null, mutableListOf())
        items.add(itemToDelete)

        // Act
        val deletedItem = listManager.delete(position = 0, force = true)

        // Assert
        assertEquals(itemToDelete, deletedItem)
        assertTrue(items.isEmpty())
        verify(adapter).notifyItemRangeRemoved(0, 1)
        verify(changeHistory).push(MockitoHelper.anyObject<ListDeleteChange>())
    }

    @Test
    fun `delete at invalid position`() {
        // Arrange

        // Act
        val deletedItem = listManager.delete(10)

        // Assert
        assertNull(deletedItem)
        verify(adapter, never()).notifyItemRangeRemoved(anyInt(), anyInt())
    }
    //endregion

    //region swap
    @Test
    fun `swap valid items`() {
        // Arrange
        val itemA = createListItem("Item A", checked = false)
        val itemB = createListItem("Item B", checked = false)
        items.addAll(listOf(itemA, itemB))
        mockPreferences(preferences)

        // Act
        val result = listManager.swap(0, 1)

        // Assert
        assertTrue(result)
        assertEquals(itemB, items[0])
        assertEquals(itemA, items[1])
        verify(adapter).notifyItemMoved(0, 1)
    }


    @Test
    fun `swap invalid positions`() {
        // Act
        val result1 = listManager.swap(-1, 1)
        val result2 = listManager.swap(0, -1)
        val result3 = listManager.swap(0, 0)

        // Assert
        assertFalse(result1)
        assertFalse(result2)
        assertFalse(result3)
    }


    @Test
    fun `dont swap parent under own child item`() {
        val childItem = createListItem("Child Item", checked = false)
        // Arrange
        val parentItem = createListItem(
            "Parent Item",
            checked = false,
            isChild = false,
            children = mutableListOf(childItem)
        )
        items.addAll(listOf(parentItem, childItem))
        mockPreferences(preferences)

        // Act
        val result = listManager.swap(0, 1)

        // Assert
        assertFalse(result)
        assertEquals(parentItem, items[0])
        assertEquals(childItem, items[1])
        verify(adapter, never()).notifyItemMoved(0, 1)
    }

    @Test
    fun `swap child under own parent item`() {
        val childItem = createListItem("Child Item", checked = false)
        // Arrange
        val parentItem = createListItem(
            "Parent Item",
            checked = false,
            isChild = false,
            children = mutableListOf(childItem)
        )
        items.addAll(listOf(parentItem, childItem))
        mockPreferences(preferences)

        // Act
        val result = listManager.swap(1, 0)

        // Assert
        assertTrue(result)
        assertEquals(childItem, items[0])
        assertEquals(parentItem, items[1])
        verify(adapter).notifyItemMoved(1, 0)
    }

    @Test
    fun `dont swap under checked item with auto-sort enabled`() {
        // Arrange
        val checkedItem = createListItem("Checked Item", true)
        val uncheckedItem = createListItem("Unchecked Item", false)
        items.addAll(listOf(uncheckedItem, checkedItem))
        mockPreferences(preferences)

        // Act
        val result = listManager.swap(0, 1) // Attempt to swap checked with unchecked

        // Assert
        assertFalse(result)
    }

    @Test
    fun `swap under checked item with auto-sort disabled`() {
        // Arrange
        val checkedItem = createListItem("Checked Item", true)
        val uncheckedItem = createListItem("Unchecked Item")
        items.addAll(listOf(uncheckedItem, checkedItem))
        mockPreferences(preferences, ListItemSorting.noAutoSort)

        // Act
        val result = listManager.swap(0, 1) // Attempt to swap checked with unchecked

        // Assert
        assertTrue(result)
    }
    //endregion

    //region move
    @Test
    fun `dont move valid items by drag`() {
        // Arrange
        val itemA = createListItem("Item A")
        val itemB = createListItem("Item B")
        items.addAll(listOf(itemA, itemB))
        mockPreferences(preferences)

        // Act
        listManager.endDrag(positionFrom = 0, positionTo = 1, false)

        // Assert
        assertEquals(itemA, items[0])
        assertEquals(itemB, items[1])
        verify(adapter, never()).notifyItemChanged(0)
        verify(adapter, never()).notifyItemMoved(anyInt(), anyInt())
    }

    @Test
    fun `move child item over parent item`() {
        // Arrange
        items.add(createListItem("Some Item"))
        val childItem = createListItem("Child", isChild = true)
        val parentItem = createListItem("Parent", children = mutableListOf(childItem))
        items.addAll(listOf(parentItem, childItem))
        mockPreferences(preferences)

        // Act
        listManager.move(positionFrom = 2, positionTo = 1)

        // Assert
        assertEquals(childItem, items[1])
        assertEquals(parentItem, items[2])
        assertTrue(childItem.isChild)
        verify(adapter).notifyItemMoved(2, 1)
        verify(adapter, never()).notifyItemChanged(anyInt())
    }

    @Test
    fun `move child item up parent item to position 0`() {
        // Arrange
        val childItem = createListItem("Child", isChild = true)
        val parentItem = createListItem("Parent", children = mutableListOf(childItem))
        items.addAll(listOf(parentItem, childItem))
        mockPreferences(preferences)

        // Act
        listManager.move(positionFrom = 1, positionTo = 0)

        // Assert
        assertEquals(childItem, items[0])
        assertEquals(parentItem, items[1])
        assertFalse(childItem.isChild)
        verify(adapter).notifyItemMoved(1, 0)
        verify(adapter).notifyItemChanged(0)
    }

    @Test
    fun `move parent item up child item`() {
        // Arrange
        val childItem1 = createListItem("Child Item1", isChild = true)
        val parentItem1 = createListItem("Parent Item1", children = mutableListOf(childItem1))
        val parentItem2 = createListItem("Parent Item2", children = mutableListOf())
        items.addAll(listOf(parentItem1, childItem1, parentItem2))
        mockPreferences(preferences)

        // Act
        listManager.move(positionFrom = 2, positionTo = 1)

        // Assert
        assertEquals(parentItem1, items[0])
        assertEquals(parentItem2, items[1])
        assertEquals(childItem1, items[2])
        assertTrue(parentItem2.isChild)
        verify(adapter).notifyItemMoved(2, 1)
        verify(adapter).notifyItemChanged(1)
    }

    @Test
    fun `move parent item down to child item`() {
        // Arrange
        val parentItem1 = createListItem("Parent Item2", children = mutableListOf())
        val childItem1 = createListItem("Child Item1", isChild = true)
        val parentItem2 = createListItem("Parent Item1", children = mutableListOf(childItem1))
        items.addAll(listOf(parentItem1, parentItem2, childItem1))
        mockPreferences(preferences)

        // Act
        listManager.move(positionFrom = 0, positionTo = 1)

        // Assert
        assertEquals(parentItem2, items[0])
        assertEquals(parentItem1, items[1])
        assertEquals(childItem1, items[2])
        assertTrue(parentItem1.isChild)
        verify(adapter).notifyItemMoved(0, 1)
        verify(adapter).notifyItemChanged(1)
    }

    @Test
    fun `move parent item down below child item`() {
        // Arrange
        val parentItem1 = createListItem("Parent Item2", children = mutableListOf())
        val childItem1 = createListItem("Child Item1", isChild = true)
        val parentItem2 = createListItem("Parent Item1", children = mutableListOf(childItem1))
        items.addAll(listOf(parentItem1, parentItem2, childItem1))
        mockPreferences(preferences)

        // Act
        listManager.move(positionFrom = 0, positionTo = 2)

        // Assert
        assertEquals(parentItem2, items[0])
        assertEquals(childItem1, items[1])
        assertEquals(parentItem1, items[2])
        assertFalse(parentItem1.isChild)
        verify(adapter).notifyItemMoved(0, 2)
        verify(adapter, never()).notifyItemChanged(anyInt())
    }

    @Test
    fun `dont move parent item to one of its children position`() {
        // Arrange
        val parentItem = createListItem("Parent")
        val childItem = createListItem("Child", isChild = true)
        parentItem.children.add(childItem)
        items.addAll(listOf(parentItem, childItem))
        mockPreferences(preferences)

        // Act
        listManager.move(positionFrom = 0, positionTo = 1)

        // Assert
        assertEquals(parentItem, items[0])
        assertEquals(childItem, items[1])
        assertTrue(childItem.isChild)
        verify(adapter, never()).notifyItemMoved(anyInt(), anyInt())
        verify(adapter, never()).notifyItemChanged(anyInt()) // childItem isChild update
    }

    @Test
    fun `dont move unchecked item under checked item if auto-sort is enabled`() {
        // Arrange
        val uncheckedItem = createListItem("Unchecked Item")
        val checkedItem = createListItem("Checked Item", checked = true)
        items.addAll(listOf(uncheckedItem, checkedItem))
        mockPreferences(preferences)

        // Act
        listManager.move(positionFrom = 0, positionTo = 1)

        // Assert
        assertEquals(uncheckedItem, items[0])
        assertEquals(checkedItem, items[1])
        verify(adapter, never()).notifyItemMoved(anyInt(), anyInt())
        verify(adapter, never()).notifyItemChanged(anyInt())
    }

    @Test
    fun `move with push change`() {
        // Arrange
        items.add(createListItem("Item A"))
        items.add(createListItem("Item B"))
        mockPreferences(preferences)

        // Act
        listManager.move(positionFrom = 0, positionTo = 1, pushChange = true)

        // Assert
        verify(changeHistory).push(MockitoHelper.anyObject<ListMoveChange>())
    }

    @Test
    fun `move child to parent without children`() {
        // Arrange
        val parentItem1 = createListItem("Parent1")
        val childItem1 = createListItem("Child 1", isChild = true)
        val childItem2 = createListItem("Child 2", isChild = true)
        val parentItem2 =
            createListItem("Parent2", children = mutableListOf(childItem1, childItem2))
        items.addAll(listOf(parentItem1, parentItem2, childItem1, childItem2))
        mockPreferences(preferences)

        // Act
        mockDrag(3, 1, listManager)

        // Assert
        assertTrue(items[1].isChild)
        assertEquals(listOf(childItem2), parentItem1.children)
        assertEquals(1, parentItem1.children.size)
        assertEquals(1, parentItem2.children.size)
        verify(adapter).notifyItemMoved(3, 2)
        verify(adapter).notifyItemMoved(2, 1)
    }

    @Test
    fun `complex move parent with children under parent with children without drag`() {
        var itemA = createListItem("A")
        var itemC = createListItem("C", isChild = true)
        var itemD = createListItem("D", isChild = true)
        var itemB = createListItem("B", children = mutableListOf(itemC, itemD))
        var itemE = createListItem("E")
        var itemG = createListItem("G", isChild = true)
        var itemH = createListItem("H", isChild = true)
        var itemF = createListItem("F", children = mutableListOf(itemG, itemH))
        var itemI = createListItem("I")
        var itemJ = createListItem("J")
        var itemK = createListItem("K")
        items.addAll(
            listOf(
                itemA,
                itemB,
                itemC,
                itemD,
                itemE,
                itemF,
                itemG,
                itemH,
                itemI,
                itemJ,
                itemK
            )
        )
        mockPreferences(preferences)

        // TODO test with drag, swaps beforehand
        listManager.move(1, 9, false)

        assertEquals(itemJ.body, items[6].body)
        assertEquals(itemB.body, items[7].body)
        assertEquals(itemC.body, items[8].body)
        assertEquals(itemD.body, items[9].body)
        assertEquals(itemK.body, items[10].body)
    }

    @Test
    fun `complex move parent with children under parent with children with drag`() {
        var itemA = createListItem("A")
        var itemC = createListItem("C", isChild = true)
        var itemD = createListItem("D", isChild = true)
        var itemB = createListItem("B", children = mutableListOf(itemC, itemD))
        var itemE = createListItem("E")
        var itemG = createListItem("G", isChild = true)
        var itemH = createListItem("H", isChild = true)
        var itemF = createListItem("F", children = mutableListOf(itemG, itemH))
        var itemI = createListItem("I")
        var itemJ = createListItem("J")
        var itemK = createListItem("K")
        items.addAll(
            listOf(
                itemA,
                itemB,
                itemC,
                itemD,
                itemE,
                itemF,
                itemG,
                itemH,
                itemI,
                itemJ,
                itemK
            )
        )
        mockPreferences(preferences)

        println("Before drag")
        printList(items)
        mockDrag(1, 9, listManager)
        println("After drag 1 to 9")
        printList(items)

        assertEquals(itemJ.body, items[6].body)
        assertEquals(itemB.body, items[7].body)
        assertEquals(7, items[7].uncheckedPosition)
        assertEquals(itemC.body, items[8].body)
        assertEquals(itemD.body, items[9].body)
        assertEquals(itemK.body, items[10].body)
    }

    //endregion

    //region endDrag

    @Test
    fun `drag item to top`() {
        // Arrange
        val itemB = createListItem("Item B")
        items.add(createListItem("Item A"))
        items.add(itemB)
        mockPreferences(preferences)

        // Act
        mockDrag(1, 0, listManager)

        // Assert
        assertEquals(itemB, items[0])
    }

    @Test
    fun `drag item to bottom`() {
        // Arrange
        val itemA = createListItem("Item A")
        items.add(itemA)
        items.add(createListItem("Item B"))
        mockPreferences(preferences)

        // Act
        mockDrag(0, 1, listManager)

        // Assert
        assertEquals(itemA, items[1])
    }

    @Test
    fun `drag last item to top`() {
        // Arrange
        val itemA = createListItem("Item A")
        val itemB = createListItem("Item B")
        val itemC = createListItem("Item C")
        val itemD = createListItem("Item D")
        items.addAll(listOf(itemA, itemB, itemC, itemD))
        mockPreferences(preferences)
        println("Before drag")
        printList(items)

        // Act
        mockDrag(items.lastIndex, 0, listManager)
        println("After drag")
        printList(items)

        // Assert
        assertEquals(itemD, items[0])
        assertEquals(itemA, items[1])
        assertEquals(itemB, items[2])
        assertEquals(itemC, items[3])
        verify(adapter).notifyItemMoved(3, 2)
        verify(adapter).notifyItemMoved(2, 1)
        verify(adapter).notifyItemMoved(1, 0)
    }

    @Test
    fun `drag top item to bottom`() {
        // Arrange
        val itemA = createListItem("Item A")
        val itemB = createListItem("Item B")
        val itemC = createListItem("Item C")
        val itemD = createListItem("Item D")
        items.addAll(listOf(itemA, itemB, itemC, itemD))
        mockPreferences(preferences)
        println("Before drag")
        printList(items)

        // Act
        mockDrag(0, items.lastIndex, listManager)
        println("After drag")
        printList(items)

        // Assert
        assertEquals(itemB, items[0])
        assertEquals(itemC, items[1])
        assertEquals(itemD, items[2])
        assertEquals(itemA, items[3])
        verify(adapter).notifyItemMoved(0, 1)
        verify(adapter).notifyItemMoved(1, 2)
        verify(adapter).notifyItemMoved(2, 3)
    }

    //endregion

    //region revertMove
    @Test
    fun `revert move with no children and isChildBefore is true`() {
        // Arrange
        val itemA = createListItem("A")
        val itemB = createListItem("B", isChild = true)
        val itemC = createListItem("C", children = mutableListOf(itemB))
        items.add(itemA)
        items.add(itemC)
        items.add(itemB)

        println("Before revert move")
        printList(items)

        // Act
        listManager.revertMove(positionFrom = 1, positionTo = 2, isChildBefore = true, hadChildren = false)

        println("After revert move")
        printList(items)
        // Assert
        assertEquals(itemB, items[1])
        assertTrue(items[1].isChild)
        assertEquals(itemC, items[2])
        assertFalse(items[2].isChild)
        verify(adapter).notifyItemMoved(2, 1)
        verify(adapter, never()).notifyItemChanged(anyInt())
    }

    @Test
    fun `revert move with children and isChildBefore is null`() {
        // Arrange
        val childItem = createListItem("B", isChild = true)
        val parentItem1 = createListItem("A", children = mutableListOf(childItem))
        val parentItem2 = createListItem("C", children = mutableListOf())
        items.addAll(listOf(parentItem2, parentItem1, childItem))

        // Act
        listManager.revertMove(positionFrom = 2, positionTo = 0, isChildBefore = null, hadChildren = true)

        // Assert
        assertEquals(parentItem1, items[0]) // Child remains child
        assertEquals(childItem, items[1]) // Child remains child
        assertEquals(parentItem2, items[2]) // Child remains child
        assertTrue(childItem.isChild) // Child remains child
        verify(adapter).notifyItemMoved(0, 2) // Parent update
    }

    @Test
    fun `revert move with children and isChildBefore is false`() {
        // Arrange
        val childItem = createListItem("B", isChild = true)
        val parentItem1 = createListItem("A", children = mutableListOf(childItem))
        val parentItem2 = createListItem("C", children = mutableListOf())
        items.addAll(listOf(createListItem("D"), parentItem2, parentItem1, childItem))

        println("before revertMove")
        printList(items)

        // Act
        listManager.revertMove(positionFrom = 1, positionTo = 3, isChildBefore = false, hadChildren = true)

        println("after revertMove")
        printList(items)
        // Assert
        assertEquals(parentItem1, items[1]) // Child remains child
        assertEquals(childItem, items[2]) // Child remains child
        assertEquals(parentItem2, items[3]) // Child remains child
        assertTrue(childItem.isChild) // Child remains child
        assertFalse(parentItem1.isChild) // Child remains child
        verify(adapter).notifyItemMoved(2, 1) // Parent update
        verify(adapter).notifyItemMoved(3, 2) // Parent update
        verify(adapter, never()).notifyItemChanged(anyInt()) // Parent update
    }

    @Test
    fun `revert move with isChildBefore is different than before`() {
        // Arrange
        val itemB = createListItem("B", isChild = true)
        val itemC = createListItem("C")
        val itemA = createListItem("A", children = mutableListOf(itemB))
        val itemD = createListItem("D")
        items.addAll(listOf(itemA, itemB, itemC, itemD))
        mockPreferences(preferences)
        println("before move")
        printList(items)

        listManager.move(2, 1, false)
        println("after move")
        printList(items)

        // Act
        listManager.revertMove(positionFrom = 2, positionTo = 1, isChildBefore = false, hadChildren = false)
        println("after revert move")
        printList(items)

        // Assert
        assertEquals(itemA, items[0]) // Child remains child
        assertEquals(itemB, items[1]) // Child remains child
        assertEquals(itemC, items[2]) // Child remains child
        assertEquals(itemD, items[3]) // Child remains child
        assertFalse(itemC.isChild) // Child remains child
        verify(adapter).notifyItemMoved(1, 2) // Parent update
        verify(adapter).notifyItemChanged(2) // Parent update
    }

    @Test
    fun `revert move with children updates uncheckedposition`() {
        // Arrange
        val childItem = createListItem("B", isChild = true)
        val childItem2 =
            createListItem("A", isChild = true, children = mutableListOf(), checked = true)
        val parentItem2 = createListItem("C", children = mutableListOf())
        items.addAll(listOf(createListItem("D"), parentItem2, childItem, childItem2))

        // Act
        listManager.revertMove(positionFrom = 1, positionTo = 3, isChildBefore = true, hadChildren = false)

        // Assert
        assertEquals(0, items[0].uncheckedPosition) // Child remains child
        assertNull(items[1].uncheckedPosition) // Child remains child
        assertEquals(2, items[2].uncheckedPosition) // Child remains child
        assertEquals(3, items[3].uncheckedPosition) // Child remains child
    }

    @Test
    fun `revert move last to top`() {
        // Arrange
        val itemB = createListItem("B")
        val itemC = createListItem("C")
        val itemD = createListItem("D")
        val itemA = createListItem("A")
        items.addAll(listOf(itemB, itemC, itemD, itemA))
        println("Before revert move")
        printList(items)

        // Act
        listManager.revertMove(positionFrom = 0, positionTo = 3, isChildBefore = false, hadChildren = false)
        println("After revert move")
        printList(items)

        // Assert
        assertEquals(itemA, items[0]) // Child remains child
        assertEquals(itemB, items[1]) // Child remains child
        assertEquals(itemC, items[2]) // Child remains child
        assertEquals(itemD, items[3]) // Child remains child
        verify(adapter).notifyItemMoved(3, 0) // Parent update
        verify(adapter, never()).notifyItemChanged(anyInt()) // Parent update
    }

    @Test
    fun `revert move top to last`() {
        // Arrange
        val itemD = createListItem("D")
        val itemA = createListItem("A")
        val itemB = createListItem("B")
        val itemC = createListItem("C")
        items.addAll(listOf(itemD, itemA, itemB, itemC))
        println("Before revert move")
        printList(items)

        // Act
        listManager.revertMove(positionFrom = 3, positionTo = 0, isChildBefore = false, hadChildren = false)
        println("After revert move")
        printList(items)

        // Assert
        assertEquals(itemA, items[0]) // Child remains child
        assertEquals(itemB, items[1]) // Child remains child
        assertEquals(itemC, items[2]) // Child remains child
        assertEquals(itemD, items[3]) // Child remains child
        verify(adapter).notifyItemMoved(0, 3) // Parent update
        verify(adapter, never()).notifyItemChanged(anyInt()) // Parent update
    }

    //endregion

    //region changeChecked

    @Test
    fun `changeChecked marks item as checked and pushes change`() {
        // Arrange
        val item = createListItem("Item", checked = false)
        items.add(item)
        mockPreferences(preferences)

        // Act
        val positionAfter = listManager.changeChecked(0, checked = true, pushChange = true)

        // Assert
        assertTrue(items[0].checked)
        verify(adapter).notifyItemRangeChanged(0, 1,null)
        verify(changeHistory).push(MockitoHelper.anyObject<ListBooleanChange>())
        assertEquals(0, positionAfter)
    }

    @Test
    fun `changeChecked marks item as unchecked and updates uncheckedPosition`() {
        // Arrange
        val item = createListItem("Item", checked = true)
        items.add(item)
        mockPreferences(preferences)

        // Act
        val positionAfter = listManager.changeChecked(0, checked = false, pushChange = true)

        // Assert
        assertFalse(items[0].checked)
        assertEquals(0, items[0].uncheckedPosition)
        verify(adapter).notifyItemRangeChanged(0, 1,null)
        verify(changeHistory).push(MockitoHelper.anyObject<ListBooleanChange>())
        assertEquals(0, positionAfter)
    }

    @Test
    fun `changeChecked does nothing when state is unchanged`() {
        // Arrange
        val item = createListItem("Item", checked = true)
        items.add(item)

        // Act
        val positionAfter = listManager.changeChecked(0, checked = true, pushChange = true)

        // Assert
        assertTrue(items[0].checked)
        verify(adapter, never()).notifyItemRangeRemoved(anyInt(), anyInt())
        verify(adapter, never()).notifyItemRangeInserted(anyInt(), anyInt())
        verify(changeHistory, never()).push(MockitoHelper.anyObject<ListBooleanChange>())
        assertEquals(0, positionAfter)
    }

    @Test
    fun `changeChecked on child item marks it as checked and notifies adapter`() {
        // Arrange
        val childItem = createListItem("Child", isChild = true)
        val parentItem = createListItem("Parent", children = mutableListOf(childItem))
        items.add(parentItem)
        items.add(childItem)
        mockPreferences(preferences)

        // Act
        val positionAfter = listManager.changeChecked(1, checked = true, pushChange = true)

        // Assert
        assertFalse(items[0].checked)
        assertTrue(items[1].checked)
        assertTrue(items[1].isChild)
        verify(adapter).notifyItemChanged(1)
        verify(changeHistory).push(MockitoHelper.anyObject<ListBooleanChange>())
        assertEquals(1, positionAfter)
    }

    @Test
    fun `changeChecked on parent item checks all children and updates positions`() {
        // Arrange
        val childItem1 = createListItem("Child 1", checked = false, isChild = true)
        val childItem2 = createListItem("Child 2", checked = false, isChild = true)
        val parentItem = createListItem("Parent", checked = false, isChild = false)
        parentItem.children.addAll(listOf(childItem1, childItem2))
        items.addAll(listOf(parentItem, childItem1, childItem2))
        mockPreferences(preferences)

        // Act
        val positionAfter = listManager.changeChecked(0, checked = true, pushChange = true)

        // Assert
        assertTrue(items[0].checked) // Parent checked
        assertTrue(items[1].checked) // Child 1 checked
        assertTrue(items[2].checked) // Child 2 checked

        verify(adapter).notifyItemRangeChanged(0, 3,null)
        verify(changeHistory).push(MockitoHelper.anyObject<ListBooleanChange>())
        assertEquals(0, positionAfter)
    }

    @Test
    fun `changeChecked reorders items when sorting is enabled`() {
        // Arrange
        val childA = createListItem("Child of A", checked = false, isChild = true)
        val itemA = createListItem("Item A", checked = false, children = mutableListOf(childA))
        val itemB = createListItem("Item B", checked = false)
        items.addAll(listOf(itemA, childA, itemB))
        mockPreferences(preferences)

        // Act
        val positionAfter = listManager.changeChecked(0, checked = true, pushChange = true)

        // Assert
        assertEquals(itemA.body, items[1].body)
        assertEquals(childA.body, items[2].body)
        assertTrue(items[1].checked)
        assertTrue(items[2].checked)
        verify(adapter).notifyItemRangeChanged(0, 3,null)
        verify(changeHistory).push(MockitoHelper.anyObject<ListBooleanChange>())
        assertEquals(1, positionAfter)
    }

    //endregion

    //region changeIsChild

    @Test
    fun `changeIsChild changes parent to child and pushes change`() {
        // Arrange
        val parentItem = createListItem("Parent")
        val item = createListItem("Item", isChild = false)
        items.addAll(listOf(parentItem, item))

        // Act
        listManager.changeIsChild(1, isChild = true)

        // Assert
        assertTrue(items[1].isChild)
        assertTrue(parentItem.children.contains(item))
        verify(adapter).notifyItemChanged(1)
        verify(changeHistory).push(MockitoHelper.anyObject<ListBooleanChange>())
    }

    @Test
    fun `changeIsChild changes child to parent`() {
        // Arrange
        val parentItem = createListItem("Parent")
        val childItem = createListItem("Child", isChild = true)
        items.addAll(listOf(parentItem, childItem))

        // Act
        listManager.changeIsChild(1, isChild = false)

        // Assert
        assertFalse(items[1].isChild)
        assertEquals(0, parentItem.children.size)
        verify(adapter).notifyItemChanged(1)
    }

    @Test
    fun `changeIsChild adds all child items when item becomes a parent`() {
        // Arrange
        val parentItem = createListItem("Parent")
        val childItem1 = createListItem("Child 1", isChild = true)
        val childItem2 = createListItem("Child 2", isChild = true)
        items.addAll(listOf(parentItem, childItem1, childItem2))

        // Act
        listManager.changeIsChild(1, isChild = false)

        // Assert
        assertFalse(items[1].isChild)
        assertEquals(listOf(childItem2), childItem1.children)
        assertEquals(0, parentItem.children.size)
        verify(adapter).notifyItemChanged(1)
    }

    //endregion

    object MockitoHelper {
        fun <T> anyObject(): T {
            Mockito.any<T>()
            return uninitialized()
        }

        @Suppress("UNCHECKED_CAST")
        fun <T> uninitialized(): T = null as T
    }
}
