package com.huangder.lumibooks.ui.statistics

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/** Calendar boundaries used by reading history, including DST and leap years. */
class StatisticsPeriodEdgeCasesTest {
    @Test fun februaryLeapDayAndNonLeapDayArePreserved() {
        assertEquals(LocalDate.of(2024, 2, 29), StatisticsPeriod.month(2024, 1).endInclusive)
        assertEquals(LocalDate.of(2025, 2, 28), StatisticsPeriod.month(2025, 1).endInclusive)
    }

    @Test fun weeklyHistoryUsesSundayToSaturdayAcrossYearBoundary() {
        val range = StatisticsPeriod.week(LocalDate.of(2027, 1, 1), offset = 0)
        assertEquals(LocalDate.of(2026, 12, 27), range.start)
        assertEquals(LocalDate.of(2027, 1, 2), range.endInclusive)
        assertEquals(7, range.days.size)
    }

    @Test fun endMillisIsExclusiveAndRespectsSpringForward() {
        val date = LocalDate.of(2026, 3, 8)
        val period = StatisticsPeriod(date, date)
        val zone = ZoneId.of("America/Los_Angeles")
        assertEquals(23L * 60 * 60 * 1_000, period.endMillis(zone) - period.startMillis(zone))
    }

    @Test fun historicalMonthShiftCanCrossDecemberAndCannotPassToday() {
        val today = LocalDate.of(2026, 11, 21)
        val jan = StatisticsSelection(scope = StatisticsScope.MONTH, year = 2026, month0 = 0)
        val december = jan.shift(-1, today)
        assertEquals(2025, december.year)
        assertEquals(11, december.month0)
        val current = StatisticsSelection(scope = StatisticsScope.MONTH, year = 2026, month0 = 10)
        assertEquals(current, current.shift(1, today))
    }
}
