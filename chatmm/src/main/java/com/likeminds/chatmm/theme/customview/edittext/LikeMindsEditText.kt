package com.likeminds.chatmm.theme.customview.edittext

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.theme.util.ThemeUtil

internal class LikeMindsEditText @JvmOverloads constructor(
    mContext: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = android.R.attr.editTextStyle
) : AppCompatEditText(mContext, attributeSet, defStyle) {

    init {
        // fonts
        val array = context.obtainStyledAttributes(attributeSet, R.styleable.LikeMindsEditText)
        this.typeface = ThemeUtil.getTypeFace(
            context,
            array.getString(R.styleable.LikeMindsEditText_font_style)
        )

        //edittext background color
        val etType = array.getString(R.styleable.LikeMindsEditText_et_type)
        if (etType.equals("special"))
            this.backgroundTintList = ColorStateList.valueOf(LMChatAppearance.getButtonsColor())

        array.recycle()
    }

    private var showKeyboardDelayed = false

    @Override
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        maybeShowKeyboard()
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
                    imm.showSoftInput(this@LikeMindsEditText, InputMethodManager.SHOW_IMPLICIT)
                }
            }
            showKeyboardDelayed = false
        }
    }
}