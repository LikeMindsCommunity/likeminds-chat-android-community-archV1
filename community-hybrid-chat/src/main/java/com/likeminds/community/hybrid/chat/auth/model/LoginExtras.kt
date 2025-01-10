package com.likeminds.community.hybrid.chat.auth.model

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class LoginExtras(
    var isGuest: Boolean = false,
    var userName: String,
    var userId: String? = null,
) : Parcelable