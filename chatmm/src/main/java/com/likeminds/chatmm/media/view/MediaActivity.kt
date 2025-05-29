package com.likeminds.chatmm.media.view

import android.content.*
import android.os.Bundle
import android.view.MenuItem
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.chatmm.utils.ViewUtils.currentFragment
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity

class MediaActivity : BaseAppCompatActivity() {

    companion object {
        const val BUNDLE_MEDIA_EXTRAS = "BUNDLE_MEDIA_EXTRAS"
        const val ARG_MEDIA_EXTRAS = "media_extras"
        const val ARG_SINGLE_URI_DATA = "singleUriData"
        const val ARG_IS_FROM_ACTIVITY = "is_from_activity"
        const val ARG_CROP_SQUARE = "crop_square"

        @JvmStatic
        fun startActivity(context: Context, mediaExtras: MediaExtras) {
            val intent = Intent(context, MediaActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_MEDIA_EXTRAS, mediaExtras)
            intent.putExtras(bundle)
            context.startActivity(intent)
        }

        @JvmStatic
        fun getIntent(
            context: Context,
            mediaExtras: MediaExtras,
            clipData: ClipData? = null,
        ): Intent {
            val intent = Intent(context, MediaActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_MEDIA_EXTRAS, mediaExtras)
            intent.putExtras(bundle)
            if (clipData != null) {
                intent.clipData = clipData
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            return intent
        }
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().mediaComponent()?.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_media)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host)) { view, windowInsets ->
            val innerPadding = windowInsets.getInsets(
                // Notice we're using systemBars, not statusBar
                WindowInsetsCompat.Type.systemBars()
                        // Notice we're also accounting for the display cutouts
                        or WindowInsetsCompat.Type.displayCutout()
                        // If using EditText, also add
                        or WindowInsetsCompat.Type.ime()
            )
            // Apply the insets as padding to the view. Here, set all the dimensions
            // as appropriate to your layout. You can also update the view's margin if
            // more appropriate.
            view.setPadding(0, innerPadding.top, 0, innerPadding.bottom)

            // Return CONSUMED if you don't want the window insets to keep passing down
            // to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as? NavHostFragment ?: return
        val graphInflater = navHostFragment.navController.navInflater
        val navGraph = graphInflater.inflate(R.navigation.lm_chat_nav_media_graph)
        val navController = navHostFragment.navController
        if (intent != null) {
            val bundle = intent.extras
            if (bundle != null) {
                val mediaExtras = ExtrasUtil.getParcelable(
                    bundle,
                    BUNDLE_MEDIA_EXTRAS,
                    MediaExtras::class.java
                )
                if (mediaExtras != null) {
                    when (mediaExtras.mediaScreenType) {
                        MEDIA_VERTICAL_LIST_SCREEN -> {
                            initActionBar(mediaExtras, R.style.LMAppTheme_Black)
                            val args = Bundle().apply {
                                putParcelable(ARG_MEDIA_EXTRAS, mediaExtras)
                            }
                            navGraph.setStartDestination(R.id.mediaVerticalListFragment)
                            navController.setGraph(navGraph, args)
                        }

                        MEDIA_CONVERSATION_EDIT_SCREEN -> {
                            initActionBar(mediaExtras, R.style.LMAppTheme_Black)
                            val args = Bundle().apply {
                                putParcelable(ARG_MEDIA_EXTRAS, mediaExtras)
                            }
                            navGraph.setStartDestination(R.id.conversationMediaEditFragment)
                            navController.setGraph(navGraph, args)
                        }

                        MEDIA_CROP_SCREEN -> {
                            val args = Bundle().apply {
                                putParcelable(ARG_SINGLE_URI_DATA, mediaExtras.singleUriData)
                                putBoolean(ARG_IS_FROM_ACTIVITY, true)
                                putBoolean(ARG_CROP_SQUARE, mediaExtras.cropSquare ?: false)
                            }
                            navGraph.setStartDestination(R.id.imageCropFragment)
                            navController.setGraph(navGraph, args)
                        }

                        MEDIA_VIDEO_PLAY_SCREEN -> {
                            initActionBar(mediaExtras, R.style.LMAppTheme)
                            val args = Bundle().apply {
                                putParcelable(ARG_MEDIA_EXTRAS, mediaExtras)
                            }
                            navGraph.setStartDestination(R.id.playVideoFragment)
                            navController.setGraph(navGraph, args)
                        }

                        MEDIA_HORIZONTAL_LIST_SCREEN -> {
                            initActionBar(mediaExtras, R.style.LMAppTheme)
                            val args = Bundle().apply {
                                putParcelable(ARG_MEDIA_EXTRAS, mediaExtras)
                            }
                            navGraph.setStartDestination(R.id.mediaHorizontalListFragment)
                            navController.setGraph(navGraph, args)
                        }

                        MEDIA_GIF_SEND_SCREEN -> {
                            initActionBar(mediaExtras, R.style.LMAppTheme_Black)
                            val args = Bundle().apply {
                                putParcelable(ARG_MEDIA_EXTRAS, mediaExtras)
                            }
                            navGraph.setStartDestination(R.id.conversationGifSendFragment)
                            navController.setGraph(navGraph, args)
                        }

                        MEDIA_DOCUMENT_SEND_SCREEN -> {
                            initActionBar(mediaExtras, R.style.LMAppTheme_Black)
                            val args = Bundle().apply {
                                putParcelable(ARG_MEDIA_EXTRAS, mediaExtras)
                            }
                            navGraph.setStartDestination(R.id.conversationDocumentSendFragment)
                            navController.setGraph(navGraph, args)
                        }

                        MEDIA_AUDIO_EDIT_SEND_SCREEN -> {
                            initActionBar(mediaExtras, R.style.LMAppTheme_Black)
                            val args = Bundle().apply {
                                putParcelable(ARG_MEDIA_EXTRAS, mediaExtras)
                            }
                            navGraph.setStartDestination(R.id.conversationAudioEditSendFragment)
                            navController.setGraph(navGraph, args)
                        }
                    }
                }
            }
        }
    }

    private fun initActionBar(mediaExtras: MediaExtras, theme: Int) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = mediaExtras.title
        supportActionBar?.subtitle = mediaExtras.subtitle
        setTheme(theme)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        when (val fragment = supportFragmentManager.currentFragment(R.id.nav_host)) {
            is ConversationAudioSendEditFragment -> {
                fragment.onBackPress()
            }

            is ConversationDocumentSendFragment -> {
                fragment.onBackPressedFromFragment()
            }

            is ConversationMediaEditFragment -> {
                fragment.onBackPressedFromFragment()
            }
        }
        super.onBackPressed()
    }
}