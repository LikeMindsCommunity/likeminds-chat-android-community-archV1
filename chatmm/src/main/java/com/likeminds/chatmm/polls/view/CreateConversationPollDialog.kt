package com.likeminds.chatmm.polls.view

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.create.view.adapter.ChatroomItemAdapter
import com.likeminds.chatmm.chatroom.create.view.adapter.CreatePollItemAdapterListener
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.chatroom.detail.model.ChatroomViewData
import com.likeminds.chatmm.databinding.DialogCreateConversationPollBinding
import com.likeminds.chatmm.databinding.ItemCreatePollBinding
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.polls.model.CreatePollViewData
import com.likeminds.chatmm.polls.model.PollViewData
import com.likeminds.chatmm.polls.viewmodel.CreatePollViewModel
import com.likeminds.chatmm.utils.*
import com.likeminds.chatmm.utils.ErrorUtil.emptyExtrasException
import com.likeminds.chatmm.utils.ValueUtils.isValidIndex
import com.likeminds.chatmm.utils.ViewUtils.collapse
import com.likeminds.chatmm.utils.ViewUtils.expand
import com.likeminds.chatmm.utils.ViewUtils.fetchColor
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.BaseBottomSheetFragment
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class CreateConversationPollDialog :
    BaseBottomSheetFragment<DialogCreateConversationPollBinding, CreatePollViewModel>(),
    CreatePollItemAdapterListener {
    companion object {
        private const val TAG = "CreateConversationPollDialog"
        private const val ARG_CHATROOM = "ARG_CHATROOM"
        private const val ARG_CHATROOM_EXTRAS = "ARG_CHATROOM_EXTRAS"
        private const val MAX_OPTION_COUNT = 10

        @JvmStatic
        fun show(
            fragmentManager: FragmentManager,
            chatroom: ChatroomViewData?,
            chatroomDetailExtras: ChatroomDetailExtras,
        ) {
            if (chatroom == null) {
                return
            }
            CreateConversationPollDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CHATROOM, chatroom)
                    putParcelable(ARG_CHATROOM_EXTRAS, chatroomDetailExtras)
                }
            }.show(fragmentManager, TAG)
        }
    }

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var sdkPreferences: SDKPreferences

    private lateinit var chatroomItemAdapter: ChatroomItemAdapter

    private lateinit var chatroom: ChatroomViewData
    private lateinit var chatroomExtras: ChatroomDetailExtras

    override fun getViewModelClass(): Class<CreatePollViewModel> {
        return CreatePollViewModel::class.java
    }

    override fun getViewBinding(): DialogCreateConversationPollBinding {
        return DialogCreateConversationPollBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().pollsComponent()?.inject(this)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        chatroom = ExtrasUtil.getParcelable(
            arguments,
            ARG_CHATROOM,
            ChatroomViewData::class.java
        ) ?: throw emptyExtrasException(TAG)
        chatroomExtras = ExtrasUtil.getParcelable(
            requireArguments(),
            ARG_CHATROOM_EXTRAS,
            ChatroomDetailExtras::class.java
        ) ?: throw emptyExtrasException(TAG)
    }

    override fun setUpViews() {
        super.setUpViews()
        setTheme()
        initBottomSheetBehavior()
        initPollQuestionView()
        initListeners()
        initPollsRecyclerView()
        initializeExpireTimeView()
        initMultipleOptionDropdownView()
        initMultipleOptionNoDropdownView()
    }

    private fun setTheme() {
        binding.apply {
            ViewUtils.setThemeTint(switch = switchAddNewOptions)
            ViewUtils.setThemeTint(switch = switchAnonymousPoll)
            ViewUtils.setThemeTint(switch = switchLiveResults)

            btnPost.backgroundTintList = ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_enabled),
                    intArrayOf(-android.R.attr.state_enabled)
                ),
                intArrayOf(
                    LMChatAppearance.getButtonsColor(),
                    requireContext().fetchColor(R.color.lm_chat_black_14)
                )
            )
        }
    }

    override fun observeData() {
        super.observeData()
        viewModel.pollPosted.observe(viewLifecycleOwner) {
            ProgressHelper.hideProgress(binding.progressBar)
            if (it) {
                dismiss()
            } else {
                ViewUtils.showSomethingWentWrongToast(requireContext())
            }
        }
    }

    private fun initBottomSheetBehavior() {
        val bottomSheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )
        if (bottomSheet != null) {
            val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            bottomSheetBehavior.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        binding.groupToolbar.visibility = View.VISIBLE
                        binding.viewBar.visibility = View.GONE
                    } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        binding.groupToolbar.visibility = View.GONE
                        binding.viewBar.visibility = View.VISIBLE
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        }
    }

    private fun initPollQuestionView() {
        binding.etPollQuestion.addTextChangedListener {
            validateCreatePollButton(viewModel.getPollOptionsSize())
        }
    }

    private fun initListeners() {
        binding.tvCancel.setOnClickListener {
            showConfirmationDialog()
        }

        binding.tvAdvanced.setOnClickListener {
            if (binding.advancedOptionVisible) {
                binding.advancedOptionVisible = false
                binding.tvAdvanced.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.lm_chat_ic_arrow_down,
                    0
                )
                binding.constraintLayoutAdvanced.collapse()
            } else {
                binding.advancedOptionVisible = true
                binding.tvAdvanced.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.lm_chat_ic_arrow_up,
                    0
                )
                binding.constraintLayoutAdvanced.expand()
            }
        }

        binding.btnPost.setOnClickListener {
            if (userPreferences.getIsGuestUser()) {
                SDKApplication.getLikeMindsCallback()?.login()
                activity?.finish()
            } else {
                createPollConversation()
            }
        }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogTheme)
            .setTitle(getString(R.string.lm_chat_discard_poll))
            .setMessage(getString(R.string.lm_chat_discard_poll_message))
            .setPositiveButton(getString(R.string.lm_chat_discard)) { dialog, _ ->
                dialog.dismiss()
                this.dismiss()
            }
            .setNegativeButton(getString(R.string.lm_chat_cancel), null)
            .show()
    }

    private fun initPollsRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = RecyclerView.VERTICAL
        binding.rvPollOptions.layoutManager = linearLayoutManager
        chatroomItemAdapter =
            ChatroomItemAdapter(userPreferences, createPollItemAdapterListener = this)
        binding.rvPollOptions.adapter = chatroomItemAdapter
        chatroomItemAdapter.replace(viewModel.getInitialPollViewDataList())
        binding.tvAddOptions.setOnClickListener {
            addPoll()
        }
        updateItemViewCacheSize()
    }

    private fun addPoll() {
        chatroomItemAdapter.add(
            chatroomItemAdapter.items().size,
            viewModel.getInitialPollViewData()
        )
        initMultipleOptionNoDropdownView()
        updateItemViewCacheSize()
    }

    private fun updateItemViewCacheSize() {
        binding.rvPollOptions.setItemViewCacheSize(chatroomItemAdapter.items().size)
    }

    private fun initializeExpireTimeView() {
        binding.tvPollExpireTime.setOnClickListener {
            val datePickerFragment = DatePickerFragment({ _, year, month, dayOfMonth ->
                run {
                    val timePickerFragment = TimePickerFragment { _, hourOfDay, minute ->
                        run {
                            val simpleDateFormat = SimpleDateFormat(
                                "dd-MM-yy HH:mm", Locale.getDefault()
                            )
                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.YEAR, year)
                            calendar.set(Calendar.MONTH, month)
                            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)
                            if (calendar.time.after(Date())) {
                                binding.tvPollExpireTime.text =
                                    simpleDateFormat.format(calendar.time)
                                viewModel.setEndDate(calendar.timeInMillis)
                            } else {
                                showPastExpiryDateSelectedError()
                            }
                        }
                    }
                    activity?.supportFragmentManager?.let { fragmentManager ->
                        timePickerFragment.show(fragmentManager, TimePickerFragment.TAG)
                    }
                }
            }, minimumDate = Date().time)
            activity?.supportFragmentManager?.let { fragmentManager ->
                datePickerFragment.show(fragmentManager, DatePickerFragment.TAG)
            }
        }
    }

    private fun initMultipleOptionDropdownView() {
        ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_dropdown,
            viewModel.getMultipleOptionStateList(),
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
            binding.spinnerMultipleOption.adapter = adapter
            binding.spinnerMultipleOption.setSelection(0)
        }
    }

    private fun initMultipleOptionNoDropdownView() {
        ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_dropdown,
            viewModel.getMultipleOptionNoList().subList(0, chatroomItemAdapter.itemCount + 1)
        ).also { adapter ->
            adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
            binding.spinnerMultipleOptionValue.adapter = adapter
            binding.spinnerMultipleOptionValue.setSelection(0)
        }
    }

    private fun createPollConversation() {
        ViewUtils.hideKeyboard(requireActivity())
        val answerText = binding.etPollQuestion.text.toString()

        val endDate = viewModel.endDate
        if (endDate == null) {
            showErrorMessage("Please select poll expiry time")
            return
        } else if (Date(endDate).before(Date())) {
            showPastExpiryDateSelectedError()
            return
        }
        val polls: ArrayList<PollViewData> = arrayListOf()
        val pollItemBindings = viewModel.getCreatePollItemBindingMap()
        pollItemBindings.forEach {
            val pollItemBinding = it.value
            val pollText = pollItemBinding.etPoll.text.toString()
            if (pollText.isNotBlank()) {
                val poll = getPoll(
                    pollItemBinding.createPollViewData, pollText
                )
                if (poll != null) polls.add(poll)
            }
        }
        if (polls.size != pollItemBindings.size || polls.size < 1) {
            showErrorMessage("Please enter poll option")
            return
        }
        val containsSimilarText = polls.map {
            it.toBuilder().text(it.text.lowercase()).build()
        }.groupBy {
            it.text
        }.values.firstOrNull {
            it.size > 1
        } != null
        if (containsSimilarText) {
            showErrorMessage("Please avoid duplicate options")
            return
        }
        var multipleOptionValue: String? = null
        var multipleOption: String? = null
        var isLiveResults = true
        var isAnonymousPoll = false
        var isAddNewOption = false

        if (binding.advancedOptionVisible) {
            val multipleSelectEnabled = binding.spinnerMultipleOptionValue.selectedItemPosition != 0
            if (multipleSelectEnabled) {
                multipleOptionValue =
                    binding.spinnerMultipleOptionValue.selectedItem.toString()
                        .split(" ")[0]
                multipleOption = binding.spinnerMultipleOption.selectedItem.toString()

                when (multipleOption) {
                    CreatePollViewModel.MULTIPLE_OPTION_STATE_EXACTLY, CreatePollViewModel.MULTIPLE_OPTION_STATE_LEAST -> {
                        if (polls.size < multipleOptionValue.toInt()) {
                            showErrorMessage("Please enter at-least $multipleOptionValue poll option")
                            return
                        }
                    }
                }
            }
            isLiveResults = !binding.switchLiveResults.isChecked
            isAnonymousPoll = binding.switchAnonymousPoll.isChecked
            isAddNewOption = binding.switchAddNewOptions.isChecked
        }

        binding.btnPost.isEnabled = false
        ProgressHelper.showProgress(binding.progressBar)
        viewModel.createPollConversation(
            chatroom,
            answerText,
            polls,
            multipleOptionValue,
            multipleOption,
            isLiveResults,
            isAnonymousPoll,
            isAddNewOption
        )
    }

    private fun getPoll(
        createPollViewData: CreatePollViewData?,
        pollText: String,
    ): PollViewData? {
        if (createPollViewData == null) {
            return null
        }
        return PollViewData.Builder()
            .text(pollText)
            .subText(createPollViewData.subText)
            .build()
    }

    private fun showPastExpiryDateSelectedError() {
        ViewUtils.showShortToast(
            requireContext(),
            getString(R.string.lm_chat_please_select_future_date_as_poll_expire_time)
        )
    }

    private fun showErrorMessage(message: String) {
        ViewUtils.showShortToast(requireContext(), message)
    }

    private fun validateCreatePollButton(optionsSize: Int) {
        binding.btnPost.isEnabled = optionsSize >= 2 && !binding.etPollQuestion.text.isNullOrEmpty()
        if (optionsSize == MAX_OPTION_COUNT) {
            binding.tvAddOptions.hide()
        } else {
            binding.tvAddOptions.show()
        }
    }

    override fun pollCrossed(createPollViewData: CreatePollViewData) {
        val index = chatroomItemAdapter.items().indexOf(createPollViewData)
        if (index.isValidIndex()) {
            chatroomItemAdapter.removeIndex(index)
            val optionsSize = viewModel.removeItemCreatePollBinding(index)
            validateCreatePollButton(optionsSize)
        }
    }

    override fun addPollItemBinding(
        position: Int,
        itemCreatePollBinding: ItemCreatePollBinding,
    ) {
        val optionsSize =
            viewModel.addItemCreatePollBinding(position, itemCreatePollBinding)
        validateCreatePollButton(optionsSize)
    }
}