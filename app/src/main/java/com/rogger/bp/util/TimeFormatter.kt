package com.rogger.bp.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object TimeFormatter {
    private val locale = Locale("pt", "BR")

    @JvmStatic
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", locale)
        return sdf.format(Date(timestamp))
    }

    @JvmStatic
    fun getDaysRemaining(expirationTimestamp: Long): Long {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diff = expirationTimestamp - now
        return TimeUnit.MILLISECONDS.toDays(diff)
    }
}
