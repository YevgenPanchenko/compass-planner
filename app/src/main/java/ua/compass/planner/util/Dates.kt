package ua.compass.planner.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object Dates {
    fun today(): LocalDate = LocalDate.now()

    fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    fun weekStart(date: LocalDate, firstDay: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstDay))

    fun monthIndex(month: YearMonth): Int = month.year * 12 + month.monthValue - 1

    fun yearMonth(index: Int): YearMonth = YearMonth.of(Math.floorDiv(index, 12), Math.floorMod(index, 12) + 1)

    fun time(minutes: Int): String = "%02d:%02d".format(Locale.ROOT, minutes / 60, minutes % 60)

    fun format(date: LocalDate, pattern: String, locale: Locale): String =
        date.format(DateTimeFormatter.ofPattern(pattern, locale))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    fun weekRange(start: LocalDate, locale: Locale): String =
        format(start, "d MMM", locale) + " – " + format(start.plusDays(6), "d MMM", locale)

    fun dateTime(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())

    fun millis(date: LocalDate, minutes: Int): Long =
        date.atTime(minutes / 60, minutes % 60).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** First day of the week chosen in the settings. */
val LocalFirstDayOfWeek = staticCompositionLocalOf { DayOfWeek.MONDAY }
