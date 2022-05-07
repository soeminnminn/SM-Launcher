package com.s16.smluncher.services

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.s16.smluncher.activities.BatteryInfoActivity

@RequiresApi(Build.VERSION_CODES.N)
class BatteryInfoTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()

        val t = qsTile
        t.state = Tile.STATE_INACTIVE
        t.updateTile()
    }

    override fun onClick() {
        val i = Intent(applicationContext, BatteryInfoActivity::class.java)
        i.addFlags(FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(i)
        super.onClick()
    }
}