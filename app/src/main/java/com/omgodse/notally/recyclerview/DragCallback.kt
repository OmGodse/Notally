package com.omgodse.notally.recyclerview

import android.graphics.Canvas
import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.omgodse.notally.room.ListItem

/** ItemTouchHelper.Callback that allows dragging ListItem with its children. */
class DragCallback(private val elevation: Float, private val listManager: ListManager) :
    ItemTouchHelper.Callback() {

    private var lastState = ItemTouchHelper.ACTION_STATE_IDLE
    private var lastIsCurrentlyActive = false
    private var childViewHolders: List<ViewHolder> = mutableListOf()

    private var draggedItem: ListItem? = null
    private var positionFrom: Int? = null
    private var positionTo: Int? = null
    private var newPosition: Int? = null

    override fun isLongPressDragEnabled() = false

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {}

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        val drag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(drag, 0)
    }

    override fun onMove(view: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition
        if (positionFrom == null) {
            draggedItem = listManager.getItem(from).clone() as ListItem
        }
        val swapped = listManager.move(from, to, false, false)
        if (swapped != null) {
            if (positionFrom == null) {
                positionFrom = from
            }
            positionTo = to
            newPosition = swapped
        }
        return swapped != null
    }

    override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
        if (
            lastState != actionState &&
                actionState == ItemTouchHelper.ACTION_STATE_IDLE &&
                positionTo != -1
        ) {
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
        isCurrentlyActive: Boolean,
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

    override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        viewHolder.itemView.translationX = 0f
        viewHolder.itemView.translationY = 0f
        viewHolder.itemView.elevation = 0f
        childViewHolders.forEach { animateFadeIn(it) }
    }

    private fun onDragStart(viewHolder: ViewHolder, recyclerView: RecyclerView) {
        Log.d(TAG, "onDragStart")
        positionFrom = null
        positionTo = null
        newPosition = null
        draggedItem = null

        val item = listManager.getItem(viewHolder.adapterPosition)
        if (!item.isChild) {
            childViewHolders =
                item.children.mapIndexedNotNull { index, listItem ->
                    recyclerView.findViewHolderForAdapterPosition(
                        viewHolder.adapterPosition + index + 1
                    )
                }
            childViewHolders.forEach { animateFadeOut(it) }
        }
    }

    private fun onDragEnd() {
        Log.d(TAG, "onDragEnd: from: $positionFrom to: $positionTo")
        if (positionFrom == positionTo) {
            return
        }
        if (newPosition != null && draggedItem != null) {
            // The items have already been moved accordingly via move() calls
            listManager.updateChildrenAndPushMoveChange(
                positionFrom!!,
                positionTo!!,
                newPosition!!,
                draggedItem!!,
                true,
                true,
            )
        }
    }

    private fun animateFadeOut(viewHolder: ViewHolder) {
        viewHolder.itemView.animate().translationY(-100f).alpha(0f).setDuration(300).start()
    }

    private fun animateFadeIn(viewHolder: ViewHolder) {
        viewHolder.itemView.animate().translationY(0f).alpha(1f).setDuration(300).start()
    }

    companion object {
        private const val TAG = "DragCallback"
    }
}
