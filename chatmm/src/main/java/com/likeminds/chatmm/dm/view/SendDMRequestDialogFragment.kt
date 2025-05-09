package com.likeminds.chatmm.dm.view

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.databinding.DialogFragmentSendDmRequestBinding
import com.likeminds.chatmm.utils.customview.BaseDialogFragment
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import org.json.JSONObject

class SendDMRequestDialogFragment :
    BaseDialogFragment<DialogFragmentSendDmRequestBinding>() {

    private lateinit var sendDMRequestDialogListener: SendDMRequestDialogListener

    companion object {
        private const val TAG = "SendDMRequestDialogFragment"
        private var inputText: String = ""
        private var replyPrivatelyMetadata: Pair<JSONObject, ConversationViewData>? = null

        @JvmStatic
        fun showDialog(
            supportFragmentManager: FragmentManager,
            inputText: String,
            replyPrivatelyMetadata: Pair<JSONObject, ConversationViewData>? = null
        ) {
            this.inputText = inputText
            this.replyPrivatelyMetadata = replyPrivatelyMetadata
            SendDMRequestDialogFragment().show(supportFragmentManager, TAG)
        }
    }

    override fun getViewBinding(): DialogFragmentSendDmRequestBinding {
        return DialogFragmentSendDmRequestBinding.inflate(layoutInflater)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            sendDMRequestDialogListener = parentFragment as SendDMRequestDialogListener
        } catch (e: ClassCastException) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.EMERGENCY
            )
            throw ClassCastException("Calling fragment must implement SendDMRequestDialogListener interface")
        }
    }

    override fun setUpViews() {
        super.setUpViews()
        initializeListeners()
    }

    private fun initializeListeners() {
        binding.apply {
            tvCancel.setOnClickListener {
                this@SendDMRequestDialogFragment.dismiss()
            }

            tvConfirm.setOnClickListener {
                sendDMRequestDialogListener.sendDMRequest(inputText, replyPrivatelyMetadata)
                this@SendDMRequestDialogFragment.dismiss()
            }
        }
    }
}

interface SendDMRequestDialogListener {
    fun sendDMRequest(requestText: String, replyPrivatelyMetadata: Pair<JSONObject, ConversationViewData>?)
}