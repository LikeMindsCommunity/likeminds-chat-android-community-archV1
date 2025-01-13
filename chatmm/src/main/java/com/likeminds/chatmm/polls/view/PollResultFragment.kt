package com.likeminds.chatmm.polls.view

import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.databinding.FragmentPollResultBinding
import com.likeminds.chatmm.databinding.LayoutPollResultTabBinding
import com.likeminds.chatmm.polls.model.PollInfoData
import com.likeminds.chatmm.polls.model.PollResultExtras
import com.likeminds.chatmm.polls.viewmodel.PollResultViewModel
import com.likeminds.chatmm.utils.ErrorUtil.emptyExtrasException
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.customview.BaseFragment

class PollResultFragment : BaseFragment<FragmentPollResultBinding, PollResultViewModel>() {

    override fun getViewModelClass(): Class<PollResultViewModel> {
        return PollResultViewModel::class.java
    }

    override fun getViewBinding(): FragmentPollResultBinding {
        return FragmentPollResultBinding.inflate(layoutInflater)
    }

    companion object {
        const val POLL_RESULT_EXTRA = "extra of Poll Result"
        const val TAG = "PollResultFragment"

        @JvmStatic
        fun getInstance(extra: PollResultExtras): PollResultFragment {
            val fragment = PollResultFragment()
            val bundle = Bundle()
            bundle.putParcelable(POLL_RESULT_EXTRA, extra)
            fragment.arguments = bundle
            return fragment
        }
    }

    private lateinit var extra: PollResultExtras
    private lateinit var tabAdapter: PollResultTabAdapter
    private lateinit var pollInfoData: PollInfoData
    private var chatroomId: String? = null

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().pollsComponent()?.inject(this)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        val extras = requireActivity().intent?.getBundleExtra("bundle")
        extra = ExtrasUtil.getParcelable(
            extras,
            PollResultsActivity.ARG_POLL_RESULTS,
            PollResultExtras::class.java
        ) ?: throw emptyExtrasException(TAG)
    }

    override fun setUpViews() {
        super.setUpViews()
        binding.tabHeader.setSelectedTabIndicatorColor(LMChatAppearance.getButtonsColor())
        initData()
        initListener()
    }

    private fun initListener() {
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    override fun observeData() {
        super.observeData()
        viewModel.pollInfoData.observe(viewLifecycleOwner) {
            if (it != null) {
                pollInfoData = it
                initTabLayout()
            }
        }

        viewModel.chatroomId.observe(viewLifecycleOwner) {
            chatroomId = it
        }
    }

    private fun initData() {
        if (extra.conversationId != null) {
            viewModel.getPollInfoDataFromConversation(extra.conversationId)
        }
    }

    private fun initTabLayout() {
        tabAdapter = PollResultTabAdapter(
            this,
            extra.communityId,
            pollInfoData,
            extra.conversationId,
            extra.chatroomId
        )

        binding.viewPager.apply {
            adapter = tabAdapter
        }

        val screenWidth = ViewUtils.getDeviceDimension(requireContext()).first
        TabLayoutMediator(
            binding.tabHeader,
            binding.viewPager
        ) { tab, position ->
            val pollOption = pollInfoData.pollViewDataList?.get(position)
            val tabView = LayoutPollResultTabBinding.inflate(layoutInflater)
            tabView.apply {
                clLayout.maxWidth = (screenWidth * 0.48).toInt()
                clLayout.minWidth = (screenWidth * 0.33).toInt()
                pollCount = pollOption?.noVotes.toString()
                pollText = pollOption?.text

                tab.customView = root
            }
        }.attach()


        val tab = if (extra.selectedPoll != -1) {
            binding.viewPager.setCurrentItem(extra.selectedPoll, true)
            binding.tabHeader.setScrollPosition(extra.selectedPoll, 0f, true)
            binding.tabHeader.getTabAt(extra.selectedPoll)
        } else {
            binding.tabHeader.getTabAt(0)
        }

        tab?.select()
        val firstViewAtTab = tab?.customView?.findViewById<TextView>(R.id.tv_poll_count)
        firstViewAtTab?.apply {
            setTextColor(LMChatAppearance.getButtonsColor())
            setTypeface(null, Typeface.BOLD)
        }

        binding.tabHeader.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val view = tab?.customView?.findViewById<TextView>(R.id.tv_poll_count) ?: return
                view.apply {
                    setTextColor(LMChatAppearance.getButtonsColor())
                    setTypeface(null, Typeface.BOLD)
                }

                val pollData = pollInfoData.pollViewDataList?.get(tab.position)
                viewModel.sendPollResultsToggled(
                    extra.communityId,
                    extra.communityName,
                    extra.chatroomId ?: chatroomId,
                    extra.conversationId,
                    pollData?.id,
                    pollData?.text
                )
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val view = tab?.customView?.findViewById<TextView>(R.id.tv_poll_count) ?: return
                view.apply {
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.lm_chat_grey))
                    setTypeface(null, Typeface.NORMAL)
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                val view = tab?.customView?.findViewById<TextView>(R.id.tv_poll_count) ?: return
                view.apply {
                    setTextColor(LMChatAppearance.getButtonsColor())
                    setTypeface(null, Typeface.BOLD)
                }
            }
        })
    }
}