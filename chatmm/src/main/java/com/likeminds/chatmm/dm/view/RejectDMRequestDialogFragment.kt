package com.likeminds.chatmm.dm.view

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.likeminds.chatmm.databinding.DialogFragmentRejectDmRequestBinding
import com.likeminds.chatmm.utils.customview.BaseDialogFragment
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

class RejectDMRequestDialogFragment :
    BaseDialogFragment<DialogFragmentRejectDmRequestBinding>() {

    private lateinit var rejectDMRequestDialogListener: RejectDMRequestDialogListener

    companion object {
        private const val TAG = "RejectDMRequestDialogListener"

        @JvmStatic
        fun showDialog(
            supportFragmentManager: FragmentManager
        ) {
            RejectDMRequestDialogFragment().show(supportFragmentManager, TAG)
        }
    }

    override fun getViewBinding(): DialogFragmentRejectDmRequestBinding {
        return DialogFragmentRejectDmRequestBinding.inflate(layoutInflater)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            rejectDMRequestDialogListener =
                parentFragment as RejectDMRequestDialogListener
        } catch (e: ClassCastException) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.EMERGENCY
            )
            throw ClassCastException("Calling fragment must implement RejectDMRequestDialogListener interface")
        }
    }

    override fun setUpViews() {
        super.setUpViews()
        initializeListeners()
    }

    private fun initializeListeners() {
        binding.apply {
            tvCancel.setOnClickListener {
                this@RejectDMRequestDialogFragment.dismiss()
            }

            tvReject.setOnClickListener {
                rejectDMRequestDialogListener.rejectDMRequest()
                this@RejectDMRequestDialogFragment.dismiss()
            }

            tvReportReject.setOnClickListener {
                rejectDMRequestDialogListener.reportAndRejectDMRequest()
                this@RejectDMRequestDialogFragment.dismiss()
            }
        }
    }
}

interface RejectDMRequestDialogListener {
    fun rejectDMRequest()
    fun reportAndRejectDMRequest()
}