package biz.itonline.trackrecord.support

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun String.urlSafe(): String? {
    return this.let {
        try {
            URLEncoder.encode(it, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            null
        }
    }
}

fun String.getUrlSafe(): String? {
    return this.let {
        try {
            URLDecoder.decode(it, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
            null
        }
    }
}

fun Context.hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

fun Location.toTrackString(): String {

    val locale = Locale.US
    val symbols = DecimalFormatSymbols(locale)
    val df = DecimalFormat("#.#####", symbols)
    return "{\"latitude\":${df.format(latitude)},\"longitude\":${df.format(longitude)},\"altitude\":${
        df.format(
            altitude
        )
    }}"
}