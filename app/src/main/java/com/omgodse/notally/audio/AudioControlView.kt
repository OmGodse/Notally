package com.omgodse.notally.audio

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.material.slider.Slider
import com.omgodse.notally.R

class AudioControlView(context: Context, attrs: AttributeSet) : RelativeLayout(context, attrs) {

    private lateinit var length: TextView
    private lateinit var progress: Slider
    private lateinit var chronometer: TextView

    private var started = false
    private var visible = false
    private var running = false

    private var base = 0L
    private var duration = 0L
    private val recycle = StringBuilder(8)

    private var seeking = false
    var onSeekComplete: ((milliseconds: Long) -> Unit)? = null

    @SuppressLint("RestrictedApi")
    override fun onFinishInflate() {
        super.onFinishInflate()
        length = findViewById(R.id.Length)
        progress = findViewById(R.id.Progress)
        chronometer = findViewById(R.id.Chronometer)

        progress.setLabelFormatter { value ->
            val milliseconds = value.toLong()
            val seconds = milliseconds / 1000
            DateUtils.formatElapsedTime(seconds)
        }

        progress.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {

            override fun onStartTrackingTouch(slider: Slider) {
                seeking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                seeking = false
                onSeekComplete?.invoke(progress.value.toLong())
            }
        })

        setCurrentPosition(0)
    }

    fun setDuration(milliseconds: Long) {
        duration = milliseconds
        progress.valueTo = milliseconds.toFloat()
        length.text = DateUtils.formatElapsedTime(recycle, milliseconds / 1000)
    }

    fun setCurrentPosition(position: Int) {
        val now = SystemClock.elapsedRealtime()
        base = now - position
        updateComponents(now)
    }

    fun setStarted(started: Boolean) {
        this.started = started
        updateRunning()
    }


    @Synchronized
    private fun updateComponents(now: Long) {
        var milliseconds = now - base
        if (milliseconds > duration) {
            milliseconds = duration
        }
        chronometer.text = DateUtils.formatElapsedTime(recycle, milliseconds / 1000)
        if (!seeking) {
            progress.value = milliseconds.toFloat()
        }
    }


    private fun updateRunning() {
        val running = visible && started && isShown()
        if (this.running != running) {
            if (running) {
                updateComponents(SystemClock.elapsedRealtime())
                postDelayed(tickRunnable, 100)
            } else {
                removeCallbacks(tickRunnable)
            }
            this.running = running
        }
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (running) {
                updateComponents(SystemClock.elapsedRealtime())
                postDelayed(this, 100)
            }
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        visible = false
        updateRunning()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        visible = (visibility == VISIBLE)
        updateRunning()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateRunning()
    }
}