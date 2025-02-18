package com.likeminds.chatmm.homefeed.view

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.databinding.DialogFragmentChatroomInviteBinding
import com.likeminds.chatmm.homefeed.model.ChatroomInviteDialogExtras
import com.likeminds.chatmm.utils.ErrorUtil
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.chatmm.utils.customview.BaseDialogFragment
import com.likeminds.likemindschat.chatroom.model.ChannelInviteStatus
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

class JoinChatroomInviteDialogFragment :
    BaseDialogFragment<DialogFragmentChatroomInviteBinding>() {

    private lateinit var joinChatroomInviteDialogListener: JoinChatroomInviteDialogListener

    private lateinit var chatroomInviteDialogExtras: ChatroomInviteDialogExtras

    companion object {
        private const val TAG = "JoinChatroomInviteDialogFragment"
        private const val JOIN_CHATROOM_INVITE_EXTRAS = "JOIN_CHATROOM_INVITE_EXTRAS"

        @JvmStatic
        fun showDialog(
            supportFragmentManager: FragmentManager,
            chatroomInviteDialogExtras: ChatroomInviteDialogExtras
        ) {
            JoinChatroomInviteDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(JOIN_CHATROOM_INVITE_EXTRAS, chatroomInviteDialogExtras)
                }
            }.show(supportFragmentManager, TAG)
        }
    }

    override fun getViewBinding(): DialogFragmentChatroomInviteBinding {
        return DialogFragmentChatroomInviteBinding.inflate(layoutInflater)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            joinChatroomInviteDialogListener = parentFragment as JoinChatroomInviteDialogListener
        } catch (e: ClassCastException) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.EMERGENCY
            )
            throw ClassCastException("Calling fragment must implement JoinChatroomInviteDialogListener interface")
        }
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)

        chatroomInviteDialogExtras = ExtrasUtil.getParcelable(
            arguments,
            JOIN_CHATROOM_INVITE_EXTRAS,
            ChatroomInviteDialogExtras::class.java
        ) ?: throw ErrorUtil.emptyExtrasException(TAG)
    }

    override fun setUpViews() {
        super.setUpViews()

        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()

            tvTitle.text = chatroomInviteDialogExtras.chatroomInviteDialogTitle
            tvChatroomInviteDescription.text =
                chatroomInviteDialogExtras.chatroomInviteDialogSubtitle

            tvCancel.setOnClickListener {
                this@JoinChatroomInviteDialogFragment.dismiss()
            }

            tvConfirm.setOnClickListener {
                joinChatroomInviteDialogListener.onChatroomInviteDialogConfirmed(
                    chatroomInviteDialogExtras.chatroomId,
                    chatroomInviteDialogExtras.channelInviteStatus
                )
                this@JoinChatroomInviteDialogFragment.dismiss()
            }
        }
    }
}

interface JoinChatroomInviteDialogListener {
    fun onChatroomInviteDialogConfirmed(
        invitedChatroomId: String,
        channelInviteStatus: ChannelInviteStatus
    )
}