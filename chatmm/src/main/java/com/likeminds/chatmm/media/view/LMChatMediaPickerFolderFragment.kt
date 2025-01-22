package com.likeminds.chatmm.media.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.navigation.fragment.findNavController
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.databinding.LmChatFragmentMediaPickerFolderBinding
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.view.LMChatMediaPickerActivity.Companion.ARG_MEDIA_PICKER_RESULT
import com.likeminds.chatmm.media.view.adapter.MediaPickerAdapter
import com.likeminds.chatmm.media.view.adapter.MediaPickerAdapterListener
import com.likeminds.chatmm.media.viewmodel.MediaViewModel
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.AndroidUtils
import com.likeminds.chatmm.utils.customview.BaseFragment
import com.likeminds.chatmm.utils.file.model.LocalAppData
import com.likeminds.chatmm.utils.recyclerview.GridSpacingItemDecoration

class LMChatMediaPickerFolderFragment :
    BaseFragment<LmChatFragmentMediaPickerFolderBinding, MediaViewModel>(),
    MediaPickerAdapterListener {

    private lateinit var mediaPickerAdapter: MediaPickerAdapter

    private lateinit var mediaPickerExtras: LMChatMediaPickerExtras
    private val appsList by lazy { ArrayList<LocalAppData>() }

    companion object {
        const val BUNDLE_MEDIA_PICKER_FOLDER = "bundle of media picker folder"
        const val REQUEST_KEY = "request key of media item"
        const val RESULT_KEY = "result of media item"
        const val TAG = "MediaPickerFolder"

        @JvmStatic
        fun getInstance(extras: LMChatMediaPickerExtras): LMChatMediaPickerFolderFragment {
            val fragment = LMChatMediaPickerFolderFragment()
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_MEDIA_PICKER_FOLDER, extras)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getViewModelClass(): Class<MediaViewModel> {
        return MediaViewModel::class.java
    }

    override fun getViewBinding(): LmChatFragmentMediaPickerFolderBinding {
        return LmChatFragmentMediaPickerFolderBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().mediaComponent()?.inject(this)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        mediaPickerExtras =
            LMChatMediaPickerFolderFragmentArgs.fromBundle(requireArguments()).mediaPickerExtras
        getExternalAppList()
    }

    override fun setUpViews() {
        super.setUpViews()
        setHasOptionsMenu(true)
        initializeUI()
        initializeListeners()
        viewModel.fetchAllFolders(requireContext(), mediaPickerExtras.mediaTypes)
            .observe(viewLifecycleOwner) {
                mediaPickerAdapter.replace(it)
            }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        appsList.forEachIndexed { index, localAppData ->
            menu.add(0, localAppData.appId, index, localAppData.appName)
            menu.getItem(index).icon = localAppData.appIcon
        }
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val localAppData = appsList.find {
            it.appId == item.itemId
        }

        return if (localAppData != null) {
            val extra = MediaPickerResult.Builder()
                .mediaPickerResultType(MEDIA_RESULT_BROWSE)
                .mediaTypes(mediaPickerExtras.mediaTypes)
                .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
                .browseClassName(
                    Pair(
                        localAppData.resolveInfo.activityInfo.applicationInfo.packageName,
                        localAppData.resolveInfo.activityInfo.name
                    )
                )
                .build()
            val intent = Intent().apply {
                putExtras(Bundle().apply {
                    putParcelable(
                        ARG_MEDIA_PICKER_RESULT, extra
                    )
                })
            }
            requireActivity().setResult(Activity.RESULT_OK, intent)
            requireActivity().finish()
            true
        } else {
            false
        }
    }


    private fun initializeUI() {
        binding.toolbar.title = ""

        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        initializeTitle()

        binding.toolbarColor = LMChatAppearance.getToolbarColor()
        binding.headerColor = LMChatAppearance.getHeaderColor()

        mediaPickerAdapter = MediaPickerAdapter(this)
        binding.rvFolder.apply {
            addItemDecoration(GridSpacingItemDecoration(2, 12))
            adapter = mediaPickerAdapter
        }
    }

    private fun initializeTitle() {
        binding.tvToolbarTitle.text =
            if (InternalMediaType.isBothImageAndVideo(mediaPickerExtras.mediaTypes)
                && mediaPickerExtras.senderName?.isNotEmpty() == true
            ) {
                String.format("Send to %s", mediaPickerExtras.senderName)
            } else {
                getString(R.string.lm_chat_gallery)
            }
    }

    private fun initializeListeners() {
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun getExternalAppList() {
        when {
            InternalMediaType.isBothImageAndVideo(mediaPickerExtras.mediaTypes) -> {
                appsList.addAll(AndroidUtils.getExternalMediaPickerApps(requireContext()))
            }

            InternalMediaType.isImage(mediaPickerExtras.mediaTypes) -> {
                appsList.addAll(AndroidUtils.getExternalImagePickerApps(requireContext()))
            }

            InternalMediaType.isVideo(mediaPickerExtras.mediaTypes) -> {
                appsList.addAll(AndroidUtils.getExternalVideoPickerApps(requireContext()))
            }
        }
    }

    override fun onFolderClicked(folderData: MediaFolderViewData) {
        val extras = MediaPickerItemExtras.Builder()
            .bucketId(folderData.bucketId)
            .folderTitle(folderData.title)
            .mediaTypes(mediaPickerExtras.mediaTypes)
            .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
            .build()

        findNavController().navigate(
            LMChatMediaPickerFolderFragmentDirections.actionFolderToItems(extras)
        )
    }
}