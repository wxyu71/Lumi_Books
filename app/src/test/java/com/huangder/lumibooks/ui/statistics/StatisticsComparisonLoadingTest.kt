package com.huangder.lumibooks.ui.statistics

import java.time.LocalDate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class StatisticsComparisonLoadingTest {
    private val date = LocalDate.of(2026, 11, 21)
    private val selection = StatisticsSelection(
        scope = StatisticsScope.MONTH, year = date.year, month0 = date.monthValue - 1
    )
    private val data = ReadingInsights(StatisticsPeriod.month(2026, 10), date)

    @Test fun missingComparisonEmissionNeverBlocksPrimaryReadingStatistics() = runBlocking {
        // A comparison source which completes without emitting must not blank
        // the user's main statistics or prevent changing to another month.
        val result = withTimeout(2_000) {
            combineStatisticsResults(selection, flowOf(data), emptyFlow()).first()
        }
        assertEquals(StatisticsScope.MONTH, result.selection.scope)
        assertSame(data, result.insights)
        assertNull(result.comparison)
    }

    @Test fun availableComparisonStillRendersAlongsidePrimaryStatistics() = runBlocking {
        val prev = StatisticsPeriod.month(2026, 9)
        val cmp = PeriodComparison(data.period, prev, 1500, 1000)
        val updated = withTimeout(2_000) {
            combineStatisticsResults(selection, flowOf(data), flowOf(cmp)).first {
                it.comparison != null
            }
        }
        assertSame(data, updated.insights)
        assertSame(cmp, updated.comparison)
    }
}
