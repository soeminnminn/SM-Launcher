package com.s16.smluncher.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.map

class PreferencesManager(private val dataStore: DataStore<Preferences>) {

    val hideApps = dataStore.data.map {
        it[HIDE_APPS] ?: emptySet()
    }.asLiveData()

    private fun getHideApp(): MutableSet<String> {
        val set = mutableSetOf<String>()
        hideApps.value?.let {
            set.addAll(it)
        }
        return set
    }

    suspend fun addHideApp(packageName: String) {
        val currentVal = getHideApp()
        dataStore.edit { prefs ->
            if (!currentVal.contains(packageName)) {
                currentVal.add(packageName)
                prefs[HIDE_APPS] = currentVal
            }
        }
    }

    suspend fun removeHideApp(packageName: String) {
        val currentVal = getHideApp()
        dataStore.edit { prefs ->
            if (currentVal.contains(packageName)) {
                currentVal.remove(packageName)
                prefs[HIDE_APPS] = currentVal
            }
        }
    }

    companion object {
        val HIDE_APPS = stringSetPreferencesKey("HIDE_APPS")
    }
}