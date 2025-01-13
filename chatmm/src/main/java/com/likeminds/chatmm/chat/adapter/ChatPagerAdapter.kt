package com.likeminds.chatmm.chat.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.likeminds.chatmm.chat.view.CommunityHybridChatFragment
import com.likeminds.chatmm.dm.view.NetworkingChatFragment
import com.likeminds.chatmm.homefeed.view.CommunityChatFragment

class ChatPagerAdapter(
    private val fragment: CommunityHybridChatFragment,
) : FragmentStateAdapter(fragment) {

    companion object {
        const val LIST_SIZE = 2
    }

    override fun getItemCount(): Int {
        return LIST_SIZE
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                CommunityChatFragment.getInstance()
            }

            1 -> {
                NetworkingChatFragment.getInstance(fragment.dmMeta)
            }

            else -> {
                throw IndexOutOfBoundsException()
            }
        }
    }
}