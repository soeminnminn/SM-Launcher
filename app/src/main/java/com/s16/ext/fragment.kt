package com.s16.ext

import androidx.fragment.app.Fragment

inline fun <T> Fragment.runOnUiThread(crossinline action: () -> T) {
    requireActivity().runOnUiThread { action() }
}