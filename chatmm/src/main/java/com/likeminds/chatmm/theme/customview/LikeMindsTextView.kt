package com.likeminds.chatmm.theme.customview

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMTheme
import com.likeminds.chatmm.theme.util.ThemeUtil

internal class LikeMindsTextView : AppCompatTextView {

    constructor(context: Context) : super(context) {
        initiate(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initiate(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initiate(attrs)
    }

    private fun initiate(attrs: AttributeSet?) {
        // fonts
        val array = context.obtainStyledAttributes(attrs, R.styleable.LikeMindsTextView)
        val drawableType = array.getString(R.styleable.LikeMindsTextView_drawableType)

        typeface = ThemeUtil.getTypeFace(
            context,
            array.getString(R.styleable.LikeMindsTextView_fontType)
        )

        //text color
        setTextColor(
            ThemeUtil.getTextColor(
                this.currentTextColor,
                array.getString(R.styleable.LikeMindsTextView_textType)
            )
        )

        // sets text link color
        setLinkTextColor(LMTheme.getTextLinkColor())

        // applies button color to button drawables
        if (drawableType.equals("special")) {
            compoundDrawables.forEach {
                it?.setTintList(ColorStateList.valueOf(LMTheme.getButtonsColor()))
            }
        }

        array.recycle()
    }
}