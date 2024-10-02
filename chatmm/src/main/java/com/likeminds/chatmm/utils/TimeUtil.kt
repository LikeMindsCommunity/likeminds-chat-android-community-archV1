package com.likeminds.chatmm.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
object TimeUtil {

    private const val APPROX_INITIAL_LAUNCH_MILLIS = 1546281000000
    private const val MILLIS_IN_DAY = 24 * 60 * 60 * 1000

    private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000
    private const val HOUR_IN_MILLIS = 60 * 60 * 1000
    private const val MINUTE_IN_MILLIS = 60 * 1000

    private val DATE_SDF = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("dd/MM/yy")
        }
    }

    private val DATE_SDF_1 = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        }
    }

    private val TIME_SDF = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("HH:mm")
        }
    }

    /**
     * @param time in millis
     * @return text to show
     */
    fun getLastConversationTime(time: Long?): String {
        if (time == null) {
            return ""
        }
        val newTime = if (isInMillis(time)) {
            time
        } else {
            time * 1000
        }
        val midnightTimestamp = getMidnightTimestamp()
        return when {
            newTime > midnightTimestamp -> {
                TIME_SDF.get()?.format(Date(newTime)) ?: ""
            }
            newTime > (midnightTimestamp - MILLIS_IN_DAY) -> {
                "Yesterday"
            }
            else -> {
                DATE_SDF.get()?.format(Date(newTime)) ?: ""
            }
        }
    }

    /**
     * Returns current time in HH:mm format
     */
    fun generateCreatedAt(conversationCreatedEpoch: Long): String {
        return TIME_SDF.get()?.format(conversationCreatedEpoch) ?: ""
    }

    /**
     * Returns current date in dd MMM yyyy format
     */
    fun generateDate(conversationCreatedEpoch: Long): String {
        return DATE_SDF_1.get()?.format(conversationCreatedEpoch) ?: ""
    }

    fun isInMillis(time: Long): Boolean {
        return time > APPROX_INITIAL_LAUNCH_MILLIS
    }

    private fun getMidnightTimestamp(): Long {
        val date = GregorianCalendar()
        date.set(Calendar.HOUR_OF_DAY, 0)
        date.set(Calendar.MINUTE, 0)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)
        return date.time.time
    }

    // to get the relative time string for two given timestamps
    fun getRelativeTimeInString(createdTime: Long, relativeTime: Long): String {
        val timeDifference = relativeTime - createdTime
        return getDaysHoursOrMinutes(timeDifference)
    }

    // Sets the time of the post as
    // x min (if days & hours are 0 and min > 0)
    // x h (if days are 0)
    // x d (if days are greater than 1)
    // Just Now (otherwise)
    private fun getDaysHoursOrMinutes(timestamp: Long): String {
        val days = (timestamp / DAY_IN_MILLIS).toInt()
        val hours = ((timestamp - (days * DAY_IN_MILLIS)) / HOUR_IN_MILLIS).toInt()
        val minutes =
            ((timestamp - (days * DAY_IN_MILLIS) - (hours * HOUR_IN_MILLIS)) / MINUTE_IN_MILLIS).toInt()
        return when {
            days == 0 && hours == 0 && minutes > 0 -> "$minutes minutes"
            days == 0 && hours == 1 -> "$hours hours"
            days == 0 && hours > 1 -> "$hours hours"
            days == 1 && hours == 0 -> "$days days"
            days >= 1 -> "$days days"
            else -> "Just Now"
        }
    }
}