package com.likeminds.chatmm.chatroom.detail.util

import android.animation.Animator
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.likeminds.chatmm.R
import com.likeminds.chatmm.chatroom.detail.model.*
import com.likeminds.chatmm.databinding.FragmentChatroomDetailBinding
import com.likeminds.chatmm.media.model.SingleUriData
import com.likeminds.chatmm.media.model.VOICE_NOTE
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.file.util.FileUtil.size
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VoiceNoteUtils(
    val context: Context,
    private val voiceNoteInterface: VoiceNoteInterface,
) {

    private var animBlink = AnimationUtils.loadAnimation(context, R.anim.lm_chat_blink)
    private var animJump = AnimationUtils.loadAnimation(context, R.anim.lm_chat_jump)
    private var animJumpFast = AnimationUtils.loadAnimation(context, R.anim.lm_chat_jump_fast)
    private var handler: Handler? = null

    private var audioTotalTime: Long = 0
    private var timerTask: TimerTask? = null
    private var audioTimer: Timer? = null
    private var timeFormatter: SimpleDateFormat? = null

    private var isVoiceNoteLocked = voiceNoteInterface.isVoiceNoteLocked()

    companion object {
        const val TIME_LIMIT: Long = 901L
    }

    init {
        handler = Handler(Looper.getMainLooper())
        timeFormatter = SimpleDateFormat("mm:ss", Locale.getDefault())
    }

    private val dp =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics)

    private fun enlargeFAB(fab: FloatingActionButton) {
        fab.animate().scaleXBy(1f).scaleYBy(1f).setDuration(200)
            .setInterpolator(OvershootInterpolator()).start()
    }

    private fun smallFAB(fab: FloatingActionButton) {
        fab.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f)
            .setDuration(100).setInterpolator(LinearInterpolator()).start()
    }

    fun startVoiceNote(binding: FragmentChatroomDetailBinding) {
        binding.apply {
            voiceNoteInterface.onVoiceNoteStarted()
            inputBox.groupEditText.hide()
            enlargeFAB(fabMic)

            ivMicRecording.apply {
                show()
                clearAnimation()
                startAnimation(animBlink)
            }

            cardCancel.show()
            cardLock.show()

            ivLock.apply {
                show()
                clearAnimation()
                startAnimation(animJump)
            }

            ivLockArrow.apply {
                show()
                clearAnimation()
                startAnimation(animJumpFast)
            }

            inputBox.tvVoiceNoteTime.show()

            if (audioTimer == null) {
                audioTimer = Timer()
                timeFormatter?.timeZone = TimeZone.getTimeZone("UTC")
            }

            timerTask = object : TimerTask() {
                override fun run() {
                    handler?.post {
                        if (audioTotalTime != TIME_LIMIT) {
                            inputBox.tvVoiceNoteTime.text =
                                timeFormatter?.format(Date(audioTotalTime * 1000))
                            audioTotalTime++
                        } else {
                            isVoiceNoteLocked = false
                            audioTimer?.cancel()
                            audioTimer?.purge()
                            audioTimer = null
                            stopVoiceNote(binding, RECORDING_LOCK_DONE)
                        }
                    }
                }
            }

            audioTotalTime = 0L
            audioTimer?.schedule(timerTask, 0, 1000)
        }
    }

    fun stopVoiceNote(
        binding: FragmentChatroomDetailBinding,
        @VoiceNoteBehaviours behaviours: String,
    ) {
        binding.apply {
            smallFAB(fabMic)
            voiceNoteInterface.stopTrackingVoiceNoteAction(true)
            cardCancel.hide()
            cardLock.hide()

            ivLock.apply {
                clearAnimation()
                hide()
            }

            ivLockArrow.apply {
                clearAnimation()
                hide()
            }

            if (isVoiceNoteLocked) {
                return
            }

            when (behaviours) {
                RECORDING_LOCKED -> {
                    inputBox.ivCancelVoice.show()
                    inputBox.ivStopVoice.show()

                    voiceNoteInterface.onVoiceNoteLocked()
                }
                RECORDING_CANCELLED -> {
                    inputBox.tvVoiceNoteTime.apply {
                        clearAnimation()
                        text = ""
                        hide()
                    }

                    ivMicRecording.apply {
                        clearAnimation()
                        hide()
                    }

                    inputBox.ivCancelVoice.hide()
                    inputBox.ivStopVoice.hide()
                    inputBox.ivPlayRecording.hide()
                    delete(binding)
                    timerTask?.cancel()
                    voiceNoteInterface.onVoiceNoteCancelled()
                }
                RECORDING_RELEASED, RECORDING_LOCK_DONE -> {
                    binding.ivMicRecording.apply {
                        clearAnimation()
                        hide()
                    }
                    timerTask?.cancel()
                    inputBox.ivCancelVoice.show()
                    inputBox.ivStopVoice.hide()
                    inputBox.ivPlayRecording.show()
                    voiceNoteInterface.onVoiceNoteCompleted()
                }
                RECORDING_SEND -> {
                    inputBox.ivCancelVoice.hide()
                    inputBox.ivStopVoice.hide()
                    inputBox.ivPlayRecording.hide()
                    inputBox.tvVoiceNoteTime.apply {
                        hide()
                        text = ""
                    }
                    inputBox.groupEditText.show()
                    voiceNoteInterface.onVoiceNoteSend()
                }
                RECORDING_LOCK_SEND -> {
                    inputBox.tvVoiceNoteTime.text = ""
                    ivMicRecording.apply {
                        clearAnimation()
                        hide()
                    }
                    timerTask?.cancel()
                }
            }
        }
    }

    private fun delete(binding: FragmentChatroomDetailBinding) {
        binding.apply {
            ivMicRecording.apply {
                show()
                rotation = 0f
            }

            voiceNoteInterface.isDeletingVoiceNote(true)
            fabMic.isEnabled = false

            handler?.postDelayed({
                voiceNoteInterface.isDeletingVoiceNote(false)
                fabMic.isEnabled = true
            }, 1650)

            //Mic Jump
            ivMicRecording.animate()
                .translationY(-dp * 150)
                .rotation(180f)
                .scaleXBy(0.6f)
                .scaleYBy(0.6f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimationListener() {
                    override fun onAnimationStart(animation: Animator) {
                        val displacement = -dp * 40

                        inputBox.ivBin.translationX = displacement
                        inputBox.ivBinCover.translationX = displacement

                        //from left to right show and open
                        inputBox.ivBinCover.animate()
                            .translationX(-25f)
                            .rotation(-80f)
                            .setDuration(350)
                            .setInterpolator(DecelerateInterpolator())
                            .setListener(null)
                            .start()

                        //from left to right show
                        inputBox.ivBin.animate()
                            .translationX(0f)
                            .setDuration(350)
                            .setInterpolator(DecelerateInterpolator())
                            .setListener(object : AnimationListener() {
                                override fun onAnimationStart(animation: Animator) {
                                    inputBox.groupBin.show()
                                }
                            }).start()
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        ivMicRecording.animate()
                            .translationY(0f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(350)
                            .setListener(object : AnimationListener() {
                                override fun onAnimationEnd(animation: Animator) {
                                    ivMicRecording.apply {
                                        rotation = 0f
                                        hide()
                                    }

                                    val displacement = -dp * 40

                                    //close bin cover
                                    inputBox.ivBinCover.animate()
                                        .translationX(25f)
                                        .rotation(0f)
                                        .setDuration(150)
                                        .setStartDelay(50)
                                        .setListener(null)
                                        .start()

                                    inputBox.ivBin.animate()
                                        .translationX(displacement)
                                        .setDuration(200)
                                        .setStartDelay(250)
                                        .setInterpolator(DecelerateInterpolator())
                                        .start()

                                    //from right to left go
                                    inputBox.ivBinCover.animate()
                                        .translationX(displacement)
                                        .setDuration(200)
                                        .setStartDelay(250)
                                        .setInterpolator(DecelerateInterpolator())
                                        .setListener(object : AnimationListener() {
                                            override fun onAnimationEnd(animation: Animator) {
                                                Handler(Looper.getMainLooper()).postDelayed({
                                                    inputBox.groupEditText.show()
                                                }, 400)
                                            }
                                        }).start()
                                }
                            }).start()
                    }
                }).start()
        }
    }

    fun convertFileNameToSingleUriData(filePath: String): SingleUriData? {
        val uri = Uri.fromFile(File(filePath)) ?: return null
        val duration = getDuration(uri)
        val size = File(filePath).size
        val name = File(filePath).name
        return if (duration != null) {
            SingleUriData.Builder()
                .uri(uri)
                .fileType(VOICE_NOTE)
                .size(size.toLong())
                .mediaName(name)
                .duration(duration)
                .build()
        } else {
            null
        }
    }

    private fun getDuration(uri: Uri): Int? {
        try {
            val retriever = MediaMetadataRetriever()
            val fd = context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
                ?: return null
            retriever.setDataSource(fd)
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toInt() ?: return null
            retriever.release()
            return duration / 1000
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            e.printStackTrace()
        }
        return null
    }

    fun clear() {
        handler = null
        timerTask = null
        audioTimer = null
        timeFormatter = null
    }
}