package com.likeminds.chatmm.utils.databinding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.renderscript.*
import android.renderscript.RenderScript.RSMessageHandler
import androidx.annotation.ColorInt
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import java.security.MessageDigest

internal class BlurTransformation(private val context: Context) : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap? {
        val source: Bitmap = toTransform
        val scaledWidth = source.width
        val scaledHeight = source.height
        val bitmap = pool[scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888]
        return BitmapResource.obtain(
            this.blurBitmap(
                context,
                source,
                bitmap,
                Color.argb(50, 255, 255, 255)
            ), pool
        )?.get()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("blur transformation".toByteArray())
    }

    @Synchronized
    fun blurBitmap(
        context: Context,
        source: Bitmap?,
        bitmap: Bitmap,
        @ColorInt colorOverlay: Int
    ): Bitmap? {
        if (source == null) return bitmap
        Canvas(bitmap).apply {
            drawBitmap(source, 0f, 0f, Paint().apply {
                flags = Paint.FILTER_BITMAP_FLAG
            })
            drawColor(colorOverlay)
        }
        return try {
            blur(context, bitmap)
        } catch (e: RSRuntimeException) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            e.printStackTrace()
            bitmap
        }
    }

    @Throws(RSRuntimeException::class)
    private fun blur(context: Context, bitmap: Bitmap): Bitmap {
        var rs: RenderScript? = null
        var input: Allocation? = null
        var output: Allocation? = null
        var blur: ScriptIntrinsicBlur? = null
        try {
            rs = RenderScript.create(context)
            rs.messageHandler = RSMessageHandler()
            input = Allocation.createFromBitmap(
                rs,
                bitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            output = Allocation.createTyped(rs, input.type)
            blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)).apply {
                setInput(input)
                setRadius(25f)
                forEach(output)
            }
            output.copyTo(bitmap)
        } finally {
            rs?.destroy()
            input?.destroy()
            output?.destroy()
            blur?.destroy()
        }
        return bitmap
    }
}