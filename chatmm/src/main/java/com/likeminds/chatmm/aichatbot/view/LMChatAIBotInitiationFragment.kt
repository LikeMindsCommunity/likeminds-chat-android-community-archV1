package com.likeminds.chatmm.aichatbot.view

import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.aichatbot.util.LMChatAIChatBotPreferences
import com.likeminds.chatmm.aichatbot.viewmodel.LMChatAIBotViewModel
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailActivity
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailFragment.Companion.SOURCE_AI_CHATBOT
import com.likeminds.chatmm.databinding.LmChatAiBotInitiationFragmentBinding
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.customview.BaseFragment
import com.likeminds.chatmm.utils.observeInLifecycle
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class LMChatAIBotInitiationFragment :
    BaseFragment<LmChatAiBotInitiationFragmentBinding, LMChatAIBotViewModel>() {

    @Inject
    lateinit var aiChatBotPreferences: LMChatAIChatBotPreferences

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().aiChatbotComponent()?.inject(this)
    }

    override fun getViewModelClass(): Class<LMChatAIBotViewModel> {
        return LMChatAIBotViewModel::class.java
    }

    override fun getViewBinding(): LmChatAiBotInitiationFragmentBinding {
        return LmChatAiBotInitiationFragmentBinding.inflate(layoutInflater)
    }

    override fun setUpViews() {
        super.setUpViews()

        viewModel.getAIChatbots()
    }

    override fun observeData() {
        super.observeData()

        viewModel.chatroomId.observe(viewLifecycleOwner) { chatroomId ->
            if (!chatroomId.isNullOrEmpty()) {
                aiChatBotPreferences.setChatroomIDWithAIChatbot(chatroomId)

                ChatroomDetailActivity.start(
                    requireContext(),
                    ChatroomDetailExtras.Builder()
                        .chatroomId(chatroomId)
                        .source(SOURCE_AI_CHATBOT)
                        .build()
                )

                requireActivity().finish()
            }
        }

        viewModel.errorMessageFlow.onEach { response ->
            when (response) {
                is LMChatAIBotViewModel.ErrorMessageEvent.CheckDMStatus -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is LMChatAIBotViewModel.ErrorMessageEvent.CreateDMChatroom -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is LMChatAIBotViewModel.ErrorMessageEvent.GetAIChatbots -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }
            }

        }.observeInLifecycle(viewLifecycleOwner)
    }
}