package com.s16.view

import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView

class SearchEditTextHelper(private val editText: EditText) {

    private val mTextSearchTextWatcher = object : TextWatcher {

        override fun onTextChanged(
            s: CharSequence, start: Int, before: Int,
            count: Int
        ) {
            onQueryTextChanged(s, count)
        }

        override fun beforeTextChanged(
            s: CharSequence, start: Int, count: Int,
            after: Int
        ) {
        }

        override fun afterTextChanged(s: Editable) {
        }

    }

    private val mTextSearchOnKeyListener = View.OnKeyListener { _, keyCode, event ->
        if (event.action == KeyEvent.ACTION_UP) {
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> {
                    onQuerySubmit()
                    return@OnKeyListener true
                }
            }
        }

        false
    }

    private val mTextSearchOnEditorActionListener =
        TextView.OnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_GO,
                EditorInfo.IME_ACTION_NEXT,
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_SEND,
                EditorInfo.IME_ACTION_UNSPECIFIED -> {
                    onQuerySubmit()
                    true
                }
                else -> false
            }
        }

    private var mSearchText: CharSequence? = null
    private var mSearchListener: OnSearchListener? = null
    var isSearching: Boolean = false

    val searchText: CharSequence?
        get() {
            mSearchText = editText.text
            return mSearchText
        }

    interface OnSearchListener {
        fun onSearchTextChanged(searchText: CharSequence?)
        fun onSearchSubmit(searchText: CharSequence?)
    }

    init {
        editText.addTextChangedListener(mTextSearchTextWatcher)
        editText.setOnKeyListener(mTextSearchOnKeyListener)
        editText.setOnEditorActionListener(mTextSearchOnEditorActionListener)
        editText.imeOptions = EditorInfo.IME_ACTION_SEARCH
        editText.isFocusableInTouchMode = true
        editText.isSaveEnabled = true
    }

    private fun onQueryTextChanged(s: CharSequence, count: Int) {
        mSearchText = s
        if (!isSearching && mSearchListener != null) {
            mSearchListener!!.onSearchTextChanged(mSearchText)
        }
    }

    private fun onQuerySubmit() {
        if (!isSearching && mSearchListener != null) {
            mSearchText = editText.text
            mSearchListener!!.onSearchSubmit(mSearchText)
        }
    }

    fun setOnSearchListener(listener: OnSearchListener) {
        mSearchListener = listener
    }
}