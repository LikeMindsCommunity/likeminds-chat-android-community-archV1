package com.likeminds.chatmm.media.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.likeminds.chatmm.utils.membertagging.model.MemberTaggingExtras
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.theme.customview.edittext.LikeMindsEditTextListener
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.detail.viewmodel.HelperViewModel
import com.likeminds.chatmm.databinding.FragmentConversationGifSendBinding
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.viewmodel.MediaViewModel
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.utils.ProgressHelper
import com.likeminds.chatmm.utils.customview.BaseFragment
import com.likeminds.chatmm.utils.databinding.ImageBindingUtil
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.chatmm.utils.membertagging.util.MemberTaggingUtil
import com.likeminds.chatmm.utils.membertagging.util.MemberTaggingViewListener
import com.likeminds.chatmm.utils.membertagging.view.MemberTaggingView
import javax.inject.Inject

class ConversationGifSendFragment :
    BaseFragment<FragmentConversationGifSendBinding, MediaViewModel>() {

    private lateinit var mediaExtras: MediaExtras
    private var singleUriData: SingleUriData? = null

    private lateinit var memberTagging: MemberTaggingView

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var helperViewModel: HelperViewModel

    companion object {
        const val TAG = "ConversationGifSendFragment"
        private const val BUNDLE_CONVERSATION_GIF = "bundle of conversation gif"

        @JvmStatic
        fun getInstance(extras: MediaExtras): ConversationGifSendFragment {
            val fragment = ConversationGifSendFragment()
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_CONVERSATION_GIF, extras)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getViewModelClass(): Class<MediaViewModel> {
        return MediaViewModel::class.java
    }

    override fun getViewBinding(): FragmentConversationGifSendBinding {
        return FragmentConversationGifSendBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().mediaComponent()?.inject(this)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        mediaExtras =
            ConversationGifSendFragmentArgs.fromBundle(requireArguments()).mediaExtras
    }

    override fun setUpViews() {
        super.setUpViews()
        setTheme()
        singleUriData = mediaExtras.mediaUris?.firstOrNull()
        initViews()
        initMemberTagging()
        initRichEditorSupport()
        initGiphy()
    }

    override fun observeData() {
        super.observeData()

        // observes tagging data
        helperViewModel.taggingData.observe(viewLifecycleOwner) { result ->
            MemberTaggingUtil.setMembersInView(memberTagging, result)
        }
    }

    private fun setTheme() {
        binding.buttonColor = LMChatAppearance.getButtonsColor()
    }

    private fun initViews() {
        initGif()
        binding.btnBack.setOnClickListener {
            cancelSend()
        }
        binding.btnSend.setOnClickListener {
            if (userPreferences.getIsGuestUser()) {
                SDKApplication.getLikeMindsCallback()?.login()
                activity?.finish()
            } else {
                saveGifThumbnail()
            }
        }
    }

    private fun initMemberTagging() {
        memberTagging = binding.memberTaggingView
        memberTagging.initialize(
            MemberTaggingExtras.Builder()
                .editText(binding.etConversation)
                .darkMode(true)
                .color(
                    LMChatAppearance.getTextLinkColor()
                )
                .build()
        )

        memberTagging.addListener(object : MemberTaggingViewListener {
            override fun callApi(page: Int, searchName: String) {
                super.callApi(page, searchName)
                helperViewModel.getMembersForTagging(
                    mediaExtras.chatroomId,
                    page,
                    searchName
                )
            }
        })

        memberTagging.taggingEnabled = mediaExtras.isTaggingEnabled
    }

    private fun initGiphy() {
        mediaExtras.giphyMedia?.let {
            viewModel.getGiphyUri(requireContext(), it)
        }
        viewModel.giphyMedia.observe(viewLifecycleOwner) {
            it?.let { pair ->
                if (pair.first) {
                    ProgressHelper.showProgress(binding.progressBar)
                } else {
                    ProgressHelper.hideProgress(binding.progressBar)
                }
                pair.second?.let { uri ->
                    singleUriData = SingleUriData.Builder()
                        .uri(uri)
                        .fileType(GIF)
                        .build()
                    initGif()
                }
            }
        }
    }

    private fun initGif() {
        ImageBindingUtil.loadImage(binding.imageView, singleUriData?.uri)
    }

    private fun saveGifThumbnail() {
        singleUriData?.let {
            Glide.with(requireContext()).asGif().load(it.uri)
                .addListener(object : RequestListener<GifDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        initSendClick()
                        return true
                    }

                    override fun onResourceReady(
                        resource: GifDrawable,
                        model: Any,
                        target: Target<GifDrawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        val thumbnailUri = FileUtil.getUriFromBitmapWithRandomName(
                            requireContext(), resource.firstFrame, isPNGFormat = true
                        )
                        singleUriData = it.toBuilder().thumbnailUri(thumbnailUri).build()
                        initSendClick()
                        return true
                    }
                }).submit()
        } ?: initSendClick()
    }

    private fun initRichEditorSupport() {
        binding.etConversation.addListener(object : LikeMindsEditTextListener {
            override fun onMediaSelected(contentUri: Uri, mimeType: String) {
            }
        })
    }

    private fun initSendClick() {
        val text = memberTagging.replaceSelectedMembers(
            binding.etConversation.editableText
        )
        val extras = mediaExtras.toBuilder()
            .conversation(text)
            .mediaUris(arrayListOf(singleUriData!!))
            .build()
        val intent = Intent()
        intent.putExtra(
            MediaActivity.BUNDLE_MEDIA_EXTRAS,
            extras
        )
        activity?.setResult(Activity.RESULT_OK, intent)
        activity?.finish()
    }

    private fun cancelSend() {
        val intent = Intent()
        activity?.setResult(Activity.RESULT_CANCELED, intent)
        activity?.finish()
    }

}