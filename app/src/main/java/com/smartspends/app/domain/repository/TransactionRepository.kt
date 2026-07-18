package com.smartspends.app.domain.repository

import com.smartspends.app.data.database.CategorySum
import com.smartspends.app.data.database.DailySum
import com.smartspends.app.data.database.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    fun getTransactionsFiltered(
        query: String,
        type: String?,
        category: String?,
        bank: String?,
        startDate: String?,
        endDate: String?,
        sortBy: String
    ): Flow<List<TransactionEntity>>

    suspend fun getTransactionById(id: Long): TransactionEntity?
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun checkDuplicate(amount: Double, date: String, time: String, bank: String?): Int
    suspend fun deleteTransaction(transaction: TransactionEntity)

    fun getIncomeSumForDate(date: String): Flow<Double?>
    fun getExpenseSumForDate(date: String): Flow<Double?>
    fun getIncomeSumForPeriod(startDate: String, endDate: String): Flow<Double?>
    fun getExpenseSumForPeriod(startDate: String, endDate: String): Flow<Double?>
    
    fun getTransactionCount(): Flow<Int>
    fun getLargestExpense(): Flow<Double?>
    fun getLargestIncome(): Flow<Double?>
    
    fun getCategoryExpenses(): Flow<List<CategorySum>>
    fun getCategoryExpensesForPeriod(startDate: String, endDate: String): Flow<List<CategorySum>>
    fun getDailySpendingForPeriod(startDate: String, endDate: String): Flow<List<DailySum>>
    
    fun getUniqueBanks(): Flow<List<String>>
}
