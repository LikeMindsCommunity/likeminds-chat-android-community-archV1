package com.likeminds.chatmm.utils.chrometabs

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity


internal class CustomTabUtil : CustomTabServiceConnectionCallback {

    private var mCustomTabsSession: CustomTabsSession? = null
    private var mCustomTabsClient: CustomTabsClient? = null
    private var mConnection: CustomTabServiceConnection? = null
    private var mConnectionCallback: CustomTabConnectionCallback? = null

    companion object {
        private const val TAG = "CustomTabUtil"
        private const val PACKAGE_NAME = "com.android.chrome"
        private var instance: CustomTabUtil? = null

        fun getInstance(): CustomTabUtil {
            if (instance == null) {
                instance = CustomTabUtil()
            }
            return instance!!
        }

        // Always use this function to handle links
        fun openCustomTab(
            context: Context,
            customTabsIntent: CustomTabsIntent,
            uri: Uri,
            fallback: CustomTabFallback?,
        ) {
            try {
                customTabsIntent.intent.setPackage(PACKAGE_NAME)
                customTabsIntent.launchUrl(context, uri)
            } catch (e: ActivityNotFoundException) {
                LMChatLogger.getInstance()?.handleException(
                    e.message ?: "",
                    e.stackTraceToString(),
                    LMSeverity.CRITICAL
                )
                e.printStackTrace()
                fallback?.openUri(context, uri)
            } catch (e: Exception) {
                LMChatLogger.getInstance()?.handleException(
                    e.message ?: "",
                    e.stackTraceToString(),
                    LMSeverity.CRITICAL
                )
                e.printStackTrace()
                Log.e(TAG, e.message.toString())
            }
        }
    }

    fun getSession(): CustomTabsSession? {
        if (mCustomTabsClient == null) {
            mCustomTabsSession = null
        } else if (mCustomTabsSession == null) {
            mCustomTabsSession = mCustomTabsClient?.newSession(object : CustomTabsCallback() {})
        }
        return mCustomTabsSession
    }

    //callback for tabs service connection
    fun setConnectionCallback(connectionCallback: CustomTabConnectionCallback?) {
        mConnectionCallback = connectionCallback
    }

    //TODO can be used later on to pre fetch links
    fun mayLaunchUrl(uri: Uri, extras: Bundle, otherLikelyBundles: List<Bundle>): Boolean {
        if (mCustomTabsClient == null) return false
        val session = getSession() ?: return false
        return session.mayLaunchUrl(uri, extras, otherLikelyBundles)
    }

    override fun onServiceConnected(client: CustomTabsClient?) {
        mCustomTabsClient = client
        mCustomTabsClient?.warmup(0L)
        if (mConnectionCallback != null) {
            mConnectionCallback?.onCustomTabConnected()
        }
        getSession()
    }

    override fun onServiceDisconnected() {
        mCustomTabsClient = null
        mConnection = null
        if (mConnectionCallback != null) {
            mConnectionCallback?.onCustomTabDisconnected()
        }
    }

    //Bind to activity or application for Warm up
    fun bindCustomTabsService(activity: Activity) {
        if (mCustomTabsClient != null) return
        mConnection = CustomTabServiceConnection(this)
        mConnection?.let {
            CustomTabsClient.bindCustomTabsService(activity, PACKAGE_NAME, it)
            Log.e(TAG, "bindCustomTabsService : ${activity::class.java}")
        }
    }

    //Unbind for removing chromium service
    fun unbindCustomTabsService(activity: Activity) {
        if (mConnection == null) return
        activity.unbindService(mConnection!!)
        mCustomTabsClient = null
        mCustomTabsSession = null
        mConnection = null
        Log.e(TAG, "unbindCustomTabsService : ${activity::class.java}")
    }

}