package com.likeminds.chatmm.theme.model

import androidx.annotation.FontRes

/**
 * variables here, hold path for the fonts
 **/
class LMFonts private constructor(
    @FontRes
    val regular: Int?,
    @FontRes
    val medium: Int?,
    @FontRes
    val bold: Int?
) {
    class Builder {
        @FontRes
        private var regular: Int? = null

        @FontRes
        private var medium: Int? = null

        @FontRes
        private var bold: Int? = null

        fun regular(@FontRes regular: Int?) = apply { this.regular = regular }
        fun medium(@FontRes medium: Int?) = apply { this.medium = medium }
        fun bold(@FontRes bold: Int?) = apply { this.bold = bold }

        fun build() = LMFonts(
            regular,
            medium,
            bold
        )
    }

    fun toBuilder(): Builder {
        return Builder().regular(regular)
            .medium(medium)
            .bold(bold)
    }
}
