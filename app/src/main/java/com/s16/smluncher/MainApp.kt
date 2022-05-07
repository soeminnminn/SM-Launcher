package com.s16.smluncher

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import com.s16.smluncher.managers.AppManager
import com.s16.smluncher.managers.LiveApps
import com.s16.smluncher.managers.PreferencesManager

class MainApp : Application() {

    private val mDataStore by preferencesDataStore(name = "settings")
    private var mAppsLiveData : LiveApps? = null

    val preferences by lazy { PreferencesManager(mDataStore) }

    val apps by lazy {
        if (mAppsLiveData == null) {
            mAppsLiveData = LiveApps(this@MainApp)
            mAppsLiveData!!
        } else {
            mAppsLiveData!!
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}