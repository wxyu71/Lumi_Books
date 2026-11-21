package com.huangder.lumibooks.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huangder.lumibooks.data.local.DataStoreManager
import com.huangder.lumibooks.domain.repository.BookRepository
import com.huangder.lumibooks.domain.repository.ReadingRepository
import com.huangder.lumibooks.util.localDateChanges
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class DailyReading(val date: String, val duration: Long, val dayOfWeek: String)

internal data class StatisticsUiState(
    val selection: StatisticsSelection = StatisticsSelection(),
    val insights: ReadingInsights? = null,
    val comparison: PeriodComparison? = null,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class StatisticsViewModel private constructor(
    private val readingRepository: ReadingRepository,
    private val bookRepository: BookRepository,
    private val dailyGoals: Flow<Int>,
    private val dates: Flow<LocalDate>
) : ViewModel() {
    // Production observes persistent settings and today's date. UI tests use
    // deterministic sources instead of sharing the Robolectric DataStore singleton.
    @Inject constructor(
        readingRepository: ReadingRepository,
        bookRepository: BookRepository,
        dataStoreManager: DataStoreManager
    ) : this(readingRepository, bookRepository, dataStoreManager.dailyGoal, localDateChanges())

    companion object {
        internal fun forTesting(
            readingRepository: ReadingRepository,
            bookRepository: BookRepository,
            dailyGoals: Flow<Int>,
            dates: Flow<LocalDate>
        ): StatisticsViewModel = StatisticsViewModel(readingRepository, bookRepository, dailyGoals, dates)
    }

    private val selection = MutableStateFlow(StatisticsSelection())
    val uiState: StateFlow<StatisticsUiState> = combine(selection, dates, dailyGoals) { selected, today, goal ->
        Triple(selected, today, goal)
    }.flatMapLatest { (selected, today, goal) ->
        val pairs = comparisonPeriods(selected, today)
        val comparison: Flow<PeriodComparison?> = if (pairs == null) flowOf(null) else combine(
            readingRepository.getTotalDurationBetweenDates(pairs.first.start.readingDate(), pairs.first.endInclusive.readingDate()),
            readingRepository.getTotalDurationBetweenDates(pairs.second.start.readingDate(), pairs.second.endInclusive.readingDate())
        ) { current, previous -> PeriodComparison(pairs.first, pairs.second, current ?: 0, previous ?: 0) }
        combine(observeReadingInsights(readingRepository, bookRepository, selected.range(today), today, goal,
            selected.scope == StatisticsScope.ALL), comparison) { insights, compared -> StatisticsUiState(selected, insights, compared) }
            .onStart { emit(StatisticsUiState(selected)) }
            .catch { emit(StatisticsUiState(selected, error = it.message ?: "")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    fun selectScope(scope: StatisticsScope) { selection.update { it.copy(scope = scope) } }
    fun shift(delta: Int) { selection.update { it.shift(delta, LocalDate.now()) } }
}
