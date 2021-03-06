package com.s16.view

import android.content.Context
import android.util.TypedValue
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import kotlin.math.max


class GridAutoFitLayoutManager : GridLayoutManager {
    private var mColumnWidth = 0
    private var mColumnWidthChanged = true
    private var mWidthChanged = true
    private var mWidth = 0

    constructor(context: Context, columnWidth: Int) : super(context, 1) {
        /* Initially set spanCount to 1, will be changed automatically later. */
        setColumnWidth(checkedColumnWidth(context, columnWidth))
    }

    constructor(
        context: Context,
        columnWidth: Int,
        orientation: Int,
        reverseLayout: Boolean
    ) : super(context, 1, orientation, reverseLayout) {
        /* Initially set spanCount to 1, will be changed automatically later. */
        setColumnWidth(checkedColumnWidth(context, columnWidth))
    }

    private fun checkedColumnWidth(context: Context, width: Int): Int {
        var columnWidth = width
        columnWidth = if (columnWidth <= 0) {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, sColumnWidth.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        } else {
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, columnWidth.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        }
        return columnWidth
    }

    private fun setColumnWidth(newColumnWidth: Int) {
        if (newColumnWidth > 0 && newColumnWidth != mColumnWidth) {
            mColumnWidth = newColumnWidth
            mColumnWidthChanged = true
        }
    }

    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        val width = width
        val height = height
        if (width != mWidth) {
            mWidthChanged = true
            mWidth = width
        }
        if (mColumnWidthChanged && mColumnWidth > 0 && width > 0 && height > 0
            || mWidthChanged
        ) {
            val totalSpace = if (orientation == VERTICAL) {
                width - paddingRight - paddingLeft
            } else {
                height - paddingTop - paddingBottom
            }
            val spanCount = max(1, totalSpace / mColumnWidth)
            setSpanCount(spanCount)
            mColumnWidthChanged = false
            mWidthChanged = false
        }
        super.onLayoutChildren(recycler, state)
    }

    companion object {
        private const val sColumnWidth = 200 // assume cell width of 200dp
    }
}