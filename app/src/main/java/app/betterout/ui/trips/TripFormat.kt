package app.betterout.ui.trips

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.betterout.R
import app.betterout.data.Trip
import app.betterout.domain.TripRules
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

fun formatDate(date: LocalDate, locale: Locale, withYear: Boolean = false): String =
    date.format(DateTimeFormatter.ofPattern(if (withYear) "EEE d MMM yyyy" else "EEE d MMM", locale)).replace(".", "")

fun formatTime(time: LocalTime): String = time.format(DateTimeFormatter.ofPattern("HH:mm"))

/** "sab 21 nov · partenza 07:30", "sab 21 – lun 23 nov", oppure "Senza data". */
@Composable
fun tripWhen(trip: Trip, withYear: Boolean = false): String {
    val locale = currentLocale()
    val start = trip.startDate ?: return stringResource(R.string.senza_data)
    val end = trip.endDate?.takeIf { it != start }
    val dates = if (end == null) formatDate(start, locale, withYear) else formatDate(start, locale) + " – " + formatDate(end, locale, withYear)
    return trip.startTime?.let { dates + " · " + stringResource(R.string.partenza_alle, formatTime(it)) } ?: dates
}

/** "tra 6 giorni", "oggi", "13 giorni fa"; null senza data. */
@Composable
fun tripCountdown(trip: Trip, today: LocalDate): String? {
    val days = TripRules.daysUntil(trip, today)?.toInt() ?: return null
    return when {
        days == 0 -> stringResource(R.string.oggi)
        days == 1 -> stringResource(R.string.domani)
        days > 1 -> pluralStringResource(R.plurals.tra_giorni, days, days)
        else -> pluralStringResource(R.plurals.giorni_fa, -days, -days)
    }
}
