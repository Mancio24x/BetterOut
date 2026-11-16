package app.betterout.ui.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.betterout.R
import app.betterout.data.Route
import app.betterout.data.Trip
import app.betterout.domain.Climate
import app.betterout.domain.DayWeather
import app.betterout.domain.HourWeather
import app.betterout.domain.Pace
import app.betterout.domain.PointForecast
import app.betterout.domain.Sky
import app.betterout.domain.TripRules
import app.betterout.domain.WeatherPointKind
import app.betterout.domain.WeatherRules
import app.betterout.domain.WeatherRules.Plan
import app.betterout.ui.components.AppCard
import app.betterout.ui.components.ChoiceChips
import app.betterout.ui.components.SecondaryButton
import app.betterout.ui.map.Notice
import app.betterout.ui.map.estimatedMinutes
import app.betterout.ui.map.formatDuration
import app.betterout.ui.map.formatMeters
import app.betterout.ui.theme.AppIcons2
import app.betterout.ui.theme.AppIcons3
import app.betterout.ui.theme.AppIcons4
import app.betterout.ui.trips.currentLocale
import app.betterout.ui.trips.formatDate
import app.betterout.ui.trips.formatTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---- Testi ----

/** "−8°": gradi interi con il segno meno tipografico. */
fun formatTemp(value: Double): String = Math.round(value).toInt().let { if (it < 0) "−${-it}°" else "$it°" }
private fun kmh(value: Double, locale: Locale) = String.format(locale, "%.0f km/h", value)
private fun cm(value: Double, locale: Locale) = String.format(locale, if (value < 10 && value % 1.0 != 0.0) "%.1f cm" else "%.0f cm", value)
private fun clock(isoDateTime: String) = isoDateTime.substringAfter('T')
private fun dayMonth(date: LocalDate, locale: Locale) = date.format(DateTimeFormatter.ofPattern("d MMMM", locale))
private fun joined(vararg parts: String?): String? = parts.filterNotNull().joinToString(" · ").ifEmpty { null }

@Composable
private fun skyLabel(code: Int?): String = stringResource(
    when (WeatherRules.sky(code)) {
        Sky.SERENO -> R.string.meteo_cielo_sereno; Sky.POCO_NUVOLOSO -> R.string.meteo_cielo_poco_nuvoloso; Sky.PARZIALMENTE_NUVOLOSO -> R.string.meteo_cielo_parzialmente_nuvoloso
        Sky.COPERTO -> R.string.meteo_cielo_coperto; Sky.NEBBIA -> R.string.meteo_cielo_nebbia
        Sky.PIOVIGGINE -> R.string.meteo_cielo_pioviggine; Sky.PIOVIGGINE_GELATA -> R.string.meteo_cielo_pioviggine_gelata
        Sky.PIOGGIA_DEBOLE -> R.string.meteo_cielo_pioggia_debole; Sky.PIOGGIA -> R.string.meteo_cielo_pioggia; Sky.PIOGGIA_FORTE -> R.string.meteo_cielo_pioggia_forte; Sky.PIOGGIA_GELATA -> R.string.meteo_cielo_pioggia_gelata
        Sky.NEVE_DEBOLE -> R.string.meteo_cielo_neve_debole; Sky.NEVE -> R.string.meteo_cielo_neve; Sky.NEVE_FORTE -> R.string.meteo_cielo_neve_forte
        Sky.ROVESCI -> R.string.meteo_cielo_rovesci; Sky.ROVESCI_NEVE -> R.string.meteo_cielo_rovesci_neve
        Sky.TEMPORALE -> R.string.meteo_cielo_temporale; Sky.TEMPORALE_GRANDINE -> R.string.meteo_cielo_temporale_grandine
        null -> R.string.meteo_cielo_ignoto
    },
)

private fun skyIcon(code: Int?, isDay: Boolean = true): ImageVector = when (WeatherRules.sky(code)) {
    Sky.SERENO -> if (isDay) AppIcons4.Sun else AppIcons4.Moon
    Sky.POCO_NUVOLOSO, Sky.PARZIALMENTE_NUVOLOSO -> if (isDay) AppIcons4.CloudSun else AppIcons4.Cloud
    Sky.COPERTO -> AppIcons4.Cloud
    Sky.NEBBIA -> AppIcons4.Fog
    Sky.PIOVIGGINE, Sky.PIOVIGGINE_GELATA, Sky.PIOGGIA_DEBOLE, Sky.PIOGGIA, Sky.PIOGGIA_FORTE, Sky.PIOGGIA_GELATA, Sky.ROVESCI -> AppIcons4.Rain
    Sky.NEVE_DEBOLE, Sky.NEVE, Sky.NEVE_FORTE, Sky.ROVESCI_NEVE -> AppIcons4.Snow
    Sky.TEMPORALE, Sky.TEMPORALE_GRANDINE -> AppIcons4.Storm
    null -> AppIcons3.Info
}

@Composable
private fun kindLabel(kind: WeatherPointKind): String = stringResource(
    when (kind) { WeatherPointKind.LOCALITA -> R.string.meteo_punto_localita; WeatherPointKind.PARTENZA -> R.string.meteo_punto_partenza; WeatherPointKind.QUOTA_MASSIMA -> R.string.meteo_punto_quota_massima },
)

/** "Partenza · 2105 m": ogni previsione dice a quale punto e quota si riferisce. */
@Composable
private fun pointLabel(forecast: PointForecast): String {
    val kind = kindLabel(forecast.point.kind)
    return forecast.elevation?.let { stringResource(R.string.meteo_punto_con_quota, kind, formatMeters(it, currentLocale())) } ?: kind
}

@Composable
private fun minMax(day: DayWeather): String? = if (day.tempMin != null && day.tempMax != null) stringResource(R.string.meteo_min_max, formatTemp(day.tempMin), formatTemp(day.tempMax)) else null

@Composable
private fun minMaxSpoken(day: DayWeather): String = if (day.tempMin != null && day.tempMax != null) stringResource(R.string.meteo_min_max_letto, formatTemp(day.tempMin), formatTemp(day.tempMax)) else stringResource(R.string.non_disponibile)

/** "adesso", "25 minuti fa", "3 ore fa", "2 giorni fa". */
@Composable
private fun ageText(fetchedAt: Long): String {
    val minutes = ((System.currentTimeMillis() - fetchedAt) / 60_000).toInt().coerceAtLeast(0)
    return when {
        minutes < 1 -> stringResource(R.string.meteo_eta_adesso)
        minutes < 60 -> pluralStringResource(R.plurals.meteo_eta_minuti, minutes, minutes)
        minutes < 48 * 60 -> pluralStringResource(R.plurals.meteo_eta_ore, minutes / 60, minutes / 60)
        else -> pluralStringResource(R.plurals.giorni_fa, minutes / 1440, minutes / 1440)
    }
}

@Composable
private fun updatedText(fetchedAt: Long): String {
    val at = Instant.ofEpochMilli(fetchedAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val time = formatTime(at.toLocalTime())
    return if (at.toLocalDate() == LocalDate.now()) stringResource(R.string.meteo_aggiornato_oggi, time) else stringResource(R.string.meteo_aggiornato_il, formatDate(at.toLocalDate(), currentLocale()), time)
}

// ---- Pezzi comuni ----

@Composable
private fun Muted(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun LoadingRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Muted(text)
    }
}

/** Riga etichetta-valore; un valore che manca è scritto "non disponibile". */
@Composable
private fun ValueRow(label: String, value: String?) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = MaterialTheme.typography.titleSmall) else Muted(stringResource(R.string.non_disponibile))
    }
}

/** Una riga per giorno: icona, condizioni e vento, minima e massima. */
@Composable
private fun DaySummaryRow(title: String, day: DayWeather?) {
    val locale = currentLocale()
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(skyIcon(day?.code), contentDescription = null, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (day == null) stringResource(R.string.non_disponibile) else skyLabel(day.code), style = MaterialTheme.typography.titleSmall)
        }
        if (day != null) Column(horizontalAlignment = Alignment.End) {
            val spoken = minMaxSpoken(day)
            Text(minMax(day) ?: "—", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { contentDescription = spoken })
            day.windKmh?.let { Text(stringResource(R.string.meteo_vento_breve, kmh(it, locale)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

// ---- Riquadro nel dettaglio dell'uscita ----

/** Il meteo del primo giorno dell'uscita per ogni punto, oppure che cosa manca per averlo. */
@Composable
fun TripWeatherCard(vm: WeatherViewModel, onOpen: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val plan = s.plan ?: return
    val locale = currentLocale()
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (plan is Plan.Ask) stringResource(R.string.meteo_del, formatDate(plan.firstDay, locale)) else stringResource(R.string.meteo),
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (plan is Plan.Ask) s.report?.let { Text(stringResource(R.string.meteo_fonte_breve, ageText(it.fetchedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            when (plan) {
                Plan.NoDate -> Muted(stringResource(R.string.meteo_senza_data))
                Plan.NoPlace -> Muted(stringResource(R.string.meteo_senza_luogo))
                Plan.Past -> {
                    // Di un'uscita passata resta l'ultima previsione salvata: è ciò che era previsto, non ciò che è stato misurato.
                    val report = s.report
                    val start = s.trip?.startDate
                    val end = s.trip?.endDate ?: start
                    val day = if (report != null && start != null) generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.firstOrNull { d -> report.points.any { it.day(d) != null } } else null
                    if (report == null || day == null) Muted(stringResource(R.string.meteo_uscita_passata))
                    else {
                        report.points.forEachIndexed { i, forecast ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            DaySummaryRow(pointLabel(forecast) + " · " + formatDate(day, locale), forecast.day(day))
                        }
                        Text(stringResource(R.string.meteo_salvata_il, formatDate(Instant.ofEpochMilli(report.fetchedAt).atZone(ZoneId.systemDefault()).toLocalDate(), locale)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is Plan.TooEarly -> {
                    Muted(stringResource(R.string.meteo_non_ancora_titolo) + ". " + stringResource(R.string.meteo_non_ancora_breve, dayMonth(plan.availableFrom, locale)))
                    SecondaryButton(stringResource(R.string.meteo_apri), onOpen, icon = AppIcons4.Cloud)
                }
                is Plan.Ask -> {
                    val report = s.report
                    when {
                        report != null -> {
                            report.points.forEachIndexed { i, forecast ->
                                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                DaySummaryRow(pointLabel(forecast), forecast.day(plan.firstDay))
                            }
                            if (s.failed) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(AppIcons2.Warn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.meteo_errore_breve, ageText(report.fetchedAt)), style = MaterialTheme.typography.bodySmall)
                            }
                            SecondaryButton(stringResource(R.string.meteo_apri), onOpen, icon = AppIcons4.Cloud)
                        }
                        s.failed -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(AppIcons2.Warn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Muted(stringResource(R.string.meteo_errore_senza_dato))
                            }
                            SecondaryButton(stringResource(R.string.riprova), vm::refresh, icon = AppIcons4.Refresh)
                        }
                        else -> LoadingRow(stringResource(R.string.meteo_in_arrivo))
                    }
                }
            }
        }
    }
}

// ---- Schermata Meteo ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(vm: WeatherViewModel, onBack: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    var hourly by rememberSaveable { mutableStateOf(false) }
    var pointIndex by rememberSaveable { mutableIntStateOf(0) }
    var dayIndex by rememberSaveable { mutableIntStateOf(0) }
    val plan = s.plan
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meteo), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(AppIcons2.Back, contentDescription = stringResource(R.string.indietro)) } },
                actions = { if (plan is Plan.Ask) IconButton(onClick = vm::refresh, enabled = !s.loading) { Icon(AppIcons4.Refresh, contentDescription = stringResource(R.string.meteo_aggiorna)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (plan) {
                null -> if (s.trip == null) Muted(stringResource(R.string.uscita_non_trovata))
                Plan.NoDate -> Notice(null, stringResource(R.string.meteo_senza_data), AppIcons3.Info)
                Plan.NoPlace -> Notice(null, stringResource(R.string.meteo_senza_luogo), AppIcons3.Info)
                Plan.Past -> Notice(null, stringResource(R.string.meteo_uscita_passata), AppIcons3.Info)
                is Plan.TooEarly -> TooEarlyContent(plan, s, vm::refresh)
                is Plan.Ask -> ForecastContent(plan, s, vm::refresh, hourly, { hourly = it }, pointIndex, { pointIndex = it }, dayIndex, { dayIndex = it })
            }
        }
    }
}

@Composable
private fun ForecastContent(
    plan: Plan.Ask, s: WeatherViewModel.State, onRetry: () -> Unit,
    hourly: Boolean, onHourly: (Boolean) -> Unit, pointIndex: Int, onPoint: (Int) -> Unit, dayIndex: Int, onDay: (Int) -> Unit,
) {
    val locale = currentLocale()
    val report = s.report
    if (s.failed) Notice(
        stringResource(R.string.meteo_errore_titolo),
        if (report == null) stringResource(R.string.meteo_errore_senza_dato) else stringResource(R.string.meteo_errore_con_dato, ageText(report.fetchedAt)),
        AppIcons2.Warn,
    ) { TextButton(onClick = onRetry) { Text(stringResource(R.string.riprova)) } }
    if (s.loading) LoadingRow(stringResource(R.string.meteo_in_arrivo))
    if (report == null) return
    if (plan.truncated) Notice(null, stringResource(R.string.meteo_troncato, formatDate(plan.lastDay, locale)), AppIcons2.Calendar)
    ChoiceChips(listOf(false to stringResource(R.string.meteo_giorni), true to stringResource(R.string.meteo_ore)), hourly, onHourly)
    val index = pointIndex.coerceIn(report.points.indices)
    val forecast = report.points[index]
    if (report.points.size > 1) ChoiceChips(report.points.indices.map { it to pointLabel(report.points[it]) }, index, onPoint)
    if (forecast.elevationIsEstimate) Text(stringResource(R.string.meteo_quota_stimata), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val days = plan.tripDays
    if (!hourly) {
        days.forEach { DayCard(forecast, it) }
        if (plan.nearbyDays.isNotEmpty()) AppCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.meteo_giorni_vicini), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                plan.nearbyDays.forEach { date ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DaySummaryRow(formatDate(date, locale), forecast.day(date))
                }
            }
        }
    } else {
        val day = days[dayIndex.coerceIn(days.indices)]
        if (days.size > 1) ChoiceChips(days.indices.map { it to formatDate(days[it], locale) }, days.indexOf(day), onDay)
        HoursCard(forecast, day)
        WindCard(forecast, day, s.trip, s.route)
    }
    Text(updatedText(report.fetchedAt) + " · " + stringResource(R.string.meteo_fonte), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DayCard(forecast: PointForecast, date: LocalDate) {
    val locale = currentLocale()
    val day = forecast.day(date)
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(formatDate(date, locale), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                Text(pointLabel(forecast), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (day == null) { Muted(stringResource(R.string.non_disponibile)); return@Column }
            Row(Modifier.padding(vertical = 10.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(skyIcon(day.code), contentDescription = null, modifier = Modifier.size(40.dp))
                Column {
                    val spoken = minMaxSpoken(day)
                    Text(minMax(day) ?: "—", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { contentDescription = spoken })
                    Muted(joined(skyLabel(day.code), day.feelsMin?.let { stringResource(R.string.meteo_percepita, formatTemp(it)) })!!)
                }
            }
            ValueRow(stringResource(R.string.meteo_precipitazioni), joined(day.precipitationProbability?.let { "$it %" }, day.precipitationMm?.let { String.format(locale, "%.1f mm", it) }))
            ValueRow(stringResource(R.string.meteo_neve), day.snowCm?.let { cm(it, locale) })
            val compass = stringArrayResource(R.array.meteo_rosa_dei_venti)
            ValueRow(
                stringResource(R.string.meteo_vento_raffiche),
                joined(day.windKmh?.let { w -> day.windDirection?.let { stringResource(R.string.meteo_vento_da, kmh(w, locale), compass[WeatherRules.compassSector(it)]) } ?: kmh(w, locale) }, day.gustsKmh?.let { kmh(it, locale) }),
            )
            ValueRow(stringResource(R.string.meteo_zero_termico), WeatherRules.freezingRange(forecast.hours(date))?.let { (low, high) ->
                // Arrotondato a 50 m: il dato non è più preciso di così.
                val a = Math.round(low / 50) * 50; val b = Math.round(high / 50) * 50
                if (a == b) "$a m" else "$a–$b m"
            })
            ValueRow(stringResource(R.string.meteo_alba_tramonto), joined(day.sunrise?.let(::clock), day.sunset?.let(::clock)))
        }
    }
}

@Composable
private fun HoursCard(forecast: PointForecast, date: LocalDate) {
    val hours = forecast.hours(date)
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.meteo_ora_per_ora, formatDate(date, currentLocale())), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                Text(stringResource(R.string.meteo_ora_locale), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (hours.isEmpty()) { Muted(stringResource(R.string.non_disponibile)); return@Column }
            hours.chunked(6).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { HourCell(it, Modifier.weight(1f)) }
                    repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text(stringResource(R.string.meteo_legenda_ore), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HourCell(hour: HourWeather, modifier: Modifier) {
    val missing = stringResource(R.string.non_disponibile)
    val time = clock(hour.time).substringBefore(':')
    val temp = hour.temp?.let(::formatTemp)
    val probability = hour.precipitationProbability?.let { "$it %" }
    val spoken = stringResource(R.string.meteo_ora_letta, time, skyLabel(hour.code), temp ?: missing, probability ?: missing)
    Surface(modifier.clearAndSetSemantics { contentDescription = spoken }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(skyIcon(hour.code, hour.isDay != false), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(temp ?: "—", style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(probability ?: "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** Vento per fascia oraria e, per un'uscita di un giorno con ora di partenza e durata stimata, il margine di luce al rientro. */
@Composable
private fun WindCard(forecast: PointForecast, date: LocalDate, trip: Trip?, route: Route?) {
    val locale = currentLocale()
    val hours = forecast.hours(date)
    val sunset = forecast.day(date)?.sunset
    @Composable fun slot(from: Int, to: Int): String? = WeatherRules.windBetween(hours, from, to)?.let { (wind, gusts) ->
        if (gusts != null) stringResource(R.string.meteo_vento_fascia, kmh(wind, locale), String.format(locale, "%.0f", gusts)) else kmh(wind, locale)
    }
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 6.dp)) {
            Text(stringResource(R.string.meteo_vento_titolo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 10.dp).semantics { heading() })
            ValueRow(stringResource(R.string.meteo_mattina), slot(7, 11))
            ValueRow(stringResource(R.string.meteo_pomeriggio), slot(12, 16))
            val minutes = route?.takeIf { trip != null && TripRules.lengthInDays(trip) == 1 && date == trip.startDate }?.let { estimatedMinutes(it.distanceMeters, it.ascentMeters, it.descentMeters, it.partialElevation, Pace.valueOf(trip!!.pace.name)) }
            val startTime = trip?.startTime
            if (minutes != null && startTime != null) {
                val returnAt = LocalDateTime.of(date, startTime).plusMinutes(minutes.toLong())
                ValueRow(stringResource(R.string.meteo_rientro, formatTime(startTime)), formatTime(returnAt.toLocalTime()))
                val margin = WeatherRules.daylightMarginMinutes(returnAt, sunset)
                ValueRow(stringResource(R.string.meteo_tramonto), when {
                    margin == null -> null
                    margin >= 0 -> stringResource(R.string.meteo_margine, clock(sunset!!), formatDuration(margin.toInt()))
                    else -> stringResource(R.string.meteo_dopo_tramonto, clock(sunset!!), formatDuration(-margin.toInt()))
                })
                Text(stringResource(R.string.meteo_rientro_nota), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
            } else ValueRow(stringResource(R.string.meteo_tramonto), sunset?.let(::clock))
        }
    }
}

@Composable
private fun TooEarlyContent(plan: Plan.TooEarly, s: WeatherViewModel.State, onRetry: () -> Unit) {
    val locale = currentLocale()
    Notice(stringResource(R.string.meteo_non_ancora_titolo), stringResource(R.string.meteo_non_ancora_testo, dayMonth(plan.start, locale), dayMonth(plan.availableFrom, locale)), AppIcons2.Calendar)
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.meteo_clima_titolo), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            val climate = s.climate
            when {
                climate != null -> ClimateRows(climate, kindLabel(plan.points.last().kind).lowercase(locale))
                s.climateFailed -> {
                    Muted(stringResource(R.string.meteo_clima_errore))
                    SecondaryButton(stringResource(R.string.riprova), onRetry, icon = AppIcons4.Refresh)
                }
                else -> LoadingRow(stringResource(R.string.meteo_clima_in_arrivo))
            }
        }
    }
}

@Composable
private fun ClimateRows(climate: Climate, pointName: String) {
    val locale = currentLocale()
    val from = dayMonth(LocalDate.parse(climate.from), locale)
    val to = dayMonth(LocalDate.parse(climate.to), locale)
    Muted(
        climate.elevation?.let { stringResource(R.string.meteo_clima_testo, from, to, climate.years, formatMeters(it, locale), pointName) }
            ?: stringResource(R.string.meteo_clima_testo_senza_quota, from, to, climate.years, pointName),
    )
    Column {
        ValueRow(stringResource(R.string.meteo_clima_minima), climate.typicalMin?.let { formatTemp(it) + "C" })
        ValueRow(stringResource(R.string.meteo_clima_massima), climate.typicalMax?.let { formatTemp(it) + "C" })
        ValueRow(stringResource(R.string.meteo_clima_giorni_pioggia), if (climate.countedDays > 0) stringResource(R.string.meteo_clima_su, climate.wetDays, climate.countedDays) else null)
    }
    Text(stringResource(R.string.meteo_fonte_archivio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
