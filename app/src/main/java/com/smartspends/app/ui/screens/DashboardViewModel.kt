package com.smartspends.app.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.domain.usecase.DashboardStats
import com.smartspends.app.domain.usecase.DetectRecurringTransactionsUseCase
import com.smartspends.app.domain.usecase.GetDashboardStatsUseCase
import com.smartspends.app.domain.usecase.GetTransactionsUseCase
import com.smartspends.app.domain.usecase.RecurringTransaction
import com.smartspends.app.domain.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class CustomStats(
    val income: Double,
    val expense: Double,
    val savings: Double
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val repository: TransactionRepository,
    getDashboardStatsUseCase: GetDashboardStatsUseCase,
    getTransactionsUseCase: GetTransactionsUseCase,
    detectRecurringTransactionsUseCase: DetectRecurringTransactionsUseCase
) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences("smartspends_prefs", Context.MODE_PRIVATE)

    private val _customStartDate = MutableStateFlow<String?>(null)
    val customStartDate = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow<String?>(null)
    val customEndDate = _customEndDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val customStats: StateFlow<CustomStats?> = combine(_customStartDate, _customEndDate) { start, end ->
        if (start != null && end != null) Pair(start, end) else null
    }.flatMapLatest { range ->
        if (range != null) {
            combine(
                repository.getIncomeSumForPeriod(range.first, range.second),
                repository.getExpenseSumForPeriod(range.first, range.second)
            ) { inc, exp ->
                val income = inc ?: 0.0
                val expense = exp ?: 0.0
                CustomStats(income, expense, income - expense)
            }
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setCustomDateRange(start: String?, end: String?) {
        _customStartDate.value = start
        _customEndDate.value = end
    }

    val stats: StateFlow<DashboardStats> = getDashboardStatsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0)
        )

    val recentTransactions: StateFlow<List<TransactionEntity>> = getTransactionsUseCase(sortBy = "DATE_DESC")
        .map { it.take(5) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recurringTransactions: StateFlow<List<RecurringTransaction>> = detectRecurringTransactionsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
