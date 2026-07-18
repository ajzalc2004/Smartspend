package com.smartspends.app.domain.usecase

import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: TransactionEntity): Long {
        require(transaction.amount > 0) { "Amount must be greater than zero" }
        require(transaction.category.isNotEmpty()) { "Category must not be empty" }
        require(transaction.date.isNotEmpty()) { "Date must not be empty" }
        return repository.insertTransaction(transaction)
    }
}
