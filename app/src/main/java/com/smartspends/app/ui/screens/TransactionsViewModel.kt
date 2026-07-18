package com.smartspends.app.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.domain.repository.TransactionRepository
import com.smartspends.app.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    application: Application,
    private val repository: TransactionRepository,
    private val getTransactionsUseCase: GetTransactionsUseCase
) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences("smartspends_prefs", Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedType = MutableStateFlow<String?>(null)
    val selectedType = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedBank = MutableStateFlow<String?>(null)
    val selectedBank = _selectedBank.asStateFlow()

    private val _startDate = MutableStateFlow<String?>(null)
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<String?>(null)
    val endDate = _endDate.asStateFlow()

    private val _sortBy = MutableStateFlow("DATE_DESC")
    val sortBy = _sortBy.asStateFlow()

    private val _currencySymbol = MutableStateFlow("₹")
    val currencySymbol = _currencySymbol.asStateFlow()

    val uniqueBanks: StateFlow<List<String>> = repository.getUniqueBanks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<TransactionEntity>> = combine(
        _searchQuery, _selectedType, _selectedCategory, _selectedBank, _startDate, _endDate, _sortBy
    ) { array: Array<String?> ->
        val query = array[0] ?: ""
        val type = array[1]
        val category = array[2]
        val bank = array[3]
        val start = array[4]
        val end = array[5]
        val sort = array[6] ?: "DATE_DESC"
        Params(query, type, category, bank, start, end, sort)
    }.flatMapLatest { params ->
        getTransactionsUseCase(
            query = params.query,
            type = params.type,
            category = params.category,
            bank = params.bank,
            startDate = params.start,
            endDate = params.end,
            sortBy = params.sort
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: String?) {
        _selectedType.value = type
    }

    fun setFilterCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setFilterBank(bank: String?) {
        _selectedBank.value = bank
    }

    fun setDateRange(start: String?, end: String?) {
        _startDate.value = start
        _endDate.value = end
    }

    fun setSortBy(sort: String) {
        _sortBy.value = sort
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    private data class Params(
        val query: String,
        val type: String?,
        val category: String?,
        val bank: String?,
        val start: String?,
        val end: String?,
        val sort: String
    )
}
