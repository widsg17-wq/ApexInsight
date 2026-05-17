package com.example.investmentassistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.investmentassistant.data.AppDatabase
import com.example.investmentassistant.data.repository.CalendarRepository
import com.example.investmentassistant.model.CalendarEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CalendarUiState(
    val isLoading: Boolean = false,
    val todayEvents: List<CalendarEvent> = emptyList(),
    val tomorrowEvents: List<CalendarEvent> = emptyList(),
    val thisWeekEvents: List<CalendarEvent> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(ZoneId.of("Asia/Seoul")),
    val monthlyEvents: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val selectedDate: LocalDate? = null,
    val selectedDateEvents: List<CalendarEvent> = emptyList(),
    val error: String? = null,
)

class CalendarViewModel(app: Application) : AndroidViewModel(app) {

    private val kst = ZoneId.of("Asia/Seoul")
    private val repository by lazy {
        CalendarRepository(AppDatabase.getDatabase(app).calendarEventDao())
    }

    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            var fetchError: String? = null
            try {
                repository.fetchAndCacheCalendar()
            } catch (e: Exception) {
                fetchError = e.message
            }
            refreshFromDb(fetchError)
        }
    }

    fun selectDate(date: LocalDate) {
        val events = _uiState.value.monthlyEvents[date] ?: emptyList()
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            selectedDateEvents = events,
        )
    }

    fun changeMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            val monthlyEvents = buildMonthlyEvents(yearMonth)
            val selectedDate = _uiState.value.selectedDate
            _uiState.value = _uiState.value.copy(
                currentMonth = yearMonth,
                monthlyEvents = monthlyEvents,
                selectedDateEvents = selectedDate?.let { monthlyEvents[it] } ?: emptyList(),
            )
        }
    }

    private suspend fun refreshFromDb(fetchError: String? = null) {
        val today = LocalDate.now(kst)
        val tomorrow = today.plusDays(1)
        val weekEnd = today.plusDays(8)

        fun dayMs(date: LocalDate) = date.atStartOfDay(kst).toInstant().toEpochMilli()

        val todayEvents = repository.getEventsForDateRange(dayMs(today), dayMs(tomorrow))
        val tomorrowEvents = repository.getEventsForDateRange(dayMs(tomorrow), dayMs(tomorrow.plusDays(1)))
        val thisWeekEvents = repository.getEventsForDateRange(dayMs(today.plusDays(2)), dayMs(weekEnd))

        val currentMonth = _uiState.value.currentMonth
        val monthlyEvents = buildMonthlyEvents(currentMonth)
        val selectedDate = _uiState.value.selectedDate

        _uiState.value = CalendarUiState(
            isLoading = false,
            todayEvents = todayEvents,
            tomorrowEvents = tomorrowEvents,
            thisWeekEvents = thisWeekEvents,
            currentMonth = currentMonth,
            monthlyEvents = monthlyEvents,
            selectedDate = selectedDate,
            selectedDateEvents = selectedDate?.let { monthlyEvents[it] } ?: emptyList(),
            error = if (fetchError != null && todayEvents.isEmpty() && monthlyEvents.isEmpty()) fetchError else null,
        )
    }

    private suspend fun buildMonthlyEvents(yearMonth: YearMonth): Map<LocalDate, List<CalendarEvent>> {
        fun toMs(date: LocalDate) = date.atStartOfDay(kst).toInstant().toEpochMilli()
        val start = toMs(yearMonth.atDay(1))
        val end = toMs(yearMonth.atEndOfMonth().plusDays(1))
        return repository.getEventsForDateRange(start, end).groupBy { it.localDate }
    }
}
