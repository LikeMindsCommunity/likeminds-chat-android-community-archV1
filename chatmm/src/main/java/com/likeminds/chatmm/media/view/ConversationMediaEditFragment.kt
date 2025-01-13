package com.likeminds.chatmm.media.view

import android.app.Activity
import android.content.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.chatroom.create.view.adapter.ImageAdapter
import com.likeminds.chatmm.chatroom.create.view.adapter.ImageAdapterListener
import com.likeminds.chatmm.chatroom.detail.viewmodel.HelperViewModel
import com.likeminds.chatmm.databinding.FragmentConversationMediaEditBinding
import com.likeminds.chatmm.media.customviews.ColorSeekBar
import com.likeminds.chatmm.media.customviews.MediaEditMode
import com.likeminds.chatmm.media.customviews.MediaEditMode.*
import com.likeminds.chatmm.media.customviews.interfaces.CanvasListener
import com.likeminds.chatmm.media.customviews.interfaces.OnTrimVideoListener
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.util.MediaUtils
import com.likeminds.chatmm.media.view.MediaActivity.Companion.BUNDLE_MEDIA_EXTRAS
import com.likeminds.chatmm.media.viewmodel.MediaViewModel
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.theme.customview.edittext.LikeMindsEditTextListener
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.*
import com.likeminds.chatmm.utils.ValueUtils.getMediaType
import com.likeminds.chatmm.utils.customview.BaseFragment
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder
import com.likeminds.chatmm.utils.membertagging.model.MemberTaggingExtras
import com.likeminds.chatmm.utils.membertagging.util.MemberTaggingUtil
import com.likeminds.chatmm.utils.membertagging.util.MemberTaggingViewListener
import com.likeminds.chatmm.utils.membertagging.view.MemberTaggingView
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import javax.inject.Inject

class ConversationMediaEditFragment :
    BaseFragment<FragmentConversationMediaEditBinding, MediaViewModel>(),
    ImageAdapterListener,
    ColorSeekBar.OnColorChangeListener, CanvasListener {

    private lateinit var mediaExtras: MediaExtras

    private lateinit var imageAdapter: ImageAdapter

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var helperViewModel: HelperViewModel

    private lateinit var memberTagging: MemberTaggingView

    private var selectedPosition = 0
    private var selectedUri: SingleUriData? = null

    private var editMode: MediaEditMode? = null
    private var currentTextMode = -1
    private var currentTextSize = 1

    private val textSizes by lazy { MediaUtils.getTextSizes() }
    private val textTypefaces by lazy { MediaUtils.getTextTypeFaces(requireContext()) }
    private val textIcons by lazy { MediaUtils.getTextIcons() }

    companion object {
        const val TAG = "ConversationMediaEdit"
        private const val BUNDLE_CONVERSATION_MEDIA_EDIT = "bundle of conversation media edit"

        @JvmStatic
        fun getInstance(extras: MediaExtras): ConversationMediaEditFragment {
            val fragment = ConversationMediaEditFragment()
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_CONVERSATION_MEDIA_EDIT, extras)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getViewModelClass(): Class<MediaViewModel> {
        return MediaViewModel::class.java
    }

    override fun getViewBinding(): FragmentConversationMediaEditBinding {
        return FragmentConversationMediaEditBinding.inflate(layoutInflater)
    }

    override val keepBindingRetained: Boolean
        get() = true

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().mediaComponent()?.inject(this)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        if (arguments == null) return
        mediaExtras = ConversationMediaEditFragmentArgs.fromBundle(requireArguments()).mediaExtras
    }

    override fun handleResultListener() {
        super.handleResultListener()
        setFragmentResultListener(ImageCropFragment.REQUEST_KEY) { _, bundle ->
            val singleUriData = ExtrasUtil.getParcelable(
                bundle,
                ImageCropFragment.BUNDLE_ARG_URI,
                SingleUriData::class.java
            )
            initMedia(singleUriData)
            replaceItem(selectedPosition, singleUriData)
            if (singleUriData == null) {
                return@setFragmentResultListener
            }
            mediaSelected(
                selectedPosition,
                SmallMediaViewData.Builder().singleUriData(singleUriData).build()
            )
        }
    }

    override fun setUpViews() {
        super.setUpViews()
        setTheme()
        if (mediaExtras.isExternallyShared) {
            ProgressHelper.showProgress(binding.progressBar, true)
            viewModel.fetchExternallySharedUriData(
                requireContext(),
                mediaExtras.mediaUris!!.map { it.uri }
            )
        } else {
            initRVForMedias()
        }
        initRichEditorSupport()
        initMemberTagging()

        binding.colorSeekBar.setOnColorChangeListener(this)
        binding.canvas.setListener(this)

        MemberTaggingDecoder.decode(
            binding.etConversation,
            mediaExtras.text,
            LMChatAppearance.getTextLinkColor()
        )

        //hide add media button if only allowMultipleSelection is enabled.
        val allowMultipleSelect = mediaExtras.allowMultipleSelect
        binding.groupAllowMultipleSelection.isVisible = allowMultipleSelect

        binding.btnBack.setOnClickListener {
            val intent = Intent()
            viewModel.sendThirdPartyAbandoned(
                "image/video",
                mediaExtras.communityId.toString(),
                mediaExtras.communityName,
                mediaExtras.chatroomId.toString()
            )
            activity?.setResult(Activity.RESULT_CANCELED, intent)
            activity?.finish()
        }

        binding.btnAdd.setOnClickListener {
            val extra = LMChatMediaPickerExtras.Builder()
                .senderName(mediaExtras.chatroomName ?: "Chatroom")
                .mediaTypes(listOf(IMAGE, VIDEO))
                .build()
            val intent = LMChatMediaPickerActivity.getIntent(
                requireContext(),
                extra
            )
            pickerLauncher.launch(intent)
        }

        binding.btnSend.setOnClickListener {
            if (userPreferences.getIsGuestUser()) {
                SDKApplication.getLikeMindsCallback()?.login()
                activity?.finish()
            } else {
                if (editMode != null) {
                    saveBitmapAndReset(
                        true,
                        VideoTrimExtras.Builder().initiatedSendMessage(true).build()
                    )
                } else {
                    initSendClick()
                }
            }
        }

        binding.btnCropRotate.setOnClickListener {
            if (selectedUri == null) return@setOnClickListener
            saveBitmapAndReset(false)
            findNavController().navigate(
                ConversationMediaEditFragmentDirections.actionConversationMediaEditFragmentToImageCropFragment(
                    selectedUri!!
                )
            )
        }

        binding.btnUndo.setOnClickListener {
            binding.canvas.undo()
        }

        binding.btnText.setOnClickListener {
            if (editMode == TEXT) {
                changeTextMode()
            } else {
                if (editMode == DRAW) {
                    saveBitmapAndResetAndShowCanvas(TEXT)
                } else {
                    showCanvas(TEXT)
                }
                currentTextMode = -1
                changeTextMode()
                binding.etInput.visibility = View.VISIBLE
                binding.btnTextIncrease.visibility = View.VISIBLE
                binding.btnTextDecrease.visibility = View.VISIBLE
                binding.etInput.setTextColor(binding.colorSeekBar.getColor())
                ViewUtils.showKeyboard(requireContext(), binding.etInput)
                showColorChooser()
                showBackgroundDrawable(binding.colorSeekBar.getColor())
            }
        }

        binding.btnDraw.setOnClickListener {
            if (editMode == DRAW) {
                saveBitmapAndReset(false)
            } else {
                if (editMode == TEXT) {
                    addTextToCanvas()
                    saveBitmapAndResetAndShowCanvas(DRAW)
                } else {
                    showCanvas(DRAW)
                }
                showColorChooser()
                showBackgroundDrawable(binding.colorSeekBar.getColor())
            }
        }

        binding.btnTextIncrease.setOnClickListener {
            if (currentTextSize < textSizes.size - 1) {
                currentTextSize++
                changeTextSize()
            }
        }

        binding.btnTextDecrease.setOnClickListener {
            if (currentTextSize > 0) {
                currentTextSize--
                changeTextSize()
            }
        }

        KeyboardVisibilityEvent.setEventListener(
            requireActivity(), viewLifecycleOwner
        ) { isOpen ->
            if (!isOpen && binding.etInput.visibility == View.VISIBLE && editMode == TEXT) {
                addTextToCanvas()
            }
        }

        binding.btnDelete.setOnClickListener {
            deleteCurrentMedia()
        }
    }

    override fun observeData() {
        super.observeData()

        viewModel.updatedUriDataList.observe(viewLifecycleOwner) { dataList ->
            ProgressHelper.hideProgress(binding.progressBar)
            if (!dataList.isNullOrEmpty()) {
                mediaExtras =
                    mediaExtras.toBuilder().mediaUris(dataList as ArrayList<SingleUriData>).build()
                initRVForMedias()
            }
        }

        helperViewModel.taggingData.observe(viewLifecycleOwner) { result ->
            MemberTaggingUtil.setMembersInView(memberTagging, result)
        }
    }

    private fun setTheme() {
        binding.buttonColor = LMChatAppearance.getButtonsColor()
    }

    private fun initRichEditorSupport() {
        binding.etConversation.addListener(object : LikeMindsEditTextListener {
            override fun onMediaSelected(contentUri: Uri, mimeType: String) {
            }
        })
    }

    //result callback for new media pick from custom gallery
    private val pickerLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result?.resultCode == Activity.RESULT_OK && result.data != null) {
            val extras = result.data?.extras
            val mediaPickerResult = ExtrasUtil.getParcelable(
                extras,
                LMChatMediaPickerActivity.ARG_MEDIA_PICKER_RESULT,
                MediaPickerResult::class.java
            ) ?: return@registerForActivityResult
            when (mediaPickerResult.mediaPickerResultType) {
                MEDIA_RESULT_BROWSE -> {
                    val intent =
                        AndroidUtils.getExternalPickerIntent(
                            mediaPickerResult.mediaTypes,
                            mediaPickerResult.allowMultipleSelect,
                            mediaPickerResult.browseClassName
                        )
                    if (intent != null)
                        browsePickerLauncher.launch(intent)
                }

                MEDIA_RESULT_PICKED -> {
                    val mediaUris = MediaUtils.convertMediaViewDataToSingleUriData(
                        requireContext(), mediaPickerResult.medias
                    )
                    updateMediaUris(mediaUris)
                }
            }
        }
    }

    private fun deleteCurrentMedia() {
        mediaExtras.mediaUris?.removeAt(selectedPosition)

        if (mediaExtras.mediaUris?.size == 0) {
            binding.btnBack.performClick()
            return
        } else {
            if (selectedPosition == mediaExtras.mediaUris?.size) {
                selectedPosition -= 1
            }
            imageAdapter.replace(mediaExtras.mediaUris?.mapIndexed { index, singleMediaUri ->
                val isSelected = index == selectedPosition
                val smallMediaViewData = SmallMediaViewData.Builder().singleUriData(singleMediaUri)
                    .isSelected(isSelected).build()

                if (isSelected) {
                    selectedUri = smallMediaViewData.singleUriData
                }
                smallMediaViewData
            })

            initMedia(selectedUri)
        }
    }

    private fun initMedia(singleUriData: SingleUriData?) {
        if (singleUriData == null)
            return

        invalidateDeleteMediaIcon(mediaExtras.mediaUris?.size)
        invalidateEditMediaIcons(singleUriData.fileType)

        when (singleUriData.fileType) {
            VIDEO -> {
                binding.photoView.visibility = View.GONE
                binding.videoView.visibility = View.VISIBLE
                binding.videoView
                    .setOnTrimVideoListener(object : OnTrimVideoListener {
                        override fun onVideoRangeChanged() {
                            editMode = TRIM
                        }

                        override fun onTrimStarted() {
                            ProgressHelper.showProgress(binding.progressBar)
                        }

                        override fun getResult(uri: Uri, videoTrimExtras: VideoTrimExtras?) {
                            saveTrimVideo(uri)
                            ProgressHelper.hideProgress(binding.progressBar)

                            reset()
                            replaceItem(
                                selectedPosition,
                                SingleUriData.Builder().uri(uri)
                                    .fileType(uri.getMediaType(requireContext()) ?: "")
                                    .build()
                            )
                            if (videoTrimExtras?.initiatedSendMessage == true) {
                                initSendClick()
                            } else if (videoTrimExtras?.newMediaSelected == true) {
                                showUpdatedPositionData(
                                    videoTrimExtras.updatedMediaPosition!!,
                                    videoTrimExtras.updatedMediaData!!
                                )
                            }
                        }

                        override fun onFailed(videoTrimExtras: VideoTrimExtras?) {
                            ProgressHelper.hideProgress(binding.progressBar)
                            ViewUtils.showSomethingWentWrongToast(requireContext())
                        }

                        override fun cancelAction() {
                            ProgressHelper.hideProgress(binding.progressBar)
                        }

                        override fun onError(message: String) {
                            ProgressHelper.hideProgress(binding.progressBar)
                        }

                    })
                    .setVideoURI(singleUriData.uri)
                    .setVideoInformationVisibility(true)
            }

            IMAGE, GIF -> {
                binding.videoView.visibility = View.GONE
                binding.photoView.visibility = View.VISIBLE
                Glide.with(binding.photoView).load(singleUriData.uri).into(binding.photoView)
            }
        }
    }

    private fun initRVForMedias() {
        val mediaUris = mediaExtras.mediaUris.orEmpty()
        if (mediaUris.size == 1) {
            binding.rvMedias.visibility = View.GONE
        } else {
            initMediaDisplayRecyclerView(mediaUris)
        }
        if (mediaUris.isEmpty())
            return
        selectedUri = mediaUris.firstOrNull()
        initMedia(selectedUri)
    }

    private fun initMediaDisplayRecyclerView(mediaUris: List<SingleUriData>?) {
        binding.rvMedias.visibility = View.VISIBLE
        val linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.rvMedias.layoutManager = linearLayoutManager

        imageAdapter = ImageAdapter(this)
        binding.rvMedias.adapter = imageAdapter
        imageAdapter.replace(mediaUris?.mapIndexed { index, singleUriData ->
            val isSelected = index == 0
            SmallMediaViewData.Builder().singleUriData(singleUriData).isSelected(isSelected)
                .build()
        })
    }

    private fun invalidateEditMediaIcons(fileType: String?) {
        binding.btnCropRotate.isVisible = fileType != VIDEO
        binding.btnText.isVisible = fileType != VIDEO
        binding.btnDraw.isVisible = fileType != VIDEO
    }

    private fun invalidateDeleteMediaIcon(mediaFilesCount: Int?) {
        if (mediaFilesCount == 1) {
            binding.btnDelete.visibility = View.GONE
        } else {
            binding.btnDelete.visibility = View.VISIBLE
        }
    }

    override fun mediaSelected(position: Int, smallMediaViewData: SmallMediaViewData) {
        if (selectedPosition == position) return
        saveBitmapAndReset(
            false,
            VideoTrimExtras.Builder().newMediaSelected(true).updatedMediaPosition(position)
                .updatedMediaData(smallMediaViewData).build()
        )
        if (editMode != TRIM) {
            showUpdatedPositionData(position, smallMediaViewData)
        }
    }

    private fun showUpdatedPositionData(position: Int, smallMediaViewData: SmallMediaViewData) {
        selectedPosition = position
        selectedUri = smallMediaViewData.singleUriData
        initMedia(selectedUri)
        val items = imageAdapter.items().map { it as SmallMediaViewData }
        imageAdapter.update(
            selectedPosition,
            smallMediaViewData.toBuilder().isSelected(true).build()
        )
        items.indices.map { index ->
            if (index != selectedPosition) {
                imageAdapter.update(
                    index,
                    items[index].toBuilder().isSelected(false).build()
                )
            }
        }
    }

    private fun initSendClick() {
        val text = memberTagging.replaceSelectedMembers(
            binding.etConversation.editableText
        )
        val extras = mediaExtras.toBuilder().conversation(text).build()

        if (mediaExtras.isExternallyShared) {
            viewModel.sendThirdPartySharingEvent(
                "image/video", mediaExtras.chatroomType,
                mediaExtras.communityId.toString(),
                mediaExtras.communityName,
                mediaExtras.searchKey,
                mediaExtras.chatroomId.toString()
            )
        }
        val intent = Intent()
        intent.putExtra(
            BUNDLE_MEDIA_EXTRAS,
            extras
        )
        activity?.setResult(Activity.RESULT_OK, intent)
        activity?.finish()
    }

    private fun initMemberTagging() {
        memberTagging = binding.memberTaggingView
        memberTagging.initialize(
            MemberTaggingExtras.Builder()
                .editText(binding.etConversation)
                .color(
                    LMChatAppearance.getTextLinkColor()
                )
                .darkMode(true)
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

    //result callback for new media pick
    private val browsePickerLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val uris = MediaUtils.getExternalIntentPickerUris(result.data)
                viewModel.fetchUriDetails(requireContext(), uris) {
                    val mediaUris = MediaUtils.convertMediaViewDataToSingleUriData(
                        requireContext(), it
                    )
                    updateMediaUris(mediaUris, saveInCache = true)
                }
            }
        }

    private fun updateMediaUris(mediaUris: ArrayList<SingleUriData>, saveInCache: Boolean = false) {
        if (mediaUris.isNotEmpty()) {
            if (mediaExtras.mediaUris?.size == 1) {
                initMediaDisplayRecyclerView(mediaExtras.mediaUris)
            }

            if (saveInCache) {
                mediaExtras.mediaUris?.addAll(
                    AndroidUtils.moveAttachmentToCache(requireContext(), *mediaUris.toTypedArray())
                )
            } else {
                mediaExtras.mediaUris?.addAll(mediaUris)
            }

            imageAdapter.replace(mediaExtras.mediaUris?.mapIndexed { index, singleMediaUri ->
                val isSelected = index == selectedPosition
                SmallMediaViewData.Builder().singleUriData(singleMediaUri)
                    .isSelected(isSelected)
                    .build()
            })
            invalidateDeleteMediaIcon(mediaExtras.mediaUris?.size)
        }
    }

    //callback when color is changed with the seek bar
    override fun onColorChangeListener(color: Int) {
        showBackgroundDrawable(color)
        if (binding.etInput.visibility == View.VISIBLE) {
            binding.etInput.setTextColor(color)
        }
        changeColor(color)
    }

    private fun showBackgroundDrawable(color: Int) {
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        when (editMode) {
            DRAW -> binding.btnDraw.background = drawable
            TEXT -> binding.btnText.background = drawable
            else -> {}
        }
    }

    private fun saveTrimVideo(uri: Uri) {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(requireContext(), uri)
        val duration = mediaMetadataRetriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        val width = mediaMetadataRetriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toLong()
        val height = mediaMetadataRetriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toLong()
        val values = ContentValues()
        values.put(MediaStore.Video.Media.DATA, uri.path)
        values.put(MediaStore.Video.VideoColumns.DURATION, duration)
        values.put(MediaStore.Video.VideoColumns.WIDTH, width)
        values.put(MediaStore.Video.VideoColumns.HEIGHT, height)
        requireContext().contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
        )?.let { ContentUris.parseId(it) }
    }

    private fun saveBitmapAndReset(send: Boolean, videoTrimExtras: VideoTrimExtras? = null) {
        if (editMode == null) return
        if (editMode == TRIM) {
            val updatedVideoTrimExtrasBuilder =
                if (videoTrimExtras != null) videoTrimExtras.toBuilder()
                else VideoTrimExtras.Builder()

            binding.videoView.onSaveClicked(
                updatedVideoTrimExtrasBuilder.initiatedSendMessage(send).build()
            )
        } else {
            binding.headerView.visibility = View.GONE
            binding.bottomView.visibility = View.GONE
            binding.btnSend.visibility = View.GONE
            val bitmap = binding.canvas.getBitmap()
            binding.headerView.visibility = View.VISIBLE
            binding.bottomView.visibility = View.VISIBLE
            binding.btnSend.visibility = View.VISIBLE
            val uri = FileUtil.getUriFromBitmapWithRandomName(requireContext(), bitmap)
            reset()
            replaceItem(
                selectedPosition,
                SingleUriData.Builder().uri(uri ?: Uri.EMPTY)
                    .fileType(uri.getMediaType(requireContext()) ?: "")
                    .build()
            )
            if (send) {
                initSendClick()
            }
        }
    }

    private fun saveBitmapAndResetAndShowCanvas(mode: MediaEditMode) {
        if (editMode == null) return
        binding.headerView.visibility = View.GONE
        binding.bottomView.visibility = View.GONE
        binding.btnSend.visibility = View.GONE
        val bitmap = binding.canvas.getBitmap()
        binding.headerView.visibility = View.VISIBLE
        binding.bottomView.visibility = View.VISIBLE
        binding.btnSend.visibility = View.VISIBLE
        val uri = FileUtil.getUriFromBitmapWithRandomName(requireContext(), bitmap)
        reset()
        replaceItem(
            selectedPosition,
            SingleUriData.Builder().uri(uri ?: Uri.EMPTY)
                .fileType(uri.getMediaType(requireContext()) ?: "").build()
        )

        editMode = mode
        binding.canvas.mode = editMode!!
        binding.canvas.visibility = View.VISIBLE
        binding.photoView.visibility = View.GONE
        binding.canvas.drawBitmap(bitmap)
        changeColor(binding.colorSeekBar.getColor())

        //Scaling the canvas to avoid drawing on out of bounds area
        val width = bitmap.width
        val height = bitmap.height
        val params = binding.canvas.layoutParams
        params.width = width
        params.height = height
        binding.canvas.layoutParams = params
    }

    private fun replaceItem(position: Int, uri: SingleUriData?) {
        selectedUri = uri
        val newMediaExtras = mediaExtras
        mediaExtras.mediaUris?.let {
            for (i in 0 until it.size) {
                if (i == position) {
                    newMediaExtras.mediaUris!![position] = uri!!
                }
            }
        }
        mediaExtras = newMediaExtras
        imageAdapter = ImageAdapter(this)
        imageAdapter.replace(mediaExtras.mediaUris?.mapIndexed { index, singleUriData ->
            val isSelected = index == selectedPosition
            SmallMediaViewData.Builder().singleUriData(singleUriData).isSelected(isSelected)
                .build()
        })
        initMedia(uri)
    }

    private fun showColorChooser() {
        binding.colorSeekBar.visibility = View.VISIBLE
        binding.colorSeekBarHolder.visibility = View.VISIBLE
    }

    private fun hideColorChooser() {
        binding.colorSeekBar.visibility = View.GONE
        binding.colorSeekBarHolder.visibility = View.GONE
    }

    private fun reset() {
        ViewUtils.hideKeyboard(requireContext())
        binding.canvas.clear()
        hideColorChooser()
        editMode = null
        currentTextMode = -1
        currentTextSize = 1
        binding.btnText.background = null
        binding.btnDraw.background = null
        binding.btnUndo.visibility = View.GONE
        binding.etInput.visibility = View.GONE
        binding.btnTextIncrease.visibility = View.GONE
        binding.btnTextDecrease.visibility = View.GONE
        binding.canvas.visibility = View.GONE
        binding.photoView.visibility = View.VISIBLE
    }

    private fun addTextToCanvas() {
        if (binding.etInput.visibility != View.VISIBLE) {
            return
        }
        ViewUtils.hideKeyboard(binding.etInput)
        val text = binding.etInput.text.toString().trim()
        if (text.isEmpty()) {
            reset()
            return
        }
        binding.canvas.textToCenter = true
        binding.canvas.text = text
        binding.etInput.text = null
        binding.etInput.visibility = View.GONE
    }

    //Showing canvas on edit on for DRAW and TEXT
    private fun showCanvas(mode: MediaEditMode) {
        val bitmap = (binding.photoView.drawable as? BitmapDrawable)?.bitmap ?: return
        editMode = mode
        binding.canvas.visibility = View.VISIBLE
        binding.photoView.visibility = View.GONE
        binding.canvas.drawBitmap(bitmap)
        binding.canvas.mode = editMode!!
        changeColor(binding.colorSeekBar.getColor())

        //Scaling the canvas to avoid drawing on out of bounds area
        val width = bitmap.width
        val height = bitmap.height
        val params = binding.canvas.layoutParams
        params.width = width
        params.height = height
        binding.canvas.layoutParams = params
    }

    private fun changeColor(color: Int) {
        binding.canvas.setPaintColor(color)
    }

    private fun changeTextMode() {
        if (currentTextMode == 3) {
            currentTextMode = 0
        } else {
            currentTextMode++
        }
        binding.btnText.setImageResource(textIcons[currentTextMode])
        binding.canvas.setTypeface(textTypefaces[currentTextMode]!!)
        binding.etInput.typeface = textTypefaces[currentTextMode]!!
    }

    private fun changeTextSize() {
        binding.canvas.setFontSize(textSizes[currentTextSize])
        binding.etInput.textSize = textSizes[currentTextSize]
    }

    //On canvas draw start
    override fun onDrawStart() {

    }

    //On canvas draw end
    override fun onDrawEnd() {

    }

    fun onBackPressedFromFragment() {
        if (mediaExtras.isExternallyShared) {
            viewModel.sendThirdPartyAbandoned(
                "image/video",
                mediaExtras.communityId.toString(),
                mediaExtras.communityName,
                mediaExtras.chatroomId.toString()
            )
        }
    }

    override fun onUndoAvailable(undoAvailable: Boolean) {
        binding.btnUndo.visibility = if (undoAvailable) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onCanvasClick() {
        if (editMode == TEXT) {
            addTextToCanvas()
        }
    }
}