package com.smartspends.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    application: Application,
    private val addTransactionUseCase: AddTransactionUseCase
) : AndroidViewModel(application) {

    private val _amount = MutableStateFlow("")
    val amount = _amount.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _type = MutableStateFlow("EXPENSE")
    val type = _type.asStateFlow()

    private val _category = MutableStateFlow("Food")
    val category = _category.asStateFlow()

    private val _date = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
    val date = _date.asStateFlow()

    private val _time = MutableStateFlow(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
    val time = _time.asStateFlow()

    private val _transactionMode = MutableStateFlow("UPI")
    val transactionMode = _transactionMode.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess = _saveSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun setAmount(value: String) {
        if (value.isEmpty() || value.toDoubleOrNull() != null || value.endsWith(".")) {
            _amount.value = value
        }
    }

    fun setDescription(value: String) {
        _description.value = value
    }

    fun setType(value: String) {
        _type.value = value
        if (value == "EXPENSE" && _category.value !in listOf("Food", "Fuel", "Shopping", "Travel", "Bills", "Entertainment", "Healthcare", "Education", "Rent", "Other")) {
            _category.value = "Food"
        } else if (value == "INCOME" && _category.value !in listOf("Salary", "Interest", "Freelance", "Gift", "Refund", "Investment", "Other")) {
            _category.value = "Salary"
        }
    }

    fun setCategory(value: String) {
        _category.value = value
    }

    fun setDate(value: String) {
        _date.value = value
    }

    fun setTime(value: String) {
        _time.value = value
    }

    fun setTransactionMode(value: String) {
        _transactionMode.value = value
    }

    fun saveTransaction() {
        val amt = _amount.value.toDoubleOrNull()
        if (amt == null || amt <= 0.0) {
            _errorMessage.value = "Please enter a valid amount greater than 0"
            return
        }
        val desc = _description.value.trim()
        if (desc.isEmpty()) {
            _errorMessage.value = "Please enter a description"
            return
        }

        viewModelScope.launch {
            try {
                val entity = TransactionEntity(
                    amount = amt,
                    type = _type.value,
                    category = _category.value,
                    bank = null,
                    accountNumberMasked = null,
                    date = _date.value,
                    time = _time.value,
                    transactionMode = _transactionMode.value,
                    description = desc,
                    source = "MANUAL",
                    smsBody = null
                )
                addTransactionUseCase(entity)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to save transaction"
            }
        }
    }

    fun clearErrors() {
        _errorMessage.value = null
    }
}
