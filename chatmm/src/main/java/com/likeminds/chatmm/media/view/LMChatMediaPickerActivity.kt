package com.likeminds.chatmm.media.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.chatmm.utils.ViewUtils.currentFragment
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity
import com.likeminds.chatmm.utils.permissions.*

class LMChatMediaPickerActivity : BaseAppCompatActivity() {

    private lateinit var mediaPickerExtras: LMChatMediaPickerExtras

    companion object {
        const val PICK_MEDIA = 5001
        const val BROWSE_MEDIA = 5002
        const val BROWSE_DOCUMENT = 5003
        const val PICK_CAMERA = 5004
        const val CROP_IMAGE = 5005

        private const val ARG_MEDIA_PICKER_EXTRAS = "mediaPickerExtras"
        const val ARG_MEDIA_PICKER_RESULT = "mediaPickerResult"

        fun start(context: Context, extras: LMChatMediaPickerExtras) {
            val intent = Intent(context, LMChatMediaPickerActivity::class.java)
            intent.apply {
                putExtras(Bundle().apply {
                    putParcelable(ARG_MEDIA_PICKER_EXTRAS, extras)
                })
            }
            context.startActivity(intent)
        }

        fun getIntent(context: Context, extras: LMChatMediaPickerExtras): Intent {
            return Intent(context, LMChatMediaPickerActivity::class.java).apply {
                putExtras(Bundle().apply {
                    putParcelable(ARG_MEDIA_PICKER_EXTRAS, extras)
                })
            }
        }
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().mediaComponent()?.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Call before the DecorView is accessed in setContentView
        theme.applyStyle(R.style.OptOutEdgeToEdgeEnforcement, /* force */ false)

        setContentView(R.layout.lm_chat_activity_media_picker)
        val extras = ExtrasUtil.getParcelable(
            intent.extras,
            ARG_MEDIA_PICKER_EXTRAS,
            LMChatMediaPickerExtras::class.java
        )
        if (extras == null) {
            throw IllegalArgumentException("Arguments are missing")
        } else {
            mediaPickerExtras = extras
        }

        checkMediaPermission()
    }

    private fun checkMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val mediaTypes = mediaPickerExtras.mediaTypes
            if (mediaTypes.contains(PDF) || mediaTypes.contains(GIF)) {
                startMediaPickerFragment()
                return
            }
            val permissionExtras = LMChatPermission.getMediaPermissionExtras(
                this,
                mediaTypes
            )

            LMChatPermissionManager.performTaskWithPermissionExtras(
                this,
                { startMediaPickerFragment() },
                permissionExtras,
                showInitialPopup = true,
                showDeniedPopup = true,
                lmChatPermissionDeniedCallback = object : LMChatPermissionDeniedCallback {
                    override fun onDeny() {
                        onBackPressedDispatcher.onBackPressed()
                    }

                    override fun onCancel() {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            )
        } else {
            LMChatPermissionManager.performTaskWithPermission(
                this,
                { startMediaPickerFragment() },
                LMChatPermission.getStoragePermissionData(),
                showInitialPopup = true,
                showDeniedPopup = true,
                lmChatPermissionDeniedCallback = object : LMChatPermissionDeniedCallback {
                    override fun onDeny() {
                        onBackPressedDispatcher.onBackPressed()
                    }

                    override fun onCancel() {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            )
        }
    }

    private fun startMediaPickerFragment() {
        checkIfDocumentPickerInitiated()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as? NavHostFragment ?: return
        val graphInflater = navHostFragment.navController.navInflater
        val navGraph = graphInflater.inflate(R.navigation.lm_chat_nav_media_picker_graph)
        val navController = navHostFragment.navController

        when {
            InternalMediaType.isImageOrVideo(mediaPickerExtras.mediaTypes) -> {
                navGraph.setStartDestination(R.id.mediaPickerFolderFragment)
            }
            InternalMediaType.isPDF(mediaPickerExtras.mediaTypes) -> {
                navGraph.setStartDestination(R.id.mediaPickerDocumentFragment)
            }
            InternalMediaType.isAudio(mediaPickerExtras.mediaTypes) -> {
                navGraph.setStartDestination(R.id.mediaPickerAudioFragment)
            }
            else -> {
                finish()
            }
        }
        val args = Bundle().apply {
            putParcelable(ARG_MEDIA_PICKER_EXTRAS, mediaPickerExtras)
        }
        navController.setGraph(navGraph, args)
    }

    /**
     * If Media Picker type is Pdf and device version is >= Q(29), then show system app picker.
     * This is done due to storage restrictions for non-media files in Android 10.
     * */
    private fun checkIfDocumentPickerInitiated() {
        if (InternalMediaType.isPDF(mediaPickerExtras.mediaTypes)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            val intent = Intent().apply {
                putExtras(Bundle().apply {
                    putParcelable(
                        ARG_MEDIA_PICKER_RESULT, MediaPickerResult.Builder()
                            .mediaPickerResultType(MEDIA_RESULT_BROWSE)
                            .mediaTypes(mediaPickerExtras.mediaTypes)
                            .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
                            .build()
                    )
                })
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LMChatPermissionManager.REQUEST_CODE_SETTINGS_PERMISSION) {
            checkMediaPermission()
        }
    }

    override fun onBackPressed() {
        when (val fragment = supportFragmentManager.currentFragment(R.id.nav_host)) {
            is LMChatMediaPickerFolderFragment -> {
                super.onBackPressed()
            }
            is LMChatMediaPickerItemFragment -> {
                fragment.onBackPressedFromFragment()
            }
            is LMChatMediaPickerDocumentFragment -> {
                if (fragment.onBackPressedFromFragment()) super.onBackPressed()
            }
            is MediaPickerAudioFragment -> {
                if (fragment.onBackPress()) super.onBackPressed()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}