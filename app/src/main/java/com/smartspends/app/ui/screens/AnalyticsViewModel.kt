package com.smartspends.app.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartspends.app.domain.usecase.AnalyticsStats
import com.smartspends.app.domain.usecase.GetAnalyticsStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    application: Application,
    private val getAnalyticsStatsUseCase: GetAnalyticsStatsUseCase
) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences("smartspends_prefs", Context.MODE_PRIVATE)

    private val _startDate = MutableStateFlow<String?>(null)
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<String?>(null)
    val endDate = _endDate.asStateFlow()

    private val _selectedPeriodTab = MutableStateFlow(2) // 0: Today, 1: Week, 2: Month, 3: Custom
    val selectedPeriodTab = _selectedPeriodTab.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val stats: StateFlow<AnalyticsStats?> = combine(_selectedPeriodTab, _startDate, _endDate) { tab, start, end ->
        val today = java.time.LocalDate.now()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val endStr = today.format(formatter)
        val startStr = when (tab) {
            0 -> today.format(formatter)
            1 -> today.minusDays(7).format(formatter)
            2 -> today.minusDays(30).format(formatter)
            else -> start ?: today.minusDays(30).format(formatter)
        }
        val finalEnd = if (tab == 3) (end ?: endStr) else endStr
        Pair(startStr, finalEnd)
    }.flatMapLatest { (start, end) ->
        getAnalyticsStatsUseCase(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setPeriodTab(tab: Int) {
        _selectedPeriodTab.value = tab
    }

    fun setCustomDateRange(start: String?, end: String?) {
        _startDate.value = start
        _endDate.value = end
    }

    private val _currencySymbol = MutableStateFlow("₹")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    init {
        updateCurrencySymbol()
    }

    fun updateCurrencySymbol() {
        val code = sharedPref.getString("currency_code", "INR") ?: "INR"
        _currencySymbol.value = when (code) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }
    }
}
