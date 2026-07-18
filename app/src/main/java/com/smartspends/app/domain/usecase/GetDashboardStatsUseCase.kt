package com.smartspends.app.domain.usecase

import com.smartspends.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardStats(
    val todayIncome: Double,
    val todayExpense: Double,
    val todaySavings: Double,
    
    val weekIncome: Double,
    val weekExpense: Double,
    val weekSavings: Double,

    val monthIncome: Double,
    val monthExpense: Double,
    val monthSavings: Double,

    val totalTransactions: Int,
    val largestExpense: Double,
    val largestIncome: Double,
    val averageDailySpending: Double
)

class GetDashboardStatsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<DashboardStats> {
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val todayStr = today.format(dateFormatter)
        
        // Start of week (Monday)
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val startOfWeekStr = startOfWeek.format(dateFormatter)
        
        // Start of month
        val startOfMonth = today.withDayOfMonth(1)
        val startOfMonthStr = startOfMonth.format(dateFormatter)
        
        // End of period (today)
        val endOfPeriodStr = today.format(dateFormatter)

        val todayIncomeFlow = repository.getIncomeSumForDate(todayStr)
        val todayExpenseFlow = repository.getExpenseSumForDate(todayStr)
        
        val weekIncomeFlow = repository.getIncomeSumForPeriod(startOfWeekStr, endOfPeriodStr)
        val weekExpenseFlow = repository.getExpenseSumForPeriod(startOfWeekStr, endOfPeriodStr)

        val monthIncomeFlow = repository.getIncomeSumForPeriod(startOfMonthStr, endOfPeriodStr)
        val monthExpenseFlow = repository.getExpenseSumForPeriod(startOfMonthStr, endOfPeriodStr)

        val totalTxCountFlow = repository.getTransactionCount()
        val maxExpenseFlow = repository.getLargestExpense()
        val maxIncomeFlow = repository.getLargestIncome()

        // Average daily spending in last 30 days
        val start30DaysAgo = today.minusDays(30)
        val start30DaysAgoStr = start30DaysAgo.format(dateFormatter)
        val expenses30DaysFlow = repository.getExpenseSumForPeriod(start30DaysAgoStr, endOfPeriodStr)

        return combine(
            todayIncomeFlow, todayExpenseFlow,
            weekIncomeFlow, weekExpenseFlow,
            monthIncomeFlow, monthExpenseFlow,
            totalTxCountFlow, maxExpenseFlow, maxIncomeFlow,
            expenses30DaysFlow
        ) { array ->
            val todayInc = array[0] as Double? ?: 0.0
            val todayExp = array[1] as Double? ?: 0.0
            
            val weekInc = array[2] as Double? ?: 0.0
            val weekExp = array[3] as Double? ?: 0.0

            val monthInc = array[4] as Double? ?: 0.0
            val monthExp = array[5] as Double? ?: 0.0

            val txCount = array[6] as Int? ?: 0
            val maxExp = array[7] as Double? ?: 0.0
            val maxInc = array[8] as Double? ?: 0.0
            val exp30Days = array[9] as Double? ?: 0.0

            val avgDaily = exp30Days / 30.0

            DashboardStats(
                todayIncome = todayInc,
                todayExpense = todayExp,
                todaySavings = todayInc - todayExp,
                weekIncome = weekInc,
                weekExpense = weekExp,
                weekSavings = weekInc - weekExp,
                monthIncome = monthInc,
                monthExpense = monthExp,
                monthSavings = monthInc - monthExp,
                totalTransactions = txCount,
                largestExpense = maxExp,
                largestIncome = maxInc,
                averageDailySpending = avgDaily
            )
        }
    }
}
