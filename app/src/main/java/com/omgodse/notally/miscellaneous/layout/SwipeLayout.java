/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Arman Sargsyan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.omgodse.notally.miscellaneous.layout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.customview.widget.ViewDragHelper;

/**
 * This project provides an opportunity to perform swipe for any layout,
 * in the direction specified by you.
 * <p>
 * Date: 2018-09-27
 * Repository #https://github.com/zerobranch/SwipeLayout
 *
 * @author Arman Sargsyan
 */
public class SwipeLayout extends FrameLayout {
    public static final int LEFT = 1;
    public static final int RIGHT = LEFT << 1;
    public static final int HORIZONTAL = LEFT | RIGHT;
    private static final int CLOSE_POSITION = 0;
    private static final int NO_POSITION = -1;
    private static final int DEFAULT_AUTO_OPEN_SPEED = 1000;

    /**
     * Current direction of a swipe
     */
    private int currentDirection;

    /**
     * The secondary view will move along with the main view
     */
    private boolean isTogether;

    /**
     * Is enabled Swipe
     */
    private boolean isEnabledSwipe;

    /**
     * Swipe to the end of the screen.
     * Can work without a secondary view {@link #staticLeftView} and {@link #staticRightView}
     * <p>
     * If a particular direction of the swipe is used ({@link #LEFT} or {@link #RIGHT}),
     * and this flag is set, then {@link #isFreeDragAfterOpen} always will be true.
     * <p>
     * If the left and right directions of the swipe are used simultaneously ({@link #HORIZONTAL}),
     * then this flag will be ignored
     */
    private boolean isContinuousSwipe;

    /**
     * Moving the main view after it was open.
     * <p>
     * if {@link #isEmptyLeftView()} or {@link #isEmptyRightView()},
     * then this flag will be ignored
     */
    private boolean isFreeDragAfterOpen;

    /**
     * If a particular direction of the swipe is used ({@link #LEFT} or {@link #RIGHT}),
     * then this flag allows you to do the swipe in the opposite direction.
     * <p>
     * If the horizontal direction is used ({@link #HORIZONTAL}),
     * this flag allows you to move the main view continuously in both directions
     */
    private boolean isFreeHorizontalDrag;

    /**
     * The right bounding border of the swipe for the main view
     */
    private int rightDragViewPadding;

    /**
     * The left bounding border of the swipe for the main view
     */
    private int leftDragViewPadding;

    /**
     * Sensitivity of automatic closing of the main view
     */
    private double autoOpenSpeed;

    /**
     * Disable intercept touch event for draggable view
     */
    private boolean disallowIntercept;

    private int currentDraggingState = ViewDragHelper.STATE_IDLE;
    private ViewDragHelper dragHelper;
    private GestureDetectorCompat gestureDetector;
    private int draggingViewLeft;
    private int horizontalWidth;
    private boolean isLeftOpen;
    private boolean isRightOpen;
    private int staticRightViewId;
    private int staticLeftViewId;
    private int draggedViewId;
    private View draggedView;
    private View staticRightView;
    private View staticLeftView;
    private SwipeActionsListener actionsListener;
    private final ViewDragHelper.Callback dragHelperCallback = new ViewDragHelper.Callback() {
        @Override
        public void onViewDragStateChanged(int state) {
            if (state == currentDraggingState)
                return;

            if (isIdleAfterMoving(state)) {
                updateState();
            }

            currentDraggingState = state;
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            draggingViewLeft = left;

            if (isTogether) {
                if (currentDirection == LEFT) {
                    staticRightView.offsetLeftAndRight(dx);
                } else if (currentDirection == RIGHT) {
                    staticLeftView.offsetLeftAndRight(dx);
                } else if (currentDirection == HORIZONTAL) {
                    staticLeftView.offsetLeftAndRight(dx);
                    staticRightView.offsetLeftAndRight(dx);
                }
            }
        }

        @Override
        public int getViewHorizontalDragRange(@NonNull View child) {
            return horizontalWidth;
        }

        @Override
        public boolean tryCaptureView(@NonNull View view, int pointerId) {
            return view.getId() == draggedView.getId();
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            switch (currentDirection) {
                case LEFT:
                    return clampLeftViewPosition(left);
                case RIGHT:
                    return clampRightViewPosition(left);
                case HORIZONTAL:
                    return clampHorizontalViewPosition(left, dx);
                default:
                    return CLOSE_POSITION;
            }
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xVel, float yVel) {
            int finalXDraggingView = CLOSE_POSITION;

            if (currentDirection == LEFT) {
                finalXDraggingView = getFinalXLeftDirection(xVel);
            } else if (currentDirection == RIGHT) {
                finalXDraggingView = getFinalXRightDirection(xVel);
            } else if (currentDirection == HORIZONTAL) {
                finalXDraggingView = getFinalXHorizontalDirection(xVel);

                if (finalXDraggingView == NO_POSITION) {
                    finalXDraggingView = getPreviousPosition();
                }
            }

            if (dragHelper.settleCapturedViewAt(finalXDraggingView, draggedView.getTop())) {
                ViewCompat.postInvalidateOnAnimation(SwipeLayout.this);
            }
        }
    };
    private GestureDetector.OnGestureListener gestureDetectorCallBack = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        }
    };

    public SwipeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        isLeftOpen = false;
        isRightOpen = false;

        final TypedArray typedArray = getContext().obtainStyledAttributes(attrs, com.zerobranch.layout.R.styleable.SwipeLayout);
        currentDirection = typedArray.getInteger(com.zerobranch.layout.R.styleable.SwipeLayout_swipeDirection, LEFT);
        isFreeDragAfterOpen = typedArray.getBoolean(com.zerobranch.layout.R.styleable.SwipeLayout_isFreeDragAfterOpen, false);
        isFreeHorizontalDrag = typedArray.getBoolean(com.zerobranch.layout.R.styleable.SwipeLayout_isFreeHorizontalDrag, false);
        isContinuousSwipe = typedArray.getBoolean(com.zerobranch.layout.R.styleable.SwipeLayout_isContinuousSwipe, false);
        isTogether = typedArray.getBoolean(com.zerobranch.layout.R.styleable.SwipeLayout_isTogether, false);
        isEnabledSwipe = typedArray.getBoolean(com.zerobranch.layout.R.styleable.SwipeLayout_isEnabledSwipe, true);
        staticLeftViewId = typedArray.getResourceId(com.zerobranch.layout.R.styleable.SwipeLayout_leftItem, 0);
        staticRightViewId = typedArray.getResourceId(com.zerobranch.layout.R.styleable.SwipeLayout_rightItem, 0);
        draggedViewId = typedArray.getResourceId(com.zerobranch.layout.R.styleable.SwipeLayout_draggedItem, 0);
        autoOpenSpeed = typedArray.getInt(com.zerobranch.layout.R.styleable.SwipeLayout_autoMovingSensitivity, DEFAULT_AUTO_OPEN_SPEED);
        rightDragViewPadding = (int) typedArray.getDimension(com.zerobranch.layout.R.styleable.SwipeLayout_rightDragViewPadding, 0);
        leftDragViewPadding = (int) typedArray.getDimension(com.zerobranch.layout.R.styleable.SwipeLayout_leftDragViewPadding, 0);

        parametersAdjustment();
        typedArray.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        horizontalWidth = w;
        super.onSizeChanged(w, h, oldW, oldH);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (disallowIntercept && isViewGroup(draggedView)) {
            final View neededScrollView = getNeededTouchView(event, (ViewGroup) draggedView);
            final Point touchPoint = new Point((int) event.getX(), (int) event.getY());

            if (neededScrollView != null && isViewTouchTarget(neededScrollView, touchPoint)) {
                return false;
            }
        }

        return isSwipeViewTarget(event) && dragHelper.shouldInterceptTouchEvent(event);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
        this.disallowIntercept = disallowIntercept;
    }

    @Override
    protected void onFinishInflate() {
        if (draggedViewId != 0) {
            draggedView = findViewById(draggedViewId);
        }

        if (staticLeftViewId != 0) {
            staticLeftView = findViewById(staticLeftViewId);
        }

        if (staticRightViewId != 0) {
            staticRightView = findViewById(staticRightViewId);
        }

        if (draggedView == null) {
            throw new RuntimeException("'draggedItem' must be specified");
        } else if (isTogether && currentDirection == LEFT && staticRightView == null) {
            throw new RuntimeException("If 'isTogether = true' 'rightItem' must be specified");
        } else if (isTogether && currentDirection == RIGHT && staticLeftView == null) {
            throw new RuntimeException("If 'isTogether = true' 'leftItem' must be specified");
        } else if (currentDirection == LEFT && !isContinuousSwipe && staticRightView == null) {
            throw new RuntimeException("Must be specified 'rightItem' or flag isContinuousSwipe = true");
        } else if (currentDirection == RIGHT && !isContinuousSwipe && staticLeftView == null) {
            throw new RuntimeException("Must be specified 'leftItem' or flag isContinuousSwipe = true");
        } else if (currentDirection == HORIZONTAL && (staticRightView == null || staticLeftView == null)) {
            throw new RuntimeException("'leftItem' and 'rightItem' must be specified");
        }

        dragHelper = ViewDragHelper.create(this, 1.0f, dragHelperCallback);
        gestureDetector = new GestureDetectorCompat(getContext(), gestureDetectorCallBack);

        setupPost();
        super.onFinishInflate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabledSwipe && (isSwipeViewTarget(event) || isMoving())) {
            gestureDetector.onTouchEvent(event);
            dragHelper.processTouchEvent(event);
            return true;
        } else {
            return super.onTouchEvent(event);
        }
    }

    @Override
    public void computeScroll() {
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * Is enabled Swipe
     *
     * @return True if swipe is enabled, false otherwise.
     */
    public boolean isEnabledSwipe() {
        return isEnabledSwipe;
    }

    /**
     * Set the enabled swipe.
     *
     * @param enabledSwipe True if swipe is enabled, false otherwise.
     */
    public void setEnabledSwipe(boolean enabledSwipe) {
        this.isEnabledSwipe = enabledSwipe;
    }

    /**
     * Performs manual swipe to the left
     *
     * @param animated - flag to animate opening
     */
    public void openRight(boolean animated) {
        if (animated) {
            openRight();
        } else if (isDragIdle(currentDraggingState) && ((currentDirection == LEFT && !isEmptyRightView())
                || currentDirection == HORIZONTAL) && !isRightOpen) {
            if (isTogether) {
                staticRightView.offsetLeftAndRight(-1 * (isLeftOpen ? getRightViewWidth() * 2 : getRightViewWidth()));
            }

            draggedView.offsetLeftAndRight(-1 * (isLeftOpen ? getRightViewWidth() * 2 : getRightViewWidth()));
            draggingViewLeft -= (isLeftOpen ? getRightViewWidth() * 2 : getRightViewWidth());
            updateState();
        }
    }

    /**
     * Performs a full manual swipe to the left
     *
     * @param animated - flag to animate opening
     */
    public void openRightCompletely(boolean animated) {
        if (animated) {
            openRightCompletely();
        } else {
            if (isDragIdle(currentDraggingState) && currentDirection == LEFT) {
                if (isTogether) {
                    staticRightView.offsetLeftAndRight(-horizontalWidth);
                }

                draggedView.offsetLeftAndRight(-horizontalWidth);
                draggingViewLeft -= horizontalWidth;
                updateState();
            }
        }
    }

    /**
     * Performs manual swipe to the right
     *
     * @param animated - flag to animate opening
     */
    public void openLeft(boolean animated) {
        if (animated) {
            openLeft();
        } else if (isDragIdle(currentDraggingState) && ((currentDirection == RIGHT && !isEmptyLeftView())
                || currentDirection == HORIZONTAL) && !isLeftOpen) {
            if (isTogether) {
                staticLeftView.offsetLeftAndRight((isRightOpen ? getLeftViewWidth() * 2 : getLeftViewWidth()));
            }

            draggedView.offsetLeftAndRight((isRightOpen ? getLeftViewWidth() * 2 : getLeftViewWidth()));
            draggingViewLeft += (isRightOpen ? getLeftViewWidth() * 2 : getLeftViewWidth());
            updateState();
        }
    }

    /**
     * Performs a full manual swipe to the right
     *
     * @param animated - flag to animate opening
     */
    public void openLeftCompletely(boolean animated) {
        if (animated) {
            openRightCompletely();
        } else {
            if (isDragIdle(currentDraggingState) && currentDirection == RIGHT) {
                if (isTogether) {
                    staticRightView.offsetLeftAndRight(horizontalWidth);
                }

                draggedView.offsetLeftAndRight(horizontalWidth);
                draggingViewLeft += horizontalWidth;
                updateState();
            }
        }
    }

    /**
     * Performs manual close
     *
     * @param animated - flag to animate closing
     */
    public void close(boolean animated) {
        if (animated) {
            close();
        } else {
            if (isTogether) {
                if (staticLeftView != null && currentDirection == RIGHT) {
                    staticLeftView.layout(CLOSE_POSITION, staticLeftView.getTop(),
                            staticLeftView.getWidth(), staticLeftView.getBottom());
                } else if (staticRightView != null && currentDirection == LEFT) {
                    staticRightView.layout(horizontalWidth - staticRightView.getWidth(), staticRightView.getTop(),
                            horizontalWidth, staticRightView.getBottom());
                } else if (currentDirection == HORIZONTAL && staticRightView != null && staticLeftView != null) {
                    staticLeftView.layout(CLOSE_POSITION, staticLeftView.getTop(),
                            staticLeftView.getWidth(), staticLeftView.getBottom());
                    staticRightView.layout(horizontalWidth - staticRightView.getWidth(), staticRightView.getTop(),
                            horizontalWidth, staticRightView.getBottom());
                }
            }

            draggedView.layout(CLOSE_POSITION,
                    draggedView.getTop(),
                    draggedView.getWidth(),
                    draggedView.getBottom());

            draggingViewLeft = CLOSE_POSITION;
            updateState();
        }
    }

    /**
     * Performs manual swipe to the right
     */
    public void openLeft() {
        if (isDragIdle(currentDraggingState) && ((currentDirection == RIGHT && !isEmptyLeftView())
                || currentDirection == HORIZONTAL)) {
            moveTo(getLeftViewWidth());
        }
    }

    /**
     * Performs manual swipe to the left
     */
    public void openRight() {
        if (isDragIdle(currentDraggingState) && ((currentDirection == LEFT && !isEmptyRightView())
                || currentDirection == HORIZONTAL)) {
            moveTo(-getRightViewWidth());
        }
    }

    /**
     * Performs a full manual swipe to the right
     */
    public void openLeftCompletely() {
        if (isDragIdle(currentDraggingState) && currentDirection == RIGHT) {
            moveTo(horizontalWidth);
        }
    }

    /**
     * Performs a full manual swipe to the left
     */
    public void openRightCompletely() {
        if (isDragIdle(currentDraggingState) && currentDirection == LEFT) {
            moveTo(-horizontalWidth);
        }
    }

    /**
     * Performs manual close
     */
    public void close() {
        moveTo(CLOSE_POSITION);
    }

    /**
     * Is moving main view
     */
    public boolean isMoving() {
        return (currentDraggingState == ViewDragHelper.STATE_DRAGGING ||
                currentDraggingState == ViewDragHelper.STATE_SETTLING);
    }

    /**
     * Is closed main view
     */
    public boolean isClosed() {
        return draggingViewLeft == CLOSE_POSITION;
    }

    /**
     * Get current direction of a swipe
     */
    public int getCurrentDirection() {
        return currentDirection;
    }

    /**
     * Set current direction of a swipe
     */
    public SwipeLayout setCurrentDirection(int currentDirection) {
        this.currentDirection = currentDirection;
        return this;
    }

    /**
     * Is move the secondary view along with the main view
     */
    public boolean isTogether() {
        return isTogether;
    }

    /**
     * The secondary view will move along with the main view
     */
    public SwipeLayout setTogether(boolean together) {
        isTogether = together;
        return this;
    }

    /**
     * Swipe to the end of the screen.
     * Can work without a secondary view {@link #staticLeftView} and {@link #staticRightView}
     * <p>
     * If a particular direction of the swipe is used ({@link #LEFT} or {@link #RIGHT}),
     * and this flag is set, then {@link #isFreeDragAfterOpen} always will be true.
     * <p>
     * If the left and right directions of the swipe are used simultaneously ({@link #HORIZONTAL}),
     * then this flag will be ignored
     */
    public boolean isContinuousSwipe() {
        return isContinuousSwipe;
    }

    /**
     * Swipe to the end of the screen.
     * Can work without a secondary view {@link #staticLeftView} and {@link #staticRightView}
     * <p>
     * If a particular direction of the swipe is used ({@link #LEFT} or {@link #RIGHT}),
     * and this flag is set, then {@link #isFreeDragAfterOpen} always will be true.
     * <p>
     * If the left and right directions of the swipe are used simultaneously ({@link #HORIZONTAL}),
     * then this flag will be ignored
     */
    public SwipeLayout setContinuousSwipe(boolean continuousSwipe) {
        isContinuousSwipe = continuousSwipe;
        parametersAdjustment();
        return this;
    }

    /**
     * Moving the main view after it was open.
     * <p>
     * if {@link #isEmptyLeftView()} or {@link #isEmptyRightView()},
     * then this flag will be ignored
     */
    public boolean isFreeDragAfterOpen() {
        return isFreeDragAfterOpen;
    }

    /**
     * Moving the main view after it was open.
     * <p>
     * if {@link #isEmptyLeftView()} or {@link #isEmptyRightView()},
     * then this flag will be ignored
     */
    public SwipeLayout setFreeDragAfterOpen(boolean freeDragAfterOpen) {
        isFreeDragAfterOpen = freeDragAfterOpen;
        parametersAdjustment();
        return this;
    }

    /**
     * If a particular direction of the swipe is used ({@link #LEFT} or {@link #RIGHT}),
     * then this flag allows you to do the swipe in the opposite direction.
     * <p>
     * If the horizontal direction is used ({@link #HORIZONTAL}),
     * this flag allows you to move the main view continuously in both directions
     */
    public boolean isFreeHorizontalDrag() {
        return isFreeHorizontalDrag;
    }

    /**
     * If a particular direction of the swipe is used ({@link #LEFT} or {@link #RIGHT}),
     * then this flag allows you to do the swipe in the opposite direction.
     * <p>
     * If the horizontal direction is used ({@link #HORIZONTAL}),
     * this flag allows you to move the main view continuously in both directions
     */
    public SwipeLayout setFreeHorizontalDrag(boolean freeHorizontalDrag) {
        isFreeHorizontalDrag = freeHorizontalDrag;
        return this;
    }

    /**
     * Is open right view
     */
    public boolean isRightOpen() {
        return isRightOpen;
    }

    /**
     * Is open left view
     */
    public boolean isLeftOpen() {
        return isLeftOpen;
    }

    /**
     * Set swipe actions listener
     */
    public SwipeLayout setOnActionsListener(@Nullable SwipeActionsListener actionsListener) {
        this.actionsListener = actionsListener;
        return this;
    }

    /**
     * Get the right bounding border of the swipe for the main view
     */
    public int getRightDragViewPadding() {
        return rightDragViewPadding;
    }

    /**
     * Set the right bounding border of the swipe for the main view
     */
    public SwipeLayout setRightDragViewPadding(int minRightDragViewPadding) {
        this.rightDragViewPadding = minRightDragViewPadding;
        parametersAdjustment();
        return this;
    }

    /**
     * Get the left bounding border of the swipe for the main view
     */
    public int getLeftDragViewPadding() {
        return leftDragViewPadding;
    }

    /**
     * Set the left bounding border of the swipe for the main view
     */
    public SwipeLayout setLeftDragViewPadding(int minLeftDragViewPadding) {
        this.leftDragViewPadding = minLeftDragViewPadding;
        parametersAdjustment();
        return this;
    }

    /**
     * Enable touch for ViewGroup
     */
    public void enableTouchForViewGroup(@NonNull final ViewGroup viewGroup) {
        viewGroup.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
    }

    private void updateState() {
        if (isClosed()) {
            isLeftOpen = false;
            isRightOpen = false;

            if (actionsListener != null) {
                actionsListener.onClose();
            }
        } else if (isLeftOpenCompletely() || isLeftViewOpen()) {
            isLeftOpen = true;
            isRightOpen = false;

            if (actionsListener != null) {
                actionsListener.onOpen(RIGHT, isLeftOpenCompletely());
            }
        } else if (isRightOpenCompletely() || isRightViewOpen()) {
            isLeftOpen = false;
            isRightOpen = true;

            if (actionsListener != null) {
                actionsListener.onOpen(LEFT, isRightOpenCompletely());
            }
        }
    }

    private int clampLeftViewPosition(int left) {
        if (isContinuousSwipe && isEmptyRightView()) {
            if (isFreeHorizontalDrag) {
                return left > horizontalWidth ? CLOSE_POSITION : Math.max(left, -horizontalWidth);
            } else {
                return left > CLOSE_POSITION ? CLOSE_POSITION : Math.max(left, -horizontalWidth);
            }
        }

        if (isFreeDragAfterOpen) {
            if (isFreeHorizontalDrag) {
                return left > horizontalWidth ? CLOSE_POSITION
                        : Math.max(left, leftDragViewPadding - horizontalWidth);
            }
            return left > CLOSE_POSITION ? CLOSE_POSITION
                    : Math.max(left, leftDragViewPadding - horizontalWidth);
        }

        if (isFreeHorizontalDrag) {
            return left > horizontalWidth ? CLOSE_POSITION : Math.max(left, -getRightViewWidth());
        }
        return left > CLOSE_POSITION ? CLOSE_POSITION : Math.max(left, -getRightViewWidth());
    }

    private int clampRightViewPosition(int left) {
        if (isContinuousSwipe && isEmptyLeftView()) {
            if (isFreeHorizontalDrag) {
                return left < -horizontalWidth ? -horizontalWidth : Math.min(left, horizontalWidth);
            } else {
                return left < CLOSE_POSITION ? CLOSE_POSITION : Math.min(left, horizontalWidth);
            }
        }

        if (isFreeDragAfterOpen) {
            if (isFreeHorizontalDrag) {
                return left < -horizontalWidth ? -horizontalWidth
                        : Math.min(left, horizontalWidth - rightDragViewPadding);
            }
            return left < CLOSE_POSITION ? CLOSE_POSITION
                    : Math.min(left, horizontalWidth - rightDragViewPadding);
        }

        if (isFreeHorizontalDrag) {
            return left < -horizontalWidth ? -horizontalWidth : Math.min(left, getLeftViewWidth());
        }
        return left < CLOSE_POSITION ? CLOSE_POSITION : Math.min(left, getLeftViewWidth());
    }

    private int clampHorizontalViewPosition(int left, int dx) {
        if (!isFreeHorizontalDrag && isLeftOpen && dx < 0) {
            return Math.max(left, CLOSE_POSITION);
        }

        if (!isFreeHorizontalDrag && isRightOpen && dx > 0) {
            return Math.min(left, CLOSE_POSITION);
        }

        if (!isFreeDragAfterOpen && left > CLOSE_POSITION) {
            return Math.min(left, getLeftViewWidth());
        }

        if (!isFreeDragAfterOpen && left < CLOSE_POSITION) {
            return Math.max(left, -getRightViewWidth());
        }

        return left < CLOSE_POSITION ? Math.max(left, leftDragViewPadding - horizontalWidth)
                : Math.min(left, horizontalWidth - rightDragViewPadding);
    }

    private int getPreviousPosition() {
        if (isLeftOpen) {
            return getLeftViewWidth();
        } else if (isRightOpen) {
            return -getRightViewWidth();
        } else {
            return CLOSE_POSITION;
        }
    }

    private int getFinalXLeftDirection(float xVel) {
        if (isContinuousSwipe) {
            if (isEmptyRightView()) {
                if ((draggingViewLeft < CLOSE_POSITION && Math.abs(draggingViewLeft) > horizontalWidth / 2)
                        || xVel < -autoOpenSpeed) {
                    return -horizontalWidth;
                }
                return CLOSE_POSITION;
            }

            if (isContinuousSwipeToLeft(xVel)) {
                return -horizontalWidth;
            }
        }

        final boolean settleToOpen;
        if (xVel > autoOpenSpeed) {
            settleToOpen = false;
        } else if (xVel < -autoOpenSpeed) {
            settleToOpen = true;
        } else if (draggingViewLeft < CLOSE_POSITION && Math.abs(draggingViewLeft) > getRightViewWidth() / 2) {
            settleToOpen = true;
        } else if (draggingViewLeft < CLOSE_POSITION && Math.abs(draggingViewLeft) < getRightViewWidth() / 2) {
            settleToOpen = false;
        } else {
            settleToOpen = false;
        }
        return settleToOpen ? -getRightViewWidth() : CLOSE_POSITION;
    }

    private int getFinalXRightDirection(float xVel) {
        if (isContinuousSwipe) {
            if (isEmptyLeftView()) {
                if ((draggingViewLeft > CLOSE_POSITION && Math.abs(draggingViewLeft) > horizontalWidth / 2)
                        || xVel > autoOpenSpeed) {
                    return horizontalWidth;
                }
                return CLOSE_POSITION;
            }

            if (isContinuousSwipeToRight(xVel)) {
                return horizontalWidth;
            }
        }

        final boolean settleToOpen;
        if (xVel > autoOpenSpeed) {
            settleToOpen = true;
        } else if (xVel < -autoOpenSpeed) {
            settleToOpen = false;
        } else if (draggingViewLeft > CLOSE_POSITION && Math.abs(draggingViewLeft) > getLeftViewWidth() / 2) {
            settleToOpen = true;
        } else if (draggingViewLeft > CLOSE_POSITION && Math.abs(draggingViewLeft) < getLeftViewWidth() / 2) {
            settleToOpen = false;
        } else {
            settleToOpen = false;
        }
        return settleToOpen ? getLeftViewWidth() : CLOSE_POSITION;
    }

    private int getFinalXHorizontalDirection(float xVel) {
        if (isSwipeToOpenLeft(xVel)) {
            return getLeftViewWidth();
        } else if (isSwipeToOpenRight(xVel)) {
            return -getRightViewWidth();
        } else if (isSwipeToClose(xVel)) {
            return CLOSE_POSITION;
        }

        return NO_POSITION;
    }

    private boolean isContinuousSwipeToRight(float xVel) {
        return (xVel > autoOpenSpeed && Math.abs(draggingViewLeft) > getLeftViewWidth())
                || (draggingViewLeft > CLOSE_POSITION && Math.abs(draggingViewLeft) > horizontalWidth / 2);

    }

    private boolean isContinuousSwipeToLeft(float xVel) {
        return (xVel < -autoOpenSpeed && Math.abs(draggingViewLeft) > getRightViewWidth())
                || (draggingViewLeft < CLOSE_POSITION && Math.abs(draggingViewLeft) > horizontalWidth / 2);
    }

    private boolean isSwipeToOpenRight(float xVel) {
        if (xVel > 0) {
            return false;
        }
        return (draggingViewLeft < CLOSE_POSITION && xVel < -autoOpenSpeed)
                || (draggingViewLeft < CLOSE_POSITION && Math.abs(draggingViewLeft) > getRightViewWidth() / 2);
    }

    private boolean isSwipeToOpenLeft(float xVel) {
        if (xVel < 0) {
            return false;
        }
        return (draggingViewLeft > CLOSE_POSITION && xVel > autoOpenSpeed)
                || (draggingViewLeft > CLOSE_POSITION && Math.abs(draggingViewLeft) > getLeftViewWidth() / 2);
    }

    private boolean isSwipeToClose(float xVel) {
        return (draggingViewLeft >= CLOSE_POSITION && xVel < -autoOpenSpeed)
                || (draggingViewLeft <= CLOSE_POSITION && xVel > autoOpenSpeed)
                || (draggingViewLeft >= CLOSE_POSITION && Math.abs(draggingViewLeft) < getLeftViewWidth() / 2)
                || (draggingViewLeft <= CLOSE_POSITION && Math.abs(draggingViewLeft) < getRightViewWidth() / 2);
    }

    private void setupPost() {
        if (isTogether) {
            post(new Runnable() {
                @Override
                public void run() {
                    if (currentDirection == LEFT) {
                        staticRightView.setX(horizontalWidth);
                    } else if (currentDirection == RIGHT) {
                        staticLeftView.setX(-staticLeftView.getWidth());
                    } else if (currentDirection == HORIZONTAL) {
                        staticRightView.setX(horizontalWidth);
                        staticLeftView.setX(-staticLeftView.getWidth());
                    }
                }
            });
        }
    }

    private boolean isViewTouchTarget(View view, Point point) {
        return point.y >= view.getTop()
                && point.y < view.getBottom()
                && point.x >= view.getLeft()
                && point.y < view.getRight();
    }

    private View getNeededTouchView(MotionEvent event, ViewGroup rootView) {
        if (rootView.onInterceptTouchEvent(event)) {
            return rootView;
        }

        int count = rootView.getChildCount();

        for (int i = 0; i < count; i++) {
            View view = rootView.getChildAt(i);

            if (!isViewGroup(view)) {
                continue;
            }

            View neededScrollView = getNeededTouchView(event, (ViewGroup) view);

            if (neededScrollView != null) {
                return neededScrollView;
            }
        }

        return null;
    }

    private boolean isViewGroup(View view) {
        return view instanceof ViewGroup;
    }

    private boolean isSwipeViewTarget(MotionEvent event) {
        final int[] swipeViewLocation = new int[2];
        draggedView.getLocationOnScreen(swipeViewLocation);
        final int upperLimit = swipeViewLocation[1] + draggedView.getMeasuredHeight();
        final int lowerLimit = swipeViewLocation[1];
        final int y = (int) event.getRawY();
        return (y > lowerLimit && y < upperLimit);
    }

    private boolean isIdleAfterMoving(int state) {
        return (currentDraggingState == ViewDragHelper.STATE_DRAGGING
                || currentDraggingState == ViewDragHelper.STATE_SETTLING)
                && state == ViewDragHelper.STATE_IDLE;
    }

    private boolean isDragIdle(int state) {
        return state == ViewDragHelper.STATE_IDLE;
    }

    private boolean isRightViewOpen() {
        return staticRightView != null && draggingViewLeft == -getRightViewWidth();
    }

    private boolean isLeftViewOpen() {
        return staticLeftView != null && draggingViewLeft == getLeftViewWidth();
    }

    private boolean isRightOpenCompletely() {
        return draggingViewLeft == -horizontalWidth;
    }

    private boolean isLeftOpenCompletely() {
        return draggingViewLeft == horizontalWidth;
    }

    private int getLeftViewWidth() {
        return staticLeftView.getWidth();
    }

    private int getRightViewWidth() {
        return staticRightView.getWidth();
    }

    private boolean isEmptyLeftView() {
        return staticLeftView == null;
    }

    private boolean isEmptyRightView() {
        return staticRightView == null;
    }

    private void parametersAdjustment() {
        if (isContinuousSwipe && currentDirection != HORIZONTAL) {
            isFreeDragAfterOpen = true;
        }

        if (currentDirection == HORIZONTAL) {
            rightDragViewPadding = 0;
            leftDragViewPadding = 0;
        }
    }

    private void moveTo(int x) {
        dragHelper.smoothSlideViewTo(draggedView, x, draggedView.getTop());
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public interface SwipeActionsListener {
        void onOpen(int direction, boolean isContinuous);

        void onClose();
    }
}
