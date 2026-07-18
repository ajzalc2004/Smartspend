package com.smartspends.app.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.data.parser.SmsParser
import com.smartspends.app.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImportHistoricalSmsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(context: Context): Int = withContext(Dispatchers.IO) {
        var importedCount = 0
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.ADDRESS, Telephony.Sms.DATE),
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use { c ->
            val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
            val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)

            while (c.moveToNext()) {
                val body = c.getString(bodyIndex) ?: continue
                val sender = c.getString(addressIndex)
                val timestamp = c.getLong(dateIndex)

                val parsed = SmsParser.parse(body, sender, timestamp)
                if (parsed != null) {
                    val duplicateCount = repository.checkDuplicate(
                        parsed.amount,
                        parsed.date,
                        parsed.time,
                        parsed.bank
                    )
                    if (duplicateCount == 0) {
                        val entity = TransactionEntity(
                            amount = parsed.amount,
                            type = parsed.type,
                            category = parsed.category,
                            bank = parsed.bank,
                            accountNumberMasked = parsed.accountNumberMasked,
                            date = parsed.date,
                            time = parsed.time,
                            transactionMode = parsed.transactionMode,
                            description = parsed.description,
                            source = "SMS",
                            smsBody = parsed.smsBody
                        )
                        repository.insertTransaction(entity)
                        importedCount++
                    }
                }
            }
        }
        return@withContext importedCount
    }
}
