package com.likeminds.chatmm.theme.customview

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.Toolbar
import com.likeminds.chatmm.theme.model.LMChatAppearance

class LikeMindsToolbar : Toolbar {
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
        // background color
        this.setBackgroundColor(LMChatAppearance.getHeaderColor())

        // toolbar color
        val color = LMChatAppearance.getToolbarColor()
        this.overflowIcon?.setTint(color)
        this.navigationIcon?.setTint(color)
    }
}