package com.likeminds.chatmm.utils.membertagging.util

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

class MemberTaggingClickableSpan(
    val color: Int,
    val regex: String,
    val underLineText: Boolean = false,
    val memberTaggingClickableSpanListener: MemberTaggingClickableSpanListener? = null
) : ClickableSpan() {

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        try {
            ds.color = color
            ds.isUnderlineText = underLineText
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            e.printStackTrace()
        }
    }

    override fun onClick(widget: View) {
        memberTaggingClickableSpanListener?.onClick(regex)
    }

    fun getMemberUUID(): String? {
        return MemberTaggingDecoder.getMemberUUIDFromRegex(regex)
    }
}

fun interface MemberTaggingClickableSpanListener {
    fun onClick(regex: String)
}