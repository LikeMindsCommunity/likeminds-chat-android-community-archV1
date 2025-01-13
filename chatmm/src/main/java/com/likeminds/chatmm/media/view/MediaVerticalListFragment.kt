package com.likeminds.chatmm.media.view

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.create.view.adapter.ChatroomItemAdapter
import com.likeminds.chatmm.databinding.FragmentMediaVerticalListBinding
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.utils.customview.BaseFragment
import com.likeminds.chatmm.utils.model.*
import javax.inject.Inject

class MediaVerticalListFragment :
    BaseFragment<FragmentMediaVerticalListBinding, Nothing>() {

    private var mediaExtras: MediaExtras? = null

    @Inject
    lateinit var userPreferences: UserPreferences

    companion object {
        const val TAG = "MediaVerticalListFragment"
        const val BUNDLE_MEDIA_VERTICAL = "bundle of media vertical"

        @JvmStatic
        fun getInstance(extras: MediaExtras): MediaVerticalListFragment {
            val fragment = MediaVerticalListFragment()
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_MEDIA_VERTICAL, extras)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getViewModelClass(): Class<Nothing>? {
        return null
    }

    override fun getViewBinding(): FragmentMediaVerticalListBinding {
        return FragmentMediaVerticalListBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().mediaComponent()?.inject(this)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        if (arguments == null) return
        mediaExtras = MediaVerticalListFragmentArgs.fromBundle(requireArguments()).mediaExtras
    }

    override fun setUpViews() {
        super.setUpViews()
        setTheme()
        initImageListView()

        binding.tvTitle.text = mediaExtras?.title
        binding.tvSubTitle.text = getSubTitleText()
        binding.tvDate.text = mediaExtras?.subtitle

        binding.btnBack.setOnClickListener { activity?.finish() }
    }

    private fun setTheme() {
        binding.toolbarColor = LMChatAppearance.getToolbarColor()
        binding.headerColor = LMChatAppearance.getHeaderColor()
    }

    private fun initImageListView() {
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = RecyclerView.VERTICAL
        binding.rvImageList.layoutManager = linearLayoutManager
        val chatroomDocumentsItemAdapter = ChatroomItemAdapter(userPreferences)
        binding.rvImageList.adapter = chatroomDocumentsItemAdapter
        val attachmentList = mediaExtras?.attachments?.map { attachment ->
            val viewType = when {
                attachment.type == VIDEO -> ITEM_VIDEO_EXPANDED
                attachment.type == PDF -> ITEM_PDF_EXPANDED
                else -> ITEM_IMAGE_EXPANDED
            }
            return@map attachment.toBuilder()
                .dynamicType(viewType)
                .attachments(mediaExtras?.attachments)
                .communityId(mediaExtras?.communityId)
                .build()
        }?.sortedBy { it.index }
        if (!attachmentList.isNullOrEmpty()) {
            chatroomDocumentsItemAdapter.replace(attachmentList)
        }
    }

    private fun getSubTitleText(): CharSequence {
        val subTitleBuilder = StringBuilder()
        val imageCount = mediaExtras?.attachments?.filter { it.type == IMAGE }?.size ?: 0
        val gifCount = mediaExtras?.attachments?.filter { it.type == GIF }?.size ?: 0
        val videoCount = mediaExtras?.attachments?.filter { it.type == VIDEO }?.size ?: 0
        val pdfCount = mediaExtras?.attachments?.filter { it.type == PDF }?.size ?: 0
        if (imageCount > 0) {
            subTitleBuilder.append(imageCount)
            if (imageCount > 1) subTitleBuilder.append(" photos")
            else subTitleBuilder.append(" photo")
        }
        if (gifCount > 0) {
            subTitleBuilder.append(gifCount)
            if (gifCount > 1) subTitleBuilder.append(" gifs")
            else subTitleBuilder.append(" gif")
        }
        if (videoCount > 0) {
            if (subTitleBuilder.isNotEmpty()) subTitleBuilder.append(", ")
            subTitleBuilder.append(videoCount)
            if (videoCount > 1) subTitleBuilder.append(" videos")
            else subTitleBuilder.append(" video")
        }
        if (pdfCount > 0) {
            if (subTitleBuilder.isNotEmpty()) subTitleBuilder.append(", ")
            subTitleBuilder.append(pdfCount)
            if (pdfCount > 1) subTitleBuilder.append(" pdfs")
            else subTitleBuilder.append(" pdf")
        }
        return subTitleBuilder.toString()
    }
}