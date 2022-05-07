package com.s16.smluncher.helpers

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.s16.smluncher.R

object AboutDialog {

    private var mDialog: Dialog? = null
    private const val PADDING_VERT = 16
    private const val PADDING_HORIZ = 24

    private val onDismissListener = DialogInterface.OnDismissListener {
        mDialog = null
    }

    private fun dpToPixel(context: Context, dp: Int): Int {
        val metrics = context.resources.displayMetrics
        val px = dp * (metrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
        return px.toInt()
    }

    @Suppress("DEPRECATION")
    private fun createDialog(context: Context) {
        val dialogTitle = context.getString(R.string.app_name)
        val dialogMessage = context.getString(R.string.about_text)
        val dialogIcon = ContextCompat.getDrawable(context, R.drawable.ic_info_outline_gray)
        val textColor = ContextCompat.getColor(context, R.color.grey_50)

        val builder = AlertDialog.Builder(context)
        builder.setTitle(dialogTitle)
        builder.setIcon(dialogIcon)

        val messageView = TextView(context)
        val paddingVert = dpToPixel(context, PADDING_VERT)
        val paddingHoriz = dpToPixel(context, PADDING_HORIZ)
        messageView.setPadding(paddingHoriz, paddingVert, paddingHoriz, 0)
        messageView.movementMethod = LinkMovementMethod.getInstance()
        messageView.text = HtmlCompat.fromHtml(dialogMessage, HtmlCompat.FROM_HTML_MODE_COMPACT)

        val textAppearanceId = android.R.style.TextAppearance_Medium
        if (Build.VERSION.SDK_INT >= 23) {
            messageView.setTextAppearance(textAppearanceId)
        } else {
            messageView.setTextAppearance(context, textAppearanceId)
        }
        messageView.setTextColor(textColor)

        builder.setView(messageView)

        builder.setPositiveButton(null, null)
        builder.setNegativeButton(android.R.string.ok
        ) { _, _ -> }

        mDialog = builder.create()
    }

    fun showDialog(context: Context) {
        if (mDialog == null) {
            createDialog(context)
        }
        mDialog?.let {
            if (!it.isShowing) {
                it.setOnDismissListener(onDismissListener)
                it.show()
            }
        }
    }
}