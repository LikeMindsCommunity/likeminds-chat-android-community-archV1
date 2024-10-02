package com.likeminds.chatmm.member.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class SDKClientInfoViewData private constructor(
    @SerializedName("community")
    val communityId: Int,
    @SerializedName("user")
    val user: String,
    @SerializedName("user_unique_id")
    val userUniqueId: String,
    @SerializedName("uuid")
    val uuid: String
) : Parcelable {
    class Builder {
        private var communityId: Int = 0
        private var user: String = ""
        private var userUniqueId: String = ""
        private var uuid: String = ""

        fun communityId(communityId: Int) = apply { this.communityId = communityId }
        fun user(user: String) = apply { this.user = user }
        fun userUniqueId(userUniqueId: String) = apply { this.userUniqueId = userUniqueId }
        fun uuid(uuid: String) = apply { this.uuid = uuid }

        fun build() = SDKClientInfoViewData(
            communityId,
            user,
            userUniqueId,
            uuid
        )
    }

    fun toBuilder(): Builder {
        return Builder().communityId(communityId)
            .user(user)
            .userUniqueId(userUniqueId)
            .uuid(uuid)
    }
}