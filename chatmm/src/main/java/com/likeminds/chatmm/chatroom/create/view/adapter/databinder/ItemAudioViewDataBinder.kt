package com.likeminds.chatmm.chatroom.create.view.adapter.databinder

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintSet
import com.likeminds.chatmm.R
import com.likeminds.chatmm.chatroom.create.view.adapter.ChatroomItemAdapterListener
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil
import com.likeminds.chatmm.conversation.model.AttachmentViewData
import com.likeminds.chatmm.databinding.ItemAudioBinding
import com.likeminds.chatmm.utils.DateUtil
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.databinding.ImageBindingUtil
import com.likeminds.chatmm.utils.model.ITEM_AUDIO
import javax.inject.Inject

class ItemAudioViewDataBinder @Inject constructor(
    private val adapterListener: ChatroomItemAdapterListener?,
) : ViewDataBinder<ItemAudioBinding, AttachmentViewData>() {

    override val viewType: Int
        get() = ITEM_AUDIO

    override fun createBinder(parent: ViewGroup): ItemAudioBinding {
        return ItemAudioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun bindData(
        binding: ItemAudioBinding,
        data: AttachmentViewData,
        position: Int
    ) {
        binding.apply {
            this.attachment = data
            this.position = position
            parentConversation = data.parentConversation
            parentChatRoom = data.parentChatroom
            parentViewItemPosition = data.parentViewItemPosition

            setListeners(this)

            if (data.thumbnail != null) {
                viewMask.show()
                ImageBindingUtil.loadImage(
                    ivAudioCover,
                    data.thumbnail,
                    placeholder = R.drawable.lm_chat_view_corner_radius_audio,
                    cornerRadius = 8
                )
            } else {
                viewMask.hide()
                ivAudioCover.setImageResource(R.drawable.lm_chat_view_corner_radius_audio)
            }

            seekBar.max = data.meta?.duration ?: 100

            ChatroomConversationItemViewDataBinderUtil.initAudioItemView(this, data)

            val actionVisible = adapterListener?.isMediaActionVisible()
            val uploadFailed = adapterListener?.isMediaUploadFailed()

            if (actionVisible == true || uploadFailed == true) {
                ivUploadWave.show()
                playPauseGroup.hide()
            } else {
                ivUploadWave.hide()
                playPauseGroup.show()
            }

            if (data.parentConversation != null) {
                val hasAnswer = data.parentConversation.hasAnswer()
                val isLastItem = data.parentConversation.attachmentCount - 1 == position
                if (hasAnswer || !isLastItem) {
                    setConstraintsForName(this, true)
                } else {
                    setConstraintsForName(this, false)
                }
            } else {
                setConstraintsForName(this, true)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners(binding: ItemAudioBinding) {
        binding.apply {
            root.setOnClickListener {
                if (!checkForSelection(this, checkForSelectionEnabled = true)) {
                    return@setOnClickListener
                }
            }

            root.setOnLongClickListener {
                checkForSelection(this)
                true
            }

            ivPlayPause.setOnClickListener {
                if (!checkForSelection(this, checkForSelectionEnabled = true)) {
                    val attachment = attachment
                    val parentConversationId = parentConversation?.id
                    val childPosition = position
                    if (attachment != null) {
                        if (parentConversationId != null) {
                            adapterListener?.onAudioConversationActionClicked(
                                attachment,
                                parentConversationId,
                                childPosition,
                                seekBar.progress
                            )
                        }
                    }
                }
            }

            ivPlayPause.setOnLongClickListener {
                checkForSelection(this)
                true
            }

            //Variables for long click and click on seekbar
            var xAtDown = 0f
            var yAtDown = 0f
            var handler: Handler? = null

            seekBar.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!checkForSelection(this, checkForSelectionEnabled = true)) {
                            adapterListener?.onSeekBarFocussed(true)
                            xAtDown = event.x
                            yAtDown = event.y
                            handler = Handler(Looper.getMainLooper())
                            handler?.postDelayed({
                                handler?.removeCallbacksAndMessages(null)
                                handler = null
                                checkForSelection(binding)
                            }, ViewConfiguration.getLongPressTimeout().toLong())
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        adapterListener?.onSeekBarFocussed(false)
                        handler?.removeCallbacksAndMessages(null)
                        handler = null
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.x - xAtDown >= ViewUtils.dpToPx(5) || event.y - yAtDown >= ViewUtils.dpToPx(
                                5
                            )
                        ) {
                            handler?.removeCallbacksAndMessages(null)
                            handler = null
                        }
                    }
                }
                return@setOnTouchListener false
            }

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                val attachment = binding.attachment
                val parentConversationId = parentConversation?.id
                val childPosition = position

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser && attachment != null) {
                        tvAudioDuration.text =
                            DateUtil.formatSeconds(progress)
                        if (parentConversationId != null) {
                            adapterListener?.onConversationSeekBarChanged(
                                progress,
                                attachment,
                                parentConversationId,
                                childPosition
                            )
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun checkForSelection(
        binding: ItemAudioBinding,
        checkForSelectionEnabled: Boolean = false,
    ): Boolean {
        val attachmentViewData = binding.attachment
        if (attachmentViewData?.parentChatroom == null) {
            val parentConversation = attachmentViewData?.parentConversation ?: return false
            val parentPosition = attachmentViewData.parentViewItemPosition ?: return false
            if (checkForSelectionEnabled) {
                if (adapterListener?.isSelectionEnabled() == true) {
                    adapterListener.onLongPressConversation(parentConversation, parentPosition)
                    return true
                }
            } else {
                adapterListener?.onLongPressConversation(parentConversation, parentPosition)
                return true
            }
        } else {
            val parentChatroom = attachmentViewData.parentChatroom
            val parentPosition = attachmentViewData.parentViewItemPosition ?: return false
            if (checkForSelectionEnabled) {
                if (adapterListener?.isSelectionEnabled() == true) {
                    adapterListener.onLongPressChatRoom(parentChatroom, parentPosition)
                    return true
                }
            } else {
                adapterListener?.onLongPressChatRoom(parentChatroom, parentPosition)
                return true
            }
        }
        return false
    }

    private fun setConstraintsForName(binding: ItemAudioBinding, connectToParent: Boolean) {
        binding.apply {
            val constraintLayout = clLayout
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            if (connectToParent) {
                constraintSet.connect(
                    tvAudioName.id,
                    ConstraintSet.END,
                    clLayout.id,
                    ConstraintSet.END,
                    ViewUtils.dpToPx(5)
                )
            } else {
                constraintSet.connect(
                    tvAudioName.id,
                    ConstraintSet.END,
                    guideline.id,
                    ConstraintSet.START
                )
            }
            constraintSet.applyTo(constraintLayout)
        }
    }
}