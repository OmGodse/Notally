package com.omgodse.notally.recyclerview

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SwipeSelectionListener(
    private val recyclerView: RecyclerView?,
    private val onItemIntercept: (Int) -> Unit
) : RecyclerView.OnItemTouchListener {

    private var isSwipeSelecting = false
    private var lastInterceptedPosition = -1

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isSwipeSelecting = false
                lastInterceptedPosition = -1
            }
        }
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isSwipeSelecting = false
                lastInterceptedPosition = -1
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isSwipeSelecting) {
                    val childView = findChildViewUnder(e.x, e.y)
                    if (childView != null) {
                        val position = recyclerView?.getChildAdapterPosition(childView) ?: -1
                        if (position != RecyclerView.NO_POSITION) {
                            val dx = abs(e.x - (childView.left + childView.width / 2f))
                            if (dx > childView.width * 0.3f) {
                                isSwipeSelecting = true
                                lastInterceptedPosition = position
                                onItemIntercept(position)
                            }
                        }
                    }
                } else {
                    val childView = findChildViewUnder(e.x, e.y)
                    if (childView != null) {
                        val position = recyclerView?.getChildAdapterPosition(childView) ?: -1
                        if (position != RecyclerView.NO_POSITION && position != lastInterceptedPosition) {
                            lastInterceptedPosition = position
                            onItemIntercept(position)
                        }
                    }
                }
            }
        }
        return false
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    private fun findChildViewUnder(x: Float, y: Float): View? {
        return recyclerView?.findChildViewUnder(x, y)
    }
}
