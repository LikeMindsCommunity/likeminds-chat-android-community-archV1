package com.likeminds.chatmm.theme.model

import android.graphics.Color
import androidx.core.graphics.toColorInt

// responsible for all the theme-related things like colors and fonts
object LMChatAppearance {

    private var headerColor: String = "#FFFFFF"
    private var buttonsColor: String = "#00897B"
    private var textLinkColor: String = "#007AFF"
    private var fonts: LMFonts? = null

    /**
     * @param lmChatAppearanceRequest - Object to set theme with colors and fonts
     * sets headerColor, buttonsColor, textLinkColor and fonts, used throughout the app
     * */
    fun setTheme(lmChatAppearanceRequest: LMChatAppearanceRequest) {
        headerColor = lmChatAppearanceRequest.headerColor
        buttonsColor = lmChatAppearanceRequest.buttonsColor
        textLinkColor = lmChatAppearanceRequest.textLinkColor
        fonts = lmChatAppearanceRequest.fonts
    }

    // returns button color
    fun getButtonsColor(): Int {
        return buttonsColor.toColorInt()
    }

    // returns header color
    fun getHeaderColor(): Int {
        return headerColor.toColorInt()
    }

    // returns text link color
    fun getTextLinkColor(): Int {
        return textLinkColor.toColorInt()
    }

    // returns toolbar color
    fun getToolbarColor(): Int {
        return if (headerColor == "#FFFFFF") {
            Color.BLACK
        } else {
            Color.WHITE
        }
    }

    // returns color of subtitle text
    fun getSubtitleColor(): Int {
        return if (headerColor == "#FFFFFF") {
            Color.GRAY
        } else {
            Color.WHITE
        }
    }

    // returns paths of the current fonts
    fun getCurrentFonts(): LMFonts? {
        return fonts
    }
}