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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    getDashboardStatsUseCase: GetDashboardStatsUseCase,
    getTransactionsUseCase: GetTransactionsUseCase,
    detectRecurringTransactionsUseCase: DetectRecurringTransactionsUseCase
) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences("smartspends_prefs", Context.MODE_PRIVATE)

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
