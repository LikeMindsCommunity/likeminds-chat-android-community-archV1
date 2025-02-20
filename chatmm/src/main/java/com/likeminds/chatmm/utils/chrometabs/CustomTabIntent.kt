package com.likeminds.chatmm.utils.chrometabs

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.google.gson.Gson
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.conversation.model.ReportLinkExtras
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

internal object CustomTabIntent {

    private const val TAG = "CustomTabIntent"

    private val gson = Gson()
    private var tabIntent: CustomTabsIntent? = null

    fun open(context: Context, uri: String, extras: ReportLinkExtras? = null) {
        try {
            val link = Uri.parse(uri)
            if (tabIntent == null) {
                //use first builder if sessions manipulation is required
                //val builder = CustomTabsIntent.Builder(CustomTabUtil.getInstance().getSession())
                val builder = CustomTabsIntent.Builder()
                builder.setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(LMChatAppearance.getHeaderColor()).build()
                )
                builder.setStartAnimations(context, R.anim.lm_chat_slide_in_right, R.anim.lm_chat_slide_out_left)
                builder.setStartAnimations(
                    context,
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
                builder.addMenuItem(
                    "Share link",
                    createPendingIntent(context, CustomTabBroadcastReceiver.ACTION_SHARE)
                )
                builder.addMenuItem(
                    "Copy link",
                    createPendingIntent(context, CustomTabBroadcastReceiver.ACTION_COPY)
                )
                extras?.toBuilder()?.link(uri)?.build()
                builder.addMenuItem(
                    "Report link as suspicious",
                    createPendingIntent(
                        context,
                        CustomTabBroadcastReceiver.ACTION_REPORT,
                        extras
                    )
                )
                val closeIcon = CustomTabHelper.getBitmapFromVectorDrawable(
                    context,
                    R.drawable.lm_chat_ic_arrow_back_white_24dp
                )
                if (closeIcon != null) {
                    builder.setCloseButtonIcon(closeIcon)
                }
                tabIntent = builder.build()
            }
            CustomTabUtil.openCustomTab(
                context,
                tabIntent!!,
                link
            ) { context, uri -> CustomTabHelper.openLinkViaBrowser(context, uri) }
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            Log.e(TAG, e.message.toString())
        }
    }

    fun getIntent(context: Context, uri: Uri): Intent? {
        try {
            if (tabIntent == null) {
                //use first builder if sessions manipulation is required
                //val builder = CustomTabsIntent.Builder(CustomTabUtil.getInstance().getSession())
                val builder = CustomTabsIntent.Builder()
                builder.setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(LMChatAppearance.getHeaderColor()).build()
                )
                builder.setStartAnimations(context, R.anim.lm_chat_slide_in_right, R.anim.lm_chat_slide_out_left)
                builder.setStartAnimations(
                    context,
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
                builder.addMenuItem(
                    "Share link",
                    createPendingIntent(context, CustomTabBroadcastReceiver.ACTION_SHARE)
                )
                builder.addMenuItem(
                    "Copy link",
                    createPendingIntent(context, CustomTabBroadcastReceiver.ACTION_COPY)
                )
                val closeIcon = CustomTabHelper.getBitmapFromVectorDrawable(
                    context,
                    R.drawable.lm_chat_ic_arrow_back_white_24dp
                )
                if (closeIcon != null) {
                    builder.setCloseButtonIcon(closeIcon)
                }
                tabIntent = builder.build()
            }
            CustomTabUtil.openCustomTab(
                context, tabIntent!!, uri
            ) { context, uri -> CustomTabHelper.openLinkViaBrowser(context, uri) }
            return tabIntent!!.intent
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            Log.e(TAG, e.message.toString())
        }
        return null
    }

    private fun createPendingIntent(
        context: Context,
        action: String,
        extras: ReportLinkExtras? = null,
    ): PendingIntent {
        val intent = Intent(context, CustomTabBroadcastReceiver::class.java)
        intent.action = action
        intent.putExtra("extras", gson.toJson(extras, ReportLinkExtras::class.java))
        return PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}