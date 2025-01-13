package com.likeminds.chatmm.theme.customview

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.ProgressBar
import com.likeminds.chatmm.theme.model.LMChatAppearance

class LikeMindsProgressBar : ProgressBar {
    constructor(context: Context) : super(context) {
        initiate()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initiate()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initiate()
    }

    private fun initiate() {
        // color
        this.progressTintList = ColorStateList.valueOf(LMChatAppearance.getButtonsColor())
        this.indeterminateTintList = ColorStateList.valueOf(LMChatAppearance.getButtonsColor())
    }
}