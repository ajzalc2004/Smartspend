package com.smartspends.app.data.repository

import com.smartspends.app.data.database.CategorySum
import com.smartspends.app.data.database.DailySum
import com.smartspends.app.data.database.TransactionDao
import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactionsFlow()

    override fun getTransactionsFiltered(
        query: String,
        type: String?,
        category: String?,
        bank: String?,
        startDate: String?,
        endDate: String?,
        sortBy: String
    ): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsFiltered(query, type, category, bank, startDate, endDate, sortBy)

    override suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    override suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    override suspend fun checkDuplicate(amount: Double, date: String, time: String, bank: String?): Int =
        transactionDao.checkDuplicate(amount, date, time, bank)

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    override fun getIncomeSumForDate(date: String): Flow<Double?> =
        transactionDao.getIncomeSumForDate(date)

    override fun getExpenseSumForDate(date: String): Flow<Double?> =
        transactionDao.getExpenseSumForDate(date)

    override fun getIncomeSumForPeriod(startDate: String, endDate: String): Flow<Double?> =
        transactionDao.getIncomeSumForPeriod(startDate, endDate)

    override fun getExpenseSumForPeriod(startDate: String, endDate: String): Flow<Double?> =
        transactionDao.getExpenseSumForPeriod(startDate, endDate)

    override fun getTransactionCount(): Flow<Int> =
        transactionDao.getTransactionCount()

    override fun getLargestExpense(): Flow<Double?> =
        transactionDao.getLargestExpense()

    override fun getLargestIncome(): Flow<Double?> =
        transactionDao.getLargestIncome()

    override fun getCategoryExpenses(): Flow<List<CategorySum>> =
        transactionDao.getCategoryExpenses()

    override fun getDailySpendingForPeriod(startDate: String, endDate: String): Flow<List<DailySum>> =
        transactionDao.getDailySpendingForPeriod(startDate, endDate)

    override fun getUniqueBanks(): Flow<List<String>> =
        transactionDao.getUniqueBanks()
}
