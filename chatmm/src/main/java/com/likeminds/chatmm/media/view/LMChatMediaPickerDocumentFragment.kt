package com.likeminds.chatmm.media.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.databinding.LmChatFragmentMediaPickerDocumentBinding
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.view.adapter.MediaPickerAdapter
import com.likeminds.chatmm.media.view.adapter.MediaPickerAdapterListener
import com.likeminds.chatmm.media.viewmodel.MediaViewModel
import com.likeminds.chatmm.search.util.LMChatCustomSearchBar
import com.likeminds.chatmm.theme.model.LMTheme
import com.likeminds.chatmm.utils.customview.BaseFragment
import kotlin.collections.set

class LMChatMediaPickerDocumentFragment :
    BaseFragment<LmChatFragmentMediaPickerDocumentBinding, MediaViewModel>(),
    MediaPickerAdapterListener {

    private lateinit var mediaPickerAdapter: MediaPickerAdapter

    private val fragmentActivity by lazy { activity as AppCompatActivity? }

    private val selectedMedias by lazy { HashMap<String, MediaViewData>() }
    private lateinit var mediaPickerExtras: LMChatMediaPickerExtras

    private var currentSort = SORT_BY_NAME

    companion object {
        const val TAG = "MediaPickerDocument"
        private const val BUNDLE_MEDIA_PICKER_DOC = "bundle of media picker doc"

        @JvmStatic
        fun getInstance(extras: LMChatMediaPickerExtras): LMChatMediaPickerDocumentFragment {
            val fragment = LMChatMediaPickerDocumentFragment()
            val bundle = Bundle()
            bundle.putParcelable(BUNDLE_MEDIA_PICKER_DOC, extras)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getViewModelClass(): Class<MediaViewModel> {
        return MediaViewModel::class.java
    }

    override fun getViewBinding(): LmChatFragmentMediaPickerDocumentBinding {
        return LmChatFragmentMediaPickerDocumentBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().mediaComponent()?.inject(this)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        mediaPickerExtras =
            LMChatMediaPickerDocumentFragmentArgs.fromBundle(requireArguments()).mediaPickerExtras
    }

    override fun setUpViews() {
        super.setUpViews()
        setHasOptionsMenu(true)
        setTheme()
        initializeUI()
        initializeListeners()
        viewModel.fetchAllDocuments(requireContext()).observe(viewLifecycleOwner) {
            mediaPickerAdapter.replace(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.lm_chat_media_picker_document_menu, menu)
        updateMenu(menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_search -> {
                showSearchToolbar()
            }
            R.id.menu_item_sort -> {
                val menuItemView = requireActivity().findViewById<View>(item.itemId)
                showSortingPopupMenu(menuItemView)
            }
            else -> return false
        }
        return true
    }

    private fun updateMenu(menu: Menu) {
        //update search icon
        val item = menu.findItem(R.id.menu_item_search)
        item?.icon?.setTint(LMTheme.getToolbarColor())

        //update sort icon
        val item2 = menu.findItem(R.id.menu_item_sort)
        item2?.icon?.setTint(LMTheme.getToolbarColor())
    }

    private fun setTheme() {
        binding.apply {
            buttonColor = LMTheme.getButtonsColor()
            toolbarColor = LMTheme.getToolbarColor()
        }
    }

    private fun initializeUI() {
        binding.toolbar.title = ""
        fragmentActivity?.setSupportActionBar(binding.toolbar)

        mediaPickerAdapter = MediaPickerAdapter(this)
        binding.rvDocuments.apply {
            adapter = mediaPickerAdapter
        }

        updateSelectedCount()

        initializeSearchView()
    }

    private fun initializeListeners() {
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.fabSend.setOnClickListener {
            sendSelectedMedias(selectedMedias.values.toList())
        }
    }

    private fun sendSelectedMedias(medias: List<MediaViewData>) {
        val extra = MediaPickerResult.Builder()
            .mediaPickerResultType(MEDIA_RESULT_PICKED)
            .mediaTypes(mediaPickerExtras.mediaTypes)
            .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
            .medias(medias)
            .build()

        val intent = Intent().apply {
            putExtras(Bundle().apply {
                putParcelable(
                    LMChatMediaPickerActivity.ARG_MEDIA_PICKER_RESULT, extra
                )
            })
        }
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    private fun updateSelectedCount() {
        if (isMediaSelectionEnabled()) {
            binding.tvSelectedCount.text =
                String.format("%s selected", selectedMedias.size)
        } else {
            binding.tvSelectedCount.text = getString(R.string.lm_chat_tap_to_select)
        }
        binding.fabSend.isVisible = isMediaSelectionEnabled()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearSelectedMedias() {
        selectedMedias.clear()
        mediaPickerAdapter.notifyDataSetChanged()
        updateSelectedCount()
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

    override fun browseDocumentClicked() {
        val extra = MediaPickerResult.Builder()
            .mediaPickerResultType(MEDIA_RESULT_BROWSE)
            .mediaTypes(mediaPickerExtras.mediaTypes)
            .allowMultipleSelect(mediaPickerExtras.allowMultipleSelect)
            .build()
        val intent = Intent().apply {
            putExtras(Bundle().apply {
                putParcelable(
                    LMChatMediaPickerActivity.ARG_MEDIA_PICKER_RESULT, extra
                )
            })
        }
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    override fun isMultiSelectionAllowed(): Boolean {
        return mediaPickerExtras.allowMultipleSelect
    }

    private fun showSortingPopupMenu(view: View) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.lm_chat_document_sort_menu, popup.menu)
        when (currentSort) {
            SORT_BY_NAME ->
                popup.menu.findItem(R.id.menu_item_sort_name).isChecked = true

            SORT_BY_DATE ->
                popup.menu.findItem(R.id.menu_item_sort_date).isChecked = true
        }
        popup.setOnMenuItemClickListener { item ->
            item.isChecked = true
            when (item.itemId) {
                R.id.menu_item_sort_name -> {
                    if (currentSort != SORT_BY_NAME) {
                        currentSort = SORT_BY_NAME
                        viewModel.sortDocumentsByName()
                    }
                }

                R.id.menu_item_sort_date -> {
                    if (currentSort != SORT_BY_DATE) {
                        currentSort = SORT_BY_DATE
                        viewModel.sortDocumentsByDate()
                    }
                }
            }
            true
        }
        popup.show()
    }

    private fun initializeSearchView() {
        binding.searchBar.apply {
            initialize(lifecycleScope)
            setSearchViewListener(
                object : LMChatCustomSearchBar.SearchViewListener {
                    override fun onSearchViewClosed() {
                        binding.searchBar.visibility = View.GONE
                        viewModel.clearDocumentFilter()
                    }

                    override fun crossClicked() {
                        viewModel.clearDocumentFilter()
                    }

                    override fun keywordEntered(keyword: String) {
                        viewModel.filterDocumentsByKeyword(keyword)
                    }

                    override fun emptyKeywordEntered() {
                        viewModel.clearDocumentFilter()
                    }
                }
            )
            observeSearchView(false)
        }
    }

    private fun showSearchToolbar() {
        binding.searchBar.visibility = View.VISIBLE
        binding.searchBar.post {
            binding.searchBar.openSearch()
        }
    }

    fun onBackPressedFromFragment(): Boolean {
        when {
            binding.searchBar.isOpen -> binding.searchBar.closeSearch()
            isMediaSelectionEnabled() -> clearSelectedMedias()
            else -> return true
        }
        return false
    }
}