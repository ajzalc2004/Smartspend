package com.smartspends.app.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartspends.app.domain.usecase.AnalyticsStats
import com.smartspends.app.domain.usecase.GetAnalyticsStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    application: Application,
    getAnalyticsStatsUseCase: GetAnalyticsStatsUseCase
) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences("smartspends_prefs", Context.MODE_PRIVATE)

    val stats: StateFlow<AnalyticsStats?> = getAnalyticsStatsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
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
