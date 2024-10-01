package com.omgodse.notally.recyclerview

import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.changehistory.ChangeHistory
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.viewholder.MakeListVH
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.test.assertChildren
import com.omgodse.notally.test.createListItem
import com.omgodse.notally.test.mockAndroidLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.mockito.Mockito
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

open class ListManagerTestBase {

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var adapter: RecyclerView.Adapter<*>
    protected lateinit var inputMethodManager: InputMethodManager
    protected lateinit var changeHistory: ChangeHistory
    protected lateinit var makeListVH: MakeListVH
    protected lateinit var preferences: Preferences

    protected lateinit var items: MutableList<ListItem>

    protected lateinit var listManager: ListManager

    @Before
    fun setUp() {
        recyclerView = mock(RecyclerView::class.java)
        adapter = mock(RecyclerView.Adapter::class.java)
        inputMethodManager = mock(InputMethodManager::class.java)
        changeHistory = ChangeHistory() {}
        makeListVH = mock(MakeListVH::class.java)
        preferences = mock(Preferences::class.java)
        items =
            mutableListOf(
                createListItem("A", id = 0),
                createListItem("B", id = 1),
                createListItem("C", id = 2),
                createListItem("D", id = 3),
                createListItem("E", id = 4),
                createListItem("F", id = 5),
            )
        items.updateUncheckedPositions()
        listManager =
            ListManager(items, recyclerView, changeHistory, preferences, inputMethodManager)
        listManager.adapter = adapter as RecyclerView.Adapter<MakeListVH>
        // Prepare view holder
        `when`(recyclerView.findViewHolderForAdapterPosition(anyInt())).thenReturn(makeListVH)
        mockAndroidLog()
    }

    protected operator fun List<ListItem>.get(body: String): ListItem {
        // Find the item with the matching name and return it
        return this.find { it.body == body }!!
    }

    protected fun <E> MutableList<E>.assertSize(expected: Int) {
        assertEquals("size", expected, this.size)
    }

    protected fun String.assertChildren(vararg childrenBodies: String) {
        items.find { it.body == this }!!.assertChildren(*childrenBodies)
    }

    protected fun String.assertIsChecked() {
        assertTrue("checked", items.find { it.body == this }!!.checked)
    }

    protected fun String.assertIsNotChecked() {
        assertFalse("checked", items.find { it.body == this }!!.checked)
    }

    protected fun String.assertUncheckedPosition(expected: Int) {
        assertEquals(
            "uncheckedPosition",
            expected,
            items.find { it.body == this }!!.uncheckedPosition,
        )
    }

    protected fun String.assertPosition(expected: Int) {
        assertEquals("position in items", expected, items.indexOfFirst { it.body == this })
    }

    protected fun String.assertIsParent() {
        assertFalse(items.find { it.body == this }!!.isChild)
    }

    object MockitoHelper {
        fun <T> anyObject(): T {
            Mockito.any<T>()
            return uninitialized()
        }

        @Suppress("UNCHECKED_CAST") fun <T> uninitialized(): T = null as T
    }
}
