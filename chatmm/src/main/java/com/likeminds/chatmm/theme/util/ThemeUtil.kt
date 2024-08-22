package com.likeminds.chatmm.theme.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMTheme

object ThemeUtil {
    /**
     * @param context - context to retrieve assets
     * @param fontStyle - style of font to be applied
     * @return Typeface? - typeface of current font as per the [fontStyle]
     * */
    fun getTypeFace(context: Context, fontStyle: String?): Typeface? {
        val currentFont = LMTheme.getCurrentFonts()

        val typeface = when (fontStyle) {
            "bold" -> {
                if (currentFont?.bold != null) {
                    ResourcesCompat.getFont(context,  currentFont.bold)
                } else {
                    ResourcesCompat.getFont(context, R.font.lm_chat_roboto_bold)
                }
            }
            "medium" -> {
                if (currentFont?.medium != null) {
                    ResourcesCompat.getFont(context, R.font.lm_chat_roboto_regular)
                } else {
                    ResourcesCompat.getFont(context, R.font.lm_chat_roboto_medium)
                }
            }
            "regular" -> {
                if (currentFont?.regular != null) {
                    ResourcesCompat.getFont(context, R.font.lm_chat_roboto_regular)
                } else {
                    ResourcesCompat.getFont(context, R.font.lm_chat_roboto_regular)
                }
            }
            else -> {
                ResourcesCompat.getFont(context, R.font.lm_chat_roboto_regular)
            }
        }
        return typeface
    }

    /**
     * @param defaultColor - color already set to the view
     * @param textType - type of text
     * @return Int - integer color
     * */
    fun getTextColor(defaultColor: Int, textType: String?): Int {
        val color = when (textType) {
            "title" -> {
                LMTheme.getToolbarColor()
            }
            "subtitle" -> {
                LMTheme.getSubtitleColor()
            }
            "special" -> {
                LMTheme.getButtonsColor()
            }
            else -> {
                defaultColor
            }
        }
        return color
    }
}