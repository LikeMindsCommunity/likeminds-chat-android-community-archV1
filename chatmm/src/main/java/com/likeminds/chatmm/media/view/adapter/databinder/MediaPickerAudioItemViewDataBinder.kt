package com.likeminds.chatmm.media.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.databinding.ItemMediaPickerAudioBinding
import com.likeminds.chatmm.media.model.MEDIA_ACTION_NONE
import com.likeminds.chatmm.media.model.MEDIA_ACTION_PAUSE
import com.likeminds.chatmm.media.model.MEDIA_ACTION_PLAY
import com.likeminds.chatmm.media.model.MediaViewData
import com.likeminds.chatmm.media.util.MediaPickerDataBinderUtils
import com.likeminds.chatmm.media.util.MediaUtils
import com.likeminds.chatmm.media.view.adapter.MediaPickerAdapterListener
import com.likeminds.chatmm.utils.DateUtil
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.ITEM_MEDIA_PICKER_AUDIO
import javax.inject.Inject

class MediaPickerAudioItemViewDataBinder @Inject constructor(
    private val listener: MediaPickerAdapterListener,
) : ViewDataBinder<ItemMediaPickerAudioBinding, MediaViewData>() {

    override val viewType: Int
        get() = ITEM_MEDIA_PICKER_AUDIO

    override fun createBinder(parent: ViewGroup): ItemMediaPickerAudioBinding {
        val binding = ItemMediaPickerAudioBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        binding.apply {
            root.setOnClickListener {
                val mediaViewData = mediaViewData ?: return@setOnClickListener
                val position = position ?: return@setOnClickListener
                listener.onMediaItemClicked(mediaViewData, position)
            }

            ivPlayPause.setOnClickListener {
                val mediaViewData = mediaViewData ?: return@setOnClickListener
                val position = position ?: return@setOnClickListener
                listener.onAudioActionClicked(mediaViewData, position)
            }

            ivPlayStateNone.setOnClickListener {
                val mediaViewData = mediaViewData ?: return@setOnClickListener
                val position = position ?: return@setOnClickListener
                listener.onAudioActionClicked(mediaViewData, position)
            }
        }
        return binding
    }

    override fun bindData(
        binding: ItemMediaPickerAudioBinding,
        data: MediaViewData,
        position: Int,
    ) {
        binding.apply {
            this.position = position
            mediaViewData = data
            isSelected = listener.isMediaSelected(data.uri.toString())
            buttonColor = LMChatAppearance.getButtonsColor()

            if (data.filteredKeywords.isNullOrEmpty()) {
                tvAudioName.text = data.mediaName
            } else {
                tvAudioName.setText(
                    MediaPickerDataBinderUtils.getFilteredText(
                        data.mediaName ?: "",
                        data.filteredKeywords,
                        ContextCompat.getColor(root.context, R.color.lm_chat_turquoise),
                    ), TextView.BufferType.SPANNABLE
                )
            }

            when (data.playOrPause) {
                MEDIA_ACTION_NONE -> {
                    ivPlayStateNone.show()
                    ivPlayPause.hide()
                    audioProgressBar.hide()
                }
                MEDIA_ACTION_PLAY -> {
                    ivPlayStateNone.hide()
                    ivPlayPause.show()
                    ivPlayPause.setImageResource(R.drawable.lm_chat_ic_audio_pause)
                    audioProgressBar.show()
                }
                MEDIA_ACTION_PAUSE -> {
                    ivPlayStateNone.hide()
                    ivPlayPause.show()
                    ivPlayPause.setImageResource(R.drawable.lm_chat_ic_audio_play)
                    audioProgressBar.show()
                }
            }

            tvAudioSize.text = MediaUtils.getFileSizeText(data.size)
            audioProgressBar.progress = data.audioProgress ?: 0
            tvAudioDuration.text = DateUtil.formatSeconds(data.duration ?: 0)
        }
    }
}