package com.s16.smluncher.adapters

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.s16.drawables.CircleDrawable
import com.s16.drawables.SquareDrawable
import com.s16.smluncher.R
import com.s16.smluncher.managers.AppInfo
import com.s16.view.RecyclerViewArrayAdapter
import com.s16.view.RecyclerViewHolder

class DrawerAdapter(val context: Context) : RecyclerViewArrayAdapter<DrawerAdapter.DrawerViewHolder, AppInfo>() {

    private val hideApps = mutableListOf<String>()
    private var origList : List<AppInfo>? = null

    class DrawerViewHolder(itemView: View) :
        RecyclerViewHolder(itemView, R.id.iconText, R.id.iconImage, R.id.notificationBadge),
        View.OnLongClickListener,
        View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {

        init {
            itemView.setOnLongClickListener(this)
        }

        private var popupMenu : PopupMenu? = null

        private val context: Context
            get() = itemView.context

        private val activity: AppCompatActivity?
            get() = if (context is AppCompatActivity) {
                context as AppCompatActivity
            } else null

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {

        }

        override fun onLongClick(v: View): Boolean {
            if (popupMenu == null) {
                popupMenu = PopupMenu(context, v).apply {
                    setForceShowIcon(true)
                }
                val menu = popupMenu!!.menu
                menu.add(Menu.NONE, 1, 1, R.string.action_app_info).apply {
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_info_outline_gray)
                    setOnMenuItemClickListener(this@DrawerViewHolder)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    getItem()?.let { appInfo ->
                        val shortcuts = appInfo.shortcutInfo
                        for (i in shortcuts.indices) {
                            val s = shortcuts[i]
                            val shortLabel = s.shortLabel
                            val shortcutIcon = appInfo.getShortcutIcon(context, s)
                            menu.add(Menu.NONE, i + 2, i + 2, shortLabel).apply {
                                icon = shortcutIcon?.let {
                                    SquareDrawable(context, it)
                                }
                                setOnMenuItemClickListener(this@DrawerViewHolder)
                            }
                        }
                    }
                }
            }
            popupMenu!!.show()
            return true
        }

        override fun onMenuItemClick(item: MenuItem): Boolean {
            getItem()?.let { appInfo ->
                if (item.itemId == 1) {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            data = Uri.parse("package:${appInfo.packageName}")
                        }
                        activity?.onBackPressed()
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                    }
                    return true

                } else {
                    val idx = item.itemId - 2
                    if (idx > -1 && idx < appInfo.shortcutInfo.size) {
                        val shortcut = appInfo.shortcutInfo[idx]
                        activity?.onBackPressed()
                        appInfo.lunchShortcut(context, shortcut)
                        return true
                    }
                }

            }
            return false
        }

        private fun getItem() : AppInfo? {
            val adapter = bindingAdapter as DrawerAdapter
            return adapter.getItem(absoluteAdapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.drawer_item, parent, false)
        return DrawerViewHolder(view)
    }

    override fun onBindViewHolder(holder: DrawerViewHolder, position: Int, item: AppInfo) {
        holder.setText(item.label to R.id.iconText)

        val img: AppCompatImageView = holder[R.id.iconImage]
        img.setImageDrawable(null)
        item.icon?.let {
            val drawable = CircleDrawable(context, it)
            img.setImageDrawable(drawable)
        }
    }

    override fun onViewRecycled(holder: DrawerViewHolder) {
        val img: AppCompatImageView = holder[R.id.iconImage]
        img.setImageDrawable(null)

        super.onViewRecycled(holder)
    }

    override fun submitList(collection: Collection<AppInfo>) {
        origList = collection.toList()
        val list = filterHideApps(origList)
        super.submitList(list)
    }

    private fun filterHideApps(collection: List<AppInfo>?): List<AppInfo> =
        if (collection != null && hideApps.isNotEmpty()) {
            collection.filter {
                !hideApps.contains(it.key)
            }
        } else collection?.toList() ?: listOf()

    fun setHideApps(list: List<String>) {
        hideApps.clear()
        hideApps.addAll(list)
        if (itemCount > 0) {
            submitList(filterHideApps(origList))
        }
    }
}