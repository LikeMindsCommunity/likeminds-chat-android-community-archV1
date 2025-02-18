package com.likeminds.chatmm.media.customviews

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.util.LongSparseArray
import android.view.View
import com.likeminds.chatmm.R
import com.likeminds.chatmm.media.customviews.interfaces.OnVideoTimelineListener
import com.likeminds.chatmm.media.util.BackgroundExecutor
import com.likeminds.chatmm.media.util.UiThreadExecutor
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import kotlin.math.ceil

internal class TimeLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mClearView: Boolean = false
    private var mVideoUri: Uri? = null
    private var mHeightView: Int = 0
    private var mBitmapList: LongSparseArray<Bitmap>? = null

    private var videoTimelineListener: OnVideoTimelineListener? = null

    init {
        init()
    }

    private fun init() {
        mHeightView = context.resources.getDimensionPixelOffset(R.dimen.video_timeline_frame_height)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minW = paddingLeft + paddingRight + suggestedMinimumWidth
        val w = resolveSizeAndState(minW, widthMeasureSpec, 1)
        val minH = paddingBottom + paddingTop + mHeightView
        val h = resolveSizeAndState(minH, heightMeasureSpec, 1)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        if (w != oldW) getBitmap(w, mVideoUri)
    }

    private fun getBitmap(viewWidth: Int, videoUri: Uri?) {
        val executor = object : BackgroundExecutor.Task(videoUri.toString(), 0L, "") {
            override fun execute() {
                try {
                    val threshold = 11
                    val thumbnailList = LongSparseArray<Bitmap>()
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(context, videoUri)
                    val videoLengthInMs = (Integer.parseInt(
                        mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ) * 1000).toLong()
                    val frameHeight = mHeightView
                    val initialBitmap = mediaMetadataRetriever.getFrameAtTime(
                        0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    initialBitmap?.let {
                        val frameWidth =
                            ((initialBitmap.width.toFloat() / initialBitmap.height.toFloat()) * frameHeight.toFloat()).toInt()
                        var numThumbs = ceil((viewWidth.toFloat() / frameWidth)).toInt()
                        if (numThumbs < threshold) numThumbs = threshold
                        val cropWidth = viewWidth / threshold
                        val interval = videoLengthInMs / numThumbs
                        for (i in 0 until numThumbs) {
                            var bitmap = mediaMetadataRetriever.getFrameAtTime(
                                i * interval,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                            )
                            if (bitmap != null) {
                                try {
                                    bitmap = Bitmap.createScaledBitmap(
                                        bitmap,
                                        frameWidth,
                                        frameHeight,
                                        false
                                    )
                                    bitmap =
                                        Bitmap.createBitmap(bitmap, 0, 0, cropWidth, bitmap.height)
                                } catch (e: Exception) {
                                    LMChatLogger.getInstance()?.handleException(
                                        e.message ?: "",
                                        e.stackTraceToString(),
                                        LMSeverity.CRITICAL
                                    )
                                    e.printStackTrace()
                                }
                                thumbnailList.put(i.toLong(), bitmap)
                            }
                        }
                    }
                    mediaMetadataRetriever.release()
                    returnBitmaps(thumbnailList, videoUri)
                } catch (e: Exception) {
                    LMChatLogger.getInstance()?.handleException(
                        e.message ?: "",
                        e.stackTraceToString(),
                        LMSeverity.CRITICAL
                    )
                    e.printStackTrace()
                }
            }
        }
        BackgroundExecutor.execute(executor)
    }

    private fun returnBitmaps(thumbnailList: LongSparseArray<Bitmap>, videoUri: Uri?) {
        UiThreadExecutor.runTask("", Runnable {
            if (mVideoUri == videoUri) {
                mBitmapList = thumbnailList
                invalidate()
                videoTimelineListener?.onTimeLineCreated()
            }
        }, 0L)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mClearView) {
            mClearView = false
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        } else if (mBitmapList != null) {
            canvas.save()
            var x = 0
            for (i in 0 until (mBitmapList?.size() ?: 0)) {
                val bitmap = mBitmapList?.get(i.toLong())
                if (bitmap != null) {
                    canvas.drawBitmap(bitmap, x.toFloat(), 0f, null)
                    x += bitmap.width
                }
            }
        }
    }

    fun setVideo(data: Uri) {
        // Cancel previous running executor if available.
        BackgroundExecutor.cancelAll(mVideoUri.toString(), true)

        mVideoUri = data

        // Check if timeline is created previously
        if (width > 0) {
            // Clear previous created frames
            mClearView = true
            invalidate()

            // Create new video frames
            getBitmap(width, mVideoUri)
        }
    }

    fun setVideoTimelineListener(videoTimelineListener: OnVideoTimelineListener) {
        this.videoTimelineListener = videoTimelineListener
    }
}
