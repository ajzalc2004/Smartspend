package com.smartspends.app.domain.usecase

import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(
        query: String = "",
        type: String? = null,
        category: String? = null,
        bank: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        sortBy: String = "DATE_DESC"
    ): Flow<List<TransactionEntity>> {
        val q = query.trim()
        val t = if (type == "ALL" || type.isNullOrEmpty()) null else type
        val c = if (category == "ALL" || category.isNullOrEmpty()) null else category
        val b = if (bank == "ALL" || bank.isNullOrEmpty()) null else bank
        val sd = if (startDate.isNullOrEmpty()) null else startDate
        val ed = if (endDate.isNullOrEmpty()) null else endDate
        
        return repository.getTransactionsFiltered(q, t, c, b, sd, ed, sortBy)
    }
}
