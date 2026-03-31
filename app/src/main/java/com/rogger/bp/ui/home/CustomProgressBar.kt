package com.rogger.bp.ui.home

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import com.rogger.bp.R


private var loadingDialog: AlertDialog? = null
public fun showLoadingDialog(context: Context) {
    if (loadingDialog == null) {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.dialog_loading, null)

        loadingDialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    loadingDialog?.show()

    val window: Window? = loadingDialog?.window
    if (window != null) {
        val width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}

public fun hideLoadingDialog() {
    loadingDialog?.dismiss()
}
