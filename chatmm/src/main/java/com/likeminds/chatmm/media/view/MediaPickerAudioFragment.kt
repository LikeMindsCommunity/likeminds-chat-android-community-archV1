package com.likeminds.chatmm.media.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.SimpleItemAnimator
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.databinding.FragmentMediaPickerAudioBinding
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.util.LMMediaPlayer
import com.likeminds.chatmm.media.util.LMMediaPlayer.Companion.handler
import com.likeminds.chatmm.media.util.LMMediaPlayer.Companion.isDataSourceSet
import com.likeminds.chatmm.media.util.LMMediaPlayer.Companion.runnable
import com.likeminds.chatmm.media.util.MediaPlayerListener
import com.likeminds.chatmm.media.view.adapter.MediaPickerAdapter
import com.likeminds.chatmm.media.view.adapter.MediaPickerAdapterListener
import com.likeminds.chatmm.media.viewmodel.MediaViewModel
import com.likeminds.chatmm.search.util.LMChatCustomSearchBar
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.customview.BaseFragment
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

class MediaPickerAudioFragment :
    BaseFragment<FragmentMediaPickerAudioBinding, MediaViewModel>(),
    MediaPickerAdapterListener, MediaPlayerListener {
    private lateinit var mediaPickerAdapter: MediaPickerAdapter

    private var mediaPlayer: LMMediaPlayer? = null

    private val fragmentActivity by lazy {
        activity as AppCompatActivity?
    }

    companion object {
        const val TAG = "MediaPickerAudio"
        private const val BUNDLE_MEDIA_PICKER_AUDIO = "bundle of media picker audio"

        @JvmStatic
        fun getInstance(extras: LMChatMediaPickerExtras): MediaPickerAudioFragment {
            val fragment = MediaPickerAudioFragment()
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_MEDIA_PICKER_AUDIO, extras)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getViewModelClass(): Class<MediaViewModel> {
        return MediaViewModel::class.java
    }

    override fun getViewBinding(): FragmentMediaPickerAudioBinding {
        return FragmentMediaPickerAudioBinding.inflate(layoutInflater)
    }

    private lateinit var mediaPickerExtras: LMChatMediaPickerExtras
    private val selectedMedias by lazy { HashMap<String, MediaViewData>() }
    private var localItemPosition: Int = 0

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().mediaComponent()?.inject(this)
    }

    override fun setUpViews() {
        super.setUpViews()
        setTheme()
        initializeMediaPlayer()
        initializeUI()
        initializeListeners()
        setHasOptionsMenu(true)
    }

    override fun onProgressChanged(
        playedPercentage: Int,
    ) {
        val item = mediaPickerAdapter.items()?.get(localItemPosition) as MediaViewData

        mediaPickerAdapter.update(
            localItemPosition,
            item.toBuilder()
                .audioProgress(playedPercentage)
                .build()
        )
    }

    override fun observeData() {
        super.observeData()
        viewModel.fetchAllAudioFiles(requireContext()).observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                mediaPickerAdapter.replace(it)
            } else {
                ViewUtils.showShortToast(requireContext(), getString(R.string.lm_chat_no_audio_files))
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }

    override fun receiveExtras() {
        super.receiveExtras()
        mediaPickerExtras =
            LMChatMediaPickerDocumentFragmentArgs.fromBundle(requireArguments()).mediaPickerExtras
    }

    override fun onStop() {
        super.onStop()
        if (mediaPlayer?.isPlaying() == true) {
            mediaPlayer?.pause()
            val mediaPlayed = mediaPickerAdapter.items()?.get(localItemPosition) as MediaViewData
            handler?.removeCallbacks(runnable ?: Runnable { })
            updateItem(
                localItemPosition, mediaPlayed.toBuilder()
                    .audioProgress(
                        mediaPlayer?.playedPercentage()
                    )
                    .playOrPause(MEDIA_ACTION_PAUSE)
                    .build()
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.lm_chat_media_picker_audio_menu, menu)
        updateMenu(menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun updateMenu(menu: Menu) {
        val item = menu.findItem(R.id.menu_item_search)
        item?.icon?.setTint(LMChatAppearance.getToolbarColor())
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_search -> {
                showSearchToolbar()
            }
            else -> return false
        }
        return true
    }

    override fun onMediaItemClicked(mediaViewData: MediaViewData, itemPosition: Int) {
        if (isMultiSelectionAllowed()) {
            if (selectedMedias.containsKey(mediaViewData.uri.toString())) {
                selectedMedias.remove(mediaViewData.uri.toString())
            } else {
                selectedMedias[mediaViewData.uri.toString()] = mediaViewData
            }

            mediaPickerAdapter.notifyItemChanged(itemPosition)

            updateSelectedCount()
        } else {
            sendSelectedMedias(listOf(mediaViewData))
        }
    }

    override fun isMediaSelectionEnabled(): Boolean {
        return selectedMedias.isNotEmpty()
    }

    override fun isMediaSelected(key: String): Boolean {
        return selectedMedias.containsKey(key)
    }

    override fun isMultiSelectionAllowed(): Boolean {
        return mediaPickerExtras.allowMultipleSelect
    }

    override fun onAudioComplete() {
        super.onAudioComplete()
        handler?.removeCallbacks(runnable ?: Runnable { })
        val playedAudio = mediaPickerAdapter.items()?.get(localItemPosition) as MediaViewData
        updateItem(
            localItemPosition, playedAudio.toBuilder()
                .audioProgress(0)
                .playOrPause(MEDIA_ACTION_NONE)
                .build()
        )
    }

    override fun onAudioActionClicked(mediaViewData: MediaViewData, itemPosition: Int) {
        if (localItemPosition != itemPosition) {
            val previousPlayed =
                mediaPickerAdapter.items()?.get(localItemPosition) as MediaViewData

            if (previousPlayed.playOrPause == MEDIA_ACTION_PLAY) {
                handler?.removeCallbacks(runnable ?: Runnable { })
            }
            updateItem(
                localItemPosition, previousPlayed.toBuilder()
                    .audioProgress(0)
                    .playOrPause(MEDIA_ACTION_NONE)
                    .build()
            )
            isDataSourceSet = false
        }
        try {
            when (mediaViewData.playOrPause) {
                MEDIA_ACTION_PLAY -> {
                    mediaPlayer?.pause()
                    updateItem(
                        itemPosition, mediaViewData.toBuilder()
                            .audioProgress(
                                mediaPlayer?.playedPercentage()
                            )
                            .playOrPause(MEDIA_ACTION_PAUSE)
                            .build()
                    )
                }
                MEDIA_ACTION_PAUSE -> {
                    mediaPlayer?.start()
                    mediaPlayer?.setAudioProgress()
                    updateItem(
                        itemPosition, mediaViewData.toBuilder()
                            .playOrPause(MEDIA_ACTION_PLAY)
                            .build()
                    )
                }
                MEDIA_ACTION_NONE -> {
                    localItemPosition = itemPosition
                    mediaPlayer?.setMediaDataSource(mediaViewData.uri)
                    updateItem(
                        itemPosition, mediaViewData.toBuilder()
                            .playOrPause(MEDIA_ACTION_PLAY)
                            .build()
                    )
                }
            }
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            Log.e(TAG, e.stackTrace.toString())
        }
    }

    private fun setTheme() {
        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()
            toolbarColor = LMChatAppearance.getToolbarColor()
        }
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = LMMediaPlayer(requireContext(), this)
    }

    override fun doCleanup() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.doCleanup()
    }

    private fun updateItem(position: Int, mediaItem: MediaViewData) {
        mediaPickerAdapter.update(
            position,
            mediaItem
        )
    }

    private fun initializeUI() {
        binding.toolbar.title = ""
        binding.tvToolbarTitle.text = ""

        fragmentActivity?.setSupportActionBar(binding.toolbar)

        initializeTitle()

        mediaPickerAdapter = MediaPickerAdapter(this)
        binding.rvAudio.apply {
            adapter = mediaPickerAdapter
        }
        (binding.rvAudio.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        handler = Handler(Looper.getMainLooper())
        updateSelectedCount()

        initializeSearchView()
    }

    private fun initializeTitle() {
        binding.tvToolbarTitle.text =
            if (InternalMediaType.isAudio(mediaPickerExtras.mediaTypes)
                && mediaPickerExtras.senderName?.isNotEmpty() == true
            ) {
                String.format("Send to %s", mediaPickerExtras.senderName)
            } else {
                getString(R.string.lm_chat_music)
            }
    }

    private fun updateSelectedCount() {
        if (isMediaSelectionEnabled()) {
            binding.tvToolbarSubtitle.text =
                String.format("%s selected", selectedMedias.size)
        } else {
            binding.tvToolbarSubtitle.text = getString(R.string.lm_chat_tap_to_select)
        }
        binding.fabSend.isVisible = isMediaSelectionEnabled()
    }

    private fun initializeSearchView() {
        binding.searchBar.apply {
            initialize(lifecycleScope)
            setSearchViewListener(
                object : LMChatCustomSearchBar.SearchViewListener {
                    override fun onSearchViewClosed() {
                        binding.searchBar.visibility = View.GONE
                        viewModel.clearAudioFilter()
                    }

                    override fun crossClicked() {
                        viewModel.clearAudioFilter()
                    }

                    override fun keywordEntered(keyword: String) {
                        viewModel.filterAudioByKeyword(keyword)
                    }

                    override fun emptyKeywordEntered() {
                        viewModel.clearAudioFilter()
                    }
                }
            )
            observeSearchView(false)
        }
    }

    private fun initializeListeners() {
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.fabSend.setOnClickListener {
            sendSelectedMedias(selectedMedias.values.toList())
        }
    }

    private fun showSearchToolbar() {
        binding.searchBar.visibility = View.VISIBLE
        binding.searchBar.post {
            binding.searchBar.openSearch()
        }
    }

    private fun sendSelectedMedias(medias: List<MediaViewData>) {
        val extras = MediaPickerResult.Builder()
            .mediaPickerResultType(MEDIA_RESULT_PICKED)
            .mediaTypes(mediaPickerExtras.mediaTypes)
            .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
            .medias(medias)
            .build()
        val intent = Intent().apply {
            putExtras(Bundle().apply {
                putParcelable(LMChatMediaPickerActivity.ARG_MEDIA_PICKER_RESULT, extras)
            })
        }
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    private fun clearSelectedMedias() {
        selectedMedias.clear()
        mediaPickerAdapter.notifyDataSetChanged()
        updateSelectedCount()
    }

    fun onBackPress(): Boolean {
        when {
            binding.searchBar.isOpen -> binding.searchBar.closeSearch()
            isMediaSelectionEnabled() -> clearSelectedMedias()
            mediaPlayer?.isPlaying() == true -> {
                mediaPlayer?.stop()
                val previousPlayed =
                    mediaPickerAdapter.items()?.get(localItemPosition) as MediaViewData
                updateItem(
                    localItemPosition, previousPlayed.toBuilder()
                        .audioProgress(0)
                        .playOrPause(MEDIA_ACTION_NONE)
                        .build()
                )
            }
            else -> return true
        }
        return false
    }

}