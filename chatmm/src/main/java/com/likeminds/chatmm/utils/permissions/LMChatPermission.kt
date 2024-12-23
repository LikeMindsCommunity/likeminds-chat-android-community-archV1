package com.likeminds.chatmm.utils.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import com.likeminds.chatmm.R
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.utils.permissions.model.LMChatPermissionExtras

class LMChatPermission private constructor(
    val permissionName: String,
    val requestCode: Int,
    val preDialogMessage: String,
    val deniedDialogMessage: String,
    @param:DrawableRes @field:DrawableRes
    val dialogImage: Int
) {
    companion object {

        private const val WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE
        private const val RECORD_AUDIO = Manifest.permission.RECORD_AUDIO

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private const val POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private const val READ_MEDIA_VIDEO = Manifest.permission.READ_MEDIA_VIDEO

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private const val READ_MEDIA_IMAGES = Manifest.permission.READ_MEDIA_IMAGES

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private const val READ_MEDIA_AUDIO = Manifest.permission.READ_MEDIA_AUDIO

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        const val READ_MEDIA_VISUAL_USER_SELECTED =
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED

        private const val REQUEST_STORAGE = 10102
        private const val REQUEST_RECORD_AUDIO = 10103
        private const val REQUEST_NOTIFICATIONS = 10105
        const val REQUEST_GALLERY = 10106
        private const val REQUEST_AUDIO = 10107

        fun getStoragePermissionData(): LMChatPermission {
            return LMChatPermission(
                WRITE_STORAGE,
                REQUEST_STORAGE,
                "To easily receive and send photos, videos and other files, allow CommunityHood access to your device’s photos, media and files.",
                "To send media, allow CommunityHood access to your device’s photos, media and files. Tap on Settings > Permission, and turn Storage on.",
                R.drawable.lm_chat_ic_folder
            )
        }

        fun getRecordAudioPermissionData(): LMChatPermission {
            return LMChatPermission(
                RECORD_AUDIO,
                REQUEST_RECORD_AUDIO,
                "To record a Voice Message in a chatroom, allow access to your microphone",
                "To record a Voice Message in a chatroom, access to your microphone. Tap Settings > Permissions, and turn Microphone on.",
                R.drawable.lm_chat_ic_mic
            )
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun getMediaPermissionExtrasByMediaCount(
            context: Context,
            isImageVideo: Boolean
        ): LMChatPermissionExtras {
            return if (isImageVideo) {
                getGalleryPermissionExtras(context)
            } else {
                getAudioPermissionExtras(context)
            }
        }

        // returns the [PermissionExtras] as per the required permission
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun getMediaPermissionExtras(
            context: Context,
            mediaTypes: List<String>
        ): LMChatPermissionExtras {
            return when {
                (mediaTypes.contains(IMAGE)
                        || mediaTypes.contains(VIDEO)) -> {
                    getGalleryPermissionExtras(context)
                }

                mediaTypes.contains(AUDIO) -> {
                    getAudioPermissionExtras(context)
                }

                else -> {
                    getGalleryPermissionExtras(context)
                }
            }
        }

        // returns the [PermissionExtras] for gallery permissions request
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun getGalleryPermissionExtras(context: Context): LMChatPermissionExtras {
            val permissionsArray =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    arrayOf(
                        READ_MEDIA_IMAGES,
                        READ_MEDIA_VIDEO,
                        READ_MEDIA_VISUAL_USER_SELECTED
                    )
                } else {
                    arrayOf(
                        READ_MEDIA_VIDEO,
                        READ_MEDIA_IMAGES
                    )
                }

            return LMChatPermissionExtras.Builder()
                .permissions(permissionsArray)
                .requestCode(REQUEST_GALLERY)
                .preDialogMessage(context.getString(R.string.lm_chat_pre_gallery_media_permission_dialog_message))
                .deniedDialogMessage(context.getString(R.string.lm_chat_denied_gallery_media_permission_dialog_message))
                .dialogImage(R.drawable.lm_chat_ic_folder)
                .build()
        }

        // returns the [PermissionExtras] for audio permissions request
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun getAudioPermissionExtras(context: Context): LMChatPermissionExtras {
            return LMChatPermissionExtras.Builder()
                .permissions(
                    arrayOf(
                        READ_MEDIA_AUDIO
                    )
                )
                .requestCode(REQUEST_AUDIO)
                .preDialogMessage(context.getString(R.string.lm_chat_pre_audio_media_permission_dialog_message))
                .deniedDialogMessage(context.getString(R.string.lm_chat_denied_audio_media_permission_dialog_message))
                .dialogImage(R.drawable.lm_chat_ic_audio_header)
                .build()
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun getNotificationPermissionData(context: Context): LMChatPermissionExtras {
            return LMChatPermissionExtras.Builder()
                .permissions(
                    arrayOf(POST_NOTIFICATIONS)
                )
                .requestCode(REQUEST_NOTIFICATIONS)
                .preDialogMessage(context.getString(R.string.lm_chat_pre_notification_dialog_message))
                .deniedDialogMessage(context.getString(R.string.lm_chat_denied_notification_dialog_message))
                .dialogImage(R.drawable.lm_chat_ic_bell)
                .build()
        }
    }
}
