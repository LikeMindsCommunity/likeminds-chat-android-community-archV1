package com.likeminds.chatmm.member.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_MEMBER
import com.likeminds.likemindschat.user.model.UserRole
import kotlinx.parcelize.Parcelize

@Parcelize
class MemberViewData private constructor(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    val dynamicViewType: Int,
    val state: Int,
    val communityId: String?,
    val communityName: String?,
    @SerializedName("is_guest")
    val isGuest: Boolean?,
    val isOwner: Boolean?,
    val hideBottomLine: Boolean?,
    val customTitle: String?,
    val customIntroText: String?,
    val customClickText: String?,
    val memberSince: String?,
    val listOfMenu: List<MemberActionViewData>?,
    val parentViewItemPosition: Int?,
    @SerializedName("updated_at")
    val updatedAt: Long?,
    @SerializedName("user_unique_id")
    val userUniqueId: String?,
    @SerializedName("sdk_client_info")
    val sdkClientInfo: SDKClientInfoViewData,
    @SerializedName("uuid")
    val uuid: String,
    val roles: List<UserRole>
) : Parcelable, BaseViewType {
    override val viewType: Int
        get() = dynamicViewType

    class Builder {
        private var id: String? = null
        private var name: String? = null
        private var imageUrl: String? = null
        private var dynamicViewType: Int = ITEM_MEMBER
        private var state: Int = STATE_NOTHING
        private var communityId: String? = null
        private var isGuest: Boolean? = null
        private var isOwner: Boolean? = null
        private var hideBottomLine: Boolean? = null
        private var customIntroText: String? = null
        private var customClickText: String? = null
        private var memberSince: String? = null
        private var communityName: String? = null
        private var customTitle: String? = null
        private var listOfMenu: List<MemberActionViewData>? = null
        private var parentViewItemPosition: Int? = null
        private var updatedAt: Long? = null
        private var userUniqueId: String? = null
        private var sdkClientInfo: SDKClientInfoViewData = SDKClientInfoViewData.Builder().build()
        private var uuid: String = ""
        private var roles: List<UserRole> = emptyList()

        fun id(id: String?) = apply { this.id = id }
        fun name(name: String?) = apply { this.name = name }
        fun imageUrl(imageUrl: String?) = apply { this.imageUrl = imageUrl }
        fun dynamicViewType(dynamicViewType: Int) = apply { this.dynamicViewType = dynamicViewType }

        fun state(state: Int) = apply { this.state = state }
        fun communityId(communityId: String?) = apply { this.communityId = communityId }
        fun communityName(communityName: String?) = apply { this.communityName = communityName }

        fun isGuest(isGuest: Boolean?) = apply { this.isGuest = isGuest }
        fun isOwner(isOwner: Boolean?) = apply { this.isOwner = isOwner }
        fun hideBottomLine(hideBottomLine: Boolean?) =
            apply { this.hideBottomLine = hideBottomLine }

        fun customTitle(customTitle: String?) = apply { this.customTitle = customTitle }
        fun customIntroText(customIntroText: String?) =
            apply { this.customIntroText = customIntroText }

        fun customClickText(customClickText: String?) =
            apply { this.customClickText = customClickText }

        fun memberSince(memberSince: String?) = apply { this.memberSince = memberSince }
        fun listOfMenu(listOfMenu: List<MemberActionViewData>?) =
            apply { this.listOfMenu = listOfMenu }

        fun parentViewItemPosition(parentViewItemPosition: Int?) =
            apply { this.parentViewItemPosition = parentViewItemPosition }

        fun updatedAt(updatedAt: Long?) = apply { this.updatedAt = updatedAt }

        fun userUniqueId(userUniqueId: String?) = apply { this.userUniqueId = userUniqueId }
        fun sdkClientInfo(sdkClientInfo: SDKClientInfoViewData) =
            apply { this.sdkClientInfo = sdkClientInfo }

        fun uuid(uuid: String) = apply { this.uuid = uuid }
        fun roles(roles: List<UserRole>) = apply { this.roles = roles }

        fun build() = MemberViewData(
            id,
            name,
            imageUrl,
            dynamicViewType,
            state,
            communityId,
            communityName,
            isGuest,
            isOwner,
            hideBottomLine,
            customTitle,
            customIntroText,
            customClickText,
            memberSince,
            listOfMenu,
            parentViewItemPosition,
            updatedAt,
            userUniqueId,
            sdkClientInfo,
            uuid,
            roles
        )
    }

    fun toBuilder(): Builder {
        return Builder().id(id)
            .name(name)
            .imageUrl(imageUrl)
            .dynamicViewType(dynamicViewType)
            .state(state)
            .communityId(communityId)
            .communityName(communityName)
            .isGuest(isGuest)
            .isOwner(isOwner)
            .hideBottomLine(hideBottomLine)
            .customTitle(customTitle)
            .customIntroText(customIntroText)
            .customClickText(customClickText)
            .memberSince(memberSince)
            .listOfMenu(listOfMenu)
            .parentViewItemPosition(parentViewItemPosition)
            .updatedAt(updatedAt)
            .userUniqueId(userUniqueId)
            .sdkClientInfo(sdkClientInfo)
            .uuid(uuid)
            .roles(roles)
    }
}