package com.omgodse.notally.recyclerview

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.omgodse.notally.miscellaneous.ChangeHistory
import com.omgodse.notally.miscellaneous.Change
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.recyclerview.adapter.MakeListAdapter
import java.util.Collections

class DragCallback(
    private val elevation: Float,
    private val adapter: MakeListAdapter,
    private val changeHistory: ChangeHistory,
    private val preferences: Preferences
) : ItemTouchHelper.Callback() {

    override fun isLongPressDragEnabled() = false

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}


    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        val drag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(drag, 0)
    }

    override fun onMove(view: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition
        // Disallow dragging unchecked item under any checked item
        if (adapter.list[to].checked && preferences.listItemSorting.value == ListItemSorting.autoSortByChecked) {
            return false
        }
        swapItem(from, to)
        changeHistory.addChange(object : Change {
            override fun redo() {
                swapItem(from, to)
            }

            override fun undo() {
                swapItem(to, from)
            }

        })
        // TODO: fix children beneath moved object
        return true
    }

    private fun swapItem(from: Int, to: Int) {
        if (!adapter.list[from].checked) {
            adapter.list[from].uncheckedPosition = to
        }
        Collections.swap(adapter.list, from, to)
        adapter.notifyItemMoved(from, to)
    }


    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        viewHolder.itemView.translationX = dX
        viewHolder.itemView.translationY = dY
        if (isCurrentlyActive) {
            viewHolder.itemView.elevation = elevation
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        viewHolder.itemView.translationX = 0f
        viewHolder.itemView.translationY = 0f
        viewHolder.itemView.elevation = 0f
    }
}