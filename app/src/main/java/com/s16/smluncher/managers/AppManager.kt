package com.s16.smluncher.managers

import android.content.*
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.pm.ResolveInfo
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.ArrayMap
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class AppInfo {
    val icon: Drawable?
    val label: String
    val badgedLabel: String
    val packageName: String
    val name: String
    val userHandle: UserHandle
    val shortcutInfo: List<ShortcutInfo>

    private val shortcutIconCache = ArrayMap<String, Drawable>(10)
    private var hashKey: String? = null

    val key: String
        get() {
            if (hashKey == null) {
                val source = "$packageName@$name"
                try {
                    // Create MD5 Hash
                    val digest: MessageDigest = MessageDigest.getInstance("MD5")
                    digest.update(source.toByteArray())
                    val messageDigest: ByteArray = digest.digest()
                    hashKey = messageDigest.joinToString("") { b -> "%02x".format(b) }
                } catch (e: NoSuchAlgorithmException) {
                    e.printStackTrace()
                }
            }
            return hashKey ?: name
        }

    internal constructor(context: Context, info: ResolveInfo) {
        val pm = context.packageManager
        icon = info.loadIcon(pm)
        label = info.loadLabel(pm).let {
            if (it.isBlank()) info.activityInfo.packageName
            else it.toString()
        }
        name = info.activityInfo.name
        packageName = info.activityInfo.packageName
        userHandle = Process.myUserHandle()
        shortcutInfo = listOf()
        badgedLabel = label
    }

    internal constructor(context: Context, info: LauncherActivityInfo, user: UserHandle, shortcuts: List<ShortcutInfo?>?) {
        val density = context.resources.configuration.densityDpi
        icon = info.getIcon(density)
        label = info.label.let {
            if (it.isBlank()) info.applicationInfo.packageName
            else it.toString()
        }
        name = info.name
        packageName = info.applicationInfo.packageName
        userHandle = user
        shortcutInfo = shortcuts?.filterNotNull() ?: listOf()
        badgedLabel = context.packageManager.getUserBadgedLabel(label, user).toString()
    }

    override fun toString(): String = label

    private fun getLauncherApps(context: Context)
        = context.applicationContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    fun loadIcon(context: Context): Drawable? {
        val launcherApps = getLauncherApps(context)
        val density = context.resources.configuration.densityDpi
        return launcherApps.getActivityList(packageName, userHandle)?.find {
            it.name == name
        }?.getIcon(density)
    }

    fun launchApp(context: Context) {
        val component = ComponentName(packageName, name)
        val launcherApps = getLauncherApps(context)
        launcherApps.startMainActivity(component, userHandle, null, bundleOf())
    }

    fun lunchShortcut(context: Context, shortcut: ShortcutInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            lunchShortcut(context, shortcut.id)
        }
    }

    fun lunchShortcut(context: Context, shortcutId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val launcherApps = getLauncherApps(context)
            launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
        }
    }

    fun getShortcutIcon(context: Context, shortcutInfo: ShortcutInfo): Drawable?
            = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                shortcutIconCache[shortcutInfo.id] ?: loadShortcutIcon(context, shortcutInfo)
            } else null

    private fun loadShortcutIcon(context: Context, shortcutInfo: ShortcutInfo): Drawable? {
        val launcherApps = getLauncherApps(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            return try {
                val drawable = launcherApps.getShortcutIconDrawable(shortcutInfo,
                    context.resources.displayMetrics.densityDpi)
                shortcutIconCache[shortcutInfo.id] = drawable
                drawable
            } catch (e: SecurityException) {
                null
            }
        } else null
    }
}

class LiveApps(context: Context) : LiveData<List<AppInfo>>() {

    private val applicationContext = context.applicationContext

    private val launcherApps by lazy {
        applicationContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    }

    private var job: Job? = null

    private val currentPackageName: String
        get() = applicationContext.packageName

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = load()

        override fun onPackageAdded(packageName: String?, user: UserHandle?) = load()

        override fun onPackageChanged(packageName: String?, user: UserHandle?) = load()

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) = load()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) = load()

    }

    private val appReloader = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            load()
        }
    }

    override fun onActive() {
        super.onActive()
        load()
        launcherApps.registerCallback(callback)

        applicationContext.registerReceiver(appReloader,
            IntentFilter().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                    addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                    addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
                } else {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                }
            })
    }

    override fun onInactive() {
        job?.cancel()
        launcherApps.unregisterCallback(callback)
        applicationContext.unregisterReceiver(appReloader)
        super.onInactive()
    }

    private fun getShortcutInfo(packageName: String): List<ShortcutInfo?>? {
        var shortcutInfo: List<ShortcutInfo?>? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutQuery = ShortcutQuery()
            shortcutQuery.setQueryFlags(ShortcutQuery.FLAG_MATCH_DYNAMIC
                    or ShortcutQuery.FLAG_MATCH_MANIFEST
                    or ShortcutQuery.FLAG_MATCH_PINNED)
            shortcutQuery.setPackage(packageName)

            try {
                shortcutInfo = launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle())
            } catch (e: SecurityException) {
            }
        }
        return shortcutInfo
    }

    private fun load() {
        job?.cancel()

        job = CoroutineScope(Dispatchers.IO).launch {
            val appsList = mutableListOf<AppInfo>()
            val context = this@LiveApps.applicationContext

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                // LauncherApps.getProfiles() is not available for API 25, so just get all associated user profile handlers
                val profiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    launcherApps.profiles
                } else {
                    val um = context.getSystemService(ContextWrapper.USER_SERVICE) as UserManager
                    um.userProfiles
                }

                for (userHandle in profiles) {
                    val apps = launcherApps.getActivityList(null, userHandle)
                    for (info in apps) {
                        if (info.applicationInfo.packageName != currentPackageName) {
                            val shortcutInfo = getShortcutInfo(info.componentName.packageName)
                            val app = AppInfo(context, info, userHandle, shortcutInfo)
                            appsList.add(app)
                        }
                    }
                }

            } else {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                val activitiesInfo = pm.queryIntentActivities(intent, 0)
                for (info in activitiesInfo) {
                    if (info.activityInfo.packageName != currentPackageName) {
                        val app = AppInfo(context, info)
                        appsList.add(app)
                    }
                }
            }

            appsList.sortWith { o1, o2 ->
                o1.label.compareTo(o2.label, ignoreCase = true)
            }

            postValue(appsList)
        }
    }

}

object AppManager {
    private var apps: LiveApps? = null

    fun getLiveApps(context: Context): LiveApps {
        return if (apps != null) {
            apps!!
        } else {
            apps = LiveApps(context)
            apps!!
        }
    }
}
