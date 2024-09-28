package com.omgodse.notally.recyclerview

import android.graphics.Canvas
import android.util.Log
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * ItemTouchHelper.Callback that allows dragging ListItem with its children.
 */
class DragCallback(private val elevation: Float, private val listManager: ListManager) :
    ItemTouchHelper.Callback() {

    private var lastState = ItemTouchHelper.ACTION_STATE_IDLE
    private var lastIsCurrentlyActive = false
    private var childViewHolders: List<ViewHolder> = mutableListOf()

    private var positionFrom: Int = -1
    private var positionTo: Int = -1

    override fun isLongPressDragEnabled() = false

    override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
    }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        val drag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(drag, 0)
    }

    override fun onMove(view: RecyclerView, viewHolder: ViewHolder, target: ViewHolder): Boolean {
        val from = viewHolder.adapterPosition
        val to = target.adapterPosition
        val swapped = listManager.swap(from, to)
        if (swapped) {
            if (positionFrom == -1) {
                positionFrom = from
            }
            positionTo = to
        }
        return swapped
    }

    override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
        if (lastState != actionState && actionState == ItemTouchHelper.ACTION_STATE_IDLE && positionTo != -1) {
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

    override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
        viewHolder.itemView.translationX = 0f
        viewHolder.itemView.translationY = 0f
        viewHolder.itemView.elevation = 0f
        childViewHolders.forEach { animateFadeIn(it) }
    }

    private fun onDragStart(viewHolder: ViewHolder, recyclerView: RecyclerView) {
        Log.d(TAG, "onDragStart")
        positionFrom = -1
        positionTo = -1

        val item = listManager.getItem(viewHolder.adapterPosition)
        if (!item.isChild) {
            childViewHolders = item.children.mapIndexedNotNull { index, listItem ->
                recyclerView.findViewHolderForAdapterPosition(viewHolder.adapterPosition + index + 1)
            }
            childViewHolders.forEach { animateFadeOut(it) }
        }

    }


    private fun onDragEnd() {
        Log.d(TAG, "onDragEnd: from: $positionFrom to: $positionTo")
        if (positionFrom == positionTo) {
            return
        }
        listManager.move(positionFrom, positionTo, true)
    }

    private fun animateFadeOut(viewHolder: ViewHolder) {
        viewHolder.itemView.animate()
            .translationY(-100f)
            .alpha(0f)
            .setDuration(300)
            .start()
    }

    private fun animateFadeIn(viewHolder: ViewHolder) {
        viewHolder.itemView.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    companion object {
        private const val TAG = "DragCallback"
    }
}