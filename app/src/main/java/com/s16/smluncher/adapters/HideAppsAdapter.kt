package com.s16.smluncher.adapters

import android.content.Context
import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatCheckBox
import com.s16.smluncher.R
import com.s16.smluncher.managers.AppInfo
import com.s16.view.RecyclerViewArrayAdapter
import com.s16.view.RecyclerViewHolder

class HideAppsAdapter(val context: Context) : RecyclerViewArrayAdapter<RecyclerViewHolder, AppInfo>() {

    private val selectedPackages = mutableListOf<String>()
    private val selectedItems = SparseBooleanArray()
    private var isCheckedChanging = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        return RecyclerViewHolder.inflate(R.layout.list_item_app, parent).apply {
            find(android.R.id.text1)
            find(android.R.id.text1)
            find(android.R.id.icon)
            find(android.R.id.checkbox)
        }
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int, item: AppInfo) {
        holder.setText(item.label to android.R.id.text1)
        holder.setText(item.packageName to android.R.id.text2)

        val checkBox: AppCompatCheckBox = holder[android.R.id.checkbox]
        checkBox.isChecked = selectedItems.get(position, false)
        checkBox.tag = position

        checkBox.setOnClickListener { v ->
            isCheckedChanging = true
            val isChecked = (v as AppCompatCheckBox).isChecked
            (v.tag as? Int)?.let { pos ->
                val oldChecked = selectedItems.get(pos, false)
                if (oldChecked != isChecked) {
                    val checkedItem = getItem(pos)
                    if (checkedItem != null) {
                        mOnCheckedChangeListener?.OnCheckedChange(v, isChecked, checkedItem)
                    }
                }
                selectedItems.put(pos, isChecked)
            }
            isCheckedChanging = false
        }

        val img: ImageView = holder[android.R.id.icon]
        img.setImageDrawable(item.icon)
    }

    override fun onViewRecycled(holder: RecyclerViewHolder) {
        val img: ImageView = holder[android.R.id.icon]
        img.setImageDrawable(null)

        super.onViewRecycled(holder)
    }

    override fun submitList(collection: Collection<AppInfo>) {
        super.submitList(collection)
        loadSelectedPackages()
    }

    interface OnCheckedChangeListener {
        fun OnCheckedChange(view: View, isChecked: Boolean, item: AppInfo)
    }

    private var mOnCheckedChangeListener: OnCheckedChangeListener? = null

    fun setOnCheckedChangeListener(l: OnCheckedChangeListener) {
        mOnCheckedChangeListener = l
    }

    fun setSelectedPackages(packages: List<String>) {
        if (!isCheckedChanging) {
            selectedPackages.clear()
            selectedPackages.addAll(packages)
            loadSelectedPackages()
        }
    }

    private fun loadSelectedPackages() {
        if (itemCount > 0 && !isCheckedChanging) {
            for (i in selectedPackages.indices) {
                val name = selectedPackages[i]
                val itemIdx = findIndex { it.name == name }
                if (itemIdx > -1) {
                    selectedItems.put(itemIdx, true)
                    notifyItemChanged(itemIdx)
                }
            }
        }
    }
}