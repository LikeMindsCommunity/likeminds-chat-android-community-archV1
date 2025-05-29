package com.likeminds.chatmm.utils.customview

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.likeminds.chatmm.R
import com.likeminds.chatmm.utils.connectivity.ConnectivityBroadcastReceiver
import com.likeminds.chatmm.utils.connectivity.ConnectivityReceiverListener
import com.likeminds.chatmm.utils.permissions.*
import com.likeminds.chatmm.utils.permissions.LMChatPermission.Companion.READ_MEDIA_VISUAL_USER_SELECTED
import com.likeminds.chatmm.utils.permissions.LMChatPermission.Companion.REQUEST_GALLERY
import com.likeminds.chatmm.utils.permissions.model.LMChatPermissionExtras
import com.likeminds.chatmm.utils.snackbar.CustomSnackBar
import javax.inject.Inject

open class BaseAppCompatActivity : ConnectivityReceiverListener, AppCompatActivity() {
    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are *not* resumed.
     */

    @Inject
    lateinit var snackBar: CustomSnackBar

    private lateinit var lmChatSessionPermission: LMChatSessionPermission
    private val lmChatPermissionCallbackSparseArray = SparseArray<LMChatPermissionCallback>()

    private var wasNetworkGone = false

    private val connectivityBroadcastReceiver by lazy {
        ConnectivityBroadcastReceiver()
    }

    /**
     * attachs to the component
     */
    protected open fun attachDagger() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        attachDagger()
        lmChatSessionPermission = LMChatSessionPermission(application)
    }

    override fun onResume() {
        super.onResume()
        connectivityBroadcastReceiver.setListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                connectivityBroadcastReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
                Context.RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                connectivityBroadcastReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        setStatusBarColor()
    }

    override fun onPause() {
        super.onPause()
    }

    @SuppressLint("InlinedApi")
    private fun setStatusBarColor() {
        val statusBarColor = ContextCompat.getColor(this, R.color.lm_chat_white)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = true
            window.statusBarColor = statusBarColor
        } else {
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = true

            window.decorView.setBackgroundColor(statusBarColor)
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            window.insetsController?.show(WindowInsets.Type.statusBars())
        }
    }

    fun hasPermission(permission: LMChatPermission): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            true
        } else {
            checkSelfPermission(permission.permissionName) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasPermissions(permissions: Array<String>): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            true
        } else {
            var hasPermission = true
            var isPartialMediaPermission = false
            permissions.forEach { permission ->
                if (permission == READ_MEDIA_VISUAL_USER_SELECTED
                    && checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
                ) {
                    isPartialMediaPermission = true
                    return@forEach
                }
                hasPermission =
                    hasPermission && checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
            }
            return hasPermission || isPartialMediaPermission
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun requestMultiplePermissions(
        permissionExtras: LMChatPermissionExtras,
        permissionCallback: LMChatPermissionCallback
    ) {
        permissionExtras.apply {
            permissions.forEach { permissionName ->
                lmChatPermissionCallbackSparseArray.put(requestCode, permissionCallback)
                lmChatSessionPermission.setPermissionRequest(permissionName)
            }
            requestPermissions(permissions, requestCode)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun requestPermission(
        lmChatPermission: LMChatPermission,
        lmFeedPermissionCallback: LMChatPermissionCallback
    ) {
        lmChatPermissionCallbackSparseArray.put(
            lmChatPermission.requestCode,
            lmFeedPermissionCallback
        )
        lmChatSessionPermission.setPermissionRequest(lmChatPermission.permissionName)
        requestPermissions(arrayOf(lmChatPermission.permissionName), lmChatPermission.requestCode)
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun canRequestPermission(lmChatPermission: LMChatPermission): Boolean {
        return !wasRequestedBefore(lmChatPermission.permissionName) ||
                shouldShowRequestPermissionRationale(lmChatPermission.permissionName)
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun canRequestPermissions(permissions: Array<String>): Boolean {
        var canRequest = true
        permissions.forEach { permission ->
            canRequest = canRequest && (!wasRequestedBefore(permission) ||
                    shouldShowRequestPermissionRationale(permission))
        }
        return canRequest
    }

    private fun wasRequestedBefore(permissionName: String): Boolean {
        return lmChatSessionPermission.wasPermissionRequestedBefore(permissionName)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val callback = lmChatPermissionCallbackSparseArray.get(requestCode, null) ?: return
        if (grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callback.onGrant()
            } else if (requestCode == REQUEST_GALLERY
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && checkSelfPermission(READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
            ) {
                //if the API version >= 34 and the request code is REQUEST_GALLERY then we check if the READ_MEDIA_VISUAL_USER_SELECTED permission is granted
                callback.onGrant()
            } else {
                callback.onDeny()
            }
        }
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        val parentView = findViewById<ViewGroup>(android.R.id.content) ?: return
        if (parentView.childCount > 0) {
            parentView.getChildAt(0)?.let { view ->
                if (isConnected && wasNetworkGone) {
                    wasNetworkGone = false
                    snackBar.showMessage(
                        view,
                        getString(R.string.lm_chat_internet_connection_restored),
                        true
                    )
                }
                if (!isConnected) {
                    wasNetworkGone = true
                    snackBar.showNoInternet(view)
                }
            }
        }
    }
}