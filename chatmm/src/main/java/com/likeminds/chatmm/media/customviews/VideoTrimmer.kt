package com.likeminds.chatmm.media.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import com.likeminds.chatmm.databinding.LayoutVideoTrimmerBinding
import com.likeminds.chatmm.media.customviews.interfaces.OnProgressVideoListener
import com.likeminds.chatmm.media.customviews.interfaces.OnRangeSeekBarListener
import com.likeminds.chatmm.media.customviews.interfaces.OnTrimVideoListener
import com.likeminds.chatmm.media.customviews.interfaces.OnVideoListener
import com.likeminds.chatmm.media.model.VideoTrimExtras
import com.likeminds.chatmm.media.util.BackgroundExecutor
import com.likeminds.chatmm.media.util.Mp4Cutter
import com.likeminds.chatmm.media.util.TrimVideoUtils
import com.likeminds.chatmm.media.util.UiThreadExecutor
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

internal class VideoTrimmer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: LayoutVideoTrimmerBinding
    private lateinit var mSrc: Uri

    private var mMaxDuration: Int = -1
    private var mMinDuration: Int = -1
    private var mListeners: ArrayList<OnProgressVideoListener> = ArrayList()

    private var mOnTrimVideoListener: OnTrimVideoListener? = null
    private var mOnVideoListener: OnVideoListener? = null

    private var mDuration = 0f
    private var mTimeVideo = 0f
    private var mStartPosition = 0f

    private var mEndPosition = 0f
    private var mResetSeekBar = true
    private val mMessageHandler = MessageHandler(this)

    init {
        init(context)
    }

    private fun init(context: Context) {
        binding = LayoutVideoTrimmerBinding.inflate(LayoutInflater.from(context), this, true)
        setUpListeners()
        setUpMargins()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpListeners() {
        binding.apply {
            mListeners = ArrayList()
            mListeners.add(OnProgressVideoListener { time, _, _ -> updateVideoProgress(time) })

            val gestureDetector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        onClickVideoPlayPause()
                        return true
                    }
                })

            videoLoader.setOnErrorListener { _, what, _ ->
                mOnTrimVideoListener?.onError("Something went wrong reason : $what")
                false
            }

            videoLoader.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }

            handlerTop.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    onPlayerIndicatorSeekChanged(progress, fromUser)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    onPlayerIndicatorSeekStart()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    onPlayerIndicatorSeekStop(seekBar)
                }
            })

            timelineBar.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
                override fun onCreate(
                    rangeSeekBarView: RangeSeekBarView,
                    index: Int,
                    value: Float
                ) {
                }

                override fun onSeek(rangeSeekBarView: RangeSeekBarView, index: Int, value: Float) {
                    handlerTop.visibility = View.GONE
                    onSeekThumbs(index, value)
                    mOnTrimVideoListener?.onVideoRangeChanged()
                }

                override fun onSeekStart(
                    rangeSeekBarView: RangeSeekBarView,
                    index: Int,
                    value: Float
                ) {
                }

                override fun onSeekStop(
                    rangeSeekBarView: RangeSeekBarView,
                    index: Int,
                    value: Float
                ) {
                    onStopSeekThumbs()
                }
            })

            timelineView.setVideoTimelineListener {
                timelineBar.setVideoTimeLineCreated(
                    true
                )
            }

            videoLoader.setOnPreparedListener { mp -> onVideoPrepared(mp) }
            videoLoader.setOnCompletionListener { onVideoCompleted() }
        }
    }

    private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {
        val duration = (mDuration * progress / 1000L)
        if (fromUser) {
            if (duration < mStartPosition) setProgressBarPosition(mStartPosition)
            else if (duration > mEndPosition) setProgressBarPosition(mEndPosition)
        }
    }

    private fun onPlayerIndicatorSeekStart() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        binding.videoLoader.pause()
        binding.iconVideoPlay.visibility = View.VISIBLE
        notifyProgressUpdate(false)
    }

    private fun onPlayerIndicatorSeekStop(seekBar: SeekBar) {
        binding.apply {
            mMessageHandler.removeMessages(SHOW_PROGRESS)
            videoLoader.pause()
            iconVideoPlay.visibility = View.VISIBLE

            val duration = (mDuration * seekBar.progress / 1000L).toInt()
            videoLoader.seekTo(duration)
            notifyProgressUpdate(false)
        }
    }

    private fun setProgressBarPosition(position: Float) {
        if (mDuration > 0) binding.handlerTop.progress = (1000L * position / mDuration).toInt()
    }

    private fun setUpMargins() {
        binding.apply {
            val marge = timelineBar.thumbs[0].widthBitmap
            val lp = timelineView.layoutParams as LayoutParams
            lp.setMargins(marge, 0, marge, 0)
            timelineView.layoutParams = lp
        }
    }

    fun onSaveClicked(videoTrimExtras: VideoTrimExtras?) {
        try {
            binding.apply {
                mOnTrimVideoListener?.onTrimStarted()
                iconVideoPlay.visibility = View.VISIBLE
                videoLoader.pause()

                val mediaMetadataRetriever = MediaMetadataRetriever()
                mediaMetadataRetriever.setDataSource(context, mSrc)
                val metaDataKeyDuration = mediaMetadataRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: 0L

                val srcFilePath = FileUtil.getRealPath(context, mSrc).path

                if (mTimeVideo < MIN_TIME_FRAME) {
                    if (metaDataKeyDuration - mEndPosition > MIN_TIME_FRAME - mTimeVideo) mEndPosition += MIN_TIME_FRAME - mTimeVideo
                    else if (mStartPosition > MIN_TIME_FRAME - mTimeVideo) mStartPosition -= MIN_TIME_FRAME - mTimeVideo
                }

                val outputFile = FileUtil.createVideoFile(context)
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(srcFilePath)
                } catch (e: Exception) {
                    LMChatLogger.getInstance()?.handleException(
                        e.message ?: "",
                        e.stackTraceToString(),
                        LMSeverity.CRITICAL
                    )
                    e.printStackTrace()
                } finally {
                    extractor.release()
                }

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        Mp4Cutter.startTrim(
                            context,
                            mSrc,
                            outputFile,
                            mStartPosition.toLong(),
                            mEndPosition.toLong(),
                            mOnTrimVideoListener,
                            videoTrimExtras
                        )
                    } catch (e: Exception) {
                        LMChatLogger.getInstance()?.handleException(
                            e.message ?: "",
                            e.stackTraceToString(),
                            LMSeverity.CRITICAL
                        )
                        Log.e("VideoTrimmer", "onSaveClicked startTrim ${e.localizedMessage}")
                        e.printStackTrace()
                        mOnTrimVideoListener?.onFailed(videoTrimExtras)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("VideoTrimmer", "onSaveClicked ${e.localizedMessage}")
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.EMERGENCY
            )
            mOnTrimVideoListener?.onFailed(videoTrimExtras)
        }
    }

    private fun onClickVideoPlayPause() {
        binding.apply {
            if (videoLoader.isPlaying) {
                iconVideoPlay.visibility = View.VISIBLE
                mMessageHandler.removeMessages(SHOW_PROGRESS)
                videoLoader.pause()
            } else {
                iconVideoPlay.visibility = View.GONE
                if (mResetSeekBar) {
                    mResetSeekBar = false
                    videoLoader.seekTo(mStartPosition.toInt())
                }
                mMessageHandler.sendEmptyMessage(SHOW_PROGRESS)
                videoLoader.start()
            }
        }
    }

    fun onCancelClicked() {
        binding.videoLoader.stopPlayback()
        mOnTrimVideoListener?.cancelAction()
    }

    private fun onVideoPrepared(mp: MediaPlayer) {
        binding.apply {
            val videoWidth = mp.videoWidth
            val videoHeight = mp.videoHeight
            val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()
            val screenWidth = mainConstraintLayout.width
            val screenHeight = mainConstraintLayout.height
            val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
            val lp = videoLoader.layoutParams

            if (videoProportion > screenProportion) {
                lp.width = screenWidth
                lp.height = (screenWidth.toFloat() / videoProportion).toInt()
            } else {
                lp.width = (videoProportion * screenHeight.toFloat()).toInt()
                lp.height = screenHeight
            }
            videoLoader.layoutParams = lp

            iconVideoPlay.visibility = View.VISIBLE

            mDuration = videoLoader.duration.toFloat()
            setSeekBarPosition()

            setTimeFrames()

            mOnVideoListener?.onVideoPrepared()
        }
    }

    private fun setSeekBarPosition() {
        binding.apply {
            when {
                mDuration >= mMaxDuration && mMaxDuration != -1 -> {
                    mStartPosition = mDuration / 2 - mMaxDuration / 2
                    mEndPosition = mDuration / 2 + mMaxDuration / 2
                    timelineBar.setThumbValue(0, (mStartPosition * 100 / mDuration))
                    timelineBar.setThumbValue(1, (mEndPosition * 100 / mDuration))
                }
                mDuration <= mMinDuration && mMinDuration != -1 -> {
                    mStartPosition = mDuration / 2 - mMinDuration / 2
                    mEndPosition = mDuration / 2 + mMinDuration / 2
                    timelineBar.setThumbValue(0, (mStartPosition * 100 / mDuration))
                    timelineBar.setThumbValue(1, (mEndPosition * 100 / mDuration))
                }
                else -> {
                    mStartPosition = 0f
                    mEndPosition = mDuration
                }
            }
            videoLoader.seekTo(mStartPosition.toInt())
            mTimeVideo = mDuration
            timelineBar.initMaxWidth()
        }
    }

    private fun setTimeFrames() {
        binding.tvTimeSelection.text = String.format(
            "%s - %s",
            TrimVideoUtils.stringForTime(mStartPosition),
            TrimVideoUtils.stringForTime(mEndPosition)
        )

        binding.tvRemainingTime.text =
            TrimVideoUtils.stringForTime(mEndPosition - mStartPosition)
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = (mDuration * value / 100L)
                binding.videoLoader.seekTo(mStartPosition.toInt())
            }
            Thumb.RIGHT -> {
                mEndPosition = (mDuration * value / 100L)
            }
        }
        setTimeFrames()
        mTimeVideo = mEndPosition - mStartPosition
    }

    private fun onStopSeekThumbs() {
        mMessageHandler.removeMessages(SHOW_PROGRESS)
        binding.videoLoader.pause()
        binding.iconVideoPlay.visibility = View.VISIBLE
    }

    private fun onVideoCompleted() {
        binding.videoLoader.seekTo(mStartPosition.toInt())
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0f) return
        val position = binding.videoLoader.currentPosition
        if (all) {
            for (item in mListeners) {
                item.updateProgress(position.toFloat(), mDuration, (position * 100 / mDuration))
            }
        } else {
            mListeners[0].updateProgress(
                position.toFloat(),
                mDuration,
                (position * 100 / mDuration)
            )
        }
    }

    private fun updateVideoProgress(time: Float) {
        binding.apply {
            if (time <= mStartPosition && time <= mEndPosition) {
                handlerTop.visibility = View.GONE
            } else {
                handlerTop.visibility = View.VISIBLE
            }
            if (time >= mEndPosition) {
                mMessageHandler.removeMessages(SHOW_PROGRESS)
                videoLoader.pause()
                iconVideoPlay.visibility = View.VISIBLE
                handlerTop.visibility = View.GONE
                mResetSeekBar = true
                return
            }
            setProgressBarPosition(time)
        }
    }

    fun setVideoInformationVisibility(visible: Boolean): VideoTrimmer {
        binding.tvTimeSelection.visibility = if (visible) View.VISIBLE else View.GONE
        binding.tvRemainingTime.visibility = if (visible) View.VISIBLE else View.GONE
        return this
    }

    fun setOnTrimVideoListener(onTrimVideoListener: OnTrimVideoListener): VideoTrimmer {
        mOnTrimVideoListener = onTrimVideoListener
        return this
    }

    fun setOnVideoListener(onVideoListener: OnVideoListener): VideoTrimmer {
        mOnVideoListener = onVideoListener
        return this
    }

    fun destroy() {
        BackgroundExecutor.cancelAll("", true)
        UiThreadExecutor.cancelAll("")
    }

    fun setMaxDuration(maxDuration: Int): VideoTrimmer {
        mMaxDuration = maxDuration * 1000
        return this
    }

    fun setMinDuration(minDuration: Int): VideoTrimmer {
        mMinDuration = minDuration * 1000
        return this
    }

    fun setVideoURI(videoURI: Uri): VideoTrimmer {
        binding.apply {
            mSrc = videoURI
            timelineBar.reset()
            timelineBar.setVideoTimeLineCreated(false)
            videoLoader.setVideoURI(mSrc)
            videoLoader.requestFocus()
            timelineView.setVideo(mSrc)
        }
        return this
    }

    fun setTextTimeSelectionTypeface(tf: Typeface?): VideoTrimmer {
        if (tf != null) binding.tvTimeSelection.typeface = tf
        if (tf != null) binding.tvRemainingTime.typeface = tf
        return this
    }

    private class MessageHandler(view: VideoTrimmer) : Handler(Looper.getMainLooper()) {
        private val mView: WeakReference<VideoTrimmer> = WeakReference(view)
        override fun handleMessage(msg: Message) {
            val view = mView.get() ?: return
            view.notifyProgressUpdate(true)
            if (view.binding.videoLoader.isPlaying) sendEmptyMessageDelayed(0, 10)
        }
    }

    companion object {
        private const val MIN_TIME_FRAME = 1000
        private const val SHOW_PROGRESS = 2
    }
}
