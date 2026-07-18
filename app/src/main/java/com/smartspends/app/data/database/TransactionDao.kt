package com.smartspends.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, time DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE (:searchQuery = '' OR description LIKE '%' || :searchQuery || '%' OR bank LIKE '%' || :searchQuery || '%')
          AND (:type IS NULL OR type = :type)
          AND (:category IS NULL OR category = :category)
          AND (:bank IS NULL OR bank = :bank)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY 
          CASE WHEN :sortBy = 'DATE_DESC' THEN date END DESC,
          CASE WHEN :sortBy = 'DATE_DESC' THEN time END DESC,
          CASE WHEN :sortBy = 'DATE_ASC' THEN date END ASC,
          CASE WHEN :sortBy = 'DATE_ASC' THEN time END ASC,
          CASE WHEN :sortBy = 'AMOUNT_DESC' THEN amount END DESC,
          CASE WHEN :sortBy = 'AMOUNT_ASC' THEN amount END ASC
    """)
    fun getTransactionsFiltered(
        searchQuery: String,
        type: String?,
        category: String?,
        bank: String?,
        startDate: String?,
        endDate: String?,
        sortBy: String
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT COUNT(*) FROM transactions WHERE amount = :amount AND date = :date AND time = :time AND bank = :bank")
    suspend fun checkDuplicate(amount: Double, date: String, time: String, bank: String?): Int

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT SUM(amount) FROM transactions WHERE date = :date AND type = 'INCOME'")
    fun getIncomeSumForDate(date: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE date = :date AND type = 'EXPENSE'")
    fun getExpenseSumForDate(date: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE date BETWEEN :startDate AND :endDate AND type = 'INCOME'")
    fun getIncomeSumForPeriod(startDate: String, endDate: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE date BETWEEN :startDate AND :endDate AND type = 'EXPENSE'")
    fun getExpenseSumForPeriod(startDate: String, endDate: String): Flow<Double?>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>

    @Query("SELECT MAX(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun getLargestExpense(): Flow<Double?>

    @Query("SELECT MAX(amount) FROM transactions WHERE type = 'INCOME'")
    fun getLargestIncome(): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'EXPENSE' GROUP BY category")
    fun getCategoryExpenses(): Flow<List<CategorySum>>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate GROUP BY category")
    fun getCategoryExpensesForPeriod(startDate: String, endDate: String): Flow<List<CategorySum>>

    @Query("SELECT date, SUM(amount) as total FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate GROUP BY date ORDER BY date ASC")
    fun getDailySpendingForPeriod(startDate: String, endDate: String): Flow<List<DailySum>>

    @Query("SELECT DISTINCT bank FROM transactions WHERE bank IS NOT NULL AND bank != ''")
    fun getUniqueBanks(): Flow<List<String>>
}

data class CategorySum(
    val category: String,
    val total: Double
)

data class DailySum(
    val date: String,
    val total: Double
)
