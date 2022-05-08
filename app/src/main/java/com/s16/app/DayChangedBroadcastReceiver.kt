package com.s16.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import java.util.*

abstract class DayChangedBroadcastReceiver : BroadcastReceiver() {

    private var date = Calendar.getInstance()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        val currentDate = Calendar.getInstance()

        if ((action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED) && !isSameDay(currentDate)) {
            date = currentDate
            onDayChanged()
        }
    }

    private fun isSameDay(currentDate: Calendar) : Boolean {
        return date[Calendar.YEAR] == currentDate[Calendar.YEAR] &&
                date[Calendar.MONTH] == currentDate[Calendar.MONTH]
                && date[Calendar.DAY_OF_MONTH] == currentDate[Calendar.DAY_OF_MONTH]
    }

    abstract fun onDayChanged()

    companion object {

        /**
         * Create the [IntentFilter] for the [DayChangedBroadcastReceiver].
         *
         * @return The [IntentFilter]
         */
        val intentFilter : IntentFilter
            get() = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
            }
    }
}