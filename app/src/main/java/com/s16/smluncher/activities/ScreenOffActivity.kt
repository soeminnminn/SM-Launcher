package com.s16.smluncher.activities

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.s16.smluncher.R
import com.s16.smluncher.receivers.ScreenOffAdminReceiver

class ScreenOffActivity : AppCompatActivity() {

    private val policyManager by lazy {
        applicationContext.getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_off)

        val adminReceiver = ComponentName(applicationContext, ScreenOffAdminReceiver::class.java)
        if (policyManager.isAdminActive(adminReceiver)) {
            policyManager.lockNow()
        } else {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminReceiver)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.device_admin_activation_message)
                )
            }

            startActivity(intent)
        }
        waitAndExit()
    }

    private fun waitAndExit() {
        val t = object : Thread() {
            override fun run() {
                try {
                    sleep(500)
                } catch (e: InterruptedException) {
                    /* ignore this */
                }
                this@ScreenOffActivity.finish()
            }
        }
        t.start()
    }
}