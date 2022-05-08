package com.s16.smluncher.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max

// https://developer.android.com/reference/android/os/BatteryManager
// https://stackoverflow.com/questions/2439619/getting-the-battery-current-values-for-the-android-phone
// https://github.com/sufadi/BatteryInfo/blob/master/app/src/main/java/com/fadi/batterywaring/MainActivity.java

/**
 * Constructor with Intent as parameter.
 *
 *
 * {@param batteryIntent} Returned when registering a null receiver with a Intent filter ( Changes listened once )
 * or an intent from onReceive of broadcast receiver.
 */
@SuppressLint("ObsoleteSdkInt")
class BatteryStats(context: Context, batteryIntent: Intent?) {

    private var mBatteryIntent: Intent? = null
    private var mBatteryManager: BatteryManager? = null
    private var mDesignCapacity: Int = 0
    private var mMaxChargingMicroVolt: Int = 0
    private var mMaxChargingMicroAmp: Int = 0
    private var mCurrentAverage: Int = 0

    init {
        mBatteryIntent = batteryIntent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mBatteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            mCurrentAverage = mBatteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) ?: 0
        } else {
            mCurrentAverage = 0
        }
        mDesignCapacity = getBatteryDesignCapacity(context)
        mMaxChargingMicroVolt = mBatteryIntent?.getIntExtra(EXTRA_MAX_CHARGING_VOLTAGE, -1) ?: DEFAULT_CHARGING_VOLTAGE_MICRO_VOLT
        mMaxChargingMicroAmp = mBatteryIntent?.getIntExtra(EXTRA_MAX_CHARGING_CURRENT, -1) ?: -1
    }

    /**
     * Getter for batteryIntent.
     */
    val batteryIntent: Intent?
        get() = mBatteryIntent

    val isPresent: Boolean
        get() = mBatteryIntent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false) ?: false

    /**
     * Method to get the battery level.
     */
    val level: Int
        get() = mBatteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0

    /**
     * Method to get the battery level accurate. (Since the returned value is float).
     */
    val levelAccurate: Float
        get() = level / scale.toFloat()

    val scale: Int
        get() = mBatteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

    /**
     * Method to get the battery technology.
     */
    val technology: String?
        get() = mBatteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

    /**
     * Method to get the the status.
     */
    val status: Int
        get() = mBatteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

    /**
     * Method to get the battery status text.
     */
    val statusText: String
        get() = when (status) {
            BATTERY_STATUS_CHARGING -> "Charging"
            BATTERY_STATUS_FULL -> "Fully charged"
            BATTERY_STATUS_DISCHARGING -> "Discharging"
            BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            BATTERY_STATUS_UNKNOWN -> "Unknown"
            else -> "N/A"
        }

    /**
     * Method to get the the plugged.
     */
    val plugged: Int
        get() = mBatteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, BATTERY_PLUGGED_UNKNOWN) ?: BATTERY_PLUGGED_UNKNOWN

    /**
     * Method to get the battery plugged text.
     */
    val pluggedText: String
        get() = when (plugged) {
            BATTERY_PLUGGED_AC -> "Plugged by AC"
            BATTERY_PLUGGED_USB -> "Plugged by USB"
            BATTERY_PLUGGED_WIRELESS -> "Plugged by Wireless"
            else -> "Not plugged"
        }

    /**
     * Method to check whether device is charging.
     */
    val isCharging: Boolean
        get() {
            val plugState = mBatteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            return plugState == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugState == BatteryManager.BATTERY_PLUGGED_USB || plugState == BatteryManager.BATTERY_PLUGGED_WIRELESS
        }

    /**
     * Method to get the battery health.
     */
    val health: Int
        get() = mBatteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1

    /**
     * Method to get the battery health text.
     */
    val healthText: String
        get() = when (health) {
            BATTERY_HEALTH_COLD -> "Cold"
            BATTERY_HEALTH_DEAD -> "Dead"
            BATTERY_HEALTH_GOOD -> "Good"
            BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BATTERY_HEALTH_OVERHEAT -> "Overheated"
            BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failed"
            BATTERY_HEALTH_UNKNOWN -> "Unknown"
            else -> "N/A"
        }

    /**
     * Method to get the battery temperature.
     */
    val temperature: Int
        get() = mBatteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0

    /**
     * Method to get the battery temperature without text in Celsius or Fahrenheit.
     */
    fun getTemperature(fahrenheit: Boolean): Double {
        val celsius = (temperature / 10).toDouble()
        return if (fahrenheit) {
            celsius * 1.8 + 32
        } else {
            celsius
        }
    }

    /**
     * Method to get the battery temperature with text in Celsius or Fahrenheit.
     */
    fun getTemperatureText(fahrenheit: Boolean): String =
        if (fahrenheit) {
            "${getTemperature(true)}\u00b0 F"
        } else {
            "${getTemperature(false)}\u00b0 C"
        }

    /**
     * Method to get the battery voltage.
     */
    val voltage: Double
        get() = (mBatteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0

    /**
     * Method to get the charger is invalid.
     */
    val isInvalidCharger: Boolean
        get() = (mBatteryIntent?.getIntExtra(EXTRA_INVALID_CHARGER, 0) ?: 0) != 0

    val isFastCharging: Boolean
        get() = mBatteryIntent?.getBooleanExtra(EXTRA_FAST_CHARGE_STATUS, false) ?: false

    private fun getCurrentChargingCurrent(): Int {
        var result = 0
        var br: BufferedReader? = null
        try {
            var line: String
            br = BufferedReader(FileReader("/sys/class/power_supply/battery/BatteryAverageCurrent"))
            if (br.readLine().also { line = it } != null) {
                result = line.toInt()
            }
            br.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    val currentChargingCurrent: Double
        get() = getCurrentChargingCurrent() / 1000.0

    private fun getCurrentChargingVoltage(): Int {
        var result = 0
        var br: BufferedReader? = null
        try {
            var line: String
            br = BufferedReader(FileReader("/sys/class/power_supply/battery/batt_vol"))
            if (br.readLine().also { line = it } != null) {
                result = line.toInt()
            }
            br.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    val currentChargingVoltage: Double
        get() = getCurrentChargingVoltage() / 1000000.0

    /**
     * Method to get the battery max charging voltage.
     */
    val maxChargingVoltage: Double
        get() = (if (mMaxChargingMicroVolt < 1) {
            DEFAULT_CHARGING_VOLTAGE_MICRO_VOLT
        } else {
            mMaxChargingMicroVolt
        })  / 1000000.0

    /**
     * Method to get the battery max charging current.
     */
    val maxChargingCurrent: Double
        get() = (if (mMaxChargingMicroAmp > 0) {
            (mMaxChargingMicroAmp / 1000) * (mMaxChargingMicroVolt / 1000)
        } else -1)  / 1000000.0


    @SuppressLint("PrivateApi")
    private fun getBatteryDesignCapacity(context: Context): Int {
        val mPowerProfile: Any
        var batteryCapacity = 0.0
        val POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile"
        try {
            mPowerProfile = Class.forName(POWER_PROFILE_CLASS)
                .getConstructor(Context::class.java)
                .newInstance(context)
            batteryCapacity = Class
                .forName(POWER_PROFILE_CLASS)
                .getMethod("getBatteryCapacity")
                .invoke(mPowerProfile) as Double
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return batteryCapacity.toInt()
    }

    val designCapacity: Double
        get() = mDesignCapacity / 1000.0

    /**
     * Get the battery capacity at the moment (in %, from 0-100)
     *
     * @return Battery capacity (in %, from 0-100)
     */
    val capacity: Int
        get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mBatteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        } else {
            0
        }

    /**
     * Get the battery full capacity (charge counter) in mAh.
     *
     * @return Battery full capacity (in mAh)
     */
    val chargingCounter: Int
        get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mBatteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0
        } else {
            mBatteryIntent?.getIntExtra(EXTRA_CHARGE_COUNTER, 0) ?: 0
        }

    /**
     * Method to get the average battery current (milli amperes).
     */
    val currentAverage: Double
        get() = mCurrentAverage / 1000000.0

    /**
     * Get the Battery current at the moment (in mA)
     *
     * @return battery current now (in mA)
     */
    val currentNow: Double
        get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            (mBatteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0) / 1000000.0
        } else {
            0.0
        }

    /**
     * Get the battery energy counter capacity (in mWh)
     *
     * @return battery energy counter (in mWh)
     */
    val energyCounter: Double
        get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            (mBatteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) ?: 0) / 1000000.0
        } else {
            0.0
        }

    /**
     * Calculate Average Power
     * Average Power = (Average Voltage * Average Current) / 1e9
     *
     * @return Average power in integer
     */
    val averagePower: Double
        get() {
            if (voltage == 0.0) return 0.0
            if (mCurrentAverage == 0) return 0.0
            return (voltage * mCurrentAverage) / 1000000.0
        }

    /**
     * Calculates the battery's remaining energy capacity
     *
     * @return the battery remaining capacity, in Ah, as Integer
     */
    val remainingCapacity : Double
        get() {
            var counter = chargingCounter
            if (counter <= -1) {
                counter = abs(mDesignCapacity)
            }
            return if (capacity > 0 && counter > 0) {
                (counter * capacity) / 100000.0
            } else {
                val voltageNow = max(1.0, voltage)
                (energyCounter / voltageNow) / 1000.0
            }
        }

    companion object {
        /**
         * Values indicating the plugged state.
         */
        const val BATTERY_PLUGGED_UNKNOWN = 0
        const val BATTERY_PLUGGED_AC = 1
        const val BATTERY_PLUGGED_USB = 2
        const val BATTERY_PLUGGED_WIRELESS = 4

        /**
         * Values for indicating battery health.
         */
        const val BATTERY_HEALTH_COLD = 7
        const val BATTERY_HEALTH_DEAD = 4
        const val BATTERY_HEALTH_GOOD = 2
        const val BATTERY_HEALTH_OVERHEAT = 3
        const val BATTERY_HEALTH_OVER_VOLTAGE = 5
        const val BATTERY_HEALTH_UNKNOWN = 1
        const val BATTERY_HEALTH_UNSPECIFIED_FAILURE = 6

        const val BATTERY_STATUS_CHARGING = 2
        const val BATTERY_STATUS_DISCHARGING = 3
        const val BATTERY_STATUS_FULL = 5
        const val BATTERY_STATUS_NOT_CHARGING = 4
        const val BATTERY_STATUS_UNKNOWN = 1

        const val EXTRA_BATTERY_LOW = "battery_low"
        const val EXTRA_HEALTH = "health"
        const val EXTRA_ICON_SMALL = "icon-small"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_PLUGGED = "plugged"
        const val EXTRA_PRESENT = "present"
        const val EXTRA_SCALE = "scale"
        const val EXTRA_STATUS = "status"
        const val EXTRA_TECHNOLOGY = "technology"
        const val EXTRA_TEMPERATURE = "temperature"
        const val EXTRA_VOLTAGE = "voltage"

        const val EXTRA_SEQ = "seq" // int
        const val EXTRA_INVALID_CHARGER = "invalid_charger" // boolean
        const val EXTRA_MAX_CHARGING_VOLTAGE = "max_charging_voltage" // int (micro volt)
        const val EXTRA_MAX_CHARGING_CURRENT = "max_charging_current" // int (micro amperes)
        const val EXTRA_CHARGE_COUNTER = "charge_counter" // int (micro ampere per hours)
        const val EXTRA_FAST_CHARGE_STATUS = "fastcharge_status"

        const val DEFAULT_CHARGING_VOLTAGE_MICRO_VOLT = 5000000
    }
}