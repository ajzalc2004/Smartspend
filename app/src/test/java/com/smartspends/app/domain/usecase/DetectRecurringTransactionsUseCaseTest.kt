package com.smartspends.app.domain.usecase

import com.smartspends.app.data.database.CategorySum
import com.smartspends.app.data.database.DailySum
import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectRecurringTransactionsUseCaseTest {

    private class FakeTransactionRepository(private val transactions: List<TransactionEntity>) : TransactionRepository {
        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
        
        override fun getTransactionsFiltered(
            query: String, type: String?, category: String?, bank: String?, 
            startDate: String?, endDate: String?, sortBy: String
        ): Flow<List<TransactionEntity>> = flowOf(emptyList())
        
        override suspend fun getTransactionById(id: Long): TransactionEntity? = null
        override suspend fun insertTransaction(transaction: TransactionEntity): Long = 0L
        override suspend fun checkDuplicate(amount: Double, date: String, time: String, bank: String?): Int = 0
        override suspend fun deleteTransaction(transaction: TransactionEntity) {}
        override fun getIncomeSumForDate(date: String): Flow<Double?> = flowOf(0.0)
        override fun getExpenseSumForDate(date: String): Flow<Double?> = flowOf(0.0)
        override fun getIncomeSumForPeriod(startDate: String, endDate: String): Flow<Double?> = flowOf(0.0)
        override fun getExpenseSumForPeriod(startDate: String, endDate: String): Flow<Double?> = flowOf(0.0)
        override fun getTransactionCount(): Flow<Int> = flowOf(0)
        override fun getLargestExpense(): Flow<Double?> = flowOf(0.0)
        override fun getLargestIncome(): Flow<Double?> = flowOf(0.0)
        override fun getCategoryExpenses(): Flow<List<CategorySum>> = flowOf(emptyList())
        override fun getDailySpendingForPeriod(startDate: String, endDate: String): Flow<List<DailySum>> = flowOf(emptyList())
        override fun getUniqueBanks(): Flow<List<String>> = flowOf(emptyList())
    }

    @Test
    fun testDetectRecurring_NetflixSubscription() = runTest {
        val transactions = listOf(
            TransactionEntity(1, 199.0, "EXPENSE", "Entertainment", null, null, "2026-05-01", "12:00", "Card", "Netflix Subscription", "MANUAL", null),
            TransactionEntity(2, 199.0, "EXPENSE", "Entertainment", null, null, "2026-06-01", "12:00", "Card", "Netflix Subscription", "MANUAL", null),
            TransactionEntity(3, 199.0, "EXPENSE", "Entertainment", null, null, "2026-07-01", "12:00", "Card", "Netflix Subscription", "MANUAL", null)
        )
        val fakeRepo = FakeTransactionRepository(transactions)
        val useCase = DetectRecurringTransactionsUseCase(fakeRepo)

        val result = useCase().first()

        assertEquals(1, result.size)
        val item = result.first()
        assertEquals("Netflix Subscription", item.description)
        assertEquals(199.0, item.amount, 0.0)
        assertEquals("Monthly", item.frequency)
    }
}
