package com.example.audiocutterdemo.utils

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt


fun convertValue(
    currValue: Float, min1: Float, max1: Float, min2: Float, max2: Float
): Float {
    val percentOfDistance = (currValue - min1) / (max1 - min1)
    return percentOfDistance * (max2 - min2) + min2
}

fun convertValue(
    currValue: Int, min1: Int, max1: Int, min2: Int, max2: Int
): Int {
    val percentOfDistance = (currValue.toFloat() - min1) / (max1 - min1)
    return (percentOfDistance * (max2 - min2) + min2).toInt()
}

fun Context.dp2Px(dp: Float): Float {
    return (dp * resources.displayMetrics.density + 0.5f)
}

fun Context.px2Dp(px: Int): Float {
    return px / (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun Int.px2Dp(): Float {
    return this / (Resources.getSystem().displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

fun Context.dpToPx(dp: Float): Float {
    return (dp * resources.displayMetrics.density)
}

fun Context.pxToDp(px: Float): Int {
    return (px / resources.displayMetrics.density).roundToInt()
}

fun Float.toTimeString(): String {
    return this.toLong().toTimeString(isShowFractional = false)
}

fun Long.toTimeStringChoose(): String {
    val buf = StringBuffer()
    val hours = (this / (1000 * 60 * 60)).toInt()
    val minutes = (this % (1000 * 60 * 60) / (1000 * 60)).toInt()
    val secondsF = (this % (1000 * 60 * 60) % (1000 * 60) / 1000f)
    val seconds = if (secondsF > 0 && secondsF < 1) {
        1
    } else {
        secondsF.roundToInt()
    }

    if (hours > 0) {
        buf.append(String.format("%02d", hours)).append(":")
    }

    return buf.append(String.format("%02d", minutes)).append(":")
        .append(String.format("%02d", seconds)).toString()
}

fun Long.toTimeString(
    fullFormat: Boolean = false, // true -> ##:##:##.#, false -> ##:##.#
    isWithoutDot: Boolean = false, // true -> 0011001, false -> 00:11:00.1
    isShowFractional: Boolean = true // hiển thị cả đơn vị thập phân của đơn vị giây
): String {
    val timeFormat = StringBuffer()
    val hours = (this / (1000 * 60 * 60)).toInt()
    val minutes = (this % (1000 * 60 * 60) / (1000 * 60)).toInt()
    val secondsF = (this % (1000 * 60 * 60) % (1000 * 60) / 1000f)
    val seconds = if (secondsF > 0 && secondsF < 1) {
        1
    } else {
        secondsF.toInt()
    }

    if (fullFormat || hours > 0) {
        timeFormat.append(String.format("%02d", hours)).append(if (!isWithoutDot) ":" else "")
    }

    timeFormat
        .append(String.format("%02d", minutes))
        .append(if (!isWithoutDot) ":" else "")
        .append(String.format("%02d", seconds))

    if (isShowFractional) {
        val fractionalSecond = ((secondsF % 1) * 10).roundToInt()

        timeFormat
            .append(if (!isWithoutDot) "." else "")
            .append(String.format("%d", if (fractionalSecond == 10) 0 else fractionalSecond))
    }

    return timeFormat.toString()
}

fun String.toTimeLong(): Long? {

    val isNumberFormat = "[0-9]".toRegex().matches(this)

    if (!isNumberFormat)
        return null

    val times = this.split("").filter { it != "" }

    var millisSeconds = 0L

    times.forEachIndexed { index, s ->
        millisSeconds += when (index) {
            0 -> TimeUnit.HOURS.toMillis(s.toLong() * 10)
            1 -> TimeUnit.HOURS.toMillis(s.toLong())
            2 -> TimeUnit.MINUTES.toMillis(s.toLong() * 10)
            3 -> TimeUnit.MINUTES.toMillis(s.toLong())
            4 -> TimeUnit.SECONDS.toMillis(s.toLong() * 10)
            5 -> TimeUnit.SECONDS.toMillis(s.toLong())
            else -> s.toLong() * 10
        }
    }

    return millisSeconds
}

fun Int.bitrateToString(): String = this.toLong().readableFileSize().plus("/s")

fun Long.toSizeString(): String = readableFileSize()

private fun Long.readableFileSize(): String {
    if (this <= 0) return "0"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(this / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}

suspend fun Context.getIconFromPackageName(packageName: String): Drawable? =
    withContext(Dispatchers.IO) {
        val pm = packageManager
        try {
            val pi = pm.getPackageInfo(packageName, 0)
            val otherAppCtx = createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY)
            val displayMetrics = intArrayOf(
                DisplayMetrics.DENSITY_XHIGH, DisplayMetrics.DENSITY_HIGH, DisplayMetrics.DENSITY_TV
            )
            for (displayMetric in displayMetrics) {
                try {
                    val icon = pi.applicationInfo?.icon ?: return@withContext null

                    val d = otherAppCtx.resources.getDrawableForDensity(icon, displayMetric)
                    if (d != null) {
                        return@withContext d
                    }
                } catch (e: Resources.NotFoundException) {
                    continue
                }
            }
        } catch (e: Exception) {
            // Handle Error here
        }
        val appInfo: ApplicationInfo? = try {
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            return@withContext null
        }
        return@withContext appInfo?.loadIcon(pm)
    }

fun String.deAccent(): List<Char> {
    val nfdNormalizedString: String = Normalizer.normalize(this, Normalizer.Form.NFD)
    val pattern: Pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(nfdNormalizedString).replaceAll("").lowercase().replace('đ', 'd')
        .toList()
}

fun toTimeStr(hours: Int, minute: Int): String {
    val minuteStr = minute.limitTime()
    val hoursStr = hours.limitTime()

    return "$hoursStr:$minuteStr"
}

fun Int.limitTime(): String {
    return if (this < 10) {
        "0${this}"
    } else {
        "$this"
    }
}

fun onBackHandler(
    openSearch: Boolean,
    onBack: () -> Unit,
    onSearchChange: (Boolean) -> Unit,
) {
    if (openSearch) onSearchChange(false)
    else onBack()
}

fun String.nameFromPath(): String {
    return this.substring(this.lastIndexOf("/") + 1)
}

fun String.nameWithoutExtension(): String {
    return this.substringAfterLast("/").substringBeforeLast(".")
}

fun Context.getActivity(): ComponentActivity {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }

    throw IllegalAccessException("Can't get activity!!!!")
}

fun isValidName(text: String): Boolean {
    val p: Pattern = Pattern.compile("[*:\"\\\\|\\/?]", Pattern.CASE_INSENSITIVE)
    val m: Matcher = p.matcher(text)
    return !m.find()
}

@SuppressLint("SimpleDateFormat")
fun Long.toDate(): String {
    val format = "dd/MM/yyyy"
    val formatter = SimpleDateFormat(format)

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = this
    return formatter.format(calendar.time)
}

fun setAlarmDevice(context: Context, uriString: String): Boolean {
    return writeSettingAudio(
        context,
        uriString = uriString,
        ringtoneType = RingtoneManager.TYPE_ALARM
    )
}

fun setRingToneDevice(context: Context, uriString: String): Boolean {
    return writeSettingAudio(
        context,
        uriString = uriString,
        ringtoneType = RingtoneManager.TYPE_RINGTONE
    )
}

fun setNotificationDevice(context: Context, uriString: String): Boolean {
    return writeSettingAudio(
        context = context,
        uriString = uriString,
        ringtoneType = RingtoneManager.TYPE_NOTIFICATION
    )
}

private fun writeSettingAudio(
    context: Context,
    uriString: String,
    ringtoneType: Int = RingtoneManager.TYPE_NOTIFICATION
): Boolean {

    val uri = Uri.parse(uriString)

    RingtoneManager.setActualDefaultRingtoneUri(
        context,
        ringtoneType,
        uri
    )

    return (RingtoneManager.getActualDefaultRingtoneUri(context, ringtoneType)).equals(uri)
}


fun Context.openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.d("MTHAI", "ActivityNotFoundException")
    }
}

fun Context.goToStore() {
    try {
        openUrl("market://details?id=${applicationContext.packageName}")
    } catch (e: ActivityNotFoundException) {
        openUrl("https://play.google.com/store/apps/details?id=${applicationContext.packageName}")
    }
}

fun Context.getVersion(): String {
    return try {
        packageManager.getPackageInfo(packageName, 0).versionName.toString()
    } catch (e: Exception) {
        "1.0"
    }
}


fun Long.purchaseTimeFormat(): String {
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}