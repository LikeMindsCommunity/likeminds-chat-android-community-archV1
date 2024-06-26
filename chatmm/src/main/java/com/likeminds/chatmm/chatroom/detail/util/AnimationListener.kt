package com.likeminds.chatmm.chatroom.detail.util

import android.animation.Animator

abstract class AnimationListener : Animator.AnimatorListener {
    override fun onAnimationStart(animation: Animator) {}

    override fun onAnimationEnd(animation: Animator) {}

    override fun onAnimationCancel(animation: Animator) {}

    override fun onAnimationRepeat(animation: Animator) {}
}