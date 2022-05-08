package com.s16.smluncher.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.AppCompatTextView
import com.s16.smluncher.R
import com.s16.smluncher.helpers.BatteryStats
import com.s16.smluncher.views.BatteryMeterView

class BatteryInfoActivity : AppCompatActivity() {

    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battery_info)

        val binding = object {
            val batteryView : BatteryMeterView = findViewById(R.id.batteryView)
            val textPluggedState : AppCompatTextView = findViewById(R.id.textPluggedState)
            val textBatteryStatus : AppCompatTextView = findViewById(R.id.textBatteryStatus)
            val textHealthStatus : AppCompatTextView = findViewById(R.id.textHealthStatus)
            val textChemistry : AppCompatTextView = findViewById(R.id.textChemistry)
            val textLevel : AppCompatTextView = findViewById(R.id.textLevel)
            val textTemperature : AppCompatTextView = findViewById(R.id.textTemperature)
            val textVoltage : AppCompatTextView = findViewById(R.id.textVoltage)
            val textCharger : AppCompatTextView = findViewById(R.id.textCharger)
        }

        binding.batteryView.criticalChargeLevel = 15

        broadcastReceiver = object:BroadcastReceiver() {

            @SuppressLint("SetTextI18n")
            override fun onReceive(context: Context?, intent: Intent?) {
                val stats = BatteryStats(context!!, intent)

                binding.batteryView.chargeLevel = stats.level
                binding.batteryView.isCharging = stats.status == BatteryStats.BATTERY_STATUS_CHARGING

                binding.textPluggedState.text = stats.pluggedText
                binding.textBatteryStatus.text = stats.statusText
                binding.textHealthStatus.text = stats.healthText
                binding.textChemistry.text = stats.technology
                binding.textLevel.text = "${stats.level} %"
                binding.textTemperature.text = stats.getTemperatureText(true)
                binding.textVoltage.text = "${stats.voltage} V"
                binding.textCharger.text = if (stats.isInvalidCharger) {
                    "Charger is invalid"
                } else {
                    "Charger is valid"
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStop() {
        unregisterReceiver(broadcastReceiver)
        super.onStop()
    }
}