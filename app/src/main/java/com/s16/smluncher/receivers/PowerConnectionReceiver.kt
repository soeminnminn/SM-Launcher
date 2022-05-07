package com.s16.smluncher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action?.let { action ->
            if (action == Intent.ACTION_POWER_CONNECTED) {

            } else if (action == Intent.ACTION_POWER_DISCONNECTED) {

            }
        }
    }

}