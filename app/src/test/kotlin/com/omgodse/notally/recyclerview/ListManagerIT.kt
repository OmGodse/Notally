package com.omgodse.notally.recyclerview

import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.changehistory.ChangeHistory
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.viewholder.MakeListVH
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.test.createListItem
import com.omgodse.notally.test.mockAndroidLog
import com.omgodse.notally.test.mockDrag
import com.omgodse.notally.test.mockPreferences
import com.omgodse.notally.test.printList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ListManagerIT {

    @Before
    fun setup() {
        printList(items)
    }

    @Test
    fun `test 01 changeIsChild`() {
        mockPreferences(preferences)

        listManager.changeIsChild(1, true)
        listManager.changeIsChild(2, true)

        assertTrue(itemB.isChild)
        assertTrue(itemC.isChild)
        assertEquals(listOf(itemB, itemC), itemA.children)
    }

    @Test
    fun `test 02 move parent with children`() {
        listManager.move(0, 3)

        assertEquals(itemA, items[1])
        assertEquals(itemB, items[2])
        assertEquals(itemC, items[3])
        assertEquals(listOf(itemB, itemC), itemA.children)
    }

    @Test
    fun `test 03 undo move parent with children`() {
        changeHistory.undo()

        assertEquals(itemA, items[0])
        assertEquals(itemB, items[1])
        assertEquals(itemC, items[2])
        assertFalse(items[0].isChild)
        assertEquals(listOf(itemB, itemC), itemA.children)
    }


    @Test
    fun `test 04 delete parent with children`() {
        listManager.delete(0, true)

        assertEquals(3, items.size)
        assertEquals(itemD, items[0])
    }

    @Test
    fun `test 05 undo delete parent with children`() {
        changeHistory.undo()

        assertEquals(6, items.size)
        assertEquals(itemA, items[0])
        assertEquals(itemB, items[1])
        assertEquals(itemC, items[2])
        assertEquals(itemD, items[3])
    }

    @Test
    fun `test 06 move parent with children and check`() {
        listManager.move(0, 4)
        listManager.changeChecked(2, true)

        assertEquals(itemF.body, items[2].body)
        assertEquals(2, items[2].uncheckedPosition)

        assertEquals(itemA.body, items[3].body)
        assertTrue(items[3].checked)
        assertEquals(2, items[3].uncheckedPosition)

        assertEquals(itemB.body, items[4].body)
        assertTrue(items[4].checked)
        assertEquals(3, items[4].uncheckedPosition)

        assertEquals(itemC.body, items[5].body)
        assertTrue(items[5].checked)
        assertEquals(4, items[5].uncheckedPosition)
    }

    @Test
    fun `test 07 undo check parent with children`() {
        changeHistory.undo()

        assertEquals(itemF.body, items[5].body)
        assertEquals(5, items[5].uncheckedPosition)

        assertEquals(itemA.body, items[2].body)
        assertFalse(items[2].checked)
        assertEquals(2, items[2].uncheckedPosition)

        assertEquals(itemB.body, items[3].body)
        assertFalse(items[3].checked)
        assertEquals(3, items[3].uncheckedPosition)

        assertEquals(itemC.body, items[4].body)
        assertFalse(items[4].checked)
        assertEquals(4, items[4].uncheckedPosition)
    }

    @Test
    fun `test 08 drag child item into other parent's children`() {
        listManager.changeIsChild(1, true)

        mockDrag(1, 4, listManager)
        println("After drag:")
        printList(items)

        assertEquals(itemD.body, items[0].body)
        assertEquals(0, items[0].children.size)
        assertEquals(3, items[1].children.size)
        assertEquals(itemB.body, items[1].children[0].body)
        assertEquals(itemC.body, items[1].children[1].body)
        assertEquals(itemE.body, items[1].children[2].body)
    }
    
    @Test
    fun `test 09 undo drag child item into other parent's children`() {
        changeHistory.undo()
        println("After undo:")
        printList(items)

        assertEquals(itemA.body, items[2].body)
        assertEquals(2, items[2].children.size)
    }    
    
    @Test
    fun `test 10 undo changeIsChild`() {
        changeHistory.undo()

        assertFalse(items[1].isChild)
        assertEquals(0, items[0].children.size)
    }

    @Test
    fun `test 11 redo changeIsChild`() {
        changeHistory.redo()

        assertTrue(items[1].isChild)
        assertEquals(1, items[0].children.size)
        assertEquals(itemE.body, items[0].children[0].body)
    }

    @Test
    fun `test 12 undo drag item to top`() {
        val item = items[items.lastIndex]
        mockDrag(items.lastIndex, 0, listManager)
        println("After drag")
        printList(items)
        changeHistory.undo()
        println("After undo")
        printList(items)

        assertEquals(item.body, items[items.lastIndex].body)
    }

    @Test
    fun `test 13 undo drag item to bottom`() {
        val item = items[1]
        mockDrag(1, items.lastIndex, listManager)
        println("After drag")
        printList(items)
        changeHistory.undo()
        println("After undo")
        printList(items)

        assertEquals(item.body, items[1].body)
        assertEquals(1, items[0].children.size)
        assertEquals(0, items[items.lastIndex].children.size)
    }


    companion object {
        private lateinit var recyclerView: RecyclerView
        private lateinit var adapter: RecyclerView.Adapter<*>
        private lateinit var inputMethodManager: InputMethodManager
        private lateinit var changeHistory: ChangeHistory
        private lateinit var makeListVH: MakeListVH
        private lateinit var preferences: Preferences

        private lateinit var items: MutableList<ListItem>

        private lateinit var listManager: ListManager

        var itemA = createListItem("A")
        var itemB = createListItem("B")
        var itemC = createListItem("C")
        var itemD = createListItem("D")
        var itemE = createListItem("E")
        var itemF = createListItem("F")

        @JvmStatic
        @BeforeClass
        fun setUp(): Unit {
            recyclerView = mock(RecyclerView::class.java)
            adapter = mock(RecyclerView.Adapter::class.java)
            inputMethodManager = mock(InputMethodManager::class.java)
            mockAndroidLog()
            changeHistory = ChangeHistory() {}
            makeListVH = mock(MakeListVH::class.java)
            preferences = mock(Preferences::class.java)
            items = mutableListOf()
            listManager =
                ListManager(items, recyclerView, changeHistory, preferences, inputMethodManager)
            listManager.adapter = adapter as RecyclerView.Adapter<MakeListVH>
            // Prepare view holder
            `when`(recyclerView.findViewHolderForAdapterPosition(anyInt())).thenReturn(makeListVH)

            items.addAll(listOf(itemA, itemB, itemC, itemD, itemE, itemF))
        }

    }
}