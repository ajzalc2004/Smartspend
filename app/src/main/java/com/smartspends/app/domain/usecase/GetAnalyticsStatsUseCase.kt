package com.smartspends.app.domain.usecase

import com.smartspends.app.data.database.CategorySum
import com.smartspends.app.data.database.DailySum
import com.smartspends.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MonthlyBarData(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

data class AnalyticsStats(
    val categoryExpenses: List<CategorySum>,
    val dailyExpenses: List<DailySum>,
    val monthlyData: List<MonthlyBarData>,
    val spendingTrend: List<DailySum>
)

class GetAnalyticsStatsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(startDate: String? = null, endDate: String? = null): Flow<AnalyticsStats> {
        val today = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        val endStr = endDate ?: today.format(dateFormatter)
        val startStr = startDate ?: today.minusDays(30).format(dateFormatter)
        
        val categoryExpensesFlow = repository.getCategoryExpensesForPeriod(startStr, endStr)
        val dailyExpensesFlow = repository.getDailySpendingForPeriod(startStr, endStr)
        
        val start6MonthsAgo = today.minusMonths(6).withDayOfMonth(1)
        val start6MonthsAgoStr = start6MonthsAgo.format(dateFormatter)
        
        val transactionsFlow = repository.getTransactionsFiltered(
            query = "",
            type = null,
            category = null,
            bank = null,
            startDate = start6MonthsAgoStr,
            endDate = endStr,
            sortBy = "DATE_ASC"
        )

        return combine(categoryExpensesFlow, dailyExpensesFlow, transactionsFlow) { categorySums, dailySums, transactions ->
            val monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
            val monthlyMap = LinkedHashMap<String, Pair<Double, Double>>()

            // Pre-populate last 6 months in chronological order
            for (i in 5 downTo 0) {
                val m = today.minusMonths(i.toLong())
                val label = m.format(monthFormatter)
                monthlyMap[label] = Pair(0.0, 0.0)
            }

            for (tx in transactions) {
                try {
                    val date = LocalDate.parse(tx.date, dateFormatter)
                    val label = date.format(monthFormatter)
                    val existing = monthlyMap[label] ?: Pair(0.0, 0.0)
                    if (tx.type == "INCOME") {
                        monthlyMap[label] = Pair(existing.first + tx.amount, existing.second)
                    } else {
                        monthlyMap[label] = Pair(existing.first, existing.second + tx.amount)
                    }
                } catch (e: Exception) {
                    // Ignore malformed dates
                }
            }

            val monthlyDataList = monthlyMap.map { (month, values) ->
                MonthlyBarData(month, values.first, values.second)
            }

            AnalyticsStats(
                categoryExpenses = categorySums,
                dailyExpenses = dailySums,
                monthlyData = monthlyDataList,
                spendingTrend = dailySums
            )
        }
    }
}
