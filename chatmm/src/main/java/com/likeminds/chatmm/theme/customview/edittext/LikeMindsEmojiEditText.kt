package com.likeminds.chatmm.theme.customview.edittext

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.util.ThemeUtil
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import com.vanniktech.emoji.EmojiEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


internal class LikeMindsEmojiEditText @JvmOverloads constructor(
    mContext: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = android.R.attr.editTextStyle
) : EmojiEditText(mContext, attributeSet, defStyle) {

    init {
        setFonts(attributeSet)
    }

    private fun setFonts(attributeSet: AttributeSet?) {
        // fonts
        val array = context.obtainStyledAttributes(attributeSet, R.styleable.LikeMindsEditText)
        typeface = ThemeUtil.getTypeFace(
            context,
            array.getString(R.styleable.LikeMindsEditText_font_style)
        )
        array.recycle()
    }

    private var listener: LikeMindsEditTextListener? = null
    private var showKeyboardDelayed = false

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection? {
        val inputConnection = super.onCreateInputConnection(editorInfo)
        EditorInfoCompat.setContentMimeTypes(editorInfo, getSupportedMimeTypes())
        return inputConnection?.let {
            InputConnectionCompat.createWrapper(
                inputConnection,
                editorInfo,
                CommitContentListener(listener)
            )
        }
    }

    @Override
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        maybeShowKeyboard()
    }

    fun addListener(listener: LikeMindsEditTextListener) {
        this.listener = listener
    }

    private fun getSupportedMimeTypes(): Array<String> {
        return arrayOf("image/png", "image/gif", "image/jpeg")
    }

    internal class CommitContentListener constructor(private val listener: LikeMindsEditTextListener?) :
        InputConnectionCompat.OnCommitContentListener {

        override fun onCommitContent(
            inputContentInfo: InputContentInfoCompat,
            flags: Int,
            opts: Bundle?
        ): Boolean {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1
                && flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION != 0
            ) {
                try {
                    inputContentInfo.requestPermission()
                } catch (e: Exception) {
                    LMChatLogger.getInstance()?.handleException(
                        e.message ?: "",
                        e.stackTraceToString(),
                        LMSeverity.CRITICAL
                    )
                    return false
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                if (inputContentInfo.description.mimeTypeCount > 0) {
                    listener?.onMediaSelected(
                        inputContentInfo.contentUri,
                        inputContentInfo.description.getMimeType(0)
                    )
                }
            }
            return true
        }
    }

    /**
     * Will request focus and try to show the keyboard.
     * It has into account if the containing window has focus or not yet.
     * And delays the call to show keyboard until it's gained.
     */
    fun focusAndShowKeyboard() {
        requestFocus()
        showKeyboardDelayed = true
        maybeShowKeyboard()
    }

    private fun maybeShowKeyboard() {
        if (hasWindowFocus() && showKeyboardDelayed) {
            if (isFocused) {
                post {
                    val imm =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(this@LikeMindsEmojiEditText, InputMethodManager.SHOW_IMPLICIT)
                }
            }
            showKeyboardDelayed = false
        }
    }
}