package com.omgodse.notally.audio

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import com.omgodse.notally.miscellaneous.IO
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.room.Audio
import java.io.File

class AudioPlayService : Service() {

    private var state = IDLE
    private var stateBeforeSeeking = -1
    private var errorType = 0
    private var errorCode = 0
    private lateinit var player: MediaPlayer

    var onStateChange: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        player = MediaPlayer()
        player.setOnPreparedListener { setState(PREPARED) }
        player.setOnCompletionListener { setState(COMPLETED) }
        player.setOnSeekCompleteListener { setState(stateBeforeSeeking) }
        player.setOnErrorListener { _, what, extra ->
            errorType = what
            errorCode = extra
            setState(ERROR)
            return@setOnErrorListener true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocalBinder(this)
    }


    fun initialise(audio: Audio) {
        if (state == IDLE) {
            val audioRoot = IO.getExternalAudioDirectory(application)
            if (audioRoot != null) {
                try {
                    val file = File(audioRoot, audio.name)
                    player.setDataSource(file.absolutePath)
                    setState(INITIALISED)
                    player.prepareAsync()
                } catch (exception: Exception) {
                    setIOError()
                    Operations.log(application, exception)
                }
            } else setIOError()
        }
    }

    fun play() {
        when (state) {
            PREPARED, PAUSED, COMPLETED -> {
                player.start()
                setState(STARTED)
            }
            STARTED -> {
                player.pause()
                setState(PAUSED)
            }
        }
    }

    fun seek(milliseconds: Long) {
        if (state == PREPARED || state == STARTED || state == PAUSED || state == COMPLETED) {
            stateBeforeSeeking = state
            player.seekTo(milliseconds.toInt())
            setState(SEEKING)
        }
    }


    fun getState(): Int {
        return state
    }

    fun getErrorType(): Int {
        return errorType
    }

    fun getErrorCode(): Int {
        return errorCode
    }

    fun getCurrentPosition(): Int {
        return if (state != IDLE && state != ERROR) player.currentPosition else 0
    }


    private fun setState(state: Int) {
        this.state = state
        onStateChange?.invoke()
    }

    private fun setIOError() {
        errorCode = 0
        errorType = 15
        setState(ERROR)
    }

    companion object {
        const val IDLE = 0
        const val INITIALISED = 1
        const val PREPARED = 2
        const val STARTED = 3
        const val PAUSED = 4
        const val SEEKING = 5
        const val COMPLETED = 6
        const val ERROR = 7
    }
}