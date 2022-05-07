package com.s16.smluncher.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class ScreenOffAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }
}