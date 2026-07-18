package com.smartspends.app.domain.usecase

import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject

data class RecurringTransaction(
    val description: String,
    val amount: Double,
    val type: String, // INCOME / EXPENSE
    val category: String,
    val frequency: String, // "Monthly", "Weekly", "Quarterly", etc.
    val lastDate: String,
    val nextDueDate: String
)

class DetectRecurringTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<RecurringTransaction>> {
        return repository.getAllTransactions().map { transactions ->
            detectRecurring(transactions)
        }
    }

    private fun detectRecurring(transactions: List<TransactionEntity>): List<RecurringTransaction> {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        // Group transactions by description and category
        val groups = transactions.groupBy { it.description.lowercase(Locale.ROOT) to it.category }
        val recurringList = mutableListOf<RecurringTransaction>()

        for ((key, items) in groups) {
            val (desc, category) = key
            if (items.size < 2) {
                // If it has subscription keywords, we can flag it even with 1 occurrence as a potential recurring item
                val lowerDesc = desc.lowercase(Locale.ROOT)
                val isSuspectedSubscription = isSubscriptionKeyword(lowerDesc) || isEmiKeyword(lowerDesc)
                if (isSuspectedSubscription && items.isNotEmpty()) {
                    val lastItem = items.maxByOrNull { it.date }!!
                    try {
                        val lastLocalDate = LocalDate.parse(lastItem.date, dateFormatter)
                        val nextDueDate = lastLocalDate.plusMonths(1)
                        recurringList.add(
                            RecurringTransaction(
                                description = lastItem.description,
                                amount = lastItem.amount,
                                type = lastItem.type,
                                category = lastItem.category,
                                frequency = "Monthly (Estimated)",
                                lastDate = lastItem.date,
                                nextDueDate = nextDueDate.format(dateFormatter)
                            )
                        )
                    } catch (e: Exception) {
                        // ignore parsing error
                    }
                }
                continue
            }

            // Sort items by date ascending to check intervals
            val sortedItems = items.sortedBy { it.date }
            val intervals = mutableListOf<Long>()
            
            for (i in 0 until sortedItems.size - 1) {
                try {
                    val date1 = LocalDate.parse(sortedItems[i].date, dateFormatter)
                    val date2 = LocalDate.parse(sortedItems[i + 1].date, dateFormatter)
                    intervals.add(ChronoUnit.DAYS.between(date1, date2))
                } catch (e: Exception) {
                    // skip
                }
            }

            if (intervals.isEmpty()) continue
            
            val avgInterval = intervals.average()
            val lastItem = sortedItems.last()
            
            // Check if it matches a standard frequency
            val frequency = when {
                avgInterval in 25.0..35.0 -> "Monthly"
                avgInterval in 6.0..8.0 -> "Weekly"
                avgInterval in 80.0..100.0 -> "Quarterly"
                avgInterval in 350.0..370.0 -> "Annually"
                else -> null
            }

            // If it matches a frequency, or contains strong keywords, flag as recurring
            if (frequency != null || isSubscriptionKeyword(desc) || isEmiKeyword(desc) || category == "Salary") {
                val finalFrequency = frequency ?: "Monthly"
                try {
                    val lastLocalDate = LocalDate.parse(lastItem.date, dateFormatter)
                    val addDays = if (avgInterval in 5.0..370.0) avgInterval.toLong() else 30L
                    val nextDueDate = lastLocalDate.plusDays(addDays)
                    
                    recurringList.add(
                        RecurringTransaction(
                            description = lastItem.description,
                            amount = lastItem.amount,
                            type = lastItem.type,
                            category = lastItem.category,
                            frequency = finalFrequency,
                            lastDate = lastItem.date,
                            nextDueDate = nextDueDate.format(dateFormatter)
                        )
                    )
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        
        // Return sorted by next due date
        return recurringList.sortedBy { it.nextDueDate }
    }

    private fun isSubscriptionKeyword(desc: String): Boolean {
        val keywords = listOf("netflix", "spotify", "youtube", "premium", "prime", "hotstar", "microsoft", "adobe", "google", "apple", "icloud")
        return keywords.any { desc.contains(it) }
    }

    private fun isEmiKeyword(desc: String): Boolean {
        val keywords = listOf("emi", "loan", "mortgage", "finance", "hdfc credit card", "sbi card payment")
        return keywords.any { desc.contains(it) }
    }
}
