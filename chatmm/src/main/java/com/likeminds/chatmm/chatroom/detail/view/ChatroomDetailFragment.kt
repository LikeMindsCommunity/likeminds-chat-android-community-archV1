package com.likeminds.chatmm.chatroom.detail.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.graphics.Typeface
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.text.Spannable
import android.text.style.ImageSpan
import android.util.*
import android.util.Base64
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.ui.*
import com.giphy.sdk.ui.themes.GPHTheme
import com.giphy.sdk.ui.themes.GridType
import com.giphy.sdk.ui.views.GiphyDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.likeminds.chatmm.*
import com.likeminds.chatmm.R
import com.likeminds.chatmm.chatroom.detail.model.*
import com.likeminds.chatmm.chatroom.detail.util.*
import com.likeminds.chatmm.chatroom.detail.util.ChatroomUtil.getTypeName
import com.likeminds.chatmm.chatroom.detail.view.adapter.*
import com.likeminds.chatmm.chatroom.detail.viewmodel.ChatroomDetailViewModel
import com.likeminds.chatmm.chatroom.detail.viewmodel.HelperViewModel
import com.likeminds.chatmm.conversation.model.*
import com.likeminds.chatmm.conversation.util.ChatReplyUtil
import com.likeminds.chatmm.databinding.*
import com.likeminds.chatmm.dm.view.*
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.util.*
import com.likeminds.chatmm.media.util.MediaAudioForegroundService.Companion.AUDIO_SERVICE_PROGRESS_EXTRA
import com.likeminds.chatmm.media.util.MediaAudioForegroundService.Companion.AUDIO_SERVICE_URI_EXTRA
import com.likeminds.chatmm.media.util.MediaAudioForegroundService.Companion.BROADCAST_COMPLETE
import com.likeminds.chatmm.media.util.MediaAudioForegroundService.Companion.BROADCAST_PLAY_NEW_AUDIO
import com.likeminds.chatmm.media.util.MediaAudioForegroundService.Companion.BROADCAST_PROGRESS
import com.likeminds.chatmm.media.util.MediaAudioForegroundService.Companion.BROADCAST_SEEKBAR_DRAGGED
import com.likeminds.chatmm.media.util.MediaAudioForegroundService.Companion.PROGRESS_SEEKBAR_DRAGGED
import com.likeminds.chatmm.media.view.LMChatMediaPickerActivity
import com.likeminds.chatmm.media.view.LMChatMediaPickerActivity.Companion.ARG_MEDIA_PICKER_RESULT
import com.likeminds.chatmm.media.view.MediaActivity
import com.likeminds.chatmm.media.view.MediaActivity.Companion.BUNDLE_MEDIA_EXTRAS
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.member.util.MemberImageUtil
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.polls.model.*
import com.likeminds.chatmm.polls.util.AddPollOptionListener
import com.likeminds.chatmm.polls.view.*
import com.likeminds.chatmm.pushnotification.util.NotificationUtils
import com.likeminds.chatmm.reactions.model.ReactionViewData
import com.likeminds.chatmm.reactions.model.ReactionsListExtras
import com.likeminds.chatmm.reactions.util.ReactionsPreferences
import com.likeminds.chatmm.reactions.view.*
import com.likeminds.chatmm.reactions.viewmodel.ReactionsViewModel
import com.likeminds.chatmm.report.model.*
import com.likeminds.chatmm.report.view.ReportActivity
import com.likeminds.chatmm.report.view.ReportSuccessDialog
import com.likeminds.chatmm.search.model.LMChatSearchExtras
import com.likeminds.chatmm.search.model.LMChatSearchResult
import com.likeminds.chatmm.search.view.LMChatSearchActivity
import com.likeminds.chatmm.search.view.LMChatSearchActivity.Companion.LM_CHAT_SEARCH_RESULT
import com.likeminds.chatmm.theme.customview.edittext.LikeMindsEditTextListener
import com.likeminds.chatmm.theme.customview.edittext.LikeMindsEmojiEditText
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.*
import com.likeminds.chatmm.utils.Route.getNullableQueryParameter
import com.likeminds.chatmm.utils.ValueUtils.getEmailIfExist
import com.likeminds.chatmm.utils.ValueUtils.getMaxCountNumberText
import com.likeminds.chatmm.utils.ValueUtils.getMediaType
import com.likeminds.chatmm.utils.ValueUtils.getUrlIfExist
import com.likeminds.chatmm.utils.ValueUtils.getValidYoutubeVideoId
import com.likeminds.chatmm.utils.ValueUtils.isValidIndex
import com.likeminds.chatmm.utils.ValueUtils.isValidSize
import com.likeminds.chatmm.utils.ValueUtils.isValidYoutubeLink
import com.likeminds.chatmm.utils.ValueUtils.orEmptyMutable
import com.likeminds.chatmm.utils.ViewUtils.endRevealAnimation
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.ViewUtils.startRevealAnimation
import com.likeminds.chatmm.utils.actionmode.ActionModeCallback
import com.likeminds.chatmm.utils.actionmode.ActionModeListener
import com.likeminds.chatmm.utils.chrometabs.CustomTabIntent
import com.likeminds.chatmm.utils.customview.*
import com.likeminds.chatmm.utils.databinding.ImageBindingUtil
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.chatmm.utils.mediauploader.worker.ConversationWorker
import com.likeminds.chatmm.utils.mediauploader.worker.ConversationWorker.Companion.ARG_MEDIA_INDEX_LIST
import com.likeminds.chatmm.utils.mediauploader.worker.ConversationWorker.Companion.OUTPUT_POST_CONVERSATION_RESPONSE
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder
import com.likeminds.chatmm.utils.membertagging.model.MemberTaggingExtras
import com.likeminds.chatmm.utils.membertagging.model.TagViewData
import com.likeminds.chatmm.utils.membertagging.util.MemberTaggingUtil
import com.likeminds.chatmm.utils.membertagging.util.MemberTaggingViewListener
import com.likeminds.chatmm.utils.membertagging.view.MemberTaggingView
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_CONVERSATION_PROGRESS
import com.likeminds.chatmm.utils.observer.ChatEvent
import com.likeminds.chatmm.utils.permissions.*
import com.likeminds.chatmm.utils.recyclerview.LMSwipeController
import com.likeminds.chatmm.utils.recyclerview.SwipeControllerActions
import com.likeminds.chatmm.utils.user.LMChatUserMetaData
import com.likeminds.chatmm.widget.model.WidgetViewData
import com.likeminds.likemindschat.chatroom.model.ChatRequestState
import com.likeminds.likemindschat.user.model.MemberBlockState
import com.vanniktech.emoji.EmojiPopup
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.Pair
import kotlin.math.abs

class ChatroomDetailFragment :
    BaseFragment<FragmentChatroomDetailBinding, ChatroomDetailViewModel>(),
    ChatroomDetailAdapterListener,
    AddPollOptionListener,
    ActionModeListener<ChatroomDetailActionModeData>,
    ReactionsClickListener,
    ReactionRemovedDialogListener,
    YouTubeVideoPlayerPopup.VideoPlayerListener,
    VoiceNoteInterface,
    LeaveSecretChatroomDialogListener,
    DeleteMessageListener,
    SendDMRequestDialogListener,
    ApproveDMRequestDialogListener,
    RejectDMRequestDialogListener,
    ChatEvent.ChatObserver,
    LMChatAttachmentBarAdapterListener {

    private var actionModeCallback: ActionModeCallback<ChatroomDetailActionModeData>? = null
    private var lmSwipeController: LMSwipeController? = null

    private lateinit var chatroomDetailExtras: ChatroomDetailExtras

    private var chatroomResultExtras: ChatroomDetailResultExtras? = null

    private lateinit var emojiPopup: EmojiPopup
    private var conversationIdForEmojiReaction = ""

    private lateinit var memberTagging: MemberTaggingView

    private var isAttachmentsSheetHiding = false

    //-----------For scroll and Fab experience------------
    private var unSeenConversationsSet = HashSet<String>()
    private var unSeenCount = 0
    private var visibleBottomConversationIndex = -1
    //-----------For scroll and FAb experience------------

    private var selectedChatRoom: ChatroomViewData? = null
    private val selectedConversations by lazy { HashMap<String, ConversationViewData>() }
    private var scrolledConversationPosition = -1
    private var reportedConversationId: String = ""
    private var searchedConversationId: String = ""
    private var scrollToHighlightTitle: Boolean = false

    private var isChatroomReaction = false

    // variable to hold the last date that was inserted locally while sending a conversation
    private var lastInsertedDate: String? = null

    @Inject
    lateinit var sdkPreferences: SDKPreferences

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var reactionsPreferences: ReactionsPreferences

    @Inject
    lateinit var helperViewModel: HelperViewModel

    @Inject
    lateinit var reactionsViewModel: ReactionsViewModel

    lateinit var chatroomDetailAdapter: ChatroomDetailAdapter
    private lateinit var attachmentBarAdapter: LMChatAttachmentBarAdapter

    private var messageReactionsTray: ReactionPopup? = null
    private lateinit var chatroomScrollListener: ChatroomScrollListener
    private var updatedMuteActionTitle: String? = null
    private var updatedFollowActionTitle: String? = null
    private var updatedBlockActionTitle: String? = null

    private var blockedAccessPopUp: AlertDialog? = null

    private var cameraPath: String? = null

    //Global variable for Audio Support as Conversation
    //For handling broadcast receiver and different audio clicked
    private var mediaAudioService: MediaAudioForegroundService? = null
    private var mediaAudioServiceBound = false
    private var serviceConnection: ServiceConnection? = null
    private var localParentConversationId: String = ""
    private var localChildPosition: Int = 0

    //Variable for Voice Note
    private var singleUriDataOfVoiceNote: SingleUriData? = null
    private var showTapAndHoldToast = true
    private var motionDownHandler: Handler? = null
    private val voiceNoteUtils: VoiceNoteUtils by lazy {
        VoiceNoteUtils(requireContext(), this)
    }
    private var firstX: Float = 0f
    private var firstY: Float = 0f
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var directionOffSet: Float = 0f
    private var cancelOffSet: Float = 0f
    private var lockOffSet: Float = 0f
    private var userBehaviours = USER_NONE
    private lateinit var voiceRecorder: LMVoiceRecorder
    private var voiceNoteFilePath: String? = ""
    private var isVoiceNotePlaying = false
    private var isVoiceNoteLocked = false
    private var stopTrackingVoiceNoteAction = false
    private var isDeletingVoiceNote = false
    private var isVoiceNoteRecording = false
    private var isAudioComplete = false
    private var isDMRequestSent = false

    private var showTapToUndoLocally = true

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.extras != null) {
                val audioDurationString =
                    intent.extras!!.getString(
                        MediaAudioForegroundService.AUDIO_DURATION_STRING_EXTRA
                    )
                val audioDurationInt =
                    intent.extras!!.getInt(
                        MediaAudioForegroundService.AUDIO_DURATION_INT_EXTRA
                    )

                when {
                    isVoiceNotePlaying -> {
                        binding.inputBox.tvVoiceNoteTime.text = audioDurationString
                    }

                    else -> {
                        val item =
                            chatroomDetailAdapter.items().firstOrNull {
                                ((it is ConversationViewData) && (it.id == localParentConversationId))
                            } as? ConversationViewData ?: return

                        if ((item.attachmentCount) >= 1) {
                            var attachment =
                                item.attachments?.get(localChildPosition) ?: return

                            attachment = attachment.toBuilder()
                                .progress(audioDurationInt)
                                .currentDuration(
                                    audioDurationString
                                        ?: requireContext().getString(R.string.lm_chat_start_duration)
                                )
                                .build()

                            updateAudioVoiceNoteBinder(
                                attachment,
                                localParentConversationId,
                                localChildPosition
                            )
                        }
                    }
                }
            }
        }
    }

    private val audioCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.extras != null) {
                isAudioComplete =
                    intent.extras!!.getBoolean(
                        MediaAudioForegroundService.AUDIO_IS_COMPLETE_EXTRA,
                        false
                    )

                when {
                    isVoiceNotePlaying -> {
                        binding.inputBox.tvVoiceNoteTime.text =
                            DateUtil.formatSeconds(
                                singleUriDataOfVoiceNote?.duration ?: 0
                            )
                        isVoiceNotePlaying = false
                        binding.inputBox.ivPlayRecording.setImageResource(R.drawable.lm_chat_ic_voice_play)
                    }

                    else -> {
                        val item =
                            chatroomDetailAdapter.items().firstOrNull {
                                ((it is ConversationViewData) && (it.id == localParentConversationId))
                            } as? ConversationViewData ?: return

                        if ((item.attachmentCount) >= 1) {
                            var attachment =
                                item.attachments?.get(localChildPosition) ?: return

                            if (isAudioComplete) {
                                attachment = attachment.toBuilder()
                                    .progress(0)
                                    .currentDuration(requireContext().getString(R.string.lm_chat_start_duration))
                                    .mediaState(MEDIA_ACTION_NONE)
                                    .build()
                            }
                            updateAudioVoiceNoteBinder(
                                attachment,
                                localParentConversationId,
                                localChildPosition
                            )
                            localParentConversationId = ""
                            localChildPosition = 0
                        }
                    }
                }
            }
        }
    }

    private val workersMap by lazy { ArrayList<UUID>() }

    // variable to hold the youtube popup player
    private var inAppVideoPlayerPopup: YouTubeVideoPlayerPopup? = null

    // launcher for gallery picker
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bundle = result.data?.extras
                val data = ExtrasUtil.getParcelable(
                    bundle,
                    ARG_MEDIA_PICKER_RESULT,
                    MediaPickerResult::class.java
                )
                checkMediaPickedResult(data)
            }
        }

    // launcher for document picker
    private val documentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val data = ExtrasUtil.getParcelable(
                    extras,
                    ARG_MEDIA_PICKER_RESULT,
                    MediaPickerResult::class.java
                )
                checkMediaPickedResult(data)
            }
        }

    // launcher for audio picker
    private val audioLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val data = ExtrasUtil.getParcelable(
                    extras,
                    ARG_MEDIA_PICKER_RESULT,
                    MediaPickerResult::class.java
                )
                checkMediaPickedResult(data)
            }
        }

    // launcher for reporting conversation/chatroom
    private val reportLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(SDKApplication.LOG_TAG, "report done")
                ReportSuccessDialog("Message").show(
                    childFragmentManager,
                    ReportSuccessDialog.TAG
                )
            }
        }

    private val chatroomId
        get() = chatroomDetailExtras.chatroomId

    private val communityId
        get() = getChatroomViewData()?.communityId ?: chatroomDetailExtras.communityId

    private fun isNotAdminInAnnouncementRoom(): Boolean {
        return viewModel.isNotAdminInAnnouncementRoom()
    }

    // fetches the first conversation from the adapter
    private fun getTopConversation(): ConversationViewData? {
        return viewModel.getFirstConversationFromAdapter(
            chatroomDetailAdapter.items()
        )
    }

    // fetches the last conversation from the adapter
    private fun getBottomConversation(): ConversationViewData? {
        return viewModel.getLastConversationFromAdapter(
            chatroomDetailAdapter.items()
        )
    }

    companion object {
        private const val TAG = "ChatroomDetail"

        const val ARG_CHATROOM_DETAIL_RESULT_EXTRAS = "ARG_CHATROOM_DETAIL_RESULT_EXTRAS"
        const val CHATROOM_DETAIL_EXTRAS = "CHATROOM_DETAIL_EXTRAS"
        const val SOURCE_CHAT_ROOM_TELESCOPE = "chatroom_telescope"

        const val SCREEN_RECORD = "screen_record"
        const val SOURCE_HOME_FEED = "home_feed"
        const val SOURCE_AI_CHATBOT = "ai_chatbot"
        const val SOURCE_TAGGED_AUTO_FOLLOWED = "tagged_auto_followed"

        const val MUTE_ACTION_TITLE = "Mute notifications"
        const val UNMUTE_ACTION_TITLE = "Unmute notifications"
        const val UNMUTE_ACTION_IDENTIFIER = "unmute"

        const val POLL_CLICK_ENABLED = "POLL_CLICK_ENABLED"
        const val POLL_CLICK_DISABLED = "POLL_CLICK_DISABLED"

        const val FOLLOW_ACTION_TITLE = "Join chatroom"
        const val UNFOLLOW_ACTION_TITLE = "Leave chatroom"

        const val BLOCK_ACTION_TITLE = "Block"
        const val UNBLOCK_ACTION_TITLE = "Unblock"

        const val SOURCE_CHAT_ROOM_OVERFLOW_MENU = "chatroom_overflow_menu"

        const val REQUEST_GIFS = 3004

        private const val DM_SEND_REQUEST_TEXT_LIMIT = 300
    }

    override fun getViewModelClass(): Class<ChatroomDetailViewModel> {
        return ChatroomDetailViewModel::class.java
    }

    override fun getViewBinding(): FragmentChatroomDetailBinding {
        return FragmentChatroomDetailBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().chatroomDetailComponent()?.inject(this)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun receiveExtras() {
        super.receiveExtras()
        if (arguments == null || arguments?.containsKey(CHATROOM_DETAIL_EXTRAS) == false) {
            requireActivity().supportFragmentManager.popBackStack()
            return
        }
        chatroomDetailExtras = ExtrasUtil.getParcelable(
            requireArguments(),
            CHATROOM_DETAIL_EXTRAS,
            ChatroomDetailExtras::class.java
        ) ?: return
        isGuestUser = userPreferences.getIsGuestUser()
        checkForExplicitActions()
        fetchInitialData()
    }

    /**
     * Checks and do the necessities for explicit actions, such as actions from notification,
     * or other screen intents
     */
    private fun checkForExplicitActions() {
        //If this screen was opened after clicking a reported conversation by the community manager
        if (!chatroomDetailExtras.reportedConversationId.isNullOrEmpty()) {
            reportedConversationId = chatroomDetailExtras.reportedConversationId!!
        }
        if (!chatroomDetailExtras.conversationId.isNullOrEmpty()) {
            searchedConversationId = chatroomDetailExtras.conversationId!!
        }

        if (chatroomDetailExtras.scrollToExtremeTopForHighlightingTitle == true) {
            scrollToHighlightTitle = true
        }
        dismissChatRoomNotification()
    }

    /**
     * Dismiss the active notifications of this current chatroom if it is showing
     */
    private fun dismissChatRoomNotification() {
        var communityName = viewModel.getChatroomViewData()?.communityName

        if (communityName.isNullOrEmpty()) {
            communityName = chatroomDetailExtras.communityName
        }

        NotificationUtils.removeConversationNotification(
            requireContext(),
            communityId,
            communityName,
            chatroomId
        )
    }

    /**
     * Fetches initial data for loading chatroom and conversations
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun fetchInitialData() {
        viewModel.getInitialData(chatroomDetailExtras)
    }

    private fun getChatroomViewData(): ChatroomViewData? {
        return viewModel.getChatroomViewData()
    }

    override fun setUpViews() {
        super.setUpViews()
        initToolbar()
        setHasOptionsMenu(true)
        initView()
        initEmojiView()
        initGiphy()
        initEnterClick()
        initAttachmentClick()
        disableAnswerPosting()
        initReplyView()
        initDMRequestClickListeners()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            syncChatroom()
        }
        initMediaAudioServiceConnection()
        registerAudioCompleteBroadcast()
        registerProgressBroadcast()
        initTouchListenerOnMic()
        initVoiceNotes()
        initVoiceNoteControl()
        subscribeToChatEvent()
    }

    override fun doCleanup() {
        unsubscribeToChatEvent()
        super.doCleanup()
    }

    // initializes the toolbar
    private fun initToolbar() {
        binding.apply {
            toolbarColor = LMChatAppearance.getToolbarColor()
            buttonColor = LMChatAppearance.getButtonsColor()

            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

            ivBack.setOnClickListener {
                requireActivity().finish()
            }
        }
    }

    // initialized the fragment view
    private fun initView() {
        binding.apply {
            initRecyclerView()
            initMemberTaggingView()
            isVoiceNoteSupportEnabled = viewModel.isVoiceNoteSupportEnabled()
            inputBox.etAnswer.doAfterTextChanged {
                if (it?.toString()?.trim()
                        .isNullOrEmpty() && viewModel.isVoiceNoteSupportEnabled()
                ) {
                    if (viewModel.isDmChatroom()) {
                        val chatRequestState = viewModel.getChatroomViewData()?.chatRequestState

                        //in request as no attachment is not allowed
                        if (chatRequestState == ChatRequestState.ACCEPTED) {
                            fabSend.hide()
                            fabMic.show()
                        } else {
                            fabSend.show()
                            fabMic.hide()
                        }
                    } else {
                        fabSend.hide()
                        fabMic.show()
                    }
                } else {
                    fabSend.show()
                    fabMic.hide()
                    //LinkPreview
                    viewModel.linkPreview(
                        inputBox.etAnswer.editableText.toString().trim()
                    )
                }
                if (it?.toString()?.trim().isNullOrEmpty()) {
                    viewModel.clearLinkPreview()
                }
            }

            bottomSnack.ivCancelSnack.setOnClickListener {
                bottomSnack.root.visibility = View.GONE
                bottomSnack.tvSnack.text = ""
            }

            // scroll the recyclerview up when keyboard opens
            rvChatroom.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (!isScrollable()) {
                    hideScrollBottomFab()
                }
                if (bottom < oldBottom) {
                    rvChatroom.scrollBy(0, oldBottom - bottom)
                }
                if (KeyboardVisibilityEvent.isKeyboardVisible(requireActivity())) {
                    val height =
                        rvChatroom.height - ViewUtils.dpToPx(16)
                    memberTagging.reSetMaxHeight(height)
                }
            }

            initSwipeController()
            initRichEditorSupport()

            fabScrollBottom.setOnClickListener {
                hideScrollBottomFab()
                scrollToExtremeBottom()
            }
        }
    }

    private fun initRecyclerView() {
        binding.rvChatroom.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            linearLayoutManager.orientation = RecyclerView.VERTICAL
            layoutManager = linearLayoutManager
            chatroomDetailAdapter =
                ChatroomDetailAdapter(
                    sdkPreferences,
                    userPreferences,
                    reactionsPreferences,
                    this@ChatroomDetailFragment
                )
            adapter = chatroomDetailAdapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
                false
            attachPagination(linearLayoutManager)
        }
    }

    private fun initEmojiView() {
        emojiPopup = buildEmojiPopup(binding.inputBox.etAnswer)
    }

    private fun buildEmojiPopup(editText: LikeMindsEmojiEditText): EmojiPopup {
        binding.apply {
            return EmojiPopup.Builder.fromRootView(root)
                .setIconColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.lm_chat_brown_grey
                    )
                )
                .setSelectedIconColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.lm_chat_colorPrimary
                    )
                )
                .setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.lm_chat_emoji_kb_background
                    )
                )
                .setDividerColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.lm_chat_emoji_kb_background
                    )
                )
                .setOnEmojiPopupShownListener {
                    viewModel.sendReactionsClickEvent()
                }
                .setOnEmojiPopupDismissListener {
                    conversationIdForEmojiReaction = ""
                }
                .setOnEmojiClickListener { _, emojiString ->
                    if (conversationIdForEmojiReaction.isNotEmpty()) {
                        reactedToMessage(emojiString.unicode, conversationIdForEmojiReaction, true)
                        inputBox.etAnswer.setText("")
                        emojiPopup.dismiss()
                    }
                    if (isChatroomReaction) {
                        reactedToMessage(emojiString.unicode, chatroomId, false)
                        binding.inputBox.etAnswer.setText("")
                        emojiPopup.dismiss()
                    }
                }
                .build(editText)
        }
    }

    private fun initGiphy() {
        Giphy.configure(
            requireContext(),
            String(Base64.decode(InternalKeys.GIPHY_SDK, Base64.DEFAULT))
        )
        val settings = GPHSettings(GridType.waterfall, GPHTheme.Light)
        settings.mediaTypeConfig = arrayOf(GPHContentType.recents, GPHContentType.gif)
        settings.selectedContentType = GPHContentType.gif
        val giphyDialog = GiphyDialogFragment.newInstance(settings)
        giphyDialog.setTargetFragment(this, REQUEST_GIFS)
        binding.inputBox.tvGifs.setOnClickListener {
            if (viewModel.isDmChatroom() &&
                viewModel.getChatroomViewData()?.chatRequestState == ChatRequestState.NOTHING &&
                viewModel.getChatroomViewData()?.isPrivateMember == true
            ) {
                ViewUtils.showShortToast(
                    requireContext(),
                    getString(R.string.lm_chat_you_are_not_connected_with_this_user_yet)
                )
                return@setOnClickListener
            } else {
                giphyDialog.show(parentFragmentManager, "giphy_dialog")
            }
        }
    }

    private fun giphySelected(media: Media?) {
        if (viewModel.isGifSupportEnabled()) {
            media?.let {
                val text =
                    memberTagging.replaceSelectedMembers(binding.inputBox.etAnswer.editableText)
                val mediaExtras = MediaExtras.Builder()
                    .giphyMedia(it)
                    .mediaScreenType(MEDIA_GIF_SEND_SCREEN)
                    .chatroomId(chatroomId)
                    .communityId(communityId?.toIntOrNull())
                    .text(text)
                    .isSecretChatroom(getChatroomViewData()?.isSecret)
                    .isTaggingEnabled(!viewModel.isDmChatroom())
                    .build()

                val intent = MediaActivity.getIntent(
                    requireContext(),
                    mediaExtras,
                    activity?.intent?.clipData
                )
                imageVideoSendLauncher.launch(intent)
            }
        }
    }

    private fun initAttachmentClick() {
        binding.apply {
            inputBox.ivAttachment.setOnClickListener {
                initVisibilityOfAttachmentsBar(View.VISIBLE)
            }
        }
    }

    /**
     * create the list of supported attachments as per the chatroom
     * Case 1 -> If it is Group Chatroom
     * Allow all the attachments, but if poll and audio is enabled from backend
     *
     * Case 2 -> If it is 1-1 Chatroom with user
     * Allow all the attachments, except Polls
     *
     * Case 3 -> If it is 1-1 Chatroom with AI Chatbot
     * Allow only Camera, Gallery (images only) and Audio
     *
     * Case 4 -> If Custom widget is enabled from backend
     * Add custom widget allow.
     */
    private fun getSupportedAttachmentTypes(): List<LMChatAttachmentPickerItemViewData> {
        val supportedAttachments = mutableListOf<LMChatAttachmentPickerItemViewData>()

        //add camera
        val cameraAttachment = LMChatAttachmentPickerItemViewData.Builder()
            .attachmentType(LMChatAttachmentType.CAMERA)
            .attachmentIcon(R.drawable.lm_chat_ic_create_camera)
            .attachmentName(requireContext().getString(R.string.lm_chat_camera))
            .build()

        //add gallery
        val galleryAttachment = LMChatAttachmentPickerItemViewData.Builder()
            .attachmentType(LMChatAttachmentType.GALLERY)
            .attachmentIcon(R.drawable.lm_chat_ic_create_gallery)
            .attachmentName(requireContext().getString(R.string.lm_chat_gallery))
            .build()

        supportedAttachments.add(cameraAttachment)
        supportedAttachments.add(galleryAttachment)

        if (!viewModel.isOtherUserAIBot()) {
            //add document
            val documentAttachment = LMChatAttachmentPickerItemViewData.Builder()
                .attachmentType(LMChatAttachmentType.DOCUMENT)
                .attachmentIcon(R.drawable.lm_chat_ic_document_new)
                .attachmentName(requireContext().getString(R.string.lm_chat_document))
                .build()
            supportedAttachments.add(documentAttachment)
        }

        if (viewModel.isAudioSupportEnabled()) {
            //add audio
            val audioAttachment = LMChatAttachmentPickerItemViewData.Builder()
                .attachmentType(LMChatAttachmentType.AUDIO)
                .attachmentIcon(R.drawable.lm_chat_ic_create_music)
                .attachmentName(requireContext().getString(R.string.lm_chat_audio))
                .build()
            supportedAttachments.add(audioAttachment)
        }

        if (viewModel.isMicroPollsEnabled() && !viewModel.isDmChatroom()) {
            //add poll
            val pollAttachment = LMChatAttachmentPickerItemViewData.Builder()
                .attachmentType(LMChatAttachmentType.POLL)
                .attachmentIcon(R.drawable.lm_chat_ic_poll_create_message_selector)
                .attachmentName(requireContext().getString(R.string.lm_chat_poll))
                .build()
            supportedAttachments.add(pollAttachment)
        }

        if (viewModel.isWidgetEnabled()) {
            //add custom widget
            val customWidgetAttachment = LMChatAttachmentPickerItemViewData.Builder()
                .attachmentType(LMChatAttachmentType.CUSTOM_WIDGET)
                .attachmentIcon(R.drawable.ic_create_custom_widget_a)
                .attachmentName(requireContext().getString(R.string.lm_chat_custom_widget_a))
                .build()
            supportedAttachments.add(customWidgetAttachment)
        }

        return supportedAttachments
    }

    override fun onAttachmentBarItemClicked(attachmentItem: LMChatAttachmentPickerItemViewData) {
        when (attachmentItem.attachmentType) {
            LMChatAttachmentType.CAMERA -> {
                initCameraAttachment()
            }

            LMChatAttachmentType.GALLERY -> {
                initVisibilityOfAttachmentsBar(View.GONE)
                onScreenChanged()

                val extras = if (viewModel.isOtherUserAIBot()) {
                    LMChatMediaPickerExtras.Builder()
                        .senderName(viewModel.chatroomDetail.chatroom?.header)
                        .allowMultipleSelect(false)
                        .mediaTypes(listOf(IMAGE))
                        .build()
                } else {
                    LMChatMediaPickerExtras.Builder()
                        .senderName(viewModel.chatroomDetail.chatroom?.header)
                        .mediaTypes(listOf(IMAGE, VIDEO))
                        .build()
                }

                val intent = LMChatMediaPickerActivity.getIntent(requireContext(), extras)
                galleryLauncher.launch(intent)
            }

            LMChatAttachmentType.DOCUMENT -> {
                initVisibilityOfAttachmentsBar(View.GONE)
                onScreenChanged()
                val extra = LMChatMediaPickerExtras.Builder()
                    .senderName(viewModel.chatroomDetail.chatroom?.header)
                    .mediaTypes(listOf(PDF))
                    .build()
                val intent = LMChatMediaPickerActivity.getIntent(requireContext(), extra)
                documentLauncher.launch(intent)
            }

            LMChatAttachmentType.AUDIO -> {
                initVisibilityOfAttachmentsBar(View.GONE)
                onScreenChanged()
                val extra = LMChatMediaPickerExtras.Builder()
                    .senderName(viewModel.chatroomDetail.chatroom?.header)
                    .mediaTypes(listOf(AUDIO))
                    .build()
                val intent = LMChatMediaPickerActivity.getIntent(requireContext(), extra)
                audioLauncher.launch(intent)
            }

            LMChatAttachmentType.POLL -> {
                initVisibilityOfAttachmentsBar(View.GONE)
                CreateConversationPollDialog.show(
                    childFragmentManager,
                    getChatroomViewData(),
                    chatroomDetailExtras
                )
            }

            LMChatAttachmentType.CUSTOM_WIDGET -> {
                onCustomWidgetAAttachmentClicked()
            }
        }
    }

    private fun initAttachmentsPickerBarView() {
        attachmentBarAdapter = LMChatAttachmentBarAdapter(this)

        binding.layoutAttachments.apply {
            rvAttachments.adapter = attachmentBarAdapter
            rvAttachments.layoutManager = GridLayoutManager(requireContext(), 3)

            attachmentBarAdapter.replace(getSupportedAttachmentTypes())

            clBottomBar.setOnClickListener {
                initVisibilityOfAttachmentsBar(View.GONE)
            }
        }
    }

    //on click function when custom widget A is clicked
    private fun onCustomWidgetAAttachmentClicked() {
        postConversation(
            conversation = "Hey CW!",
            metadata = JSONObject().apply {
                put("as", "popopo")
                put("name", "Ishaan")
            }
        )
    }

    private fun disableAnswerPosting() {
        editAnswerEnableState(false)
    }

    private fun enableAnswerPosting() {
        editAnswerEnableState(true)
    }

    private fun editAnswerEnableState(state: Boolean) {
        binding.apply {
            fabSend.isEnabled = state
            inputBox.ivAttachment.isEnabled = state
            inputBox.etAnswer.isEnabled = state
            inputBox.etAnswer.setHint(R.string.lm_chat_type_your_response)
        }
    }

    /**
     * Checks for external files shared from third party apps.
     * At present, we don't allow to share pdf along with any other multi media
     */
    private fun checkForExternalSharedContent() {
        val clipData = activity?.intent?.clipData ?: return
        if (clipData.description.label != context?.getString(R.string.lm_chat_third_party_share)) {
            return
        }

        val multiMediaList = arrayListOf<SingleUriData>()
        val gifList = arrayListOf<SingleUriData>()
        val documentList = arrayListOf<SingleUriData>()
        val audioList = arrayListOf<SingleUriData>()
        for (i in 0 until clipData.itemCount) {
            val uri = clipData.getItemAt(i).uri
            when (val type = uri.getMediaType(requireContext())) {
                IMAGE, VIDEO -> {
                    multiMediaList.add(uriToSingleUri(uri, type))
                }

                AUDIO -> {
                    audioList.add(uriToSingleUri(uri, type))
                }

                GIF -> {
                    gifList.add(uriToSingleUri(uri, type))
                }

                PDF -> {
                    documentList.add(uriToSingleUri(uri, type))
                }
            }
        }
        val text = clipData.getItemAt(0)?.text?.toString()
        if (!text.isNullOrEmpty() && multiMediaList.isEmpty() && gifList.isEmpty() && documentList.isEmpty() && audioList.isEmpty()) {
            return
        }

        if (multiMediaList.isEmpty() && gifList.isEmpty() && documentList.isEmpty() && audioList.isEmpty()) {
            return
        }

        var counterForMultipleType = 0
        if (multiMediaList.isNotEmpty()) counterForMultipleType++
        if (gifList.isNotEmpty()) counterForMultipleType++
        if (documentList.isNotEmpty()) counterForMultipleType++
        if (audioList.isNotEmpty()) counterForMultipleType++

        if (counterForMultipleType > 1) {
            ViewUtils.showShortToast(
                requireContext(),
                getString(R.string.lm_chat_warning_multimedia_and_document_share_error)
            )
            return
        }

        //Images, Videos
        if (multiMediaList.isNotEmpty()) {
            showPickImagesListScreen(
                *multiMediaList.toTypedArray(),
                isExternallyShared = true,
                textAlreadyPresent = text
            )
            return
        }

        //Documents
        if (documentList.isNotEmpty()) {
            showPickDocumentsListScreen(
                *documentList.toTypedArray(),
                isExternallyShared = true,
                textAlreadyPresent = text
            )
            return
        }

        //Audio
        if (audioList.isNotEmpty()) {
            showPickAudioListScreen(
                *audioList.toTypedArray(),
                isExternallyShared = true,
                textAlreadyPresent = text
            )
        }
    }

    /**
     * Convert [uri] to [SingleUriData]
     * @param uri File uri
     * @param mediaType [InternalMediaType]
     */
    private fun uriToSingleUri(uri: Uri, @InternalMediaType mediaType: String): SingleUriData {
        return SingleUriData.Builder()
            .uri(uri)
            .fileType(mediaType)
            .build()
    }

    // handles the chatroom if it is a DM chatroom
    private fun handleDmChatrooms() {
        if (viewModel.isDmChatroom()) {
            memberTagging.taggingEnabled = false
            checkDMStatus()
            disableAllGraphicViewTypes()
            removeChatroomItem()
        }
    }

    private fun checkDMStatus() {
        viewModel.checkDMStatus(chatroomId)
    }

    private fun initReplyView() {
        binding.inputBox.viewReply.ivReplyClose.setOnClickListener {
            if (isEditConversationViewVisible()) {
                clearEditTextAnswer()
            }
            setChatInputBoxViewType(CHAT_BOX_NORMAL)
        }
    }

    // initializes click listeners on dm request approve reject buttons
    private fun initDMRequestClickListeners() {
        binding.apply {
            tvApproveDmRequest.setOnClickListener {
                ApproveDMRequestDialogFragment.showDialog(childFragmentManager)
            }

            tvRejectDmRequest.setOnClickListener {
                RejectDMRequestDialogFragment.showDialog(childFragmentManager)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun syncChatroom() {
        val pair = viewModel.syncChatroom(requireContext(), chatroomId)
        val worker = pair.first
        val isFirstTime = pair.second
        worker.observe(viewLifecycleOwner) { state ->
            when (state) {
                WorkInfo.State.SUCCEEDED -> {
                    Log.i(TAG, "blocker syncChatroom state - SUCCEEDED")
                    //If shimmer is showing that means chatroom is not present. So after syncing fetch again.
                    if (isShimmerShowing()) {
                        /* Adding a flag to extras that we are trying to fetch the data again after syncing. Still if
                        chatroom is not found, that means chatroom id is invalid or user don't have access to it */
                        chatroomDetailExtras = chatroomDetailExtras.toBuilder()
                            .loadingAfterSync(true)
                            .build()
                        fetchInitialData()
                    }
                    if (isFirstTime) {
                        startBackgroundSync()
                    }

                    //we should observe the live conversation once, sync is complete to avoid duplicate conversation
                    viewModel.observeLiveConversations(requireContext(), chatroomId)
                }

                WorkInfo.State.CANCELLED -> {
                    Log.i(TAG, "blocker syncChatroom got cancelled")
                }

                else -> {
                    Log.i(TAG, "blocker syncChatroom state - $state")
                }
            }
        }
    }

    /**
     * Is shimmer showing on the screen
     */
    private fun isShimmerShowing(): Boolean {
        return viewModel.isShimmerPresent(chatroomDetailAdapter.items())
    }

    private fun startBackgroundSync() {
        viewModel.startBackgroundFirstSync(requireContext(), chatroomId)
            .observe(viewLifecycleOwner) { state ->
                when (state) {
                    WorkInfo.State.SUCCEEDED -> {
                        Log.i(TAG, "background syncChatroom state - $state")
                        if (isShimmerShowing()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                fetchInitialData()
                            }
                        }
                        viewModel.setIsFirstTimeSync(false)
                    }

                    else -> {
                        Log.i(TAG, "background syncChatroom state - $state")
                    }
                }
            }
    }

    private fun initMediaAudioServiceConnection() {
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MediaAudioForegroundService.MediaBinder
                mediaAudioService = binder.getService()
                mediaAudioServiceBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                mediaAudioServiceBound = false
                mediaAudioService = null
            }
        }
    }

    private fun registerAudioCompleteBroadcast() {
        val filter = IntentFilter(BROADCAST_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(audioCompleteReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            requireActivity().registerReceiver(audioCompleteReceiver, filter)
        }
    }

    private fun registerProgressBroadcast() {
        val filter = IntentFilter(BROADCAST_PROGRESS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(progressReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            requireActivity().registerReceiver(progressReceiver, filter)
        }
    }

    /**------------------------------------------------------------
     * Code for Voice Notes
    ---------------------------------------------------------------*/

    private fun initVoiceNotes() {
        voiceRecorder = LMVoiceRecorder()
    }

    private fun initVoiceNoteControl() {
        binding.inputBox.apply {
            ivStopVoice.setOnClickListener {
                isVoiceNoteLocked = false
                voiceNoteUtils.stopVoiceNote(binding, RECORDING_LOCK_DONE)
            }
            ivCancelVoice.setOnClickListener {
                isVoiceNoteLocked = false
                if (isVoiceNotePlaying && serviceConnection != null) {
                    stopAudioService()
                }
                viewModel.sendVoiceNoteCanceled()
                voiceNoteUtils.stopVoiceNote(binding, RECORDING_CANCELLED)
            }

            ivPlayRecording.setOnClickListener {
                if (mediaAudioService?.isPlaying() == true && !isVoiceNotePlaying) {
                    mediaAudioService?.stopMedia()
                    val item =
                        chatroomDetailAdapter.items().firstOrNull {
                            ((it is ConversationViewData) && (it.id == localParentConversationId))
                        } as? ConversationViewData ?: return@setOnClickListener

                    if ((item.attachmentCount) >= 1) {
                        var attachment =
                            item.attachments?.get(localChildPosition) ?: return@setOnClickListener

                        attachment = attachment.toBuilder()
                            .progress(0)
                            .currentDuration(requireContext().getString(R.string.lm_chat_start_duration))
                            .mediaState(MEDIA_ACTION_NONE)
                            .build()

                        updateAudioVoiceNoteBinder(
                            attachment,
                            localParentConversationId,
                            localChildPosition
                        )

                        localParentConversationId = ""
                        localChildPosition = 0
                    }
                }
                if (singleUriDataOfVoiceNote != null) {
                    when {
                        !isVoiceNotePlaying -> {
                            isVoiceNotePlaying = true
                            playAudio(singleUriDataOfVoiceNote?.uri!!, 0)
                            viewModel.sendVoiceNotePreviewed()
                            ivPlayRecording.setImageResource(R.drawable.lm_chat_ic_pause_voice_note)
                        }

                        mediaAudioService?.isPlaying() == true -> {
                            mediaAudioService?.pauseAudio()
                            ivPlayRecording.setImageResource(R.drawable.lm_chat_ic_voice_play)
                        }

                        mediaAudioService?.isPlaying() == false -> {
                            mediaAudioService?.playAudio()
                            ivPlayRecording.setImageResource(R.drawable.lm_chat_ic_pause_voice_note)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTouchListenerOnMic() {
        binding.fabMic.setOnTouchListener { _, event ->

            if (isDeletingVoiceNote || isVoiceNoteLocked) return@setOnTouchListener true

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (viewModel.isDmChatroom() &&
                        viewModel.getChatroomViewData()?.chatRequestState == ChatRequestState.NOTHING &&
                        viewModel.getChatroomViewData()?.isPrivateMember == true
                    ) {
                        return@setOnTouchListener true
                    }

                    if (isGuestUser) {
                        callGuestFlowCallback()
                    } else {
                        showTapAndHoldToast = true
                        motionDownHandler = Handler(Looper.getMainLooper())
                        LMChatPermissionManager.performTaskWithPermission(
                            activity as BaseAppCompatActivity,
                            { },
                            LMChatPermission.getRecordAudioPermissionData(),
                            showInitialPopup = true,
                            showDeniedPopup = true,
                            lmChatPermissionDeniedCallback = object :
                                LMChatPermissionDeniedCallback {
                                override fun onDeny() {}

                                override fun onCancel() {}
                            }
                        )
                        motionDownHandler?.postDelayed({
                            showTapAndHoldToast = false
                            stopTrackingVoiceNoteAction = false
                            if ((activity as BaseAppCompatActivity).hasPermission(LMChatPermission.getRecordAudioPermissionData())) {
                                startVoiceNote()
                            }
                        }, 400)
                        firstX = event.rawX
                        firstY = event.rawY
                        val screenWidth = ViewUtils.getDeviceDimension(requireContext()).first
                        cancelOffSet = (screenWidth / 2.8).toFloat()
                        lockOffSet = (screenWidth / 2.5).toFloat()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if ((activity as BaseAppCompatActivity).hasPermission(LMChatPermission.getRecordAudioPermissionData())) {
                        if (showTapAndHoldToast) {
                            if (viewModel.isDmChatroom() &&
                                viewModel.getChatroomViewData()?.chatRequestState == ChatRequestState.NOTHING &&
                                viewModel.getChatroomViewData()?.isPrivateMember == true
                            ) {
                                ViewUtils.showShortToast(
                                    requireContext(),
                                    getString(R.string.lm_chat_you_are_not_connected_with_this_user_yet)
                                )
                                return@setOnTouchListener true
                            }

                            ViewUtils.showAnchoredToast(binding.voiceNoteTapHoldToast.layoutToast)

                            requireView().performHapticFeedback(
                                HapticFeedbackConstants.LONG_PRESS,
                                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                            )
                        } else {
                            if (viewModel.isDmChatroom() &&
                                viewModel.getChatroomViewData()?.chatRequestState == ChatRequestState.NOTHING &&
                                viewModel.getChatroomViewData()?.isPrivateMember == true
                            ) {
                                return@setOnTouchListener true
                            }

                            if (isVoiceNoteRecording) {
                                resetVoiceNoteVariables()
                                voiceNoteUtils.stopVoiceNote(binding, RECORDING_RELEASED)
                            }
                        }
                        motionDownHandler?.removeCallbacksAndMessages(null)
                        motionDownHandler = null
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (stopTrackingVoiceNoteAction || !voiceRecorder.isRecording()) {
                        return@setOnTouchListener true
                    }

                    if (viewModel.isDmChatroom() &&
                        viewModel.getChatroomViewData()?.chatRequestState == ChatRequestState.NOTHING &&
                        viewModel.getChatroomViewData()?.isPrivateMember == true
                    ) {
                        return@setOnTouchListener true
                    }

                    var direction = USER_NONE
                    val motionX = abs(firstX - event.rawX)
                    val motionY = abs(firstY - event.rawY)

                    if (motionX > directionOffSet && lastX < firstX && lastY < firstY) {
                        if (motionX > motionY && lastX < firstX) {
                            direction = USER_CANCELING
                        } else if (motionY > motionX && lastY < firstY) {
                            direction = USER_LOCKING
                        }
                    } else if (motionX > motionY && motionX > directionOffSet && lastX < firstX) {
                        direction = USER_CANCELING
                    } else if (motionY > motionX && motionY > directionOffSet && lastY < firstY) {
                        direction = USER_LOCKING
                    }

                    if (direction == USER_CANCELING) {
                        if (userBehaviours == USER_NONE || event.rawY + binding.fabMic.width / 2 > firstX) {
                            userBehaviours = USER_CANCELING
                        }

                        if (userBehaviours == USER_CANCELING) {
                            translateX(-(firstX - event.rawX))
                        }
                    } else if (direction == USER_LOCKING) {
                        if (userBehaviours == USER_NONE || event.rawX + binding.fabMic.width / 2 > firstX) {
                            userBehaviours = USER_LOCKING
                        }

                        if (userBehaviours == USER_LOCKING) {
                            translateY(-(firstY - event.rawY))
                        }
                    }
                    lastX = event.rawX
                    lastY = event.rawY
                }
            }
            return@setOnTouchListener true
        }
    }

    private fun startVoiceNote() {
        isVoiceNoteRecording = true
        voiceNoteUtils.startVoiceNote(binding)
        if (sdkPreferences.getSlideUpVoiceNoteToast()) {
            sdkPreferences.setSlideUpVoiceNoteToast(false)
            ViewUtils.showAnchoredToast(binding.voiceNoteSlideUpToast.layoutToast)
        }
        requireView().performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        viewModel.sendVoiceNoteRecorded()
        motionDownHandler?.removeCallbacksAndMessages(null)
        motionDownHandler = null
    }

    private fun translateX(x: Float) {
        binding.apply {
            if (x < -cancelOffSet) {
                voiceNoteCancelled()
                fabMic.translationX = 0f
                cardCancel.translationX = 0f
                return
            }

            fabMic.translationX = x
            cardLock.translationY = 0f
            fabMic.translationY = 0f

            if (abs(x) < binding.ivMicRecording.width / 2) {
                if (cardLock.visibility != View.VISIBLE) {
                    cardLock.show()
                }
            } else {
                if (cardLock.visibility != View.GONE) {
                    cardLock.hide()
                    voiceNoteSlideUpToast.layoutToast.hide()
                }
            }
        }
    }

    private fun translateY(y: Float) {
        binding.apply {
            if (y < -lockOffSet) {
                voiceNoteLocked()
                fabMic.translationY = 0f
                return
            }

            if (cardLock.visibility != View.VISIBLE) {
                cardLock.show()
            }

            fabMic.translationY = y
            fabMic.translationX = 0f
        }
    }

    private fun voiceNoteCancelled() {
        stopTrackingVoiceNoteAction = true
        resetVoiceNoteVariables()
        viewModel.sendVoiceNoteCanceled()
        voiceNoteUtils.stopVoiceNote(binding, RECORDING_CANCELLED)
    }

    private fun voiceNoteLocked() {
        stopTrackingVoiceNoteAction = true
        resetVoiceNoteVariables()
        voiceNoteUtils.stopVoiceNote(binding, RECORDING_LOCKED)
        isVoiceNoteLocked = true
    }

    private fun resetVoiceNoteVariables() {
        firstX = 0f
        firstY = 0f
        lastX = 0f
        lastY = 0f
        userBehaviours = USER_NONE
    }

    private fun isEditConversationViewVisible(): Boolean {
        if (isReplyViewVisible()) {
            val chatReplyData = binding.inputBox.viewReply.chatReplyData
            return chatReplyData?.isEditMessage == true
        }
        return false
    }

    private fun clearEditTextAnswer() {
        binding.inputBox.etAnswer.apply {
            setText("")
            requestFocus()
            viewModel.clearLinkPreview()
        }
    }

    /**
     * Use to set different input box type, See [ChatBoxType] for all different input views
     * @param type of [ChatBoxType]
     */
    private fun setChatInputBoxViewType(
        @ChatBoxType type: Int? = null,
        showDM: Boolean? = null,
        isBlocked: Boolean? = null
    ) {
        binding.apply {
            if (viewModel.isDmChatroom()) {
                setChatInputBoxViewTypeForDM(
                    type,
                    (showDM ?: true),
                    isBlocked
                )
            } else {
                setChatInputBoxViewTypeForGroupChat(type)
            }
            if (type == null) {
                return
            }
        }
    }

    // sets the chat input box for DM chat
    private fun setChatInputBoxViewTypeForDM(
        @ChatBoxType type: Int? = null,
        showDM: Boolean,
        isBlocked: Boolean? = null
    ) {
        binding.apply {
            if (showDM) {
                val isPrivateMember = viewModel.getChatroomViewData()?.isPrivateMember
                if (isBlocked == true) {
                    hideAllChatBoxViews()
                    tvRestrictedMessage.visibility = View.VISIBLE
                    tvRestrictedMessage.text =
                        getString(R.string.lm_chat_you_cannot_send_message_to_rejected_connection)
                    tvRestrictedMessage.gravity = Gravity.CENTER
                    return
                } else if (isBlocked == false) {
                    setupCommonChatInputBox(type)
                    return
                }
                when (viewModel.getChatroomViewData()?.chatRequestState) {
                    ChatRequestState.NOTHING -> {
                        // dm is not initiated and request is not send, showing message to send DM request
                        tvSendDmRequestToMember.show()
                        cvDmRequest.hide()
                        tvSendDmRequestToMember.text =
                            getString(
                                R.string.lm_chat_send_a_dm_request_to_s,
                                viewModel.getOtherDmMember()?.name
                            )

                        if (isPrivateMember == false) {
                            tvSendDmRequestToMember.hide()
                        }

                        //as no attachment is allowed in request
                        fabSend.show()
                        fabMic.hide()

                        isDMRequestSent = true
                        inputBox.ivAttachment.visibility = View.INVISIBLE
                        return
                    }

                    ChatRequestState.INITIATED -> {
                        // chatroom request is sent, i.e., chatroom is initiated
                        tvSendDmRequestToMember.hide()
                        if (viewModel.getLoggedInMemberId() !=
                            viewModel.getChatroomViewData()?.chatRequestedById
                        ) {
                            // logged in member is the request receiver, so showing DM request view
                            cvDmRequest.show()
                        } else {
                            // logged in member is the request sender, so hiding DM request view
                            cvDmRequest.hide()
                        }
                        // hides the input box and shows DM request pending message
                        hideAllChatBoxViews()
                        tvRestrictedMessage.visibility = View.VISIBLE
                        tvRestrictedMessage.text =
                            getString(R.string.lm_chat_dm_request_pending_message)
                        tvRestrictedMessage.gravity = Gravity.CENTER
                        return
                    }

                    ChatRequestState.ACCEPTED -> {
                        // DM is accepted so hiding the DM request view
                        cvDmRequest.hide()
                        tvSendDmRequestToMember.hide()
                    }

                    ChatRequestState.REJECTED -> {
                        // DM is rejected
                        tvSendDmRequestToMember.hide()
                        cvDmRequest.hide()
                        if (viewModel.getLoggedInMemberId() ==
                            viewModel.getChatroomViewData()?.chatRequestedById
                        ) {
                            // logged in user has rejected the request
                            if (showTapToUndoLocally) {
                                updateTapToUndoLocally(true)
                            }

                            // hides the input box and shows rejected connection message
                            hideAllChatBoxViews()
                            tvRestrictedMessage.visibility = View.VISIBLE
                            tvRestrictedMessage.text =
                                getString(R.string.lm_chat_you_cannot_send_message_to_rejected_connection)
                            tvRestrictedMessage.gravity = Gravity.CENTER
                        } else {
                            hideAllChatBoxViews()
                            tvRestrictedMessage.visibility = View.VISIBLE
                            tvRestrictedMessage.text =
                                getString(R.string.lm_chat_dm_request_pending_message)
                            tvRestrictedMessage.gravity = Gravity.CENTER
                        }
                        return
                    }

                    else -> {
                        return
                    }
                }
            } else {
                hideAllChatBoxViews()
                tvRestrictedMessage.visibility = View.VISIBLE
                tvRestrictedMessage.text = getString(
                    R.string.lm_chat_direct_messaging_among_members_has_been_disabled_by_the_community_manager
                )
                return
            }
            setupCommonChatInputBox(type)
        }
    }

    // sets the chat input box for group chat
    private fun setChatInputBoxViewTypeForGroupChat(
        @ChatBoxType type: Int? = null
    ) {
        binding.apply {
            if (!viewModel.hasMemberRespondRight()) {
                hideAllChatBoxViews()
                tvRestrictedMessage.visibility = View.VISIBLE
                tvRestrictedMessage.text =
                    getString(R.string.lm_chat_the_community_managers_have_restricted_you_from_responding_here)
                return
            }
            if (!viewModel.isAdminMember() && viewModel.isAnnouncementChatroom()) {
                hideAllChatBoxViews()
                tvRestrictedMessage.visibility = View.VISIBLE
                tvRestrictedMessage.text =
                    getString(R.string.lm_chat_only_community_managers_can_respond)
                tvRestrictedMessage.gravity = Gravity.CENTER
                return
            }
            if (!viewModel.isAdminMember() && viewModel.canMembersCanMessage() == false) {
                hideAllChatBoxViews()
                tvRestrictedMessage.visibility = View.VISIBLE
                tvRestrictedMessage.text =
                    getString(R.string.lm_chat_only_community_managers_can_respond)
                tvRestrictedMessage.gravity = Gravity.CENTER
                return
            }
            if (isSecretChatRoom() && getChatRoom()?.followStatus != true) {
                hideAllChatBoxViews()
                tvRestrictedMessage.visibility = View.VISIBLE
                tvRestrictedMessage.text =
                    getString(R.string.lm_chat_secret_chatroom_restricted_message)
                return
            }
            setupCommonChatInputBox(type)
        }
    }

    private fun setupCommonChatInputBox(
        @ChatBoxType type: Int? = null
    ) {
        binding.apply {
            inputBox.clChatContainer.visibility = View.VISIBLE
            tvRestrictedMessage.visibility = View.GONE
            when (type) {
                CHAT_BOX_NORMAL -> {
                    if (!memberTagging.isShowing) {
                        inputBox.clChatContainer.setBackgroundResource(R.drawable.lm_chat_background_white_24_black10_1)
                    }
                    if (!isVoiceNoteLocked && !isVoiceNoteRecording && !isDMRequestSent) {
                        inputBox.ivAttachment.visibility = View.VISIBLE
                    }
                    inputBox.viewLink.clLink.visibility = View.GONE
                    inputBox.viewReply.clReply.visibility = View.GONE
                    if (inputBox.etAnswer.text?.trim().isNullOrEmpty() &&
                        viewModel.isVoiceNoteSupportEnabled()
                    ) {
                        if (viewModel.isDmChatroom()) {
                            val chatRequestState = viewModel.getChatroomViewData()?.chatRequestState
                            //in request as no attachment is not allowed
                            if (chatRequestState == ChatRequestState.ACCEPTED) {
                                fabSend.hide()
                                fabMic.show()
                            } else {
                                fabSend.show()
                                fabMic.hide()
                            }
                        } else {
                            fabSend.hide()
                            fabMic.show()
                        }
                    } else {
                        fabSend.show()
                        fabMic.hide()
                    }
                }

                CHAT_BOX_REPLY -> {
                    if (!memberTagging.isShowing) {
                        inputBox.clChatContainer.setBackgroundResource(R.drawable.lm_chat_background_white_12top_24_bottom_black10_1)
                    }
                    if (!isVoiceNoteLocked && !isVoiceNoteRecording) {
                        inputBox.ivAttachment.visibility = View.VISIBLE
                    }
                    inputBox.viewLink.clLink.visibility = View.GONE
                    inputBox.viewReply.clReply.visibility = View.VISIBLE
                }

                CHAT_BOX_LINK -> {
                    if (!memberTagging.isShowing) {
                        inputBox.clChatContainer.setBackgroundResource(R.drawable.lm_chat_background_white_12top_24_bottom_black10_1)
                    }
                    inputBox.ivAttachment.visibility = View.INVISIBLE
                    inputBox.viewLink.clLink.visibility = View.VISIBLE
                    inputBox.viewReply.clReply.visibility = View.GONE
                }

                CHAT_BOX_INTERNAL_LINK -> {
                    if (!memberTagging.isShowing) {
                        inputBox.clChatContainer.setBackgroundResource(R.drawable.lm_chat_background_white_12top_24_bottom_black10_1)
                    }
                    fabSend.show()
                    inputBox.ivAttachment.visibility = View.INVISIBLE
                    inputBox.viewLink.clLink.visibility = View.GONE
                    inputBox.viewReply.clReply.visibility = View.GONE
                }
            }
        }
    }

    // handles tap to undo view in the adapter
    private fun handleTapToUndo(
        index: Int,
        conversationViewData: ConversationViewData,
        toShowTapToUndo: Boolean
    ) {
        val updatedConversation = conversationViewData.toBuilder()
            .showTapToUndo(toShowTapToUndo)
            .build()

        chatroomDetailAdapter.update(index, updatedConversation)
    }

    // removes tap to undo view from the adapter
    private fun updateTapToUndoLocally(toShowTapToUndo: Boolean) {
        val conversationIndex =
            chatroomDetailAdapter.items()
                .indexOfLast { chatroomItem ->
                    (chatroomItem is ConversationViewData && chatroomItem.state == STATE_DM_REJECTED)
                }

        val conversationViewData =
            chatroomDetailAdapter[conversationIndex] as? ConversationViewData ?: return

        handleTapToUndo(
            conversationIndex,
            conversationViewData,
            toShowTapToUndo
        )
    }

    private fun hideAllChatBoxViews() {
        binding.apply {
            inputBox.clChatContainer.visibility = View.GONE
            fabSend.visibility = View.GONE
            fabMic.visibility = View.GONE
        }
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onImagePickedFromCamera()
            }
        }

    private fun onImagePickedFromCamera() {
        if (cameraPath != null) {
            val uri = Uri.fromFile(File(cameraPath!!))
            if (uri.isValidSize(requireContext())) {
                val singleUri = SingleUriData.Builder().uri(uri)
                    .fileType(uri.getMediaType(requireContext()) ?: "")
                    .build()
                showPickImagesListScreen(singleUri)
            } else {
                ViewUtils.showShortToast(
                    requireContext(),
                    getString(R.string.lm_chat_large_file_select_error_message)
                )
            }
        }
    }

    /**
     * function triggers after Camera permission is allowed.
     * Also, function start a intent to launch default camera of the device
     **/
    private fun initCameraAttachment() {
        initVisibilityOfAttachmentsBar(View.GONE)
        onScreenChanged()
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireContext().packageManager).also {
                val cameraFile = try {
                    val file = FileUtil.createImageFile(requireContext())
                    cameraPath = file.absolutePath
                    file
                } catch (ex: IOException) {
                    Log.e("errorCreateFile", "errorCreateFile", ex)
                    null
                }
                if (cameraFile == null) {
                    ViewUtils.showShortToast(
                        requireContext(),
                        getString(R.string.lm_chat_image_not_found)
                    )
                } else {
                    try {
                        val photoURI = FileProvider.getUriForFile(
                            requireContext(),
                            FileUtil.getFileProviderPackage(requireContext()),
                            cameraFile
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        cameraLauncher.launch(takePictureIntent)
                    } catch (e: Exception) {
                        ViewUtils.showShortToast(requireContext(), "Image not found")
                        Log.e(SDKApplication.LOG_TAG, "provider not found, ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    private fun callGuestFlowCallback() {
        SDKApplication.getLikeMindsCallback()?.login()
        activity?.finish()
    }

    private fun initMemberTaggingView() {
        binding.apply {
            memberTagging = memberTaggingView
            memberTagging.initialize(
                MemberTaggingExtras.Builder()
                    .editText(binding.inputBox.etAnswer)
                    .maxHeightInPercentage(0.4f)
                    .color(LMChatAppearance.getTextLinkColor())
                    .build()
            )
            memberTagging.addListener(object : MemberTaggingViewListener {
                override fun onMemberTagged(user: TagViewData) {
                    viewModel.sendUserTagEvent(
                        user,
                        communityId
                    )
                }

                override fun onShow() {
                    inputBox.clChatContainer.setBackgroundResource(R.drawable.lm_chat_background_white_24_bottom_black10_1)
                }

                override fun onHide() {
                    if (isReplyViewVisible() || isLinkViewVisible()) {
                        inputBox.clChatContainer.setBackgroundResource(R.drawable.lm_chat_background_white_12top_24_bottom_black10_1)
                    } else {
                        inputBox.clChatContainer.setBackgroundResource(R.drawable.lm_chat_background_white_24_black10_1)
                    }
                }

                override fun callApi(page: Int, searchName: String) {
                    helperViewModel.getMembersForTagging(
                        chatroomId,
                        page,
                        searchName
                    )
                }
            })
        }
    }

    private fun isReplyViewVisible(): Boolean {
        return binding.inputBox.viewReply.clReply.visibility == View.VISIBLE
    }

    private fun isLinkViewVisible(): Boolean {
        return binding.inputBox.viewLink.clLink.visibility == View.VISIBLE
    }

    private fun isSecretChatRoom() = getChatroomViewData()?.isSecret == true

    /**
     * Attach scroll listener to the recyclerview for events
     */
    private fun attachPagination(linearLayoutManager: LinearLayoutManager) {
        chatroomScrollListener = object : ChatroomScrollListener(linearLayoutManager) {
            override fun onScroll() {
                if (messageReactionsTray?.isShowing == true) {
                    messageReactionsTray?.dismiss()
                }
            }

            override fun onLoadMore(scrollState: Int) {
                fetchPaginatedConversations(scrollState)
            }

            override fun onBottomReached() {
                hideScrollBottomFab()
            }

            override fun onScrollingToTop() {
                showScrollBottomFab(null)
            }

            override fun onScrollingToBottom(lastItemPosition: Int) {
                updateFabOnScrollingToBottom(lastItemPosition)
            }

            override fun onChatRoomVisibilityChanged(show: Boolean) {
                if (show) {
                    fadeInTopChatroomView()
                } else {
                    fadeOutTopChatroomView()
                }
            }
        }
        binding.rvChatroom.addOnScrollListener(chatroomScrollListener)
    }

    /**
     * Fetches paginated data for top or bottom based on the scroll state.
     * This is called only when the user scrolls the list up or down by gestures.
     * @param scrollState SCROLL_UP or SCROLL_DOWN
     */
    private fun fetchPaginatedConversations(@ScrollState scrollState: Int) {
        val conversation = if (scrollState == SCROLL_UP) {
            getTopConversation()
        } else {
            getBottomConversation()
        } ?: return
        viewModel.fetchPaginatedDataOnScroll(
            scrollState,
            conversation,
            chatroomDetailAdapter.items()
        )
    }

    private fun initEnterClick() {
        binding.apply {
            fabSend.setOnClickListener {
                if (isGuestUser) {
                    callGuestFlowCallback()
                } else {
                    var editableText = inputBox.etAnswer.editableText
                    if (isReplyViewVisible()) {
                        val chatReplyData = inputBox.viewReply.chatReplyData
                        if (chatReplyData?.isEditMessage == true) {
                            postEditConversation(editableText)
                            return@setOnClickListener
                        }
                    }

                    if (viewModel.isVoiceNoteSupportEnabled()) {
                        if (isVoiceNotePlaying && serviceConnection != null) {
                            stopAudioService()
                        }

                        if (isVoiceNoteLocked) {
                            isVoiceNoteLocked = false
                            singleUriDataOfVoiceNote = voiceRecorder.stopRecording(requireContext())
                            voiceNoteUtils.stopVoiceNote(this, RECORDING_LOCK_SEND)
                        }
                    }

                    val listUris =
                        if (singleUriDataOfVoiceNote != null) {
                            listOf(singleUriDataOfVoiceNote!!)
                        } else null

                    editableText = if (singleUriDataOfVoiceNote != null) {
                        null
                    } else {
                        inputBox.etAnswer.editableText
                    }

                    val inputText = inputBox.etAnswer.editableText.toString().trim()
                    val shareLink = if (inputText.getEmailIfExist().isNullOrEmpty()) {
                        inputText.getUrlIfExist()
                    } else {
                        null
                    }

                    if (viewModel.isVoiceNoteSupportEnabled()) {
                        isVoiceNotePlaying = false
                        voiceNoteUtils.stopVoiceNote(this, RECORDING_SEND)
                    }

                    // show dialog to send dm request if the chatroom is of type DM & chatRequestState is null
                    if (
                        viewModel.isDmChatroom()
                        && (viewModel.getChatroomViewData()?.chatRequestState == ChatRequestState.NOTHING)
                    ) {
                        if (inputText.length >= DM_SEND_REQUEST_TEXT_LIMIT) {
                            ViewUtils.showShortToast(
                                requireContext(),
                                getString(
                                    R.string.lm_chat_request_cant_be_more_than_s_characters,
                                    DM_SEND_REQUEST_TEXT_LIMIT.toString()
                                )
                            )
                            return@setOnClickListener
                        }

                        // if the DM is M2M then show dialog otherwise send dm request directly
                        if (viewModel.getChatroomViewData()?.isPrivateMember == true) {
                            SendDMRequestDialogFragment.showDialog(childFragmentManager, inputText)
                            setChatInputBoxViewType(
                                CHAT_BOX_NORMAL,
                                viewModel.showDM.value
                            )
                        } else {
                            viewModel.sendDMRequest(
                                viewModel.getChatroomViewData()?.id.toString(),
                                ChatRequestState.ACCEPTED,
                                true,
                                inputText
                            )
                        }
                        clearEditTextAnswer()
                        return@setOnClickListener
                    }

                    postConversation(
                        editableConversation = editableText,
                        shareLink = shareLink,
                        fileUris = listUris
                    )
                }
            }
        }
    }

    private fun postConversation(
        editableConversation: Editable? = null,
        conversation: String? = null,
        fileUris: List<SingleUriData>? = null,
        shareLink: String? = null,
        metadata: JSONObject? = null
    ) {
        binding.apply {
            val shareTextLink = shareLink?.trim()

            if (!conversation.isNullOrEmpty() || !editableConversation.isNullOrEmpty() || fileUris != null || shareTextLink?.isBlank() == false || metadata != null) {

                scrollToExtremeBottom()

                // Update Chatroom follow status
                if (getChatroomViewData()?.followStatus == false) {
                    removeFollowView()
                    viewModel.updateChatRoomFollowStatus(true)
                    updateChatroomFollowStatus(true)
                    ViewUtils.showShortToast(
                        requireContext(),
                        getString(R.string.lm_chat_added_to_your_joined_chat_rooms)
                    )
                }

                val updatedConversation = conversation?.trim()
                    ?: memberTagging.replaceSelectedMembers(editableConversation).trim()

                var replyConversationId: String? = null
                var replyChatRoomId: String? = null
                var replyChatData: ChatReplyViewData? = null

                if (isReplyViewVisible()) {
                    when (inputBox.viewReply.replySourceType) {
                        REPLY_SOURCE_CHATROOM -> {
                            val repliedChatRoom = inputBox.viewReply.chatRoomViewData
                            replyChatRoomId = repliedChatRoom?.id
                        }

                        REPLY_SOURCE_CONVERSATION -> {
                            val repliedConversation =
                                inputBox.viewReply.conversationViewData
                            replyConversationId = repliedConversation?.id
                        }
                    }
                    replyChatData = inputBox.viewReply.chatReplyData
                }

                val conversationCreatedEpoch = System.currentTimeMillis()
                val temporaryId = "-$conversationCreatedEpoch"

                val uuidString = viewModel.postConversation(
                    requireContext(),
                    updatedConversation,
                    conversationCreatedEpoch,
                    fileUris,
                    shareTextLink,
                    replyConversationId,
                    replyChatRoomId,
                    metadata
                )

                uuidString?.let { uuid ->
                    observeCreateConversationWorker(
                        uuid,
                        temporaryId,
                        replyChatData,
                        replyConversationId,
                        replyChatRoomId
                    )
                }

                clearEditTextAnswer()
                if (isLinkViewVisible() || isReplyViewVisible()) {
                    setChatInputBoxViewType(CHAT_BOX_NORMAL)
                }
            } else {
                ViewUtils.showShortSnack(
                    root,
                    getString(R.string.lm_chat_please_enter_your_response)
                )
            }
        }
    }

    //observes workinfo and related to create conversation worker
    private fun observeCreateConversationWorker(
        uuid: String,
        conversationId: String,
        replyChatData: ChatReplyViewData?,
        replyConversationId: String?,
        replyChatRoomId: String?
    ) {
        val workerUUID = UUID.fromString(uuid) ?: return
        if (!workersMap.contains(workerUUID)) {
            workersMap.add(workerUUID)

            //get worker info
            WorkManager.getInstance(requireContext())
                .getWorkInfoByIdLiveData(workerUUID)
                .observe(viewLifecycleOwner) { workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            //get output data
                            val successResponseString =
                                workInfo.outputData.getString(OUTPUT_POST_CONVERSATION_RESPONSE)

                            successResponseString?.let {
                                //convert to LMResponse
                                val successResponse =
                                    viewModel.parseCreateConversationResponse(it)

                                //get data
                                val data = successResponse.data
                                if (data != null) {
                                    //get conversation
                                    val createdConversation = data.conversation
                                    val conversationViewData =
                                        ViewDataConverter.convertConversation(createdConversation)

                                    //send analytics
                                    conversationViewData?.let { conversation ->
                                        viewModel.sendCreateConversationAnalytics(
                                            conversation,
                                            memberTagging.getTaggedMembers(),
                                            replyChatData,
                                            replyConversationId,
                                            replyChatRoomId
                                        )

                                        //send analytics
                                        val chatReplyData = binding.inputBox.viewReply.chatReplyData

                                        if (chatReplyData != null) {
                                            viewModel.sendMessageReplyEvent(
                                                conversation,
                                                chatReplyData.repliedMemberId,
                                                chatReplyData.repliedMemberState,
                                                conversation.replyConversation?.id,
                                                chatReplyData.type
                                            )
                                        }
                                        viewModel.sendChatroomResponded(
                                            memberTagging.getTaggedMembers().map { it.name },
                                            conversation
                                        )
                                        if (ChatroomUtil.getConversationType(conversation) == VOICE_NOTE) {
                                            viewModel.sendVoiceNoteSent(conversation.id)
                                        }
                                    }

                                    //clear old values
                                    memberTagging.clearTaggedMembers()
                                    binding.inputBox.viewReply.chatReplyData = null
                                }
                            }
                        }

                        WorkInfo.State.FAILED -> {
                            //get output data
                            val errorResponseString =
                                workInfo.outputData.getString(OUTPUT_POST_CONVERSATION_RESPONSE)

                            errorResponseString?.let {
                                // convert to LMResponse
                                val errorResponse =
                                    viewModel.parseCreateConversationResponse(it)

                                //show error toast
                                ViewUtils.showErrorMessageToast(
                                    requireContext(),
                                    errorResponse.errorMessage
                                )
                            }

                            val failedMediaIndex =
                                workInfo.outputData.getIntArray(ARG_MEDIA_INDEX_LIST)

                            failedMediaIndex?.let { indexList ->
                                val position = getIndexOfConversation(conversationId)
                                if (position >= 0) {
                                    val oldConversation = chatroomDetailAdapter[position]
                                            as? ConversationViewData
                                        ?: return@observe

                                    val updatedConversation = oldConversation.toBuilder()
                                        .attachments(
                                            oldConversation.attachments?.map { attachment ->
                                                if (indexList.contains(attachment.index ?: -1)) {
                                                    attachment
                                                } else {
                                                    attachment.toBuilder()
                                                        .awsFolderPath("")
                                                        .build()
                                                }
                                            } as ArrayList<AttachmentViewData>?)
                                        .build()
                                    chatroomDetailAdapter.update(position, updatedConversation)
                                }
                            }
                        }

                        WorkInfo.State.CANCELLED -> {
                            val position = getIndexOfConversation(conversationId)
                            if (position >= 0) {
                                chatroomDetailAdapter.notifyItemChanged(position)
                            }
                        }

                        else -> {
                            val progress =
                                ConversationWorker.getProgress(workInfo) ?: return@observe
                            val position = getIndexOfConversation(conversationId)
                            if (position.isValidIndex()) {
                                val oldConversation = chatroomDetailAdapter[position]
                                        as? ConversationViewData
                                    ?: return@observe

                                val updatedConversation =
                                    if ((progress.first / (progress.second * 1.0)) == 1.0) {
                                        oldConversation.toBuilder()
                                            .attachmentsUploaded(true)
                                            .attachmentUploadProgress(null)
                                            .attachments(
                                                oldConversation.attachments?.map { attachment ->
                                                    attachment.toBuilder()
                                                        .awsFolderPath("")
                                                        .build()
                                                } as ArrayList<AttachmentViewData>?)
                                            .build()
                                    } else {
                                        oldConversation.toBuilder()
                                            .attachmentUploadProgress(progress)
                                            .build()
                                    }
                                chatroomDetailAdapter.update(position, updatedConversation)
                            }
                            Log.i(
                                TAG,
                                "create conversation worker - state - ${workInfo.state}"
                            )
                        }
                    }
                }
        }
    }

    private fun postEditConversation(editableText: Editable?) {
        binding.apply {
            if (!editableText.isNullOrEmpty()) {
                val updatedText = memberTagging.replaceSelectedMembers(editableText).trim()
                when (inputBox.viewReply.replySourceType) {
                    REPLY_SOURCE_CHATROOM -> {
                        val chatroom = inputBox.viewReply.chatRoomViewData
                        if (chatroom != null) {
                            viewModel.postEditedChatroom(updatedText, chatroom)
                        } else {
                            ViewUtils.showShortSnack(
                                root,
                                getString(R.string.lm_chat_please_enter_your_response)
                            )
                        }
                    }

                    REPLY_SOURCE_CONVERSATION -> {
                        val conversation = inputBox.viewReply.conversationViewData
                        if (conversation != null) {
                            val link =
                                inputBox.etAnswer.editableText.toString().trim().getUrlIfExist()

                            viewModel.postEditedConversation(
                                updatedText,
                                link,
                                conversation
                            )
                            //to check if we are editing a message which set as current topic
                            if (conversation.id == viewModel.getCurrentTopic()
                                    ?.id
                            ) {
                                viewModel.updateChatroomWhileEditingTopic(
                                    conversation,
                                    updatedText
                                )
                                initTopChatroomView(getChatroomViewData()!!)
                            }
                        } else {
                            ViewUtils.showShortSnack(
                                root,
                                getString(R.string.lm_chat_please_enter_your_response)
                            )
                        }
                    }
                }
                clearEditTextAnswer()
                if (isReplyViewVisible()) {
                    setChatInputBoxViewType(CHAT_BOX_NORMAL)
                }
            } else {
                ViewUtils.showShortSnack(
                    root,
                    getString(R.string.lm_chat_please_enter_your_response)
                )
            }
        }
    }

    /**
     * Check if the recyclerview can scroll up or down
     */
    private fun isScrollable() = binding.rvChatroom.canScrollVertically(1)
            || binding.rvChatroom.canScrollVertically(-1)

    /**
     * Hide the scroll counter floating action button
     */
    private fun hideScrollBottomFab() {
        binding.apply {
            unSeenConversationsSet.clear()
            unSeenCount = 0
            fabScrollBottom.hide(object :
                FloatingActionButton.OnVisibilityChangedListener() {
                override fun onHidden(fab: FloatingActionButton?) {
                    super.onHidden(fab)
                    tvScrollBottom.visibility = View.GONE
                }
            })
        }
    }

    /**
     * Update the scroll counter floating action button when the user is scrolling down
     * @param position Position of the last completely visible item position
     */
    private fun updateFabOnScrollingToBottom(position: Int) {
        if (unSeenConversationsSet.size <= 0 || visibleBottomConversationIndex == position) {
            return
        }
        visibleBottomConversationIndex = position
        val item = chatroomDetailAdapter[position]
        if (item is ConversationViewData && unSeenConversationsSet.contains(item.id)) {
            unSeenConversationsSet.remove(item.id)
            unSeenCount--
            if (unSeenCount <= 0) {
                return
            }
            showScrollBottomFab(unSeenCount.getMaxCountNumberText())
        }
    }

    /**
     * Show the scroll counter floating action button, which triggers only if the recyclerview is scrollable
     * @param text Text to show on the counter, eg - 99+, 8, etc
     */
    private fun showScrollBottomFab(text: String?) {
        binding.fabScrollBottom.apply {
            if (!isScrollable()) {
                return
            }
            if (text == null) {
                show()
            } else {
                if (isOrWillBeShown) {
                    configureScrollBottomFab(text)
                } else {
                    show(
                        object : FloatingActionButton.OnVisibilityChangedListener() {
                            override fun onShown(fab: FloatingActionButton?) {
                                super.onShown(fab)
                                configureScrollBottomFab(text)
                            }
                        })
                }
            }
        }
    }

    /**
     * Configure the scroll counter floating action button based on the text length and visibility
     * @param text Text to show on the counter, eg - 99+, 8, etc
     */
    private fun configureScrollBottomFab(text: String) {
        binding.tvScrollBottom.apply {
            if (text.length > 2) {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            } else {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            }
            this.text = text
            visibility = View.VISIBLE
        }
    }

    /**
     * Show the count of unseen conversations on scroll counter floating action button
     * @param showBlank Show the scroll FAB even though there are no unseen conversations
     */
    private fun showUnseenCount(showBlank: Boolean) {
        when {
            unSeenCount != 0 -> {
                showScrollBottomFab(unSeenCount.getMaxCountNumberText())
            }

            showBlank -> {
                showScrollBottomFab(null)
            }
        }
    }

    private fun fadeInTopChatroomView() {
        if (viewModel.isDmChatroom()) {
            return
        }
        binding.apply {
            fadeInTopChatroomEachView(viewTopBackground)
            fadeInTopChatroomEachView(memberImage)
            fadeInTopChatroomEachView(tvChatroomMemberName)
            fadeInTopChatroomEachView(tvChatroom)
            val topic = getChatRoom()?.topic
            if (topic == null) {
                fadeInTopChatroomEachView(ivDateDot)
                fadeInTopChatroomEachView(tvChatroomDate)
            } else {
                when {
                    topic.isDeleted() -> {
                        fadeInTopChatroomEachView(ivDateDot)
                        fadeInTopChatroomEachView(tvChatroomDate)
                    }

                    topic.ogTags != null -> {
                        if (topic.ogTags.image != null) {
                            fadeInTopChatroomEachView(topicImage)
                        }
                    }

                    topic.attachmentCount > 0 -> {
                        when (topic.attachments?.firstOrNull()?.type) {
                            IMAGE -> {
                                fadeInTopChatroomEachView(binding.topicImage)
                            }

                            VIDEO -> {
                                fadeInTopChatroomEachView(binding.topicImage)
                            }

                            GIF -> {
                                fadeInTopChatroomEachView(binding.topicImage)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fadeInTopChatroomEachView(view: View) {
        if (view.visibility == View.GONE) {
            view.visibility = View.VISIBLE
            view.alpha = 0.0f
            view.animate()
                .setDuration(500)
                .alpha(1.0f)
                .setListener(null)
        }
    }

    private fun fadeOutTopChatroomView() {
        binding.apply {
            fadeOutTopChatroomEachView(viewTopBackground)
            fadeOutTopChatroomEachView(memberImage)
            fadeOutTopChatroomEachView(tvChatroomMemberName)
            fadeOutTopChatroomEachView(ivDateDot)
            fadeOutTopChatroomEachView(tvChatroomDate)
            fadeOutTopChatroomEachView(tvChatroom)
            fadeOutTopChatroomEachView(topicImage)
        }
    }

    private fun fadeOutTopChatroomEachView(view: View) {
        if (view.visibility == View.VISIBLE) {
            view.animate()
                .setDuration(500)
                .alpha(0.0f)
                .setListener(fadeOutAnimatorListener(view))
        }
    }

    private fun fadeOutAnimatorListener(view: View): Animator.AnimatorListener {
        return object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                view.visibility = View.GONE
            }
        }
    }

    private fun initSwipeController() {
        lmSwipeController = LMSwipeController(
            requireContext(),
            swipeControllerActions = SwipeControllerActions { position ->
                val chatItem =
                    chatroomDetailAdapter[position] ?: return@SwipeControllerActions
                if (chatItem is ConversationViewData) {
                    if (chatItem.isFailed()) {
                        showFailedConversationMenu(chatItem, position)
                    } else if (chatItem.isSent()) {
                        setChatInputBoxViewType(CHAT_BOX_REPLY)
                        setReplyViewConversationData(chatItem, "swipe")
                    }
                } else if (chatItem is ChatroomViewData) {
                    setChatInputBoxViewType(CHAT_BOX_REPLY)
                    setReplyViewChatRoomData(chatItem, "swipe")
                }
            }
        )

        val itemTouchHelper = ItemTouchHelper(lmSwipeController!!)
        itemTouchHelper.attachToRecyclerView(binding.rvChatroom)
    }

    private fun initRichEditorSupport() {
        binding.inputBox.etAnswer.addListener(object : LikeMindsEditTextListener {
            override fun onMediaSelected(contentUri: Uri, mimeType: String) {
                mimeType.getMediaType()?.let { openMedia(contentUri, it) }
            }
        })
    }

    private fun openMedia(contentUri: Uri, mimeType: String) {
        val singleUri = SingleUriData.Builder()
            .uri(contentUri)
            .fileType(mimeType)
            .build()
        when {
            viewModel.isGifSupportEnabled() && singleUri.fileType == GIF -> {
                showGifEditScreen(singleUri)
            }

            singleUri.fileType == IMAGE -> {
                showPickImagesListScreen(singleUri)
            }
        }
    }

    /**
     * Initialize the top chatroom overlay, which shows if the chatroom header item is not in the screen viewport
     * @param chatroomViewData Chatroom object
     */
    private fun initTopChatroomView(chatroomViewData: ChatroomViewData) {
        if (viewModel.isDmChatroom()) {
            showTopView(false)
            return
        }

        binding.apply {
            val topic = chatroomViewData.topic
            if (topic == null) {
                setTopViewToChatroom(chatroomViewData)
            } else {
                tvChatroomMemberName.text = getString(R.string.lm_chat_current_topic_text)
                tvChatroomMemberName.setTypeface(tvChatroomMemberName.typeface, Typeface.BOLD)
                when {
                    topic.isDeleted() -> {
                        setTopViewToChatroom(chatroomViewData)
                    }

                    topic.ogTags != null -> {
                        setTopViewMemberImage(topic.memberViewData)

                        if (topic.ogTags.image != null) {
                            ImageBindingUtil.loadImage(
                                topicImage,
                                topic.ogTags.image
                            )
                        }
                        val answer =
                            ChatroomUtil.getTopicMediaData(requireContext(), topic)
                        tvChatroom.setText(answer, TextView.BufferType.SPANNABLE)
                    }

                    chatroomViewData.topic.attachmentCount > 0 -> {
                        setTopViewMemberImage(topic.memberViewData)

                        val answer =
                            ChatroomUtil.getTopicMediaData(requireContext(), topic)
                        tvChatroom.setText(answer, TextView.BufferType.SPANNABLE)

                        when (topic.attachments?.firstOrNull()?.type) {
                            IMAGE -> {
                                ImageBindingUtil.loadImage(
                                    topicImage,
                                    topic.attachments.firstOrNull()?.uri
                                )
                            }

                            VIDEO -> {
                                ImageBindingUtil.loadImage(
                                    topicImage,
                                    topic.attachments.firstOrNull()?.thumbnail
                                )
                            }

                            GIF -> {
                                ImageBindingUtil.loadImage(
                                    topicImage,
                                    topic.attachments.firstOrNull()?.thumbnail
                                )
                            }
                        }
                    }

                    topic.state == STATE_POLL -> {
                        setTopViewMemberImage(topic.memberViewData)

                        val answer = ChatroomUtil.getTopicMediaData(requireContext(), topic)
                        tvChatroom.setText(answer, TextView.BufferType.SPANNABLE)
                    }

                    else -> {
                        setTopViewMemberImage(topic.memberViewData)
                        MemberTaggingDecoder.decode(
                            tvChatroom,
                            topic.answer,
                            false,
                            LMChatAppearance.getTextLinkColor()
                        )
                    }
                }
            }

            initTopViewClick(topic)
            removeExtraViewAfterTopic(chatroomViewData)

            if (chatroomScrollListener.shouldShowTopChatRoom()) {
                fadeInTopChatroomView()
            }
        }
    }

    private fun setTopViewMemberImage(member: MemberViewData?) {
        if (viewModel.isDmChatroom()) {
            showTopView(false)
        } else {
            showTopView(true)
            if (chatroomScrollListener.shouldShowTopChatRoom()) {
                binding.memberImage.visibility = View.VISIBLE
                MemberImageUtil.setImage(
                    member?.imageUrl,
                    member?.name,
                    member?.sdkClientInfo?.uuid,
                    binding.memberImage
                )
            }
        }
    }

    private fun showTopView(show: Boolean) {
        if (chatroomScrollListener.shouldShowTopChatRoom() && show) {
            fadeInTopChatroomView()
        } else if (!show) {
            fadeOutTopChatroomView()
        }
    }

    private fun setTopViewToChatroom(chatroom: ChatroomViewData) {
        binding.apply {
            if (viewModel.isDmChatroom()) {
                showTopView(false)
            } else {
                showTopView(true)
                val creator = chatroom.memberViewData
                MemberImageUtil.setImage(
                    creator.imageUrl,
                    creator.name,
                    creator.sdkClientInfo.uuid,
                    memberImage
                )

                tvChatroomMemberName.text = creator.name
                tvChatroomMemberName.setTypeface(
                    tvChatroomMemberName.typeface,
                    Typeface.NORMAL
                )

                tvChatroomDate.text = chatroom.cardCreationTime
                MemberTaggingDecoder.decode(
                    tvChatroom,
                    chatroom.title,
                    false,
                    LMChatAppearance.getTextLinkColor()
                )
            }
        }
    }

    private fun initTopViewClick(topic: ConversationViewData?) {
        binding.viewTopBackground.setOnClickListener {
            if (topic == null || topic.isDeleted()) {
                scrollToExtremeTop()
            } else {
                scrollToRepliedAnswer(topic, topic.id)
            }
        }
    }

    private fun removeExtraViewAfterTopic(chatroomViewData: ChatroomViewData) {
        binding.apply {
            val topic = chatroomViewData.topic
            if (topic == null) {
                topicImage.visibility = View.GONE
            } else {
                tvChatroomDate.visibility = View.GONE
                ivDateDot.visibility = View.GONE
                when {
                    topic.isDeleted() -> {
                        topicImage.visibility = View.GONE
                    }

                    topic.ogTags != null -> {
                        if (topic.ogTags.image == null) {
                            topicImage.visibility = View.GONE
                        }
                    }

                    chatroomViewData.topic.attachmentCount > 0 -> {
                        when (topic.attachments?.firstOrNull()?.type) {
                            PDF, AUDIO, VOICE_NOTE -> {
                                topicImage.visibility = View.GONE
                            }
                        }
                    }

                    topic.state == STATE_POLL -> {
                        topicImage.visibility = View.GONE
                    }

                    else -> {
                        topicImage.visibility = View.GONE
                    }
                }
            }
        }
    }

    /**
     * Scroll to the bottom of the chatroom
     * Check if all bottom conversations are already added or else add it
     */
    private fun scrollToExtremeBottom() {
        val bottomConversation = getBottomConversation()
        if (bottomConversation == null) {
            scrollToPosition(SCROLL_DOWN)
        } else {
            if (viewModel.isAllBottomConversationsAdded(bottomConversation)) {
                scrollToPosition(SCROLL_DOWN)
            } else {
                chatroomScrollListener.topLoadingDone()
                viewModel.fetchBottomConversationsOnClick(
                    bottomConversation,
                    chatroomDetailAdapter.items()
                )
            }
        }
    }

    private fun setLastSeenTrueAndSaveDraftResponse() {
        try {
            viewModel.setLastSeenTrueAndSaveDraftResponse(
                chatroomId,
                binding.inputBox.etAnswer.text?.toString()
            )
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    fun consumeTouch(): Boolean {
        when {
            memberTagging.isShowing -> {
                memberTagging.hide()
                return true
            }

            isAttachmentsBarDismissing() -> return true

            isAttachmentsBarVisible() -> {
                initVisibilityOfAttachmentsBar(View.GONE)
                return true
            }

            inAppVideoPlayerPopup?.isShowing == true -> {
                if (!inAppVideoPlayerPopup!!.isFullScreen) {
                    dismissVideoPlayerPopup()
                } else {
                    changeVideoPlayerToPopupScreen()
                }
                return true
            }
        }
        return false
    }

    private fun isAttachmentsBarDismissing(): Boolean {
        return isAttachmentsSheetHiding
    }

    private fun isAttachmentsBarVisible(): Boolean {
        return binding.layoutAttachments.clBottomBar.visibility == View.VISIBLE
    }

    private fun initVisibilityOfAttachmentsBar(visibility: Int) {
        binding.layoutAttachments.apply {
            if (visibility == View.VISIBLE) {
                root.startRevealAnimation(binding.inputBox.ivAttachment)
            } else {
                isAttachmentsSheetHiding = true
                root.endRevealAnimation(binding.inputBox.ivAttachment) {
                    isAttachmentsSheetHiding = false
                }
            }
        }
    }

    /**
     * Scroll to a position on the recyclerview
     * @param pos Index of the item to scroll to
     */
    private fun scrollToPosition(pos: Int) {
        binding.rvChatroom.apply {
            if (pos.isValidIndex(chatroomDetailAdapter.itemCount)) {
                scrollToPosition(pos)
            } else {
                hideScrollBottomFab()
                scrollToPosition(chatroomDetailAdapter.itemCount - 1)
            }
        }
    }

    private fun stopAudioService() {
        mediaAudioServiceBound = false
        mediaAudioService?.stopMedia()
        mediaAudioService?.stopSelf()
        mediaAudioService?.stopForeground(true)
        mediaAudioService?.removeHandler()
        requireActivity().unbindService(serviceConnection!!)
    }

    private fun updateAudioVoiceNoteBinder(
        data: AttachmentViewData,
        parentPositionId: String,
        childPosition: Int,
    ) {
        if (data.type == AUDIO) {
            updateAudioConversationList(data, parentPositionId, childPosition)
        } else {
            updateVoiceNote(data, parentPositionId, childPosition)
        }
    }

    private fun updateAudioConversationList(
        data: AttachmentViewData,
        parentPositionId: String,
        childPosition: Int,
    ) {
        val parentPosition = chatroomDetailAdapter.items()
            .indexOfFirst { ((it is ConversationViewData) && (it.id == parentPositionId)) }

        if (parentPosition.isValidIndex()) {
            val item =
                chatroomDetailAdapter.items()[parentPosition] as? ConversationViewData
                    ?: return

            item.attachments?.set(childPosition, data)

            chatroomDetailAdapter.items()[parentPosition] = item
        }

        val itemConversationAudioBinding =
            (binding.rvChatroom.findViewHolderForAdapterPosition(
                parentPosition
            ) as? DataBoundViewHolder<*>)?.binding as? ItemConversationAudioBinding
                ?: return
        val itemAudioBinding = (itemConversationAudioBinding.audioView.binding.rvAudio
            .findViewHolderForAdapterPosition(childPosition) as? DataBoundViewHolder<*>)
            ?.binding as? ItemAudioBinding ?: return

        itemAudioBinding.attachment = data
        ChatroomConversationItemViewDataBinderUtil.initAudioItemView(itemAudioBinding, data)
    }

    private fun updateVoiceNote(
        data: AttachmentViewData,
        parentPositionId: String,
        childPosition: Int,
    ) {
        val parentPosition = chatroomDetailAdapter.items()
            .indexOfFirst { ((it is ConversationViewData) && (it.id == parentPositionId)) }

        if (parentPosition.isValidIndex()) {
            val item =
                chatroomDetailAdapter.items()[parentPosition] as? ConversationViewData
                    ?: return
            item.attachments?.set(childPosition, data)

            chatroomDetailAdapter.update(parentPosition, item)
        }
    }

    private fun postConversationWithMedia(mediaExtras: MediaExtras?) {
        postConversation(
            conversation = mediaExtras?.conversation?.trim() ?: "",
            fileUris = mediaExtras?.mediaUris
        )
    }

    override fun observeData() {
        super.observeData()
        observeInitialData()
        observePaginatedData()
        observeScrolledData()
        observeTaggingList()
        observeConversations()
        observeChatroomActions()
        observeLinkOgTags()
        observeNewOptionAddedLiveData()
        observePollResponse()
        observeTopic()
        observeMemberState()
        observeMemberBlocked()
        observeErrorMessage()
        observeDMStatus()
        observeChatRequestState()
        observeDMInitiatedForCM()
    }

    /**
     * Observes for initial data for loading chatroom and conversations
     */
    private fun observeInitialData() {
        viewModel.initialData.observe(viewLifecycleOwner) { initialData ->
            //chatroom is invalid or chatroom is deleted
            if (initialData == null) {
                ViewUtils.showShortToast(
                    context,
                    getString(R.string.lm_chat_this_chatroom_doesnt_exist)
                )
                requireActivity().finish()
                return@observe
            }

            //chatroom is not present
            if (initialData.chatRoom == null) {
                if (chatroomDetailExtras.loadingAfterSync) {
                    //chatroom id is invalid or user don't have access to it
                    ViewUtils.showShortToast(
                        context,
                        getString(R.string.lm_chat_this_chatroom_doesnt_exist)
                    )
                    requireActivity().finish()
                    return@observe
                }
                updateRecyclerViewForInitialData(initialData)
                return@observe
            }

            updateHeader(
                initialData.chatRoom.header ?: "Chatroom",
                initialData.chatRoom.isSecret ?: false
            )
            setInputBoxText()
            updateRecyclerViewForInitialData(initialData)
            viewModel.sendViewEvent(chatroomDetailExtras)
            invalidateActionsMenu()
            observeChatroom()
            enableAnswerPosting()
            checkForExternalSharedContent()
            handleDmChatrooms()
        }
    }

    /**
     * Observes for paginated data due to gesture scrolling by the user
     */
    private fun observePaginatedData() {
        viewModel.paginatedData.observe(viewLifecycleOwner) { data ->
            when (data.scrollState) {
                SCROLL_UP -> {
                    var lastDate: String? = null

                    // Find the date of the last item to be added in the adapter list
                    for (i in data.data.size - 1 downTo 0) {
                        val viewData = data.data[i]
                        if (viewData is ConversationViewData) {
                            lastDate = viewData.date
                            break
                        }

                        if (viewData is ChatroomDateViewData) {
                            lastDate = viewData.date
                            break
                        }
                    }

                    if (!lastDate.isNullOrEmpty()) {
                        // Find the index of the lastDate and remove it from the adapter if it is present, as it will be added again!
                        val dateIndex = getIndexOfDate(lastDate)
                        if (dateIndex != -1) {
                            chatroomDetailAdapter.removeIndex(dateIndex)
                        }
                    }

                    chatroomDetailAdapter.addAll(0, data.data)
                    binding.rvChatroom.post {
                        chatroomScrollListener.topLoadingDone()
                        updateChatroomPosition()
                        if (data.extremeScrollTo == SCROLL_UP) {
                            fadeOutTopChatroomView()
                            scrollToPosition(SCROLL_UP)
                        }
                        highlightConversation(data.repliedConversationId)
                        highlightChatroom(data.repliedChatRoomId)
                        removeChatroomItem()
                    }
                }

                SCROLL_DOWN -> {
                    val indexToAdd = getIndexOfAnyGraphicItem()
                    if (indexToAdd.isValidIndex()) {
                        chatroomDetailAdapter.addAll(indexToAdd, data.data)
                    } else {
                        chatroomDetailAdapter.addAll(data.data)
                    }
                    binding.rvChatroom.post {
                        chatroomScrollListener.bottomLoadingDone()
                        if (data.extremeScrollTo == SCROLL_DOWN) {
                            scrollToPosition(SCROLL_DOWN)
                        }
                        removeChatroomItem()
                    }
                }
            }
            getUnseenConversationsAndShow(data.data, false)
            filterConversationWithWidget(data.data.filterIsInstance<ConversationViewData>())
        }
    }

    /**
     * Observes for scrolled data which triggers on manual click scroll, like header click, scroll button click, etc
     */
    private fun observeScrolledData() {
        viewModel.scrolledData.observe(viewLifecycleOwner) { data ->
            val conversations = maintainAudioState(data.data)
            chatroomDetailAdapter.setItemsViaDiffUtilForChatroomDetail(conversations)
            when (data.scrollState) {
                SCROLL_UP -> {
                    chatroomScrollListener.topLoadingDone()
                    updateChatroomPosition()
                    if (!highlightConversation(data.repliedConversationId)) {
                        scrollToPosition(SCROLL_UP)
                        fadeOutTopChatroomView()
                        showUnseenCount(true)
                    }
                    highlightChatroom(data.repliedChatRoomId)
                    removeChatroomItem()
                }

                SCROLL_DOWN -> {
                    chatroomScrollListener.bottomLoadingDone()
                    scrollToPosition(SCROLL_DOWN)
                    updateChatroomPosition(-1)
                    fadeInTopChatroomView()
                    removeChatroomItem()
                }
            }
        }
    }

    private fun maintainAudioState(data: List<BaseViewType>): List<BaseViewType> {
        val conversations = data.toMutableList()
        val playedItemIndex = conversations.indexOfFirst {
            it is ConversationViewData && it.id == localParentConversationId
        }
        if (playedItemIndex.isValidIndex()) {
            val playedItem = conversations[playedItemIndex]
            conversations[playedItemIndex] = (playedItem as ConversationViewData)
                .toBuilder()
                .attachments(
                    playedItem.attachments?.mapIndexed { index, attachment ->
                        if (index == localChildPosition) {
                            attachment.toBuilder()
                                .mediaState(if (mediaAudioService?.isPlaying() == true) MEDIA_ACTION_PLAY else MEDIA_ACTION_PAUSE)
                                .build()
                        } else {
                            attachment
                        }
                    } as ArrayList<AttachmentViewData>?
                ).build()
        }
        return conversations
    }

    /**
     * Observes for member tagging list, This is a live observer which will update itself on addition of new members
     * [taggingData] contains first -> page called in api
     * second -> Community Members and Groups
     */
    private fun observeTaggingList() {
        helperViewModel.taggingData.observe(viewLifecycleOwner) { result ->
            MemberTaggingUtil.setMembersInView(memberTagging, result)
        }
    }

    /**
     * Observes chatroom conversations
     */
    private fun observeConversations() {
        viewModel.conversationEventFlow.onEach { response ->
            when (response) {
                is ChatroomDetailViewModel.ConversationEvent.UpdatedConversation -> {
                    //Observe for any updates to conversations already appended to the recyclerview, usually for
                    //deleted, edited, updating temporary conversations
                    getIndexedConversations(response.conversations).forEach { item ->
                        chatroomDetailAdapter.update(item.key, item.value)
                    }
                    filterConversationWithWidget(response.conversations)
                }

                is ChatroomDetailViewModel.ConversationEvent.NewConversation -> {
                    //Observe for any new conversations triggered by the database callback
                    val conversations =
                        getNonPresentConversations(response.conversations).toMutableList()

                    val indexOfHeaderConversation = conversations.indexOfFirst { conversation ->
                        conversation.state == STATE_HEADER
                    }
                    if (
                        indexOfHeaderConversation.isValidIndex() &&
                        !isConversationAlreadyPresent(conversations[indexOfHeaderConversation].id)
                    ) {
                        //Contains a header conversation
                        chatroomDetailAdapter.add(
                            0,
                            conversations[indexOfHeaderConversation]
                        )
                        updateChatroomPosition()
                        if (conversations.size > 1) {
                            conversations.removeAt(indexOfHeaderConversation)
                        }
                    }

                    if (conversations.isNotEmpty()) {
                        //Add the conversations to recyclerview

                        //get last conversation from the callback
                        val lastNewConversation = conversations.last()

                        val progressItemIndex = chatroomDetailAdapter.items().indexOfFirst {
                            it.viewType == ITEM_CONVERSATION_PROGRESS
                        }

                        if (progressItemIndex.isValidIndex()) {
                            chatroomDetailAdapter.removeIndex(progressItemIndex)
                        }

                        //Add the conversations to recyclerview
                        val indexToAdd = getIndexOfAnyGraphicItem()
                        if (indexToAdd.isValidIndex()) {
                            chatroomDetailAdapter.addAll(
                                indexToAdd,
                                conversations as List<BaseViewType>
                            )
                        } else {
                            chatroomDetailAdapter.addAll(conversations as List<BaseViewType>)
                        }

                        filterConversationWithWidget(conversations)

                        //Check if the new conversation is created by the user itself
                        if (
                            (conversations.count { conversation ->
                                conversation.memberViewData.sdkClientInfo.uuid == userPreferences.getUUID()
                            } == conversations.size)) {
                            scrollToPosition(SCROLL_DOWN)
                        } else {
                            //Add unseen conversations if present
                            unSeenCount += conversations.size
                            unSeenConversationsSet.addAll(conversations.map { conversation ->
                                conversation.id
                            })
                            showUnseenCount(false)
                        }

                        //last new conversation DM REJECTED conversation
                        if (lastNewConversation.state == STATE_DM_REJECTED
                            && viewModel.getLoggedInMemberId() ==
                            viewModel.getChatroomViewData()?.chatRequestedById
                        ) {
                            val lastConversationIndex =
                                getIndexOfConversation(lastNewConversation.id)

                            //add tap to undo to the last conversation with DM REJECTED state
                            handleTapToUndo(
                                lastConversationIndex,
                                lastNewConversation,
                                true
                            )
                        }
                    }
                }

                is ChatroomDetailViewModel.ConversationEvent.PostedConversation -> {
                    //Observe for new posted conversation by the user. This is a temporary conversation
                    if (!isConversationAlreadyPresent(response.conversation.id)) {
                        val indexToAdd = getIndexOfAnyGraphicItem()
                        var index = indexToAdd

                        // find the last date in the chatroom if [lastInsertedDate] is null
                        if (lastInsertedDate == null) {
                            chatroomDetailAdapter.items().forEach { item ->
                                if (item is ChatroomDateViewData) {
                                    lastInsertedDate = item.date
                                    return@forEach
                                }
                            }
                        }

                        // adds the date view only if the [lastInsertedDate] is different from the current conversation date and updates [lastInsertedDate]
                        if (lastInsertedDate?.trim() != response.conversation.date?.trim()) {
                            lastInsertedDate = response.conversation.date
                            chatroomDetailAdapter.add(viewModel.getDateView(response.conversation.date))
                        }

                        val progressItemIndex = chatroomDetailAdapter.items().indexOfFirst {
                            it.viewType == ITEM_CONVERSATION_PROGRESS
                        }

                        if (progressItemIndex.isValidIndex()) {
                            chatroomDetailAdapter.removeIndex(progressItemIndex)
                        }

                        if (indexToAdd.isValidIndex()) {
                            chatroomDetailAdapter.add(indexToAdd, response.conversation)
                        } else {
                            chatroomDetailAdapter.add(response.conversation)
                            index = chatroomDetailAdapter.itemCount - 1
                        }

                        filterConversationWithWidget(listOf(response.conversation))

                        //add progress bar to waiting response from AI bot
                        val isAIBotConversation = response.isBotConversation
                        if (isAIBotConversation) {
                            val progressViewData =
                                LMChatConversationProgressViewData.Builder().build()

                            val indexToAddProgress = getIndexOfAnyGraphicItem()
                            if (indexToAddProgress.isValidIndex(chatroomDetailAdapter.items())) {
                                chatroomDetailAdapter.add(indexToAddProgress, progressViewData)
                            } else {
                                chatroomDetailAdapter.add(progressViewData)
                            }
                        }

                        //add tap to undo if dm is rejected and the logged in member has rejected the DM request
                        if (response.conversation.state == STATE_DM_REJECTED
                            && viewModel.getLoggedInMemberId() ==
                            viewModel.getChatroomViewData()?.chatRequestedById
                        ) {
                            handleTapToUndo(
                                index,
                                response.conversation,
                                true
                            )
                        }

                        scrollToPosition(SCROLL_DOWN)
                    }
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)
    }

    //filter conversation with widgets and return to customer
    private fun filterConversationWithWidget(conversations: List<ConversationViewData>) {
        val filteredHashMap = HashMap<String?, WidgetViewData?>()

        conversations.filter { conversation ->
            conversation.widgetViewData != null
        }.forEach {
            filteredHashMap[it.id] = it.widgetViewData
        }

        SDKApplication.getLikeMindsCallback()
            ?.getWidgetCallback(filteredHashMap)
    }

    /**
     * Sets the initial data to the recyclerview
     * @param initialData The received data containing conversations, scroll index and chatroom
     */
    private fun updateRecyclerViewForInitialData(initialData: InitialViewData) {
        chatroomDetailAdapter.replace(initialData.data)
        binding.rvChatroom.apply {
            post {
                when {
                    reportedConversationId.isNotEmpty() -> {
                        scrolledConversationPosition =
                            getIndexOfConversation(reportedConversationId)
                        scrollToPositionWithOffset(scrolledConversationPosition)
                    }

                    searchedConversationId.isNotEmpty() -> {
                        scrolledConversationPosition =
                            getIndexOfConversation(searchedConversationId)
                        scrollToPositionWithOffset(scrolledConversationPosition)
                    }

                    scrollToHighlightTitle -> {
                        scrolledConversationPosition =
                            getIndexOfChatRoom(chatroomDetailExtras.chatroomId)
                        scrollToPositionWithOffset(scrolledConversationPosition)
                    }

                    else -> {
                        this@ChatroomDetailFragment.scrollToPosition(initialData.scrollPosition)
                    }
                }
                visibility = View.VISIBLE
                post {
                    //Added for a crash fix due to fragment getting unattached
                    if (context != null) {
                        getChatroomViewData()?.let { chatroom ->
                            updateChatroomPosition()
                            initTopChatroomView(chatroom)
                            getUnseenConversationsAndShow(
                                initialData.data,
                                initialData.scrollPosition == SCROLL_UP
                            )
                        }
                    }
                }
            }
        }
        filterConversationWithWidget(initialData.data.filterIsInstance<ConversationViewData>())
    }

    /**
     * Update the recyclerview position of chatroom in the scroll listener
     * @param index Pass the index if it is already knows. Pass -1 to reset the index in the scroll listener
     */
    private fun updateChatroomPosition(index: Int? = null) {
        if (index != null) {
            chatroomScrollListener.setChatRoomPosition(index)
            return
        }
        val indexOfChatroom = chatroomDetailAdapter.items().indexOfFirst {
            it is ChatroomViewData
        }
        if (indexOfChatroom.isValidIndex()) {
            chatroomScrollListener.setChatRoomPosition(indexOfChatroom)
        }
    }

    /**
     * Retrieves unseen conversations from the data set and show unseen count on the scroll FAB
     * @param data The recyclerview data set to show
     * @param showBlank Show the scroll FAB even though there are no unseen conversations
     */
    private fun getUnseenConversationsAndShow(
        data: List<BaseViewType>,
        showBlank: Boolean = false,
    ) {
        val unseen = data.asSequence()
            .filterIsInstance(ConversationViewData::class.java)
            .filter {
                it.lastSeen == false
            }.map {
                it.id
            }
        unSeenConversationsSet.addAll(unseen)
        showUnseenCount(showBlank)
    }

    // updates the header name on chatroom
    private fun updateHeader(header: String, isSecretChatRoom: Boolean) {
        binding.apply {
            if (viewModel.isDmChatroom()) {
                tvToolbarSubTitle.hide()
                val member = viewModel.getOtherDmMember() ?: return
                tvToolbarTitle.text = member.name
                ivMemberImage.show()
                MemberImageUtil.setImage(
                    member.imageUrl,
                    member.name,
                    member.sdkClientInfo.uuid,
                    imageView = ivMemberImage,
                    showRoundImage = true
                )
            } else {
                tvToolbarTitle.text = header
                ivMemberImage.hide()
                tvToolbarSubTitle.show()
            }

            //secret chatroom lock icon
            if (isSecretChatRoom) {
                ivLock.show()
            } else {
                ivLock.hide()
            }
        }
    }

    private fun setInputBoxText() {
        binding.apply {
            val draftConversation = getChatroomViewData()?.draftConversation
            if (!draftConversation.isNullOrEmpty()) {

                inputBox.etAnswer.setText(draftConversation)
                inputBox.etAnswer.setSelection(draftConversation.length)
                fabSend.show()
                fabMic.hide()
            }
        }
    }

    /**
     * Observes for chatroom detail data changes
     */
    private fun observeChatroom() {
        viewModel.chatroomDetailLiveData.observe(viewLifecycleOwner) {
            val chatroomDetail = it ?: return@observe
            if (chatroomDetail.chatroom == null || chatroomDetail.chatroom.isDeleted()) {
                childFragmentManager.popBackStack()
                return@observe
            } else if (chatroomDetail.chatroom.isSecret == true && chatroomDetail.canAccessSecretChatRoom == false) {
                childFragmentManager.popBackStack()
                return@observe
            }
            when {
                viewModel.isAnnouncementChatroom() -> {
                    updateUIForAnnouncementRoom()
                }

                else -> {
                    if (chatroomDetail.participantCount != null) {
                        updateParticipantsCount(chatroomDetail.participantCount)
                    }
                }
            }
            invalidateActionsMenu()
            updateChatroom(chatroomDetail.chatroom)
        }
    }

    // updates the UI for announcement room
    private fun updateUIForAnnouncementRoom() {
        binding.apply {
            val chatroomViewData = getChatroomViewData() ?: return
            val communityName = chatroomViewData.communityName
            // Updates community name in the header bar for Introduction room only
            if (communityName.isNotEmpty()) {
                tvToolbarSubTitle.show()
                tvToolbarSubTitle.text = communityName
            }
            // Disable swipe
            lmSwipeController?.setSwipeEnabled(!isNotAdminInAnnouncementRoom())
        }
    }

    // updates participants count as per the count
    private fun updateParticipantsCount(count: Int) {
        if (!viewModel.isDmChatroom()) {
            binding.tvToolbarSubTitle.apply {
                show()
                text = resources.getQuantityString(
                    R.plurals.lm_chat_participants_s,
                    count,
                    count
                )
            }
        }
    }

    // updates the chatroom in the adapter
    private fun updateChatroom(chatroom: ChatroomViewData?) {
        val chatroomPosition = getIndexOfChatroom() ?: return
        chatroomDetailAdapter.update(chatroomPosition, chatroom)
    }

    /**
     * Observes for chatroom actions
     */
    private fun observeChatroomActions() {
        // Observes for chatroom leave
        viewModel.leaveSecretChatroomResponse.observe(viewLifecycleOwner) {
            ViewUtils.showShortToast(
                requireContext(),
                requireContext().getString(R.string.lm_chat_you_have_left_the_chat_room)
            )
            //send a result back to the user
            if (!viewModel.isAdminMember()) {
                setChatroomDeletedStatus()
            }
            activity?.onBackPressed()
        }

        viewModel.deleteConversationsResponse.observe(viewLifecycleOwner) { size ->
            if (size > 1) {
                ViewUtils.showShortToast(
                    requireContext(),
                    requireContext().getString(R.string.lm_chat_messages_deleted)
                )
            } else {
                ViewUtils.showShortToast(
                    requireContext(),
                    requireContext().getString(R.string.lm_chat_message_deleted)
                )
            }
        }
    }

    private fun setChatroomDeletedStatus() {
        chatroomResultExtras = chatroomResultExtras?.also {
            chatroomResultExtras?.toBuilder()
                ?.chatroomId(chatroomId)
                ?.isChatroomDeleted(true)
                ?.build()
        } ?: run {
            ChatroomDetailResultExtras.Builder()
                .chatroomId(chatroomId)
                .isChatroomDeleted(true)
                .build()
        }
        setChatroomDetailActivityResult()
    }

    /**
     * observes linkOgTags live data
     */
    private fun observeLinkOgTags() {
        viewModel.linkOgTags.observe(viewLifecycleOwner) {
            initLinkView(it)
        }
    }


    private fun initLinkView(linkOgTags: LinkOGTagsViewData?) {
        binding.inputBox.apply {
            hideLinkProgress()
            when {
                isReplyViewVisible() -> return
                linkOgTags == null -> setChatInputBoxViewType(CHAT_BOX_NORMAL)

                linkOgTags.title.isNullOrEmpty() &&
                        linkOgTags.image.isNullOrEmpty() -> {
                    setChatInputBoxViewType(CHAT_BOX_NORMAL)
                }

                etAnswer.editableText.toString().trim().getUrlIfExist()
                    .isNullOrEmpty() -> viewModel.clearLinkPreview()

                else -> {
                    setChatInputBoxViewType(CHAT_BOX_LINK)

                    val title = if (!linkOgTags.title.isNullOrEmpty()) {
                        linkOgTags.title
                    } else {
                        getString(R.string.lm_chat_link)
                    }
                    viewLink.tvLinkTitle.text = title

                    viewLink.tvLinkDescription.isVisible =
                        !linkOgTags.description.isNullOrEmpty()
                    if (!linkOgTags.description.isNullOrEmpty()) {
                        viewLink.tvLinkDescription.text =
                            linkOgTags.description
                    }

                    ImageBindingUtil.loadImage(
                        viewLink.ivLink,
                        linkOgTags.image,
                        placeholder = R.drawable.lm_chat_ic_link_primary_40dp
                    )

                    viewLink.tvLinkUrl.text = linkOgTags.url

                    viewLink.ivCross.setOnClickListener {
                        viewModel.removeLinkPreview()
                        viewLink.clLink.visibility = View.GONE
                        setChatInputBoxViewType(CHAT_BOX_NORMAL)
                    }

                    viewLink.clLink.setOnClickListener {
                        linkOgTags.url?.let { url ->
                            externalLinkClicked(null, url, null)
                        }
                    }
                }
            }
        }
    }

    private fun hideLinkProgress() {
        ProgressHelper.hideProgress(binding.inputBox.viewLink.progressBar)
    }

    private fun observeNewOptionAddedLiveData() {
        viewModel.addOptionResponse.observe(viewLifecycleOwner) {
            ProgressHelper.hideProgress(binding.progressBar)
        }
    }

    private fun observePollResponse() {
        viewModel.pollAnswerUpdated.observe(viewLifecycleOwner) {
            val conversationId = it?.pollViewDataList?.firstOrNull()?.parentId
            onPollSubmitComplete(it, conversationId)
        }
    }

    private fun onPollSubmitComplete(
        pollInfoData: PollInfoData?,
        conversationId: String? = null,
    ) {
        //remove Follow button if showing
        if (pollInfoData?.pollType == POLL_TYPE_INSTANT) {
            val message = if (removeFollowView()) {
                getString(R.string.lm_chat_your_vote_submitted_successfully_poll_room_has_been_added_to_your_followed_chat_rooms)
            } else {
                getString(R.string.lm_chat_your_vote_submitted_successfully)
            }
            ViewUtils.showShortToast(requireContext(), message)
        } else {
            if (removeFollowView()) {
                ViewUtils.showShortToast(
                    requireContext(),
                    getString(R.string.lm_chat_added_to_your_joined_chat_rooms)
                )
            }
            PollVoteSubmitSuccessDialog.newInstance(
                childFragmentManager,
                DateUtil.getPollExpireTimeString(
                    pollInfoData?.expiryTime
                )
            )
            viewModel.sendPollVotingEditedEvent(conversationId)
        }
        viewModel.updateChatRoomFollowStatus(true)
    }


    private fun observeTopic() {
        viewModel.setTopicResponse.observe(viewLifecycleOwner) { response ->
            if (getChatroomViewData() != null) {
                initTopChatroomView(getChatroomViewData()!!)
            }
            val hasAnswer = !response?.answer.isNullOrEmpty()
            val hasAttachments = response?.attachmentCount!! > 0
            val answer = when {
                hasAnswer -> "${requireContext().getString(R.string.lm_chat_changed_current_topic_to)} ${response.answer}"
                hasAttachments -> when (response.attachments?.firstOrNull()?.type) {
                    IMAGE -> requireContext().getString(R.string.lm_chat_set_a_photo_message_to_current_topic)
                    VIDEO -> requireContext().getString(R.string.lm_chat_set_a_video_message_to_current_topic)
                    PDF -> requireContext().getString(R.string.lm_chat_set_a_document_message_to_current_topic)
                    GIF -> requireContext().getString(R.string.lm_chat_set_a_gif_message_to_current_topic)
                    AUDIO -> requireContext().getString(R.string.lm_chat_set_an_audio_message_to_current_topic)
                    VOICE_NOTE -> requireContext().getString(R.string.lm_chat_set_a_voice_note_to_current_topic)
                    else -> ""
                }

                else -> ""
            }

            val conversation =
                viewModel.createTemporaryAutoFollowAndTopicConversation(
                    STATE_TOPIC, answer
                )

            val indexToAdd = getIndexOfAnyGraphicItem()
            if (indexToAdd.isValidIndex()) {
                chatroomDetailAdapter.add(indexToAdd, conversation)
            } else {
                chatroomDetailAdapter.add(conversation)
            }
        }
    }

    private fun getIndexOfAnyGraphicItem(): Int {
        return chatroomDetailAdapter.items().indexOfFirst {
            chatroomDetailAdapter.getGraphicViewTypes().contains(it.viewType)
        }
    }

    private fun handleScreenshot(options: List<String>?) {
        if (options?.contains(SCREEN_RECORD) == false) {
            activity?.window?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun observeMemberState() {
        viewModel.canMemberRespond.observe(viewLifecycleOwner) { canMemberRespond ->
            if (canMemberRespond != null) {
                initAttachmentsPickerBarView()
            }
            setChatInputBoxViewType(CHAT_BOX_NORMAL)
        }
        viewModel.canMemberCreatePoll.observe(viewLifecycleOwner) { canMemberCreatePoll ->
            if (canMemberCreatePoll != null) {
                initAttachmentsPickerBarView()
            }
        }
    }

    // observes memberBlocked live data
    private fun observeMemberBlocked() {
        viewModel.memberBlocked.observe(viewLifecycleOwner) { memberBlocked ->
            showTapToUndoLocally = false
            setChatInputBoxViewType(
                CHAT_BOX_NORMAL,
                viewModel.showDM.value,
                memberBlocked
            )
            val message = if (memberBlocked) {
                getString(R.string.lm_chat_member_blocked)
            } else {
                getString(R.string.lm_chat_member_unblocked)
            }
            ViewUtils.showShortToast(requireContext(), message)
        }
    }

    // observes error messages
    private fun observeErrorMessage() {
        viewModel.errorMessageFlow.onEach { response ->
            when (response) {
                is ChatroomDetailViewModel.ErrorMessageEvent.SendDMRequest -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.BlockMember -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.AddPollOption -> {
                    ViewUtils.showShortToast(
                        context,
                        context?.getString(R.string.lm_chat_sorry_unfortunately_we_could_not_submit_your_choices_please_try_again)
                    )
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.DeleteConversation -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.EditChatroomTitle -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.EditConversation -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.FollowChatroom -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.LeaveSecretChatroom -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.PostConversation -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.SetChatroomTopic -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.SubmitPoll -> {
                    ViewUtils.showShortToast(
                        context,
                        context?.getString(R.string.lm_chat_sorry_unfortunately_we_could_not_submit_your_choices_please_try_again)
                    )
                }

                is ChatroomDetailViewModel.ErrorMessageEvent.MuteChatroom -> {
                    ViewUtils.showShortToast(requireContext(), response.errorMessage)
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)
    }

    // observes the showDM LiveData
    private fun observeDMStatus() {
        viewModel.showDM.observe(viewLifecycleOwner) { showDM ->
            setChatInputBoxViewType(
                CHAT_BOX_NORMAL,
                showDM
            )
        }
    }

    // observes updatedChatRequestState live data
    private fun observeChatRequestState() {
        viewModel.updatedChatRequestState.observe(viewLifecycleOwner) {
            setChatInputBoxViewType(
                CHAT_BOX_NORMAL,
                viewModel.showDM.value
            )
        }
    }

    // observes dmInitiatedForCM live data
    private fun observeDMInitiatedForCM() {
        viewModel.dmInitiatedForCM.observe(viewLifecycleOwner) { dmInitiatedForCM ->
            if (dmInitiatedForCM && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)) {
                syncChatroom()
            }
        }
    }

    /**--------------------------------
     * Media Picker Utility
    ---------------------------------*/

    private val browseDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onPdfPicked(result.data)
            }
        }

    private fun onPdfPicked(data: Intent?) {
        val uris = MediaUtils.getExternalIntentPickerUris(data)
        viewModel.fetchUriDetails(requireContext(), uris) {
            val mediaUris = MediaUtils.convertMediaViewDataToSingleUriData(
                requireContext(), it
            )
            if (mediaUris.isNotEmpty()) {
                showPickDocumentsListScreen(*mediaUris.toTypedArray(), saveInCache = true)
            }
        }
    }

    private val browseMediaLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                onMediaPickedFromGallery(result.data)
            }
        }

    private fun onMediaPickedFromGallery(data: Intent?) {
        val uris = MediaUtils.getExternalIntentPickerUris(data)
        viewModel.fetchUriDetails(requireContext(), uris) {
            val mediaUris = MediaUtils.convertMediaViewDataToSingleUriData(
                requireContext(), it
            )
            if (mediaUris.isNotEmpty()) {
                showPickImagesListScreen(*mediaUris.toTypedArray(), saveInCache = true)
            }
        }
    }

    private fun checkMediaPickedResult(result: MediaPickerResult?) {
        if (result != null) {
            when (result.mediaPickerResultType) {
                MEDIA_RESULT_BROWSE -> {
                    if (InternalMediaType.isPDF(result.mediaTypes)) {
                        val intent = AndroidUtils.getExternalDocumentPickerIntent(
                            allowMultipleSelect = result.allowMultipleSelect
                        )
                        browseDocumentLauncher.launch(intent)
                    } else {
                        val intent = AndroidUtils.getExternalPickerIntent(
                            result.mediaTypes,
                            result.allowMultipleSelect,
                            result.browseClassName
                        )
                        if (intent != null) {
                            browseMediaLauncher.launch(intent)
                        }
                    }
                }

                MEDIA_RESULT_PICKED -> {
                    onMediaPicked(result)
                }
            }
        }
    }

    private fun onMediaPicked(result: MediaPickerResult) {
        val data =
            MediaUtils.convertMediaViewDataToSingleUriData(requireContext(), result.medias)
        if (data.isNotEmpty()) {
            when {
                InternalMediaType.isPDF(result.mediaTypes) -> {
                    showPickDocumentsListScreen(*data.toTypedArray())
                }

                InternalMediaType.isAudio(result.mediaTypes) -> {
                    showPickAudioListScreen(*data.toTypedArray())
                }

                else -> {
                    showPickImagesListScreen(*data.toTypedArray())
                }
            }
        }
    }

    private var documentSendLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val data = ExtrasUtil.getParcelable(
                    extras,
                    BUNDLE_MEDIA_EXTRAS,
                    MediaExtras::class.java
                )
                postConversationWithMedia(data)
            } else if (result?.resultCode == Activity.RESULT_FIRST_USER) {
                activity?.finish()
            }
        }

    private fun showPickDocumentsListScreen(
        vararg mediaUris: SingleUriData,
        saveInCache: Boolean = false,
        isExternallyShared: Boolean = false,
        textAlreadyPresent: String? = null,
    ) {
        val attachments = if (saveInCache) {
            AndroidUtils.moveAttachmentToCache(requireContext(), *mediaUris)
        } else {
            mediaUris.asList()
        }
        val text = if (!textAlreadyPresent.isNullOrEmpty()) {
            textAlreadyPresent
        } else {
            memberTagging.replaceSelectedMembers(binding.inputBox.etAnswer.editableText)
        }

        val arrayList = ArrayList<SingleUriData>()
        arrayList.addAll(attachments)

        val mediaExtras = MediaExtras.Builder()
            .mediaScreenType(MEDIA_DOCUMENT_SEND_SCREEN)
            .mediaUris(arrayList)
            .chatroomId(chatroomId)
            .chatroomName(getChatroomViewData()?.header)
            .communityName(getChatroomViewData()?.communityName)
            .chatroomType(getChatroomViewData()?.getTypeName())
            .searchKey(chatroomDetailExtras.searchKey)
            .communityId(communityId?.toIntOrNull())
            .text(text)
            .isExternallyShared(isExternallyShared)
            .isSecretChatroom(getChatroomViewData()?.isSecret)
            .isTaggingEnabled(!viewModel.isDmChatroom())
            .build()
        if (attachments.isNotEmpty()) {
            val intent =
                MediaActivity.getIntent(requireContext(), mediaExtras, activity?.intent?.clipData)
            documentSendLauncher.launch(intent)
        }
    }

    private val audioSendLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val data = ExtrasUtil.getParcelable(
                    extras,
                    BUNDLE_MEDIA_EXTRAS,
                    MediaExtras::class.java
                )
                postConversationWithMedia(data)
            } else if (result?.resultCode == Activity.RESULT_FIRST_USER) {
                activity?.finish()
            }
        }

    private fun showPickAudioListScreen(
        vararg mediaUris: SingleUriData,
        saveInCache: Boolean = false,
        isExternallyShared: Boolean = false,
        textAlreadyPresent: String? = null,
    ) {
        val attachments = if (saveInCache) {
            AndroidUtils.moveAttachmentToCache(requireContext(), *mediaUris)
        } else {
            mediaUris.asList()
        }

        if (attachments.isNotEmpty()) {
            val text = if (!textAlreadyPresent.isNullOrEmpty()) {
                textAlreadyPresent
            } else {
                memberTagging.replaceSelectedMembers(binding.inputBox.etAnswer.editableText)
            }

            val arrayList = ArrayList<SingleUriData>()
            arrayList.addAll(attachments)

            val mediaExtras = MediaExtras.Builder()
                .mediaScreenType(MEDIA_AUDIO_EDIT_SEND_SCREEN)
                .mediaUris(arrayList)
                .chatroomId(chatroomId)
                .chatroomName(getChatroomViewData()?.header)
                .communityId(communityId?.toIntOrNull())
                .communityName(getChatroomViewData()?.communityName)
                .chatroomType(getChatroomViewData()?.getTypeName())
                .searchKey(chatroomDetailExtras.searchKey)
                .text(text)
                .isExternallyShared(isExternallyShared)
                .isSecretChatroom(getChatroomViewData()?.isSecret)
                .isTaggingEnabled(!viewModel.isDmChatroom())
                .build()
            if (attachments.isNotEmpty()) {
                val intent = MediaActivity.getIntent(
                    requireContext(),
                    mediaExtras,
                    activity?.intent?.clipData
                )
                audioSendLauncher.launch(intent)
            }
        }
    }

    private var imageVideoSendLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val data = ExtrasUtil.getParcelable(
                    extras,
                    BUNDLE_MEDIA_EXTRAS,
                    MediaExtras::class.java
                )
                postConversationWithMedia(data)
            } else if (result?.resultCode == Activity.RESULT_FIRST_USER) {
                activity?.finish()
            }
        }

    private fun showGifEditScreen(singleUriData: SingleUriData) {
        val attachments = AndroidUtils.moveAttachmentToCache(requireContext(), singleUriData)
        if (attachments.isEmpty()) return

        onScreenChanged()
        val arrayList = ArrayList<SingleUriData>()
        arrayList.addAll(attachments)
        val text = memberTagging.replaceSelectedMembers(binding.inputBox.etAnswer.editableText)
        val mediaExtras = MediaExtras.Builder()
            .mediaScreenType(MEDIA_GIF_SEND_SCREEN)
            .mediaUris(arrayList)
            .chatroomId(chatroomId)
            .communityId(communityId?.toIntOrNull())
            .text(text)
            .isSecretChatroom(getChatroomViewData()?.isSecret)
            .isTaggingEnabled(!viewModel.isDmChatroom())
            .build()
        val intent =
            MediaActivity.getIntent(requireContext(), mediaExtras, activity?.intent?.clipData)
        imageVideoSendLauncher.launch(intent)
    }

    private fun showPickImagesListScreen(
        vararg mediaUris: SingleUriData,
        saveInCache: Boolean = false,
        isExternallyShared: Boolean = false,
        textAlreadyPresent: String? = null,
    ) {
        val attachments = if (saveInCache) {
            AndroidUtils.moveAttachmentToCache(requireContext(), *mediaUris)
        } else {
            mediaUris.asList()
        }
        if (attachments.isNotEmpty()) {
            val text = if (!textAlreadyPresent.isNullOrEmpty()) {
                textAlreadyPresent
            } else {
                memberTagging.replaceSelectedMembers(binding.inputBox.etAnswer.editableText)
            }

            val arrayList = ArrayList<SingleUriData>()
            arrayList.addAll(attachments)

            val mediaExtras = MediaExtras.Builder()
                .mediaScreenType(MEDIA_CONVERSATION_EDIT_SCREEN)
                .mediaUris(arrayList)
                .chatroomId(chatroomId)
                .chatroomName(getChatroomViewData()?.header)
                .communityName(getChatroomViewData()?.communityName)
                .chatroomType(getChatroomViewData()?.getTypeName())
                .searchKey(chatroomDetailExtras.searchKey)
                .communityId(communityId?.toIntOrNull())
                .text(text)
                .isExternallyShared(isExternallyShared)
                .isSecretChatroom(getChatroomViewData()?.isSecret)
                .isTaggingEnabled(!viewModel.isDmChatroom())
                .allowMultipleSelect(!viewModel.isOtherUserAIBot())
                .build()

            val intent =
                MediaActivity.getIntent(requireContext(), mediaExtras, activity?.intent?.clipData)
            imageVideoSendLauncher.launch(intent)
        }
    }

    private fun updateChatroomFollowStatus(updatedFollowStatus: Boolean) {
        chatroomResultExtras = chatroomResultExtras?.also {
            chatroomResultExtras?.toBuilder()
                ?.chatroomId(chatroomId)
                ?.isChatroomFollowChanged(true)
                ?.updatedFollowStatus(updatedFollowStatus)
                ?.build()
        } ?: run {
            ChatroomDetailResultExtras.Builder()
                .chatroomId(chatroomId)
                .isChatroomFollowChanged(true)
                .updatedFollowStatus(updatedFollowStatus)
                .build()
        }
        setChatroomDetailActivityResult()
    }

    fun setChatroomDetailActivityResult() {
        val intent = Intent().apply {
            putExtras(Bundle().apply {
                putParcelable(ARG_CHATROOM_DETAIL_RESULT_EXTRAS, chatroomResultExtras)
            })
        }
        activity?.setResult(Activity.RESULT_OK, intent)
    }

    /**--------------------------------
     * Remove graphic utility functions
    ---------------------------------*/

    private fun removeFollowView(): Boolean {
        if (getChatroomViewData()?.showFollowTelescope == true) {
            val index = chatroomDetailAdapter.items().indexOfLast {
                it is FollowItemViewData
            }
            if (index.isValidIndex(chatroomDetailAdapter.items())) {
                chatroomDetailAdapter.removeIndex(index)
                return true
            }
        }
        return false
    }

    /**------------------------------------------------------------
     * Action mode listeners
    ---------------------------------------------------------------*/

    /**
     * Performs action based on menu item click from action mode on toolbar
     * @param item Menu item
     */
    override fun onActionItemClick(item: MenuItem?) {
        when (item?.itemId) {
            R.id.menu_item_reply -> {
                if (selectedConversations.isNotEmpty()) {
                    val conversation = selectedConversations.values.firstOrNull()
                    if (conversation != null) {
                        setChatInputBoxViewType(CHAT_BOX_REPLY)
                        setReplyViewConversationData(conversation, "press")
                    }
                } else {
                    selectedChatRoom?.let {
                        setChatInputBoxViewType(CHAT_BOX_REPLY)
                        setReplyViewChatRoomData(it, "press")
                    }
                }
            }

            R.id.menu_item_copy -> {
                copyConversations()
            }

            R.id.menu_item_edit -> {
                if (selectedConversations.isNotEmpty()) {
                    val conversation = selectedConversations.values.firstOrNull()
                    if (conversation != null) {
                        setChatInputBoxViewType(CHAT_BOX_REPLY)
                        setEditMessageViewConversationData(conversation)
                    }
                } else {
                    selectedChatRoom?.let {
                        when {
                            ChatroomType.isAnnouncementRoom(it.type) -> {
                                //todo
//                                editAnnouncementMessageClicked(it)
                            }

                            else -> {
                                setChatInputBoxViewType(CHAT_BOX_REPLY)
                                setEditMessageViewChatRoomData(it)
                            }
                        }
                    }
                }
            }

            R.id.menu_item_delete -> {
                showDeleteMessageConfirmationPopup(selectedConversations.values.toList())
            }

            R.id.menu_item_report -> {
                reportConversation()
            }

            R.id.menu_item_set_topic -> {
                setChatroomTopic()
            }
        }
    }

    override fun onActionItemUpdate(
        item: Menu?,
        actionModeData: ChatroomDetailActionModeData?,
    ) {
        actionModeData?.let {
            item?.findItem(R.id.menu_item_reply)?.isVisible = it.showReplyAction
            item?.findItem(R.id.menu_item_copy)?.isVisible = it.showCopyAction
            item?.findItem(R.id.menu_item_edit)?.isVisible = it.showEditAction
            item?.findItem(R.id.menu_item_delete)?.isVisible = it.showDeleteAction
            item?.findItem(R.id.menu_item_report)?.isVisible = it.showReportAction
            item?.findItem(R.id.menu_item_set_topic)?.isVisible = it.showSetAsTopic
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActionModeDestroyed() {
        selectedChatRoom = null
        selectedConversations.clear()
        chatroomDetailAdapter.notifyDataSetChanged()
    }

    /**------------------------------------------------------------
     * Adapter listeners
    ---------------------------------------------------------------*/

    override fun follow(value: Boolean, source: String) {
        if (value) {
            removeFollowView()
            if (!isSecretChatRoom()) {
                ViewUtils.showShortSnack(
                    binding.root,
                    getString(R.string.lm_chat_added_to_your_joined_chat_rooms)
                )
            }
        } else {
            ViewUtils.showShortSnack(
                binding.root,
                getString(R.string.lm_chat_removed_from_your_joined_chat_rooms)
            )
        }
        viewModel.followChatroom(
            chatroomDetailExtras.chatroomId,
            value,
            source
        )
        updateChatroomFollowStatus(value)

        if (isSecretChatRoom() && value) {
            setChatInputBoxViewType(CHAT_BOX_NORMAL)
        }
    }

    override fun getChatRoomType(): Int? {
        return getChatroomViewData()?.type
    }

    override fun getChatRoom(): ChatroomViewData? {
        return getChatroomViewData()
    }

    override fun updateSeenFullConversation(position: Int, alreadySeenFullConversation: Boolean) {
        val item = chatroomDetailAdapter[position]
        if (item is ConversationViewData) {
            val newViewData = item.toBuilder()
                .alreadySeenFullConversation(alreadySeenFullConversation)
                .build()
            chatroomDetailAdapter.update(position, newViewData)
        } else if (item is ChatroomViewData) {
            val newViewData = item.toBuilder()
                .alreadySeenFullConversation(alreadySeenFullConversation)
                .build()
            chatroomDetailAdapter.update(position, newViewData)
        }
    }

    override fun isSelectionEnabled(): Boolean {
        return selectedConversations.isNotEmpty() || selectedChatRoom != null
    }

    override fun isChatRoomSelected(chatRoomId: String): Boolean {
        return selectedChatRoom?.id == chatRoomId
    }

    override fun isConversationSelected(conversationId: String): Boolean {
        return selectedConversations.containsKey(conversationId)
    }

    /**
     * Invoked on click of a replied conversation. The motive is to scroll the recyclerview to the
     * original conversation so that it comes in the viewport of the screen and gets highlighted.
     * @param conversation The current clicked conversation object
     * @param repliedConversationId Id of the original conversation in which [conversation] was replied.
     */
    override fun scrollToRepliedAnswer(
        conversation: ConversationViewData,
        repliedConversationId: String,
    ) {
        if (!highlightConversation(repliedConversationId)) {
            viewModel.fetchRepliedConversationOnClick(
                repliedConversationId,
                chatroomDetailAdapter.items()
            )
        }
    }

    /**
     * Invoked on click of a replied chatRoom. The motive is to scroll the recyclerview to the
     * original chatroom so that it comes in the viewport of the screen and gets highlighted.
     * @param repliedChatroomId Id of the original chatRoom in which conversation was replied.
     */
    override fun scrollToRepliedChatroom(repliedChatroomId: String) {
        if (!highlightChatroom(repliedChatroomId)) {
            scrollToExtremeTop(repliedChatroomId)
        }
    }

    override fun isScrolledConversation(position: Int): Boolean {
        if (scrolledConversationPosition != -1 && scrolledConversationPosition == position) {
            scrolledConversationPosition = -1
            return true
        }
        return false
    }

    override fun isReportedConversation(conversationId: String?): Boolean {
        return reportedConversationId == conversationId
    }

    override fun showActionDialogForReportedMessage() {
        takeActionOnReportedMessage()
        activity?.onBackPressed()
    }

    override fun keepFollowingChatRoomClicked() {
        removeAutoFollowedTaggedView()
        viewModel.muteChatroom(chatroomId, false)
        viewModel.sendChatroomMuted(false)
        viewModel.followChatroom(
            chatroomDetailExtras.chatroomId,
            true,
            SOURCE_TAGGED_AUTO_FOLLOWED
        )
        ViewUtils.showShortSnack(
            binding.root,
            getString(R.string.lm_chat_you_are_joining_this_chat_room)
        )
    }

    override fun unFollowChatRoomClicked() {
        removeAutoFollowedTaggedView()
        viewModel.followChatroom(
            chatroomDetailExtras.chatroomId,
            false,
            SOURCE_TAGGED_AUTO_FOLLOWED
        )
        ViewUtils.showShortSnack(
            binding.root,
            getString(R.string.lm_chat_removed_from_your_joined_chat_rooms)
        )
    }

    override fun onAudioConversationActionClicked(
        data: AttachmentViewData,
        parentPositionId: String,
        childPosition: Int,
        progress: Int
    ) {
        if (localParentConversationId != parentPositionId || localChildPosition != childPosition) {

            val item =
                chatroomDetailAdapter.items().firstOrNull {
                    ((it is ConversationViewData) && (it.id == localParentConversationId))
                } as? ConversationViewData

            if (item != null && item.attachmentCount >= 1) {
                val attachment = item.attachments?.get(localChildPosition)

                if (attachment != null) {
                    isAudioComplete = false
                    if (attachment.mediaState == MEDIA_ACTION_PLAY) {
                        mediaAudioService?.removeHandler()
                    }
                    updateAudioVoiceNoteBinder(
                        attachment.toBuilder()
                            .progress(0)
                            .currentDuration(requireContext().getString(R.string.lm_chat_start_duration))
                            .mediaState(MEDIA_ACTION_NONE)
                            .build(),
                        localParentConversationId,
                        localChildPosition
                    )
                }
            }
        }

        when (data.mediaState) {
            MEDIA_ACTION_NONE -> {
                if (voiceRecorder.isRecording()) {
                    voiceNoteUtils.stopVoiceNote(binding, RECORDING_LOCK_DONE)
                }
                if (isVoiceNotePlaying) {
                    mediaAudioService?.stopMedia()
                    binding.inputBox.tvVoiceNoteTime.text =
                        DateUtil.formatSeconds(singleUriDataOfVoiceNote?.duration ?: 0)
                    isVoiceNotePlaying = false
                    binding.inputBox.ivPlayRecording.setImageResource(R.drawable.lm_chat_ic_voice_play)
                }
                localParentConversationId = parentPositionId
                localChildPosition = childPosition
                isVoiceNotePlaying = false

                playAudio(data.uri, progress)
                when (data.type) {
                    VOICE_NOTE -> {
                        viewModel.sendVoiceNotePlayed(parentPositionId)
                    }

                    AUDIO -> {
                        viewModel.sendAudioPlayedEvent(parentPositionId)
                    }
                }
                isAudioComplete = false
                updateAudioVoiceNoteBinder(
                    data.toBuilder()
                        .progress(progress)
                        .currentDuration(DateUtil.formatSeconds(progress))
                        .mediaState(MEDIA_ACTION_PLAY)
                        .build(),
                    parentPositionId,
                    childPosition
                )
            }

            MEDIA_ACTION_PLAY -> {
                mediaAudioService?.pauseAudio()
                updateAudioVoiceNoteBinder(
                    data.toBuilder().mediaState(MEDIA_ACTION_PAUSE).build(),
                    parentPositionId,
                    childPosition
                )
            }

            MEDIA_ACTION_PAUSE -> {
                mediaAudioService?.playAudio()
                updateAudioVoiceNoteBinder(
                    data.toBuilder().mediaState(MEDIA_ACTION_PLAY).build(),
                    parentPositionId,
                    childPosition
                )
            }
        }
    }

    override fun onLongPressConversation(
        conversation: ConversationViewData,
        itemPosition: Int,
        from: String
    ) {
        if (conversation.isSending()) {
            return
        }

        /**
         * Reaction popup will show only when already selected conversations size is 0,
         * meaning a new conversation is just selected
         */
        if (selectedConversations.size == 0) {
            if (emojiPopup.isShowing) {
                emojiPopup.dismiss()
            }
            if (conversation.deletedBy == null && selectedChatRoom == null) {
                showReactionsPopup(
                    itemPosition,
                    conversation.id,
                    from
                )
            }
        }

        if (selectedConversations.containsKey(conversation.id)) {
            selectedConversations.remove(conversation.id)
        } else {
            selectedConversations[conversation.id] = conversation
        }

        chatroomDetailAdapter.notifyItemChanged(itemPosition)

        invalidateActionMenu()
    }

    // opens reaction popup and triggers reaction tray opened event
    private fun showReactionsPopup(itemPosition: Int, conversationId: String, from: String) {
        hideVideoPlayerAndDismissReactionTray()
        messageReactionsTray?.showPopup(conversationId, itemPosition)

        reactionsViewModel.sendEmoticonTrayOpenedEvent(
            from,
            conversationId,
            chatroomId,
            communityId
        )
    }

    override fun onConversationSeekbarChanged(
        progress: Int,
        attachmentViewData: AttachmentViewData,
        parentConversationId: String,
        childPosition: Int
    ) {
        if (localParentConversationId == parentConversationId && childPosition == localChildPosition) {
            sendProgressBroadcast(progress)
        }
    }

    override fun onLongPressChatRoom(chatRoom: ChatroomViewData, itemPosition: Int) {
        selectedChatRoom = if (selectedChatRoom?.id == chatRoom.id) null else chatRoom

        if (selectedChatRoom != null && selectedConversations.isEmpty()) {
            showChatroomReactionPopup(itemPosition)
        }

        chatroomDetailAdapter.notifyItemChanged(itemPosition)

        invalidateActionMenu()
    }

    private fun showChatroomReactionPopup(itemPosition: Int) {
        hideVideoPlayerAndDismissReactionTray()
        messageReactionsTray?.showChatroomReactionPopup(chatroomId, itemPosition)

        reactionsViewModel.sendEmoticonTrayOpenedEvent(
            LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS,
            "",
            chatroomId,
            communityId
        )
    }

    override fun externalLinkClicked(
        conversationId: String?,
        url: String,
        reportLinkExtras: ReportLinkExtras?
    ) {
        if (url.isValidYoutubeLink() &&
            url.getValidYoutubeVideoId().isNotEmpty()
        ) {
            showVideoPlayerPopup(url.getValidYoutubeVideoId())
        } else {
            viewModel.sendChatLinkClickedEvent(conversationId, url)
            val intent = Route.handleDeepLink(requireContext(), url)
            if (intent == null) {
                CustomTabIntent.open(requireContext(), url, reportLinkExtras)
            } else {
                startActivity(intent)
            }
        }
    }

    override fun onMultipleItemsExpanded(
        conversation: ConversationViewData,
        position: Int,
    ) {
        if (position == chatroomDetailAdapter.items().size - 1) {
            binding.rvChatroom.post {
                scrollToPositionWithOffset(position)
            }
        }

        chatroomDetailAdapter.update(
            position,
            conversation.toBuilder().isExpanded(true).build()
        )
    }

    override fun onRetryConversationMediaUpload(conversationId: String, attachmentCount: Int) {
        val uuid = viewModel.createRetryConversationMediaWorker(
            requireContext(),
            conversationId,
            attachmentCount
        )

        if (uuid.isNotEmpty()) {
            val conversation = getIndexedConversation(conversationId)?.second ?: return
            val chatReplyViewData = ChatReplyUtil.getConversationReplyData(
                conversation,
                userPreferences.getUUID()
            )

            observeCreateConversationWorker(
                uuid,
                conversationId,
                chatReplyViewData,
                conversation.replyConversation?.id,
                conversation.replyChatroomId
            )
        }
    }

    override fun onFailedConversationClick(
        conversation: ConversationViewData,
        itemPosition: Int,
    ) {
        showFailedConversationMenu(conversation, itemPosition)
    }

    override fun showMemberProfile(member: MemberViewData) {
        SDKApplication.getLikeMindsCallback()?.openProfile(member)
    }

    //add this function for every navigation from chatroom
    override fun onScreenChanged() {
        if (isVoiceNoteLocked) {
            isVoiceNoteLocked = false
            voiceNoteUtils.stopVoiceNote(binding, RECORDING_LOCK_DONE)
        }

        if (mediaAudioServiceBound && mediaAudioService?.isPlaying() == true) {
            stopAudioService()

            val item =
                chatroomDetailAdapter.items().firstOrNull {
                    ((it is ConversationViewData) && (it.id == localParentConversationId))
                } as? ConversationViewData

            var attachment = item?.attachments?.get(localChildPosition) ?: return

            attachment = attachment.toBuilder()
                .progress(0)
                .mediaState(MEDIA_ACTION_NONE)
                .currentDuration(requireContext().getString(R.string.lm_chat_start_duration))
                .build()

            updateAudioVoiceNoteBinder(
                attachment,
                localParentConversationId,
                localChildPosition
            )
        }
    }

    override fun onLinkClicked(conversationId: String?, url: String) {
        super.onLinkClicked(conversationId, url)
        viewModel.sendChatLinkClickedEvent(conversationId, url)
    }

    override fun showConversationPollVotersList(
        conversationId: String,
        pollId: String?,
        hasPollEnded: Boolean,
        toShowResult: Boolean?,
        positionOfPoll: Int
    ) {
        when {
            toShowResult == true -> {
                val extra = PollResultExtras.Builder()
                    .communityId(communityId)
                    .communityName(getChatroomViewData()?.communityName)
                    .conversationId(conversationId)
                    .selectedPoll(positionOfPoll)
                    .build()
                viewModel.sendMicroPollResultsViewed(conversationId)
                navigateToPollResult(extra)
            }

            hasPollEnded -> {
                val extra = PollResultExtras.Builder()
                    .communityId(communityId)
                    .communityName(getChatroomViewData()?.communityName)
                    .conversationId(conversationId)
                    .selectedPoll(positionOfPoll)
                    .build()
                viewModel.sendMicroPollResultsViewed(conversationId)
                navigateToPollResult(extra)
            }

            else -> {
                ViewUtils.showShortToast(
                    requireContext(),
                    getString(R.string.lm_chat_poll_ended_result_message)
                )
            }
        }
    }

    override fun onConversationPollSubmitClicked(
        conversation: ConversationViewData,
        pollViewDataList: List<PollViewData>
    ) {
        if (isGuestUser) {
            callGuestFlowCallback()
        } else {
            viewModel.submitConversationPoll(
                requireContext(),
                conversation,
                pollViewDataList
            )
        }
    }

    override fun showToastMessage(message: String) {
        binding.bottomSnack.tvSnack.text = message
        binding.bottomSnack.root.visibility = View.VISIBLE
    }

    override fun dismissToastMessage() {
        binding.bottomSnack.root.visibility = View.GONE
    }

    override fun getBinding(conversationId: String?): DataBoundViewHolder<*>? {
        if (conversationId.isNullOrEmpty()) {
            return null
        }
        val index = chatroomDetailAdapter.items().indexOfFirst {
            it is ConversationViewData && it.id == conversationId
        }
        if (index.isValidIndex()) {
            return binding.rvChatroom.findViewHolderForAdapterPosition(index)
                    as? DataBoundViewHolder<*>
        }
        return null
    }

    override fun addConversationPollOptionClicked(conversationId: String) {
        AddPollOptionDialog.newInstance(
            childFragmentManager,
            AddPollOptionExtras.Builder().conversationId(conversationId).build()
        )
    }

    override fun onConversationMembersVotedCountClick(
        conversation: ConversationViewData,
        hasPollEnded: Boolean,
        isAnonymous: Boolean?,
        isCreator: Boolean,
    ) {
        if (isAnonymous == true) {
            showAnonymousPollDialog(requireContext())
        } else {
            when {
                conversation.pollInfoData?.toShowResult == true -> {
                    val extra = PollResultExtras.Builder()
                        .communityId(communityId)
                        .communityName(getChatroomViewData()?.communityName)
                        .conversationId(conversation.id)
                        .build()
                    viewModel.sendMicroPollResultsViewed(conversation.id)
                    navigateToPollResult(extra)
                }

                hasPollEnded -> {
                    val extra = PollResultExtras.Builder()
                        .communityId(communityId)
                        .communityName(getChatroomViewData()?.communityName)
                        .conversationId(conversation.id)
                        .build()
                    viewModel.sendMicroPollResultsViewed(conversation.id)
                    navigateToPollResult(extra)
                }

                else -> {
                    ViewUtils.showShortToast(
                        requireContext(),
                        getString(R.string.lm_chat_poll_ended_result_message)
                    )
                }
            }
        }
    }

    // shows alert dialog for anonymous poll
    private fun showAnonymousPollDialog(context: Context) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.lm_chat_anonymous_poll))
            .setMessage(context.getString(R.string.lm_chat_anonymous_poll_message))
            .setPositiveButton(context.getString(R.string.lm_chat_okay)) { dialog, _ ->
                dialog.dismiss()
            }
        val alertDialog = builder.create()
        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.lm_chat_black_40
                )
            )
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(LMChatAppearance.getButtonsColor())
        }
        alertDialog.show()
    }

    override fun getPollRemainingTime(expiryTime: Long?): String? {
        return DateUtil.getPollRemainingTime(expiryTime)
    }

    /**------------------------------------------------------------
     * Voice note listeners
    ---------------------------------------------------------------*/

    override fun onVoiceNoteStarted() {
        if (mediaAudioService?.isPlaying() == true) {
            val item =
                chatroomDetailAdapter.items().firstOrNull {
                    ((it is ConversationViewData) && (it.id == localParentConversationId))
                } as? ConversationViewData ?: return

            if ((item.attachmentCount) >= 1) {
                var attachment = item.attachments?.get(localChildPosition) ?: return

                attachment = attachment.toBuilder()
                    .mediaState(MEDIA_ACTION_PLAY)
                    .build()

                onAudioConversationActionClicked(
                    attachment,
                    localParentConversationId,
                    localChildPosition,
                    0
                )
            }
        }

        try {
            isVoiceNotePlaying = false
            voiceNoteFilePath =
                "${requireContext().externalCacheDir?.absolutePath}/VOC_${System.currentTimeMillis()}.mp3"
            voiceRecorder.startRecording(voiceNoteFilePath ?: "")
        } catch (e: IllegalStateException) {
            voiceNoteUtils.stopVoiceNote(binding, RECORDING_RELEASED)
        }
    }

    override fun onVoiceNoteLocked() {
        binding.fabMic.hide()
        binding.fabSend.show()
    }

    override fun onVoiceNoteCompleted() {
        binding.apply {
            fabMic.hide()
            fabSend.show()
            mediaAudioService?.isDataSourceSet = false
            singleUriDataOfVoiceNote = if (voiceRecorder.isRecording()) {
                voiceRecorder.stopRecording(requireContext())
            } else {
                null
            }
            inputBox.tvVoiceNoteTime.text =
                DateUtil.formatSeconds(singleUriDataOfVoiceNote?.duration ?: 0)
        }
    }

    override fun onVoiceNoteCancelled() {
        binding.apply {
            isVoiceNoteRecording = false
            isVoiceNotePlaying = false
            fabMic.show()
            fabSend.hide()
            inputBox.ivPlayRecording.setImageResource(R.drawable.lm_chat_ic_voice_play)
            singleUriDataOfVoiceNote = null
            if (voiceRecorder.isRecording()) {
                voiceRecorder.stopRecording(requireContext())
            }
        }
    }

    override fun onVoiceNoteSend() {
        binding.apply {
            isVoiceNoteRecording = false
            fabMic.show()
            inputBox.ivPlayRecording.setImageResource(R.drawable.lm_chat_ic_voice_play)
            fabSend.hide()
            isVoiceNotePlaying = false
            singleUriDataOfVoiceNote = null
        }
    }

    override fun isVoiceNoteLocked(): Boolean {
        return isVoiceNoteLocked
    }

    override fun isDeletingVoiceNote(value: Boolean) {
        isDeletingVoiceNote = value
    }

    override fun stopTrackingVoiceNoteAction(value: Boolean) {
        stopTrackingVoiceNoteAction = value
    }

    /**------------------------------------------------------------
     * Adapter utility functions
    ---------------------------------------------------------------*/

    private fun getIndexOfChatroom(): Int? {
        val index = chatroomScrollListener.getChatroomPosition()
        return if (index.isValidIndex(chatroomDetailAdapter.itemCount)) {
            index
        } else {
            null
        }
    }

    private fun disableAllGraphicViewTypes() {
        if (viewModel.isDmChatroom()) {
            val index = chatroomDetailAdapter.items().indexOfFirst {
                chatroomDetailAdapter.getGraphicViewTypes().contains(it.viewType)
            }
            if (index.isValidIndex()) {
                chatroomDetailAdapter.removeIndex(index)
            }
        }
    }

    private fun removeChatroomItem() {
        if (viewModel.isDmChatroom()) {
            val index = chatroomDetailAdapter.items().indexOfFirst {
                it is ChatroomViewData
            }
            if (index.isValidIndex()) {
                chatroomDetailAdapter.removeIndex(index)
            }
        }
    }

    /**
     * @return A map containing the position as key and the conversation object as value.
     * The position is mapped with the recyclerview.
     */
    private fun getIndexedConversations(
        conversations: List<ConversationViewData>,
    ): Map<Int, ConversationViewData> {
        val map = mutableMapOf<Int, ConversationViewData>()
        var i = 0
        val max = conversations.size - 1
        for ((index, item) in chatroomDetailAdapter.items().withIndex()) {
            if (i > max) {
                break
            }
            if (
                item is ConversationViewData &&
                (item.id == conversations[i].id || item.id == conversations[i].temporaryId)
            ) {
                map[index] = conversations[i].toBuilder()
                    .showTapToUndo(item.showTapToUndo)
                    .build()
                i++
            }
        }
        return map
    }

    /**
     * @return filters and returns those conversations which are not yet added in the recyclerview
     */
    private fun getNonPresentConversations(
        conversations: List<ConversationViewData>,
    ): List<ConversationViewData> {
        val map = conversations.map { it.id }.toMutableList()
        var i = 0
        val max = conversations.size - 1
        for (item in chatroomDetailAdapter.items().reversed()) {
            if (i > max) {
                break
            }
            if (
                item is ConversationViewData &&
                (map.contains(item.id))
            ) {
                map.remove(item.id)
                i++
            }
        }
        return conversations.filter { map.contains(it.id) }
    }

    /**
     * @return true if conversation is already added in the recyclerview else false
     */
    private fun isConversationAlreadyPresent(conversationId: String): Boolean {
        return chatroomDetailAdapter.items().lastOrNull {
            it is ConversationViewData && it.id == conversationId
        } != null
    }

    /**
     * Highlight the conversation view. This is usually used to highlight the replied conversation's parent
     * @param conversationId The conversation id to highlight
     * @return true if successfully highlighted
     */
    private fun highlightConversation(conversationId: String?): Boolean {
        if (!conversationId.isNullOrEmpty()) {
            val index = getIndexOfConversation(conversationId)
            if (index.isValidIndex()) {
                scrolledConversationPosition = index
                scrollToPositionWithOffset(index)
                chatroomDetailAdapter.notifyItemChanged(index)
                return true
            }
        }
        return false
    }

    /**
     * Highlight the chatRoom view. This is usually used to highlight the replied conversation's parent
     * @param chatroomId The chatRoom id to highlight
     */
    private fun highlightChatroom(chatroomId: String?): Boolean {
        if (!chatroomId.isNullOrEmpty()) {
            val index = getIndexOfChatRoom(chatroomId)
            if (index.isValidIndex()) {
                scrolledConversationPosition = index
                scrollToPositionWithOffset(index)
                chatroomDetailAdapter.notifyItemChanged(index)
                return true
            }
        }
        return false
    }

    /**
     * Returns the current index of the conversation from the recyclerview
     * @param id Conversation id
     */
    private fun getIndexOfConversation(id: String): Int {
        return chatroomDetailAdapter.items().indexOfFirst {
            it is ConversationViewData && it.id == id
        }
    }

    /**
     * Returns the current index of the date if exists from the recyclerview
     * @param date Date string
     */
    private fun getIndexOfDate(date: String): Int {
        chatroomDetailAdapter.items().forEachIndexed { index, item ->
            if (item is ChatroomDateViewData && item.date == date) {
                return index
            }
        }
        return -1
    }

    /**
     * Returns the current index of the chatRoom from the recyclerview
     * @param id ChatRoom id
     */
    private fun getIndexOfChatRoom(id: String): Int {
        return chatroomDetailAdapter.items().indexOfFirst {
            it is ChatroomViewData && it.id == id
        }
    }

    /**
     * Scroll to a position with offset from the top header
     * @param position Index of the item to scroll to
     */
    private fun scrollToPositionWithOffset(position: Int) {
        binding.apply {
            val px = if (viewTopBackground.height == 0) {
                (ViewUtils.dpToPx(75) * 1.5).toInt()
            } else {
                (viewTopBackground.height * 1.5).toInt()
            }
            (rvChatroom.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                position,
                px
            )
        }
    }

    /**
     * Scroll to the top of the chatroom, where chatroom object is present
     * Check if chatroom object is already added or else add it
     * @param repliedChatRoomId Id of the original chatRoom in which conversation was replied.
     */
    private fun scrollToExtremeTop(repliedChatRoomId: String? = null) = lifecycleScope.launch {
        if (chatroomScrollListener.containsChatRoom()) {
            fadeOutTopChatroomView()
            scrollToPosition(SCROLL_UP)
            showUnseenCount(true)
        } else {
            val topConversation = getTopConversation()
            if (topConversation == null) {
                scrollToPosition(SCROLL_UP)
                showUnseenCount(true)
            } else {
                chatroomScrollListener.bottomLoadingDone()
                viewModel.fetchTopConversationsOnClick(
                    topConversation,
                    chatroomDetailAdapter.items(),
                    repliedChatRoomId
                )
            }
        }
    }

    /**------------------------------------------------------------
     * Utility functions
    ---------------------------------------------------------------*/

    private fun invalidateActionsMenu() {
        activity?.invalidateOptionsMenu()
    }

    /**
     * @return Pair containing the current index and the conversation object matched with the recyclerview
     * @param id Conversation id
     */
    private fun getIndexedConversation(id: String): Pair<Int, ConversationViewData>? {
        val index = chatroomDetailAdapter.items().indexOfFirst {
            it is ConversationViewData && it.id == id
        }
        if (index.isValidIndex()) {
            val conversation = chatroomDetailAdapter[index] as? ConversationViewData
            if (conversation != null) {
                return Pair(index, conversation)
            }
        }
        return null
    }

    private fun hideVideoPlayerAndDismissReactionTray() {
        if (inAppVideoPlayerPopup?.isShowing == true) {
            inAppVideoPlayerPopup?.playOrPauseVideo(play = false)
        }
        if (messageReactionsTray?.isShowing == true) {
            messageReactionsTray?.dismiss()
        }

        if (messageReactionsTray == null) {
            messageReactionsTray =
                ReactionPopup(binding, requireActivity())
            messageReactionsTray?.attachListener(this)
        }
        messageReactionsTray?.contentView = null
    }

    private fun takeActionOnReportedMessage() {
        chatroomResultExtras = chatroomResultExtras?.also {
            chatroomResultExtras?.toBuilder()
                ?.chatroomId(chatroomId)
                ?.build()
        } ?: run {
            ChatroomDetailResultExtras.Builder()
                .chatroomId(chatroomId)
                .build()
        }
        setChatroomDetailActivityResult()
    }

    private fun removeAutoFollowedTaggedView() {
        if (getChatroomViewData()?.showFollowAutoTag == true) {
            val index = chatroomDetailAdapter.items().indexOfLast {
                it is AutoFollowedTaggedActionViewData
            }
            if (index.isValidIndex(chatroomDetailAdapter.items())) {
                chatroomDetailAdapter.removeIndex(index)
            }
        }
    }

    private fun playAudio(uri: Uri, progress: Int) {
        if (serviceConnection == null) return
        if (!mediaAudioServiceBound) {
            val serviceIntent =
                Intent(requireContext(), MediaAudioForegroundService::class.java)
            serviceIntent.putExtra(AUDIO_SERVICE_URI_EXTRA, uri)
            serviceIntent.putExtra(AUDIO_SERVICE_PROGRESS_EXTRA, progress)
            activity?.startService(serviceIntent)
            activity?.bindService(serviceIntent, serviceConnection!!, Context.BIND_AUTO_CREATE)
        } else {
            val broadcastNewAudio = Intent(BROADCAST_PLAY_NEW_AUDIO)
            broadcastNewAudio.putExtra(AUDIO_SERVICE_URI_EXTRA, uri)
            broadcastNewAudio.putExtra(AUDIO_SERVICE_PROGRESS_EXTRA, progress)
            broadcastNewAudio.setPackage(requireContext().packageName)
            activity?.sendBroadcast(broadcastNewAudio)
        }
    }

    private fun changeVideoPlayerToPopupScreen() {
        fullScreenClicked(isFullScreen = false)
        inAppVideoPlayerPopup?.setPopupWindowConstraints(false)
        inAppVideoPlayerPopup?.setPopupWindowUI()
    }

    /**
     * showCopyAction -> This will be true if either of the selected chatroom or conversation has title/answer text and is not deleted.
     * showReplyAction -> This will be true if either only chatroom is selected or single conversation is selected.
     * showEditAction -> This will be true if either only chatroom is selected or single conversation is selected.
     *                    Also selected chatroom or conversation should have title/answer text and is should not be deleted.
     * showDeleteAction -> This will be true if Chatroom is not selected and any of the following case is true:
     *     Case 1: Multiple conversations are selected then all these should be true.
     *          -> Each conversation should be user's own message.
     *          -> Selected conversations shouldn't contain any deleted message.
     *     Case 2: If Single conversation is selected then all these should be true.
     *          -> Conversation should be user's own message.
     *          -> Selected conversation shouldn't be a deleted message.
     *
     * showReportAction -> This will be true if ChatRoom is not selected & only 1 conversation is selected of another member.
     * showSetAsTopic -> This will be true if only single conversation is selected and Member is the creator of the chatroom.
     * */
    private fun invalidateActionMenu() {
        val isChatRoomSelected = selectedChatRoom != null
        if (!isChatRoomSelected && selectedConversations.isEmpty()) {
            actionModeCallback?.finishActionMode()
        } else {
            startActionMode()

            var showReplyAction = false
            var showCopyAction = false
            var showEditAction = false
            var showDeleteAction = false
            var showReportAction = false
            var showSetAsTopic = false

            if (isChatRoomSelected && selectedConversations.isEmpty()) {
                if (!isNotAdminInAnnouncementRoom()) {
                    showReplyAction = true
                }
                showReportAction = false
                showCopyAction = selectedChatRoom?.hasTitle() == true
                showDeleteAction = false

                showEditAction = if (viewModel.isAnnouncementChatroom()) {
                    !isNotAdminInAnnouncementRoom() && viewModel.hasEditCommunityDetailRight()
                } else {
                    selectedChatRoom?.memberViewData?.sdkClientInfo?.uuid == userPreferences.getUUID()
                            && selectedChatRoom?.hasTitle() == true
                }

            } else if (!isChatRoomSelected && selectedConversations.size == 1) {
                val conversation = selectedConversations[selectedConversations.keys.first()]!!

                showReplyAction = conversation.isNotDeleted()
                showSetAsTopic =
                    (viewModel.canSetChatroomTopic())
                            && (conversation.isNotDeleted())
                            && (conversation.id != getChatroomViewData()?.topic?.id)

                showCopyAction = conversation.hasAnswer() && conversation.isNotDeleted()

                when {
                    conversation.memberViewData.sdkClientInfo.uuid == userPreferences.getUUID() -> {
                        showReportAction = false
                        showDeleteAction = conversation.isNotDeleted()
                        showEditAction = conversation.hasAnswer() && conversation.isNotDeleted()
                    }

                    viewModel.hasDeleteChatRoomRight() -> {
                        showReportAction = true
                        showDeleteAction = conversation.isNotDeleted()
                        showEditAction = false
                    }

                    else -> {
                        showReportAction = true
                        showDeleteAction = false
                        showEditAction = false
                    }
                }

            } else if (!isChatRoomSelected && selectedConversations.size > 1) {
                showReplyAction = false
                showReportAction = false
                showSetAsTopic = false

                if (selectedConversations.values.any { it.hasAnswer() && it.isNotDeleted() }) {
                    showCopyAction = true
                }

                if (selectedConversations.values.none {
                        it.memberViewData.sdkClientInfo.uuid != userPreferences.getUUID()
                                || it.deletedBy != null
                                || !ChatroomUtil.hasOriginalConversationId(it)
                    }) {
                    showDeleteAction = true
                }

                showEditAction = false

            } else if (isChatRoomSelected && selectedConversations.size > 0) {
                showReplyAction = false
                showReportAction = false

                if (selectedChatRoom?.hasTitle() == true
                    || selectedConversations.values.any { it.hasAnswer() && it.isNotDeleted() }
                ) {
                    showCopyAction = true
                }

                showDeleteAction = false
                showEditAction = false
            }

            actionModeCallback?.invalidate(
                ChatroomDetailActionModeData.Builder()
                    .showReplyAction(showReplyAction)
                    .showCopyAction(showCopyAction)
                    .showEditAction(showEditAction)
                    .showDeleteAction(showDeleteAction)
                    .showReportAction(showReportAction)
                    .showSetAsTopic(showSetAsTopic)
                    .build()
            )
        }
    }

    /**
     * Start action mode on toolbar and show menu items. This gets triggered on selecting conversations via long click
     */
    private fun startActionMode() {
        if (actionModeCallback == null) {
            actionModeCallback = ActionModeCallback()
        }
        if (actionModeCallback?.isActionModeEnabled() != true) {
            actionModeCallback?.startActionMode(
                this,
                requireActivity() as AppCompatActivity,
                R.menu.lm_chat_message_actions_menu
            )
        }
        val selectedSize = (if (selectedChatRoom != null) 1 else 0) + selectedConversations.size
        actionModeCallback?.updateTitle(selectedSize.toString())
    }

    private fun sendProgressBroadcast(progress: Int) {
        val broadcastSeekBarDragged = Intent(BROADCAST_SEEKBAR_DRAGGED)
        broadcastSeekBarDragged.putExtra(PROGRESS_SEEKBAR_DRAGGED, progress)
        broadcastSeekBarDragged.setPackage(requireContext().packageName)
        activity?.sendBroadcast(broadcastSeekBarDragged)
    }

    private fun showVideoPlayerPopup(videoId: String) {
        if (inAppVideoPlayerPopup?.isShowing == true && inAppVideoPlayerPopup?.videoId != videoId) {
            dismissVideoPlayerPopup()
        }
        if (inAppVideoPlayerPopup == null) {
            inAppVideoPlayerPopup =
                YouTubeVideoPlayerPopup(
                    requireContext(),
                    binding.rvChatroom,
                    requireActivity()
                )
            inAppVideoPlayerPopup!!.attachListener(this)
            inAppVideoPlayerPopup!!.contentView = null
            if (videoId.isNotBlank()) {
                inAppVideoPlayerPopup!!.showPopup(lifecycle, videoId)
            }
        }
    }

    private fun dismissVideoPlayerPopup() {
        inAppVideoPlayerPopup?.dismiss()
        inAppVideoPlayerPopup = null
    }

    private fun showFailedConversationMenu(
        conversation: ConversationViewData,
        position: Int,
    ) {
        val view = binding.rvChatroom.layoutManager?.findViewByPosition(position) ?: return
        val popUpMenu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            PopupMenu(requireContext(), view, Gravity.END, 0, R.style.PopupMenu)
        } else {
            PopupMenu(requireContext(), view, Gravity.END)
        }
        popUpMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_resend -> {
                    resendFailedConversation(conversation, position)
                }

                R.id.menu_delete -> {
                    deleteFailedConversation(conversation.id, position)
                }
            }
            return@setOnMenuItemClickListener true
        }
        popUpMenu.menuInflater.inflate(R.menu.lm_chat_conversation_menu, popUpMenu.menu)
        popUpMenu.show()
    }

    private fun resendFailedConversation(conversation: ConversationViewData, index: Int) {
        val updatedConversation = conversation.toBuilder()
            .localCreatedEpoch(System.currentTimeMillis())
            .build()

        val workerUUID = viewModel.postFailedConversation(requireContext(), updatedConversation)
        val chatReplyViewData = ChatReplyUtil.getConversationReplyData(
            conversation,
            userPreferences.getUUID()
        )

        observeCreateConversationWorker(
            workerUUID,
            conversation.id,
            chatReplyViewData,
            conversation.replyConversation?.id,
            conversation.replyChatroomId
        )

        chatroomDetailAdapter.update(index, updatedConversation)
    }

    private fun deleteFailedConversation(conversationId: String, index: Int) {
        viewModel.deleteFailedConversation(conversationId)
        chatroomDetailAdapter.removeIndex(index)
    }

    /**
     * Initializes the reply view in input box when the user tries to reply on a conversation
     * @param conversation Conversation object
     */
    private fun setReplyViewConversationData(conversation: ConversationViewData, type: String) {
        binding.inputBox.viewReply.apply {
            replySourceType = REPLY_SOURCE_CONVERSATION
            conversationViewData = conversation
            val replyData = ChatReplyUtil.getConversationReplyData(
                conversation,
                userPreferences.getUUID(),
                type = type
            )
            setReplyViewData(replyData)
        }
    }

    /**
     * Initializes the reply view in input box when the user tries to reply on a ChatRoom
     * @param chatRoom ChatRoom object
     */
    private fun setReplyViewChatRoomData(chatRoom: ChatroomViewData, type: String) {
        binding.inputBox.viewReply.apply {
            replySourceType = REPLY_SOURCE_CHATROOM
            chatRoomViewData = chatRoom
            val replyData = ChatReplyUtil.getChatRoomReplyData(
                chatRoom,
                userPreferences.getUUID(),
                type = type
            )
            setReplyViewData(replyData)
        }
    }

    private fun setReplyViewData(replyData: ChatReplyViewData) {
        binding.inputBox.viewReply.apply {
            chatReplyData = replyData

            val placeholder = if (replyData.attachmentType == AUDIO) {
                R.drawable.lm_chat_placeholder_audio
            } else {
                R.drawable.lm_chat_image_placeholder
            }

            if (replyData.imageUrl.isNullOrEmpty()) {
                ivReplyAttachment.visibility = View.GONE
            } else {
                ivReplyAttachment.visibility = View.VISIBLE
                ImageBindingUtil.loadImage(
                    ivReplyAttachment,
                    replyData.imageUrl,
                    placeholder
                )
            }

            when {
                replyData.isMessageDeleted -> {
                    tvConversation.text = replyData.deleteMessage
                }

                else -> {
                    MemberTaggingDecoder.decode(
                        tvConversation,
                        replyData.conversationText,
                        false,
                        LMChatAppearance.getTextLinkColor()
                    )
                }
            }
            if (replyData.drawable != null && tvConversation.editableText != null) {
                tvConversation.editableText.setSpan(
                    ImageSpan(requireContext(), replyData.drawable),
                    0,
                    1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
            ViewUtils.showKeyboard(requireContext(), binding.inputBox.etAnswer)
        }
    }

    private fun copyConversations() {
        val conversations = selectedConversations.values.filter { conversation ->
            !conversation.isDeleted() && conversation.answer.isNotEmpty()
        }

        val chatRoom = if (selectedChatRoom != null && selectedChatRoom?.hasTitle() == true) {
            selectedChatRoom
        } else null

        if (chatRoom != null && conversations.isEmpty()) {
            copyConversationText(chatRoom.title, true)
        } else if (chatRoom == null && conversations.size == 1) {
            val conversation = conversations.firstOrNull() ?: return
            copyConversationText(conversation.answer, true)
        } else {
            val message = StringBuilder()
            if (chatRoom != null)
                message.append("[")
                    .append(chatRoom.date)
                    .append(", ")
                    .append(DateUtil.createDateFormat("hh:mm", chatRoom.createdAt))
                    .append("] ")
                    .append(chatRoom.memberViewData.name).append(": ")
                    .append(MemberTaggingDecoder.decode(chatRoom.title))
                    .append("\n")

            conversations.forEach {
                message.append("[")
                    .append(it.date)
                    .append(", ")
                    .append(it.createdAt).append("] ")
                    .append(it.memberViewData.name).append(": ")
                    .append(MemberTaggingDecoder.decode(it.answer))
                    .append("\n")
            }
            if (message.isNotEmpty())
                copyConversationText(message.toString(), false)
        }
    }

    private fun copyConversationText(text: String, isSingleConversation: Boolean) {
        val message = if (isSingleConversation) {
            requireContext().getString(R.string.lm_chat_message_copied)
        } else {
            requireContext().getString(R.string.lm_chat_messages_copied)
        }
        viewModel.sendMessageCopyEvent(text)
        ViewUtils.copyToClipboard(
            requireContext(),
            MemberTaggingDecoder.decode(text),
            message,
            "chat_message"
        )
    }

    /**
     * Initializes the edit conversation view in input box when the user tries to edit their own conversation
     * @param conversation Conversation object
     */
    private fun setEditMessageViewConversationData(conversation: ConversationViewData) {
        binding.inputBox.apply {
            ivAttachment.visibility = View.INVISIBLE
            viewReply.replySourceType = REPLY_SOURCE_CONVERSATION
            viewReply.conversationViewData = conversation
            val editData = ChatReplyUtil.getEditConversationData(conversation)
            setEditViewData(editData)
        }
    }

    /**
     * Initializes the edit chatroom view in input box when the user tries to edit their own chatroom
     * @param chatRoom ChatRoom object
     */
    private fun setEditMessageViewChatRoomData(chatRoom: ChatroomViewData) {
        binding.inputBox.apply {
            ivAttachment.visibility = View.INVISIBLE
            viewReply.replySourceType = REPLY_SOURCE_CHATROOM
            viewReply.chatRoomViewData = chatRoom
            val editData = ChatReplyUtil.getEditChatRoomData(chatRoom)
            setEditViewData(editData)
        }
    }

    private fun setEditViewData(editData: ChatReplyViewData) {
        binding.inputBox.apply {
            viewReply.chatReplyData = editData
            MemberTaggingDecoder.decode(
                viewReply.tvConversation,
                editData.conversationText,
                false,
                LMChatAppearance.getTextLinkColor()
            )
            MemberTaggingDecoder.decode(
                etAnswer,
                editData.conversationText,
                LMChatAppearance.getTextLinkColor()
            )
            etAnswer.setSelection(etAnswer.text?.length ?: 0)
            ViewUtils.showKeyboard(requireContext(), etAnswer)
        }
    }

    /**
     * Shows a dialog to confirm the deletion of the selected conversations
     * @param conversations List of all the selected conversations to delete
     */
    private fun showDeleteMessageConfirmationPopup(conversations: List<ConversationViewData>) {
        val topic = conversations.firstOrNull { conversation ->
            (conversation.id == viewModel.getCurrentTopic()?.id)
        }

        val dialog = DeleteMessagesDialog(this, conversations, topic)
        dialog.show(childFragmentManager, DeleteMessagesDialog.TAG)
    }

    private fun reportConversation() {
        //get conversation to be reported
        val conversation = selectedConversations.values.firstOrNull() ?: return

        //create extras for [ReportActivity]
        val reportExtras = ReportExtras.Builder()
            .conversationId(conversation.id)
            .type(REPORT_TYPE_CONVERSATION)
            .conversationType(ChatroomUtil.getConversationType(conversation))
            .chatroomId(getChatroomViewData()?.id)
            .chatroomName(getChatroomViewData()?.header)
            .communityId(getChatroomViewData()?.communityId)
            .uuid(conversation.memberViewData.sdkClientInfo.uuid)
            .build()

        //get Intent for [ReportActivity]
        val intent = ReportActivity.getIntent(requireContext(), reportExtras)

        //start [ReportActivity] and check for result
        reportLauncher.launch(intent)
    }

    private fun setChatroomTopic() {
        val conversation = selectedConversations.values.firstOrNull() ?: return
        viewModel.setChatroomTopic(chatroomId, conversation)
    }

    /**------------------------------------------------------------
     * Other listeners
    ---------------------------------------------------------------*/

    override fun reactionClicked(
        unicode: String,
        conversationId: String,
        isConversation: Boolean,
    ) {
        reactedToMessage(unicode, conversationId, isConversation)
    }

    // processes the reaction on message request
    private fun reactedToMessage(
        reaction: String,
        conversationId: String,
        isConversation: Boolean,
    ) {
        if (isGuestUser) {
            callGuestFlowCallback()
        } else {
            ViewUtils.hideKeyboard(requireContext())
            //dismiss the message action mode
            actionModeCallback?.finishActionMode()
            messageReactionsTray?.dismiss()

            val member =
                viewModel.currentMemberDataFromMemberState ?: return
            val reactionViewData = ReactionViewData.Builder()
                .memberViewData(member)
                .reaction(reaction)
                .conversationId(if (isConversation) conversationId else null)
                .chatroomId(if (isConversation) null else chatroomId)
                .build()

            // updates the grid
            if (isConversation) {
                reactionsViewModel.putConversationReaction(conversationId, reactionViewData)
                updateReactionsGridUI(reactionViewData, conversationId)
            } else {
                reactionsViewModel.putChatroomReaction(chatroomId, reactionViewData)
                updateChatroomReactionGridUI(reactionViewData)
            }

            // follows the chatroom in case it is not followed already
            if (getChatroomViewData()?.followStatus == false) {
                removeFollowView()
                viewModel.followChatroom(
                    chatroomDetailExtras.chatroomId,
                    true,
                    LMAnalytics.Source.MESSAGE_REACTIONS
                )
            }
            val finalConversationId = if (isConversation) {
                conversationId
            } else {
                ""
            }

            // sends reaction added analytics event
            reactionsViewModel.sendReactionAddedEvent(
                reaction,
                LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS,
                finalConversationId,
                chatroomId,
                communityId
            )
        }
    }

    /**
     * Add or remove the reaction of the conversation in recyclerview
     * @param messageReactionViewData The message reaction object. Pass null to remove the reaction
     * @param conversationId The id of the conversation
     */
    private fun updateReactionsGridUI(
        messageReactionViewData: ReactionViewData?,
        conversationId: String,
    ) {
        val conversationIndexed = getIndexedConversation(conversationId) ?: return
        val conversation = conversationIndexed.second
        val index = conversationIndexed.first
        var shouldScrollToBottom = false

        val messageReactions = conversation.reactions.orEmptyMutable()
        val indexToBeRemoved = messageReactions.indexOfFirst { reaction ->
            reaction.memberViewData.sdkClientInfo.uuid == userPreferences.getUUID()
        }
        if (indexToBeRemoved.isValidIndex()) {
            messageReactions.removeAt(indexToBeRemoved)
        }
        if (messageReactionViewData != null) {
            messageReactions.add(messageReactionViewData)
            if (messageReactions.size == 1 && getBottomConversation()?.id == conversationId) {
                shouldScrollToBottom = true
            }
        }
        chatroomDetailAdapter.update(
            index,
            conversation.toBuilder()
                .reactions(messageReactions)
                .build()
        )
        if (shouldScrollToBottom) {
            binding.rvChatroom.post {
                scrollToPosition(SCROLL_DOWN)
            }
        }
        if (inAppVideoPlayerPopup?.isShowing == true) {
            inAppVideoPlayerPopup?.playOrPauseVideo(play = true)
        }
    }

    // updates the chatroom reaction grid based on addition/removal of reaction
    private fun updateChatroomReactionGridUI(reactionViewData: ReactionViewData?) {
        val chatroomIndex = getIndexOfChatroom() ?: return
        val chatroom = getChatroomViewData()

        val reactions = chatroom?.reactions.orEmptyMutable()
        val indexToBeRemoved = reactions.indexOfFirst { reaction ->
            reaction.memberViewData.sdkClientInfo.uuid == userPreferences.getUUID()
        }
        if (indexToBeRemoved.isValidIndex()) {
            reactions.removeAt(indexToBeRemoved)
        }
        if (reactionViewData != null) {
            reactions.add(reactionViewData)
        }
        if (chatroom != null) {
            val newViewData = chatroom.toBuilder()
                .reactions(reactions)
                .build()
            chatroomDetailAdapter.update(chatroomIndex, newViewData)
        }
        if (inAppVideoPlayerPopup?.isShowing == true) {
            inAppVideoPlayerPopup?.playOrPauseVideo(play = true)
        }
    }

    override fun moreReactionsClicked(conversationId: String?, isConversation: Boolean) {
        if (isGuestUser) {
            callGuestFlowCallback()
        } else {
            if (conversationId != null && isConversation) {
                conversationIdForEmojiReaction = conversationId
            }
            if (!isConversation) {
                isChatroomReaction = true
            }
            messageReactionsTray?.dismiss()
            if (!KeyboardVisibilityEvent.isKeyboardVisible(requireActivity())) {
                binding.inputBox.etAnswer.focusAndShowKeyboard()
            }
            emojiPopup.show()
        }
    }

    override fun removedReaction(conversationId: String) {
        childFragmentManager.popBackStack()
        ViewUtils.showShortToast(requireContext(), getString(R.string.lm_chat_reaction_removed))
        reactionsViewModel.deleteConversationReaction(conversationId)
        updateReactionsGridUI(null, conversationId)
        reactionsViewModel.sendReactionRemovedEvent(
            conversationId,
            chatroomId,
            communityId
        )
    }

    override fun removedChatroomReaction() {
        childFragmentManager.popBackStack()
        ViewUtils.showShortToast(requireContext(), getString(R.string.lm_chat_reaction_removed))
        reactionsViewModel.deleteChatroomReaction(chatroomId)
        updateChatroomReactionGridUI(null)
        reactionsViewModel.sendReactionRemovedEvent(
            "",
            chatroomId,
            communityId
        )
    }

    override fun reactionHintShown() {
        reactionsViewModel.reactionHintShown()
    }

    override fun emoticonGridClicked(
        conversationViewData: ConversationViewData,
        reaction: String?,
        position: Int,
    ) {
        ReactionsListDialog.newInstance(
            ReactionsListExtras.Builder()
                .conversation(conversationViewData)
                .isConversation(true)
                .gridPositionClicked(position)
                .build()
        ).show(childFragmentManager, ReactionsListDialog.TAG)
        if (!conversationViewData.chatroomId
                .isNullOrEmpty() && !conversationViewData.communityId.isNullOrEmpty()
        )
            reactionsViewModel.sendReactionListOpenedEvent(
                conversationViewData.id,
                conversationViewData.chatroomId,
                conversationViewData.communityId
            )
    }

    override fun chatroomEmoticonGridClicked(
        chatroomViewData: ChatroomViewData,
        reaction: String?,
        position: Int,
    ) {
        ReactionsListDialog.newInstance(
            ReactionsListExtras.Builder()
                .chatroom(chatroomViewData)
                .gridPositionClicked(position)
                .isConversation(false)
                .build()
        ).show(childFragmentManager, ReactionsListDialog.TAG)
        if (chatroomViewData.communityId.isNotEmpty())
            reactionsViewModel.sendReactionListOpenedEvent(
                "",
                chatroomId,
                chatroomViewData.communityId
            )
    }

    override fun deleteMessages(
        conversations: List<ConversationViewData>,
        topic: ConversationViewData?
    ) {
        viewModel.deleteConversations(conversations)
        if (topic != null) {
            viewModel.updateChatroomWhileDeletingTopic()
            initTopChatroomView(getChatroomViewData()!!)
        }
        val audioPlayingConversation =
            conversations.firstOrNull { it.id == localParentConversationId }
        if (mediaAudioService != null && mediaAudioService?.isPlaying() == true && audioPlayingConversation != null) {
            stopAudioService()
        }
    }

    override fun positiveButtonClick() {
        viewModel.leaveChatRoom(chatroomId)
    }

    override fun crossClicked() {
        dismissVideoPlayerPopup()
    }

    override fun fullScreenClicked(isFullScreen: Boolean) {
        ChatroomUtil.setVidePlayerDimensions(
            requireActivity(),
            inAppVideoPlayerPopup,
            isFullScreen
        )
        ChatroomUtil.setStatusBarColor(requireActivity(), requireContext(), isFullScreen)
    }

    // sends dm request when the user clicks on confirm
    override fun sendDMRequest(requestText: String) {
        viewModel.sendDMRequest(chatroomId, ChatRequestState.INITIATED, requestText = requestText)
        clearEditTextAnswer()
    }

    // approves dm request when the user accepts DM request
    override fun approveDMRequest() {
        binding.cvDmRequest.hide()
        viewModel.sendDMRequest(chatroomId, ChatRequestState.ACCEPTED)
    }

    // rejects dm request when the user rejects DM request
    override fun rejectDMRequest() {
        binding.cvDmRequest.hide()
        viewModel.sendDMRequest(chatroomId, ChatRequestState.REJECTED)
    }

    // rejects dm request and reports the user when the user rejects DM request
    override fun reportAndRejectDMRequest() {
        binding.cvDmRequest.hide()
        viewModel.sendDMRequest(chatroomId, ChatRequestState.REJECTED)
        performReportAbuse()
    }

    // unblocks the member and updates the tap to undo
    override fun blockMember(index: Int, state: MemberBlockState) {
        val conversationViewData = chatroomDetailAdapter[index] as ConversationViewData
        val updatedConversationViewData = conversationViewData.toBuilder()
            .showTapToUndo(false)
            .build()

        chatroomDetailAdapter.update(index, updatedConversationViewData)
        updatedBlockActionTitle = BLOCK_ACTION_TITLE
        invalidateActionsMenu()
        viewModel.blockMember(
            chatroomId,
            state
        )
    }

    override fun onMemberTagClicked(memberTag: Uri) {
        super.onMemberTagClicked(memberTag)

        if (memberTag.host == Route.ROUTE_MEMBER
            || memberTag.host == Route.ROUTE_MEMBER_PROFILE
            || memberTag.host == Route.ROUTE_USER_PROFILE
        ) {
            val uuid = memberTag.getNullableQueryParameter("uuid")
                ?: memberTag.lastPathSegment ?: return

            val member = viewModel.getMemberFromDB(uuid)
            SDKApplication.getLikeMindsCallback()?.openProfile(member)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.lm_chat_chatroom_menu, menu)
        updateChatroomActionMenu(menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun newPollOptionEntered(addPollOptionExtras: AddPollOptionExtras) {
        ProgressHelper.showProgress(binding.progressBar)
        if (addPollOptionExtras.conversationId != null) {
            viewModel.addNewConversationPollOption(
                addPollOptionExtras.conversationId,
                addPollOptionExtras.pollOptionText ?: ""
            )
        }
    }

    private fun updateChatroomActionMenu(actionsMenu: Menu?) {
        if (getChatroomViewData() == null) {
            return
        }

        val searchMenuItem = actionsMenu?.findItem(R.id.menu_item_search)
        searchMenuItem?.isVisible = true
        searchMenuItem?.icon?.setTint(LMChatAppearance.getToolbarColor())

        viewModel.getChatroomActions()?.forEach { chatroomActionViewData ->
            when (chatroomActionViewData.id) {
                "2" -> {
                    val item = actionsMenu?.findItem(R.id.view_participants)
                    item?.isVisible = true
                    item?.title = chatroomActionViewData.title
                }

                "3" -> {
                    val item = actionsMenu?.findItem(R.id.invite)
                    item?.isVisible = true
                    item?.title = chatroomActionViewData.title
                    val item2 = actionsMenu?.findItem(R.id.share_chatroom_icon)
                    item2?.isVisible = true
                    item2?.icon?.setTint(LMChatAppearance.getToolbarColor())
                }

                "4", "9" -> {
                    val item = actionsMenu?.findItem(R.id.join_leave_chatroom)
                    item?.isVisible = true
                    if (updatedFollowActionTitle != null) {
                        item?.title = updatedFollowActionTitle
                    } else {
                        item?.title = chatroomActionViewData.title
                    }
                }

                "5" -> {
                    val item = actionsMenu?.findItem(R.id.view_community)
                    item?.isVisible = true
                    item?.title = chatroomActionViewData.title
                }

                "6", "8" -> {
                    val item = actionsMenu?.findItem(R.id.mute_unmute_notifications)
                    item?.isVisible = true
                    if (updatedMuteActionTitle != null) {
                        item?.title = updatedMuteActionTitle
                    } else {
                        item?.title = chatroomActionViewData.title
                    }
                }

                "10" -> {
                    val item = actionsMenu?.findItem(R.id.report_chatroom)
                    item?.isVisible = true
                    item?.title = chatroomActionViewData.title
                }

                "15" -> {
                    val item = actionsMenu?.findItem(R.id.leave_chatroom)
                    item?.isVisible = true
                    item?.title = chatroomActionViewData.title
                }

                "16" -> {
                    val item = actionsMenu?.findItem(R.id.add_all_member)
                    item?.isVisible = true
                    item?.title = chatroomActionViewData.title
                }

                "17" -> {
                    val item = actionsMenu?.findItem(R.id.chatroom_settings)
                    item?.isVisible = true
                    item?.title = chatroomActionViewData.title
                }

                "21" -> {
                    val item = actionsMenu?.findItem(R.id.view_profile)
                    item?.isVisible = false
                    item?.title = chatroomActionViewData.title
                }

                "27", "28" -> {
                    val item = actionsMenu?.findItem(R.id.block_unblock)
                    item?.isVisible = true
                    if (updatedBlockActionTitle != null) {
                        item?.title = updatedBlockActionTitle
                    } else {
                        item?.title = chatroomActionViewData.title
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressed()
            }

            R.id.view_participants -> {
                openViewParticipantsActivity()
            }

            R.id.mute_unmute_notifications -> {
                muteUnmuteNotifications(item)
            }

            R.id.view_community -> {
                // todo:
//                redirectToCommunityDetail()
            }

            R.id.join_leave_chatroom -> {
                joinLeaveChatroom()
            }

            R.id.report_chatroom -> {
                performReportAbuse()
            }

            R.id.invite -> {
                shareChatroom()
            }

            R.id.share_chatroom_icon -> {
                shareChatroom()
            }

            R.id.leave_chatroom -> {
                showLeaveChatroomConfirmationPopup()
            }

            R.id.block_unblock -> {
                val value: MemberBlockState
                if (item.title.toString() == BLOCK_ACTION_TITLE
                ) {
                    updatedBlockActionTitle = UNBLOCK_ACTION_TITLE
                    value = MemberBlockState.MEMBER_BLOCKED
                } else {
                    updatedBlockActionTitle = BLOCK_ACTION_TITLE
                    value = MemberBlockState.MEMBER_UNBLOCKED
                    updateTapToUndoLocally(false)
                }
                invalidateActionsMenu()
                viewModel.blockMember(
                    chatroomId,
                    value
                )
            }

            R.id.menu_item_search -> {
                searchConversations()
            }

            // todo: profile
//            R.id.view_profile -> {
//                redirectToProfile()
//            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val searchConversationsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val resultExtras = ExtrasUtil.getParcelable(
                    extras,
                    LM_CHAT_SEARCH_RESULT,
                    LMChatSearchResult::class.java
                ) ?: return@registerForActivityResult

                val searchConversationId = resultExtras.conversationId
                if (!searchConversationId.isNullOrEmpty()) {
                    scrolledConversationPosition =
                        getIndexOfConversation(searchConversationId)
                    scrollToPositionWithOffset(scrolledConversationPosition)
                }
            }
        }

    private fun searchConversations() {
        val extras = LMChatSearchExtras.Builder()
            .chatroomId(chatroomId)
            .build()

        searchConversationsLauncher.launch(LMChatSearchActivity.getIntent(requireContext(), extras))
    }

    private fun openViewParticipantsActivity() {
        if (isGuestUser) {
            callGuestFlowCallback()
        } else {
            val chatroomViewData = getChatroomViewData() ?: return
            onScreenChanged()
            val extras = ViewParticipantsExtras.Builder()
                .chatroomId(chatroomViewData.id)
                .communityId(chatroomViewData.communityId)
                .isSecretChatroom(chatroomViewData.isSecret ?: false)
                .chatroomName(chatroomViewData.header ?: "")
                .build()
            ViewParticipantsActivity.start(requireContext(), extras)
        }
    }

    private fun muteUnmuteNotifications(item: MenuItem) {
        if (isGuestUser) {
            callGuestFlowCallback()
        } else {
            val value: Boolean
            if (item.title.toString().lowercase()
                    .contains(UNMUTE_ACTION_IDENTIFIER)
            ) {
                updatedMuteActionTitle = MUTE_ACTION_TITLE
                value = false
                ViewUtils.showShortToast(
                    requireContext(),
                    requireContext().getString(R.string.lm_chat_notifications_unmuted_for_this_chatroom)
                )
            } else {
                updatedMuteActionTitle = UNMUTE_ACTION_TITLE
                value = true
                ViewUtils.showShortToast(
                    requireContext(),
                    requireContext().getString(R.string.lm_chat_notifications_muted_for_this_chatroom)
                )
            }
            invalidateActionsMenu()
            viewModel.muteChatroom(
                chatroomId,
                value
            )
            viewModel.sendChatroomMuted(value)
            updateChatroomMuteStatus(value)
        }
    }

    private fun updateChatroomMuteStatus(updatedMuteStatus: Boolean) {
        chatroomResultExtras = chatroomResultExtras?.also {
            chatroomResultExtras?.toBuilder()
                ?.chatroomId(chatroomId)
                ?.isChatroomMutedChanged(true)
                ?.updatedMuteStatus(updatedMuteStatus)
                ?.build()
        } ?: run {
            ChatroomDetailResultExtras.Builder()
                .chatroomId(chatroomId)
                .isChatroomMutedChanged(true)
                .updatedMuteStatus(updatedMuteStatus)
                .build()
        }
        setChatroomDetailActivityResult()
    }

    private fun joinLeaveChatroom() {
        if (isGuestUser) {
            callGuestFlowCallback()
        } else {
            val value: Boolean
            if (getChatroomViewData()?.followStatus == true) {
                updatedFollowActionTitle = FOLLOW_ACTION_TITLE
                value = false
            } else {
                updatedFollowActionTitle = UNFOLLOW_ACTION_TITLE
                value = true
            }
            invalidateActionsMenu()
            follow(value, SOURCE_CHAT_ROOM_OVERFLOW_MENU)
        }
    }

    private fun performReportAbuse() {
        if (context == null) {
            return
        }
        onScreenChanged()

        //create extras for [ReportActivity]
        val reportExtras = ReportExtras.Builder()
            .type(REPORT_TYPE_CHATROOM)
            .chatroomId(getChatroomViewData()?.id)
            .chatroomName(getChatroomViewData()?.header)
            .communityId(getChatroomViewData()?.communityId)
            .build()

        //get Intent for [ReportActivity]
        val intent = ReportActivity.getIntent(requireContext(), reportExtras)

        //start [ReportActivity] and check for result
        reportLauncher.launch(intent)
    }

    private fun shareChatroom() {
        if (isGuestUser) {
            callGuestFlowCallback()
        } else {
            ViewUtils.hideKeyboard(requireActivity())
            ShareUtils.shareChatroom(
                requireContext(),
                (viewModel.chatroomDetail.chatroom?.id ?: ""),
                LMChatUserMetaData.getInstance().domain ?: ShareUtils.DOMAIN
            )
            viewModel.sendChatroomShared()
        }
    }

    private fun showLeaveChatroomConfirmationPopup() {
        val dialog = LeaveSecretChatroomDialog(this)
        dialog.show(childFragmentManager, LeaveSecretChatroomDialog.TAG)
    }

    //start PollResultActivity
    private fun navigateToPollResult(extras: PollResultExtras) {
        PollResultsActivity.start(requireContext(), extras)
    }

    /**------------------------------------------------------------
     * Lifecycle functions
    ---------------------------------------------------------------*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) {
            return
        }

        if (requestCode == REQUEST_GIFS) {
            giphySelected(data?.getParcelableExtra(GiphyDialogFragment.MEDIA_DELIVERY_KEY))
        }
    }

    override fun onPause() {
        super.onPause()
        if (inAppVideoPlayerPopup?.isShowing == true) {
            inAppVideoPlayerPopup?.playOrPauseVideo(play = false)
        }
    }

    override fun onStop() {
        setLastSeenTrueAndSaveDraftResponse()
        if (isVoiceNoteLocked) {
            isVoiceNoteLocked = false
            voiceNoteUtils.stopVoiceNote(binding, RECORDING_LOCK_DONE)
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (blockedAccessPopUp != null && blockedAccessPopUp?.isShowing == true) {
            blockedAccessPopUp?.dismiss()
        }
        if (mediaAudioServiceBound) {
            requireActivity().unregisterReceiver(progressReceiver)
            requireActivity().unregisterReceiver(audioCompleteReceiver)
            stopAudioService()
            mediaAudioService = null
            serviceConnection = null
        }
        if (isVoiceNoteLocked) {
            isVoiceNoteLocked = false
            voiceRecorder.stopRecording()
        }
        voiceNoteUtils.clear()
        super.onDestroy()
    }

    private fun subscribeToChatEvent() {
        ChatEvent.getPublisher().subscribe(this)
    }

    private fun unsubscribeToChatEvent() {
        ChatEvent.getPublisher().unsubscribe(this)
    }

    override fun update(postData: Any) {
        if (postData is HashMap<*, *>) {
            postData.map {
                val conversationID = it.key ?: return
                if (conversationID is String) {
                    val widgetViewData = it.value ?: return
                    if (widgetViewData is WidgetViewData) {
                        val index = getIndexOfConversation(conversationID)
                        if (index >= 0) {
                            var conversationViewData =
                                chatroomDetailAdapter.items()[index] as? ConversationViewData
                                    ?: return
                            conversationViewData =
                                conversationViewData.toBuilder().widget(widgetViewData).build()

                            //update recycler view
                            chatroomDetailAdapter.update(index, conversationViewData)
                        }
                    }
                }
            }
        }
    }
}