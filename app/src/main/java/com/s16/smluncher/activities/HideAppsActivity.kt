package com.s16.smluncher.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.s16.smluncher.MainApp
import com.s16.smluncher.R
import com.s16.smluncher.managers.AppInfo
import com.s16.smluncher.adapters.HideAppsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HideAppsActivity : AppCompatActivity() {

    private val recyclerView by lazy { findViewById<RecyclerView>(R.id.recyclerHideApps) }
    private lateinit var appsAdapter: HideAppsAdapter

    private val mainApp by lazy { application as MainApp }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hide_apps)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        appsAdapter = HideAppsAdapter(this)
        recyclerView.apply {
            adapter = appsAdapter
            layoutManager = LinearLayoutManager(this@HideAppsActivity, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(DividerItemDecoration(this@HideAppsActivity, LinearLayoutManager.VERTICAL))
        }

        appsAdapter.setOnCheckedChangeListener(object: HideAppsAdapter.OnCheckedChangeListener {

            override fun OnCheckedChange(view: View, isChecked: Boolean, item: AppInfo) {
                CoroutineScope(Dispatchers.IO).launch {
                    if (isChecked) {
                        mainApp.preferences.addHideApp(item.key)
                    } else {
                        mainApp.preferences.removeHideApp(item.key)
                    }
                }
            }

        })

        mainApp.apps.observe(this) {
            appsAdapter.submitList(it)
        }

        mainApp.preferences.hideApps.observe(this) {
            appsAdapter.setSelectedPackages(it.toList())
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}