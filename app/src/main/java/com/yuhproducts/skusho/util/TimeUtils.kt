package com.yuhproducts.skusho.util

import java.util.concurrent.TimeUnit

object TimeUtils {
    fun formatDurationMillis(millis: Long): String {
        if (millis <= 0L) return "0秒"

        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> String.format("%d時間%02d分%02d秒", hours, minutes, seconds)
            minutes > 0 -> String.format("%d分%02d秒", minutes, seconds)
            else -> String.format("%d秒", seconds)
        }
    }
}
