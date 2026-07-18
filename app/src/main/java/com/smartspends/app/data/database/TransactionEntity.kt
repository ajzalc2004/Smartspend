package com.smartspends.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val bank: String?,
    val accountNumberMasked: String?,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val transactionMode: String, // e.g., UPI, ATM, IMPS, Cash, Card
    val description: String,
    val source: String, // "SMS" or "MANUAL"
    val smsBody: String?
)
