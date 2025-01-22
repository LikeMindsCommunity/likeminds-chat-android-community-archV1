package com.likeminds.chatmm.search.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.Configuration
import android.text.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleCoroutineScope
import com.likeminds.chatmm.databinding.LmChatLayoutSearchBarBinding
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.AnimationUtils
import com.likeminds.chatmm.utils.ViewUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class LMChatCustomSearchBar @JvmOverloads constructor(
    mContext: Context,
    attributeSet: AttributeSet? = null
) : ConstraintLayout(mContext, attributeSet) {

    private val isHardKeyboardAvailable: Boolean
        get() = this.resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS

    /**
     * The previous query text.
     */
    private var mOldQuery: CharSequence = ""

    /**
     * The current query text.
     */
    private var mCurrentQuery = ""

    /**
     * Listener for when the search view opens and closes.
     */
    private var mSearchViewListener: SearchViewListener? = null

    /**
     * Determines if the search view is opened or closed.
     * @return True if the search view is open, false if it is closed.
     */
    /**
     * Whether or not the search view is open right now.
     */
    var isOpen = false
        private set

    private val binding =
        LmChatLayoutSearchBarBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var lifecycleScope: LifecycleCoroutineScope

    private fun displayClearButton(display: Boolean) {
        binding.ivClose.visibility = if (display) View.VISIBLE else View.GONE
    }

    companion object {
        private const val TAG = "CustomSearchBar"
    }

    init {
        binding.apply {
            ivClose.setOnClickListener {
                etSearch.setText("")
                mSearchViewListener?.crossClicked()
            }
            ivBack.setOnClickListener { closeSearch() }
            initSearchView()
        }
    }

    fun initialize(lifecycleScope: LifecycleCoroutineScope) {
        this.lifecycleScope = lifecycleScope
    }

    private fun initSearchView() {
        binding.etSearch.apply {
            setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onSubmitQuery()
                    return@OnEditorActionListener true
                }
                false
            })
            onFocusChangeListener = OnFocusChangeListener { view, hasFocus ->
                // If we gain focus, show keyboard and show suggestions.
                if (hasFocus) {
                    showKeyboard(view)
                }
            }
        }
    }

    /**
     * Adds TextWatcher to edit text with Flow operators
     * **/
    @ExperimentalCoroutinesApi
    @CheckResult
    fun EditText.textChanges(): Flow<CharSequence?> {
        return callbackFlow<CharSequence?> {
            val listener = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = Unit
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (isOpen) {
                        onTextChanged(s.toString(), this@callbackFlow)
                    }
                }
            }
            addTextChangedListener(listener)
            awaitClose { removeTextChangedListener(listener) }
        }.onStart { emit(text) }
    }

    /**
     * Filters and updates the buttons when text is changed.
     * @param newText The new text.
     */
    private fun onTextChanged(newText: CharSequence, producerScope: ProducerScope<CharSequence?>) {
        // Get current query
        mCurrentQuery = binding.etSearch.text.toString()

        // If the text is not empty, show the empty button and hide the voice button
        if (!TextUtils.isEmpty(mCurrentQuery)) {
            displayClearButton(true)
        } else {
            displayClearButton(false)
        }

        // If we have a query listener and the text has changed, call it.
        if ((!TextUtils.isEmpty(mCurrentQuery) || !TextUtils.isEmpty(mOldQuery))
        ) {
            producerScope.trySend(newText.toString())
        }

        mOldQuery = mCurrentQuery
    }

    /**
     * Closes the search view if necessary.
     */
    fun closeSearch() {
        binding.apply {
            if (!isOpen) {
                return
            }
            isOpen = false
            // Clear text, values, and focus.
            etSearch.setText("")
            clearFocusInHere()
            val listenerAdapter: AnimatorListenerAdapter = object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    // After the animation is done. Hide the root view.
                    searchToolbar.visibility = View.GONE
                }
            }
            AnimationUtils.circleHideView(searchToolbar, listenerAdapter)
            mSearchViewListener?.onSearchViewClosed()
        }
    }

    /**
     * Called when a query is submitted. This will close the search view.
     */
    private fun onSubmitQuery() {
        // Get the query.
        val query: CharSequence? = binding.etSearch.text
        // If the query is not null and it has some text, submit it.
        if (query != null && TextUtils.getTrimmedLength(query) > 0) {
            clearFocusInHere()
        }
    }

    fun openSearch() {
        // If search is already open, just return.
        if (isOpen) {
            return
        }
        binding.apply {
            // Get focus
            etSearch.setText("")
            etSearch.requestFocus()
            AnimationUtils.circleRevealView(searchToolbar)
            setBackgroundColor(LMChatAppearance.getHeaderColor())
            elevation = 20F
            isOpen = true
            mSearchViewListener?.onSearchViewOpened()
        }
    }

    private fun showKeyboard(view: View?) {
        view?.requestFocus()
        if (isHardKeyboardAvailable.not()) {
            val inputMethodManager =
                view?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            inputMethodManager?.showSoftInput(view, 0)
        }
    }

    fun setSearchViewListener(mSearchViewListener: SearchViewListener?) {
        this.mSearchViewListener = mSearchViewListener
    }

    private fun clearFocusInHere() {
        ViewUtils.hideKeyboard(this)
        binding.etSearch.clearFocus()
    }

    fun hasKeyword(): Boolean {
        return mCurrentQuery.isNotEmpty()
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun observeSearchView(debounce: Boolean = true) {
        binding.etSearch.apply {
            if (debounce) {
                textChanges()
                    .debounce(500)
                    .distinctUntilChanged()
                    .onEach { keyword ->
                        if (!keyword.isNullOrEmpty()) {
                            mSearchViewListener?.keywordEntered(keyword.toString())
                        } else {
                            mSearchViewListener?.emptyKeywordEntered()
                        }
                    }
                    .launchIn(lifecycleScope)
            } else {
                textChanges()
                    .distinctUntilChanged()
                    .onEach { keyword ->
                        if (!keyword.isNullOrEmpty()) {
                            mSearchViewListener?.keywordEntered(keyword.toString())
                        } else {
                            mSearchViewListener?.emptyKeywordEntered()
                        }
                    }
                    .launchIn(lifecycleScope)
            }
        }
    }

    /**
     * Interface that handles the opening and closing of the SearchView.
     */
    interface SearchViewListener {
        fun onSearchViewOpened() {
            //triggered when a user clicks on search icon and search view is opened
        }

        fun onSearchViewClosed()

        fun crossClicked()

        fun keywordEntered(keyword: String) {
            //triggered when a user enters a text to search
        }

        fun emptyKeywordEntered() {
            //triggered when a back-presses till last of the text
        }
    }
}