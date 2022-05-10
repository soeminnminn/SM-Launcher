package com.s16.smluncher.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.s16.app.DayChangedBroadcastReceiver
import com.s16.ext.setLightStatusBar
import com.s16.ext.startActivity
import com.s16.smluncher.R
import com.s16.smluncher.helpers.AboutDialog
import com.s16.smluncher.helpers.BatteryStats
import com.s16.smluncher.views.AppDrawerLayout
import com.s16.smluncher.views.BatteryMeterView
import com.s16.smluncher.views.HomeView
import com.s16.smluncher.views.MonthView

class MainActivity : AppCompatActivity() {

    private val mainView by lazy { findViewById<DrawerLayout>(R.id.mainLayout)!! }
    private val homeView by lazy { findViewById<HomeView>(R.id.homeView)!! }
    private val drawerView by lazy { findViewById<AppDrawerLayout>(R.id.drawerLayout)!! }
    private val batteryLayout by lazy { findViewById<ViewGroup>(R.id.batteryLayout)!! }
    private val calendarView by lazy { findViewById<MonthView>(R.id.calendarView)!! }

    private lateinit var drawerBehavior: BottomSheetBehavior<ViewGroup>
    private lateinit var powerReceiver: BroadcastReceiver
    private lateinit var dayChangedReceiver: DayChangedBroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        homeView.isFocusable = true
        homeView.isFocusableInTouchMode = true

        drawerBehavior = BottomSheetBehavior.from(drawerView)
        drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        drawerBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    window.setLightStatusBar(true)
                } else {
                    window.setLightStatusBar(false)
                    homeView.requestFocus()
                }
            }
        })

        homeView.setOnGestureListener(object: HomeView.OnGestureListener {
            override fun onDoubleTap(event: MotionEvent?) {
                this@MainActivity.startActivity<ScreenOffActivity>()
            }

            override fun onLongPress(event: MotionEvent?) {
                this@MainActivity.lunchWallpaperPicker()
            }

            override fun onTopOverScroll(event: MotionEvent?) {
                if (drawerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED ||
                    drawerBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    this@MainActivity.pullStatusBar()
                }
            }

            override fun onBottomOverScroll(event: MotionEvent?) {
                if (drawerBehavior.state == BottomSheetBehavior.STATE_COLLAPSED ||
                    drawerBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                    drawerBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        })

        setupNavigation()
        System.gc()
    }

    private fun setupNavigation() {
        val batteryView : BatteryMeterView = findViewById(R.id.batteryView)
        val textBatteryLevel: AppCompatTextView = findViewById(R.id.textBatteryLevel)
        val textBatteryStatus: AppCompatTextView = findViewById(R.id.textBatteryStatus)

        batteryView.criticalChargeLevel = 15

        powerReceiver = object:BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                val stats = BatteryStats(context!!, intent)

                batteryView.chargeLevel = stats.level
                batteryView.isCharging = stats.status == BatteryStats.BATTERY_STATUS_CHARGING

                textBatteryLevel.text = "${stats.level} %"
                textBatteryStatus.text = stats.statusText
            }
        }

        batteryLayout.setOnClickListener {
            onBackPressed()
            startActivity<BatteryInfoActivity>()
        }

        dayChangedReceiver = object : DayChangedBroadcastReceiver() {

            override fun onDayChanged() {
                calendarView.today()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::dayChangedReceiver.isInitialized) {
            registerReceiver(dayChangedReceiver, DayChangedBroadcastReceiver.intentFilter)
        }
        if (this::powerReceiver.isInitialized) {
            registerReceiver(powerReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
    }

    override fun onStop() {
        if (this::dayChangedReceiver.isInitialized) {
            unregisterReceiver(dayChangedReceiver)
        }
        if (this::powerReceiver.isInitialized) {
            unregisterReceiver(powerReceiver)
        }
        super.onStop()
    }

    override fun onBackPressed() {
        if (mainView.isDrawerOpen(GravityCompat.START)) {
            mainView.closeDrawer(GravityCompat.START)

        } else if (drawerBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
            drawerBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when(item.itemId) {
        R.id.action_hide_apps -> {
            onBackPressed()
            startActivity<HideAppsActivity>()
            true
        }
        R.id.action_manage_apps -> {
            onBackPressed()
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            startActivity(intent)
            true
        }
        R.id.action_about -> {
            AboutDialog.showDialog(this)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun pullStatusBar() {
        try {
            @SuppressLint("WrongConstant")
            val sbs = getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel")(sbs)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun lunchWallpaperPicker() {
        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
        startActivity(Intent.createChooser(intent, "Select Wallpaper"))
    }
}