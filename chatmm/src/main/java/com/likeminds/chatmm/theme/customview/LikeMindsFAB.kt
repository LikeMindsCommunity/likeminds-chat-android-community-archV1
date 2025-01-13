package com.likeminds.chatmm.theme.customview

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMChatAppearance

class LikeMindsFAB : FloatingActionButton {
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
        val array = context.obtainStyledAttributes(attrs, R.styleable.LikeMindsFAB)
        val fabType = array.getString(R.styleable.LikeMindsFAB_fab_type)

        // color
        if (!fabType.equals("normal")) {
            this.backgroundTintList = ColorStateList.valueOf(LMChatAppearance.getButtonsColor())
        }

        array.recycle()
    }
}