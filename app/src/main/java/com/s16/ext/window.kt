package com.s16.ext

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController

@Suppress("DEPRECATION")
fun Window.setLightStatusBar(light: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val appearance = if (light) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0
        insetsController?.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        decorView.systemUiVisibility = if (light) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
    }
}