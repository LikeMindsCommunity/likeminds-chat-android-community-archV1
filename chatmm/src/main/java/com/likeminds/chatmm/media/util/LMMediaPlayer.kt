package com.likeminds.chatmm.media.util

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.likeminds.chatmm.utils.DateUtil
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

class LMMediaPlayer(
    private val context: Context,
    private val listener: MediaPlayerListener
) : Player.Listener {
    private lateinit var mediaPlayer: ExoPlayer

    init {
        initializeMediaPlayer()
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = ExoPlayer.Builder(context).setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    32 * 1024,
                    64 * 1024,
                    32 * 1024,
                    1024
                )
                .build()
        ).build()
        mediaPlayer.playWhenReady = true
        mediaPlayer.repeatMode = Player.REPEAT_MODE_OFF
        mediaPlayer.addListener(this)
    }

    fun start() = mediaPlayer.play()

    fun pause() = mediaPlayer.pause()

    fun stop() = mediaPlayer.stop()

    fun release() = mediaPlayer.release()

    fun seekTo(whereTo: Long) = mediaPlayer.seekTo(whereTo)

    fun isPlaying() = mediaPlayer.isPlaying

    fun clear() = mediaPlayer.clearMediaItems()

    private fun currentDurationInInt() = ((mediaPlayer.currentPosition) / 1000).toInt()

    private fun getCurrentDurationInString() =
        DateUtil.formatSeconds(((mediaPlayer.currentPosition) / 1000).toInt())

    fun playedPercentage(): Int {
        return ((mediaPlayer.currentPosition.toDouble() / mediaPlayer.duration.toDouble()) * 100).toInt()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> {
                Log.d(TAG, "buffer state")
            }
            Player.STATE_ENDED -> {
                Log.d(TAG, "ended state")
                listener.onAudioComplete()
            }
            Player.STATE_IDLE -> {
                Log.d(TAG, "idle state")
            }
            Player.STATE_READY -> {
                Log.d(TAG, "ready state")
            }
        }
    }

    fun setMediaDataSource(uri: Uri?, progress: Long = 0) {
        if (uri == null) return
        val item = MediaItem.fromUri(uri)
        mediaPlayer.addMediaItem(item)
        if (mediaPlayer.hasNextMediaItem()) {
            mediaPlayer.seekToNextMediaItem()
        }
        mediaPlayer.prepare()
        mediaPlayer.play()
        mediaPlayer.seekTo(progress)
        setAudioProgress()
        isDataSourceSet = true
    }

    fun setAudioProgress() {
        runnable = Runnable {
            try {
                listener.onProgressChanged(getCurrentDurationInString())
                listener.onProgressChanged(getCurrentDurationInString(), currentDurationInInt())
                listener.onProgressChanged(playedPercentage())

                handler?.postDelayed(runnable ?: Runnable { }, 1000)
            } catch (e: Exception) {
                LMChatLogger.getInstance()?.handleException(
                    e.message ?: "",
                    e.stackTraceToString(),
                    LMSeverity.EMERGENCY
                )
                Log.e(TAG, e.stackTrace.toString())
            }
        }
        handler?.postDelayed(runnable ?: Runnable { }, 1000)
    }

    companion object {
        var isDataSourceSet: Boolean = false
        const val TAG = "LMMediaPlayer"
        var handler: Handler? = null
        var runnable: Runnable? = null
    }
}

interface MediaPlayerListener {
    fun onAudioComplete() {}
    fun onProgressChanged(playedPercentage: Int) {}
    fun onProgressChanged(currentDuration: String) {}
    fun onProgressChanged(currentDuration: String, playedPercentage: Int) {}
}