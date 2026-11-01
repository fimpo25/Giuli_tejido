package com.giuli.tejido.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

val ArLocale: Locale = Locale("es", "AR")

/** 1:05:09 */
fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%d:%02d:%02d", h, m, s)
}

/** 3 h 05 min */
fun formatHoursMinutes(ms: Long): String {
    val totalMin = abs(ms) / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    val txt = if (h > 0) "$h h ${m.toString().padStart(2, '0')} min" else "$m min"
    return if (ms < 0) "−$txt" else txt
}

fun formatMoney(value: Double): String =
    NumberFormat.getCurrencyInstance(ArLocale).format(value)

fun formatQty(value: Double): String {
    val nf = NumberFormat.getNumberInstance(ArLocale)
    nf.maximumFractionDigits = 2
    return nf.format(value)
}

/** Para precargar un campo de texto con un número guardado. */
fun numberForInput(value: Double): String =
    if (value == Math.floor(value) && abs(value) < 1e12) value.toLong().toString()
    else value.toString().replace('.', ',')

/**
 * Acepta "1500", "1.500", "1.500,50", "12,5" o "12.5".
 * Un punto seguido de exactamente 3 dígitos se toma como separador de miles.
 */
fun parseNumber(raw: String): Double? {
    var s = raw.trim().replace(" ", "").replace("$", "")
    if (s.isEmpty()) return null
    val dots = s.count { it == '.' }
    s = when {
        s.contains(',') && dots > 0 -> s.replace(".", "").replace(',', '.')
        s.contains(',') -> s.replace(',', '.')
        dots > 1 -> s.replace(".", "")
        dots == 1 && s.substringAfter('.').length == 3 && s.substringBefore('.') != "0" &&
            s.substringBefore('.').isNotEmpty() -> s.replace(".", "")
        else -> s
    }
    return s.toDoubleOrNull()
}

fun formatDateTime(ms: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", ArLocale).format(Date(ms))

fun formatDate(ms: Long): String =
    SimpleDateFormat("dd/MM/yyyy", ArLocale).format(Date(ms))

fun formatClock(ms: Long): String =
    SimpleDateFormat("HH:mm", ArLocale).format(Date(ms))

/** Hora actual que se actualiza cada segundo mientras [active] sea true. */
@Composable
fun rememberNow(active: Boolean): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        now = System.currentTimeMillis()
        while (active) {
            delay(1000L - (System.currentTimeMillis() % 1000L))
            now = System.currentTimeMillis()
        }
    }
    return now
}
