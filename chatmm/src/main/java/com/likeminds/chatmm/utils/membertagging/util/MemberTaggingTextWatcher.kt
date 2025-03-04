package com.likeminds.chatmm.utils.membertagging.util

import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

internal class MemberTaggingTextWatcher(
    var taggingEnabled: Boolean,
    val editText: EditText
) : TextWatcher {

    private var textWatcherListener: TextWatcherListener? = null

    private val handler = WeakReferenceHandler(this)
    private var globalPosition = -1
    private var spanToRemove: MemberTaggingClickableSpan? = null

    @JvmSynthetic
    internal fun startObserving() {
        editText.addTextChangedListener(this)
    }

    @JvmSynthetic
    internal fun stopObserving() {
        editText.removeTextChangedListener(this)
    }

    @JvmSynthetic
    internal fun addTextWatcherListener(textWatcherListener: TextWatcherListener) {
        this.textWatcherListener = textWatcherListener
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        if (taggingEnabled) {
            val backSpace = count - after > 0
            val position = start + count - 1
            if (backSpace) {
                //To delete user spannable
                val message = editText.editableText
                val spans = message.getSpans(0, position, MemberTaggingClickableSpan::class.java)
                val spanLength = spans.size
                if (spanLength > 0) {
                    val lastSpan = MemberTaggingUtil.getLastSpan(message, spans)
                    val spanEnd = message.getSpanEnd(lastSpan)
                    if (position == spanEnd - 1) {
                        spanToRemove = lastSpan
                        return
                    }
                }
            }
            spanToRemove = null
        }
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (taggingEnabled) {
            try {
                val value = s.toString()
                val backSpace = count - before < 0
                val position = start + count - 1
                if (backSpace) {
                    if (value.contains("@")) {
                        val lastIndex = value.lastIndexOf('@')
                        if (position >= lastIndex) {
                            val subStringFromLastIndex = value.substring(lastIndex, position + 1)
                            val countOfSpaces =
                                subStringFromLastIndex.length - subStringFromLastIndex.replace(
                                    " ",
                                    ""
                                ).length
                            //If space count is less than 1 in b/w last index of '@' and current position then update the globalPosition
                            if (countOfSpaces <= 1) {
                                globalPosition = lastIndex
                            }
                        }
                    } else {
                        //If no '@' is present reset globalPosition
                        resetGlobalPosition()
                    }
                    //To delete user spannable, Remove the tagged text on back press only when no blank space left after text
                    if (spanToRemove != null) {
                        val message = editText.editableText
                        val spanStart = message.getSpanStart(spanToRemove!!)
                        val spanEnd = message.getSpanEnd(spanToRemove!!)

                        message.delete(spanStart, spanEnd)
                        val memberViewDataSpan = spanToRemove!!
                        message.removeSpan(memberViewDataSpan)

                        textWatcherListener?.onMemberRemoved(memberViewDataSpan.regex)
                    }
                } else {
                    if (position < 0) return
                    val mChar = s[position]
                    if (mChar == '@') {
                        //To start user tagging once user types '@'
                        globalPosition = position
                    } else {
                        //This statement is true if cursor is set after some '@' manually, (used to search '@')
                        var lastTagPositionBeforeCursor = value.indexOf("@")
                        globalPosition = lastTagPositionBeforeCursor
                        while (true) {
                            lastTagPositionBeforeCursor = value.indexOf(
                                "@",
                                lastTagPositionBeforeCursor + 1
                            )
                            if (lastTagPositionBeforeCursor in 0 until position) {
                                globalPosition = lastTagPositionBeforeCursor
                            } else {
                                break
                            }
                        }
                    }

                    if (globalPosition in 0 until position) {
                        val subStringFromGlobalIndex =
                            value.substring(globalPosition, position + 1)
                        val countOfSpaces =
                            subStringFromGlobalIndex.length - subStringFromGlobalIndex.replace(
                                " ",
                                ""
                            ).length
                        //If space count is greater than 1 in b/w first index of '@' and current position then reset globalPosition
                        if (countOfSpaces > 1) {
                            resetGlobalPosition()
                        }
                    }
                }
                val nextToGlobalPosition = globalPosition + 1
                if (nextToGlobalPosition < value.length &&
                    value.elementAt(nextToGlobalPosition).isWhitespace()
                ) {
                    resetGlobalPosition()
                }
                if (globalPosition >= 0) {
                    if (value.isNotEmpty()) {
                        if (value.length > globalPosition) {
                            handler.removeDelay()
                            handler.addDelay()
                        } else {
                            resetGlobalPosition()
                        }
                    }
                } else {
                    resetGlobalPosition()
                }
            } catch (e: Exception) {
                Log.e("onTextChanged", "e", e)
                e.printStackTrace()
                LMChatLogger.getInstance()?.handleException(
                    e.message ?: "",
                    e.stackTraceToString(),
                    LMSeverity.CRITICAL
                )
                resetGlobalPosition()
            }
        }
    }

    override fun afterTextChanged(s: Editable) {

    }

    @JvmSynthetic
    internal fun replaceEditText(spannableUserName: SpannableString) {
        try {
            if (globalPosition >= 0 && editText.selectionStart >= 0) {
                if (editText.selectionStart > globalPosition) {
                    editText.text.replace(
                        globalPosition,
                        editText.selectionStart,
                        spannableUserName
                    )
                } else {
                    editText.text.insert(editText.selectionStart, spannableUserName)
                }
                editText.text.insert(editText.selectionStart, " ")
            }
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            e.printStackTrace()
        }
    }

    @JvmSynthetic
    internal fun hitUserTaggingApi() {
        if (globalPosition >= 0 && editText.selectionStart > globalPosition) {
            val text = editText.text.toString().substring(globalPosition, editText.selectionStart)
            textWatcherListener?.onHitTaggingApi(text)
        }
    }

    fun resetGlobalPosition() {
        handler.removeDelay()
        globalPosition = -1
        textWatcherListener?.dismissMemberTagging()
    }

}

interface TextWatcherListener {

    fun onHitTaggingApi(text: String)

    fun onMemberRemoved(regex: String)

    fun dismissMemberTagging()
}