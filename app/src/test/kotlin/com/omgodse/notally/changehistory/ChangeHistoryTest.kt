package com.omgodse.notally.changehistory

import com.omgodse.notally.test.mockAndroidLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class ChangeHistoryTest {
    private lateinit var changeHistory: ChangeHistory
    private lateinit var onStackChanged: (Int) -> Unit

    @Before
    fun setUp() {
        mockAndroidLog()
        onStackChanged = mock() // Mock the onStackChanged callback
        changeHistory = ChangeHistory(onStackChanged) // Inject the mock into the class
    }

    @Test
    fun `test push adds change to stack and updates stackPointer`() {
        val change = mock<Change>() // Mock a Change object

        changeHistory.push(change)

        // Verify stackPointer is updated (since there's one item, it should be 0)
        verify(onStackChanged).invoke(0)
    }

    @Test
    fun `test undo when stack has one change`() {
        val change = mock<Change>()

        changeHistory.push(change)
        changeHistory.undo()

        // Verify undo() is called on the change
        verify(change).undo()

        // Verify stackPointer is decremented to -1
        verify(onStackChanged).invoke(-1)
    }

    @Test
    fun `test redo when stack has one change`() {
        val change = mock<Change>()

        changeHistory.push(change)
        changeHistory.undo()
        changeHistory.redo()

        // Verify redo() is called on the change
        verify(change).redo()

        // Verify stackPointer is incremented back to 0
        verify(onStackChanged, times(2)).invoke(0) // Called once during push and once during redo
    }

    @Test
    fun `test canUndo and canRedo logic`() {
        val change = mock<Change>()

        assertFalse(changeHistory.canUndo())
        assertFalse(changeHistory.canRedo())

        changeHistory.push(change)

        assertTrue(changeHistory.canUndo())
        assertFalse(changeHistory.canRedo())

        changeHistory.undo()

        assertFalse(changeHistory.canUndo())
        assertTrue(changeHistory.canRedo())
    }

    @Test
    fun `test invalidateRedos`() {
        val change1 = TestChange()
        val change2 = TestChange()
        val change3 = TestChange()
        val change4 = TestChange()

        changeHistory.push(change1)
        changeHistory.push(change2)
        changeHistory.push(change3)
        changeHistory.undo()
        changeHistory.push(change4)

        assertEquals(change4, changeHistory.lookUp())
        assertEquals(change2, changeHistory.lookUp(1))
        assertEquals(change1, changeHistory.lookUp(2))
        assertThrows(IllegalArgumentException::class.java){
            changeHistory.lookUp(3)
        }
    }

    class TestChange : Change {
        override fun redo() {

        }

        override fun undo() {

        }

    }
}