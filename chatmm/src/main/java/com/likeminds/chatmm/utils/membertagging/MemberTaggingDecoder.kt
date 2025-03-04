package com.likeminds.chatmm.utils.membertagging

import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import android.widget.TextView
import com.likeminds.chatmm.utils.membertagging.util.MemberTaggingClickableSpan
import com.likeminds.chatmm.utils.membertagging.view.adapter.MemberTaggingDecoderListener
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

object MemberTaggingDecoder {

    private val REGEX_USER_TAGGING = Regex("<<([^<>]+\\|route://\\S+)>>")

    /**
     * Decodes [text] containing the regex, highlights it with [highlightColor] and sets it on [textView] with clickable spans
     */
    @JvmStatic
    fun decode(
        textView: TextView,
        text: String?,
        enableClick: Boolean,
        highlightColor: Int,
        underLineText: Boolean = false,
        listener: MemberTaggingDecoderListener? = null,
    ) {
        if (text.isNullOrEmpty()) {
            return
        }
        val matches = REGEX_USER_TAGGING.findAll(text, 0)
        textView.setText(text, TextView.BufferType.EDITABLE)
        matches.toList().asReversed().forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last
            val value = matchResult.value
            val tag = value.substring(2, value.length - 2).split("\\|".toRegex())
            val memberName = SpannableString(tag[0])
            if (enableClick) {
                memberName.setSpan(
                    MemberTaggingClickableSpan(
                        highlightColor,
                        value,
                        underLineText
                    ) { regex ->
                        try {
                            val tagUri = getRouteFromRegex(regex)
                                ?: return@MemberTaggingClickableSpan
                            listener?.onTagClick(tagUri)
                        } catch (e: Exception) {
                            LMChatLogger.getInstance()?.handleException(
                                e.message ?: "",
                                e.stackTraceToString(),
                                LMSeverity.CRITICAL
                            )
                            e.printStackTrace()
                        }
                    }, 0, memberName.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                memberName.setSpan(
                    ForegroundColorSpan(highlightColor),
                    0,
                    memberName.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            textView.setText(
                textView.editableText.replace(
                    start,
                    end + 1,
                    memberName
                ), TextView.BufferType.EDITABLE
            )
        }
    }

    /**
     * Decodes [text] containing the regex, highlights it with [highlightColor] and sets it on [editText] with tagging spans
     */
    @JvmStatic
    fun decode(editText: EditText, text: String?, highlightColor: Int) {
        if (text.isNullOrEmpty()) {
            return
        }
        val matches = REGEX_USER_TAGGING.findAll(text, 0)
        editText.setText(text, TextView.BufferType.EDITABLE)
        matches.toList().asReversed().forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last
            val value = matchResult.value
            val tag = value.substring(2, value.length - 2).split("\\|".toRegex())
            val memberName = SpannableString(tag[0])
            memberName.setSpan(
                MemberTaggingClickableSpan(highlightColor, value),
                0,
                memberName.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            editText.setText(
                editText.editableText.replace(
                    start,
                    end + 1,
                    memberName
                ), TextView.BufferType.EDITABLE
            )
        }
    }

    /**
     * @return decoded String with member names and text instead of the whole encoded text
     * example - "Hi, <<Harsh|route://member/181>>. How are you?" will return "Hi, Harsh. How are you?"
     */
    @JvmStatic
    fun decode(text: String?): String {
        if (text.isNullOrEmpty()) {
            return ""
        }
        val matches = REGEX_USER_TAGGING.findAll(text, 0)
        if (matches.count() == 0) {
            return text
        }
        val stringBuilder = StringBuilder()
        var lastIndex = 0
        matches.forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last
            val value = matchResult.value
            if (start > lastIndex) {
                stringBuilder.append(text.substring(lastIndex, start))
            }
            val tag = value.substring(2, value.length - 2).split("\\|".toRegex())
            val name = tag.firstOrNull()
            stringBuilder.append(name)
            lastIndex = end + 1
        }
        if (text.length >= lastIndex) {
            stringBuilder.append(text.substring(lastIndex))
        }
        return stringBuilder.toString()
    }

    /**
     * Decodes the given [text], searches for all tagged members and returns a list of Pair
     * containing member id and member name sequentially.
     * @return List<Pair<Member Id, Member Name>>
     */
    @JvmStatic
    fun decodeAndReturnAllTaggedMembers(text: String?): List<Pair<String, String>> {
        if (text.isNullOrEmpty()) {
            return emptyList()
        }
        val matches = REGEX_USER_TAGGING.findAll(text, 0)
        if (matches.count() == 0) {
            return emptyList()
        }
        val result = mutableListOf<Pair<String, String>>()
        matches.forEach { matchResult ->
            val value = matchResult.value
            val tag = value.substring(2, value.length - 2).split("\\|".toRegex())
            val memberName = tag[0]
            val memberRoute = tag[1]
            val routeSplits = memberRoute.split("/".toRegex())
            val memberUUID = routeSplits[routeSplits.size - 1]
            result.add(Pair(memberUUID, memberName))
        }
        return result
    }

    /**
     * @return Whether the [text] contains tagged member or not
     */
    @JvmStatic
    fun hasMemberTagging(text: String?): Boolean {
        return REGEX_USER_TAGGING.containsMatchIn(text.toString())
    }

    fun getRouteFromRegex(text: String?): Uri? {
        if (text.isNullOrEmpty()) {
            return null
        }
        val match = REGEX_USER_TAGGING.find(text, 0)
        if (match != null) {
            val value = match.value
            val tag = value.substring(2, value.length - 2).split("\\|".toRegex())
            val memberRoute = tag[1]
            return Uri.parse(memberRoute)
        }
        return null
    }

    fun getMemberUUIDFromRegex(text: String?): String? {
        val uri = getRouteFromRegex(text) ?: return null
        val pathSegments = uri.pathSegments
        if ((uri.host == "member_profile" || uri.host == "member"
                    || uri.host == "user_profile") && pathSegments.size == 1
        )
            return pathSegments[0]
        return null
    }

}