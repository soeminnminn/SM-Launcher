package com.s16.smluncher.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.s16.ext.setLightStatusBar
import com.s16.ext.startActivity
import com.s16.smluncher.helpers.AboutDialog
import com.s16.smluncher.R
import com.s16.smluncher.views.AppDrawerLayout
import com.s16.smluncher.views.HomeView

class MainActivity : AppCompatActivity() {

    private val homeView by lazy { findViewById<HomeView>(R.id.homeView)!! }
    private val drawerView by lazy { findViewById<AppDrawerLayout>(R.id.drawerLayout)!! }
    private lateinit var drawerBehavior: BottomSheetBehavior<ViewGroup>

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
            }
        })

        System.gc()
    }

    override fun onBackPressed() {
        if (drawerBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
            drawerBehavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            drawerBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_about) {
            AboutDialog.showDialog(this)
            return true
        }
        return super.onOptionsItemSelected(item)
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