package com.rogger.bp.util

import android.content.Context
import android.content.Intent
import com.rogger.bp.R

object ShareUtil {

    fun shareApp(context: Context) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            val message = context.getString(R.string.share_app_message)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.share_app_title)
            )
        )
    }

    fun shareUserCode(context: Context, code: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            val message = context.getString(R.string.share_user_code_message, code)
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.share_user_code_title)
            )
        )
    }
}
