package com.likeminds.chatmm.utils.permissions.view

import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.databinding.DialogPermissionBinding
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity
import com.likeminds.chatmm.utils.permissions.*
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.likeminds.chatmm.R
import com.likeminds.chatmm.utils.permissions.LMChatPermissionManager.Companion.REQUEST_CODE_SETTINGS_PERMISSION
import com.likeminds.chatmm.utils.permissions.model.LMChatPermissionExtras

class LMChatPermissionDialog(
    private val activity: BaseAppCompatActivity,
    private val task: LMChatPermissionTask,
    private val permission: LMChatPermission?,
    private val mode: Mode,
    private val permissionDeniedCallback: LMChatPermissionDeniedCallback?,
    private val permissionExtras: LMChatPermissionExtras? = null
) : Dialog(activity), View.OnClickListener {
    private val dialogPermissionBinding: DialogPermissionBinding =
        DialogPermissionBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(dialogPermissionBinding.root)

        //set theme to view
        dialogPermissionBinding.imageViewIcon.setBackgroundColor(LMChatAppearance.getButtonsColor())
        dialogPermissionBinding.textViewPositiveButton.setTextColor(LMChatAppearance.getButtonsColor())

        if (permission != null) {
            existingPermissionsDialog()
        } else {
            updatedPermissionsDialog()
        }
    }

    // handles permission dialog as per the existing flow
    private fun existingPermissionsDialog() {
        dialogPermissionBinding.apply {
            if (permission == null) {
                return@apply
            }

            imageViewIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    activity,
                    permission.dialogImage
                )
            )
            when (mode) {
                Mode.INIT -> {
                    textViewMessage.text = permission.preDialogMessage
                    textViewPositiveButton.text =
                        activity.getString(R.string.lm_chat_permission_continue)
                }
                Mode.DENIED -> {
                    textViewMessage.text = permission.deniedDialogMessage
                    textViewPositiveButton.text =
                        activity.getString(R.string.lm_chat_settings)
                }
            }
            textViewPositiveButton.setOnClickListener(this@LMChatPermissionDialog)
            textViewNegativeButton.setOnClickListener(this@LMChatPermissionDialog)
            if (permissionDeniedCallback != null) {
                setOnCancelListener { permissionDeniedCallback.onDeny() }
            }
        }
    }

    // handles permission dialog as per the updated flow with [PermissionExtras]
    private fun updatedPermissionsDialog() {
        dialogPermissionBinding.apply {
            if (permissionExtras == null) {
                return@apply
            }

            imageViewIcon.setImageDrawable(
                ContextCompat.getDrawable(
                    activity,
                    permissionExtras.dialogImage
                )
            )
            when (mode) {
                Mode.INIT -> {
                    textViewMessage.text = permissionExtras.preDialogMessage
                    textViewPositiveButton.text =
                        activity.getString(R.string.lm_chat_permission_continue)
                }
                Mode.DENIED -> {
                    textViewMessage.text = permissionExtras.deniedDialogMessage
                    textViewPositiveButton.text =
                        activity.getString(R.string.lm_chat_settings)
                }
            }
            textViewPositiveButton.setOnClickListener {
                positiveButtonClicked()
            }
            textViewNegativeButton.setOnClickListener {
                negativeButtonClicked()
            }
            if (permissionDeniedCallback != null) {
                setOnCancelListener { permissionDeniedCallback.onDeny() }
            }
        }
    }

    // handles positive button click for the new flow
    private fun positiveButtonClicked() {
        if (permissionExtras == null) {
            return
        }
        when (mode) {
            Mode.INIT ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.requestMultiplePermissions(
                        permissionExtras,
                        object : LMChatPermissionCallback {
                            override fun onGrant() {
                                task.doTask()
                            }

                            override fun onDeny() {
                                permissionDeniedCallback?.onDeny()
                            }
                        })
                }
            Mode.DENIED -> {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.packageName, null)
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivityForResult(intent, REQUEST_CODE_SETTINGS_PERMISSION)
            }
        }
        dismiss()
    }

    // handles negative button click for the new flow
    private fun negativeButtonClicked() {
        permissionDeniedCallback?.onCancel()
        dismiss()
    }

    override fun onClick(v: View) {
        if (permission == null) {
            return
        }

        when (v.id) {
            R.id.textViewPositiveButton -> {
                when (mode) {
                    Mode.INIT ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            activity.requestPermission(permission, object : LMChatPermissionCallback {
                                override fun onGrant() {
                                    task.doTask()
                                }

                                override fun onDeny() {
                                    permissionDeniedCallback?.onDeny()
                                }
                            })
                        }
                    Mode.DENIED -> {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", activity.packageName, null)
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        activity.startActivityForResult(intent, REQUEST_CODE_SETTINGS_PERMISSION)
                    }
                }
                dismiss()
            }
            R.id.textViewNegativeButton -> {
                permissionDeniedCallback?.onCancel()
                dismiss()
            }
        }
    }

    enum class Mode {
        INIT, DENIED
    }

    override fun onStart() {
        super.onStart()
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}