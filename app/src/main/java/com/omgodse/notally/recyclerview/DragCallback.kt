package com.omgodse.notally.recyclerview

import android.graphics.Canvas
import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.omgodse.notally.miscellaneous.Change
import com.omgodse.notally.miscellaneous.ChangeHistory
import com.omgodse.notally.recyclerview.adapter.MakeListAdapter

class DragCallback(
    private val elevation: Float,
    private val adapter: MakeListAdapter,
    private val listManager: ListManager,
    private val changeHistory: ChangeHistory
) : ItemTouchHelper.Callback() {

    private var lastState = ItemTouchHelper.ACTION_STATE_IDLE
    private var lastIsCurrentlyActive = false
    private var childViewHolders: List<ViewHolder> = mutableListOf()

    private var fromPosition: Int = -1
    private var toPosition: Int = -1

    override fun isLongPressDragEnabled() = false

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}


    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        val drag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(drag, 0)
    }

    override fun onMove(view: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition
        if (fromPosition == -1) {
            fromPosition = from
        }
        toPosition = to
        return listManager.swap(from, to)
    }

    override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
        if (lastState != actionState && actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            onDragEnd()
        }
        lastState = actionState
        super.onSelectedChanged(viewHolder, actionState)
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
        if (lastIsCurrentlyActive != isCurrentlyActive && isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                onDragStart(viewHolder, recyclerView)
            }
        }
        lastIsCurrentlyActive = isCurrentlyActive
    }

    private fun clearItemView(viewHolder: ViewHolder) {
        viewHolder.itemView.animate()
            .translationY(-100f)
            .alpha(0f)
            .setDuration(300)
            .start()
    }

    private fun showItemView(viewHolder: ViewHolder) {
        viewHolder.itemView.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun onDragStart(viewHolder: ViewHolder, recyclerView: RecyclerView) {
        Log.d(TAG, "onDragStart")
        fromPosition = -1
        toPosition = -1
        val item = adapter.list[viewHolder.adapterPosition]
        if (!item.isChild) {
            childViewHolders = item.children.mapIndexedNotNull { index, listItem ->
                recyclerView.findViewHolderForAdapterPosition(viewHolder.adapterPosition + index + 1)
            }
            childViewHolders.forEach { clearItemView(it) }
        }

    }

    private fun onDragEnd() {
        Log.d(TAG, "onDragEnd: from: $fromPosition to: $toPosition")
        childViewHolders.forEach { showItemView(it) }
        if (fromPosition == toPosition) {
            return
        }
        val isChildBefore = listManager.move(fromPosition, toPosition, true)
        changeHistory.addChange(object : Change {
            override fun redo() {
                listManager.move(fromPosition, toPosition, false)
            }

            override fun undo() {
                listManager.revertMove(fromPosition, toPosition, isChildBefore)
            }

            override fun toString(): String {
                return "MoveChange from: $fromPosition to: $toPosition isChildBefore: $isChildBefore"
            }
        })
    }


    override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        viewHolder.itemView.translationX = 0f
        viewHolder.itemView.translationY = 0f
        viewHolder.itemView.elevation = 0f
    }

    companion object {
        private const val TAG = "DragCallback"
    }
}