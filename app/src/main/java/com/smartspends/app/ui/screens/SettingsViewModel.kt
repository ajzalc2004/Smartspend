package com.smartspends.app.ui.screens

import android.app.Application
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartspends.app.data.database.AppDatabase
import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.domain.repository.TransactionRepository
import com.smartspends.app.domain.usecase.ImportHistoricalSmsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val repository: TransactionRepository,
    private val importHistoricalSmsUseCase: ImportHistoricalSmsUseCase,
    private val database: AppDatabase
) : AndroidViewModel(application) {

    private val sharedPref = application.getSharedPreferences("smartspends_prefs", Context.MODE_PRIVATE)

    private val _smsTrackingEnabled = MutableStateFlow(sharedPref.getBoolean("sms_tracking_enabled", true))
    val smsTrackingEnabled = _smsTrackingEnabled.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(sharedPref.getBoolean("notifications_enabled", true))
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _currencyCode = MutableStateFlow(sharedPref.getString("currency_code", "INR") ?: "INR")
    val currencyCode = _currencyCode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    fun toggleSmsTracking(enabled: Boolean) {
        sharedPref.edit().putBoolean("sms_tracking_enabled", enabled).apply()
        _smsTrackingEnabled.value = enabled
    }

    fun toggleNotifications(enabled: Boolean) {
        sharedPref.edit().putBoolean("notifications_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun changeCurrency(code: String) {
        sharedPref.edit().putString("currency_code", code).apply()
        _currencyCode.value = code
    }

    fun importPastSms(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Scanning inbox for bank SMS..."
            try {
                val count = importHistoricalSmsUseCase(context)
                _statusMessage.value = "Scan complete! Imported $count new transactions."
            } catch (e: Exception) {
                _statusMessage.value = "Failed to scan SMS: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun backupDatabase(context: Context, backupUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Ensure active changes in WAL are checkpointed to main DB file
                database.close()
                
                val dbFile = context.getDatabasePath("smartspends_db")
                if (dbFile.exists()) {
                    context.contentResolver.openOutputStream(backupUri)?.use { outputStream ->
                        FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    _statusMessage.value = "Database backed up successfully!"
                } else {
                    _statusMessage.value = "Database file does not exist yet."
                }
            } catch (e: Exception) {
                _statusMessage.value = "Backup failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun restoreDatabase(context: Context, restoreUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                database.close()
                val dbFile = context.getDatabasePath("smartspends_db")
                
                context.contentResolver.openInputStream(restoreUri)?.use { inputStream ->
                    FileOutputStream(dbFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // Clear WAL and SHM files to avoid consistency mismatches
                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                _statusMessage.value = "Database restored successfully! Please restart the app."
            } catch (e: Exception) {
                _statusMessage.value = "Restore failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportTransactionsToCsv(context: Context, fileUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val transactions = repository.getAllTransactions().first()
                val csvHeader = "ID,Amount,Type,Category,Bank,Account,Date,Time,Mode,Description,Source\n"
                
                context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(csvHeader.toByteArray())
                    for (tx in transactions) {
                        val row = "${tx.id},${tx.amount},${tx.type},\"${tx.category}\",\"${tx.bank ?: ""}\",\"${tx.accountNumberMasked ?: ""}\",${tx.date},${tx.time},${tx.transactionMode},\"${tx.description}\",${tx.source}\n"
                        outputStream.write(row.toByteArray())
                    }
                }
                _statusMessage.value = "CSV report exported successfully!"
            } catch (e: Exception) {
                _statusMessage.value = "CSV export failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportTransactionsToPdf(context: Context, fileUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val transactions = repository.getAllTransactions().first().take(40) // Limit to first page for simplicity
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                val titlePaint = Paint().apply {
                    textSize = 20f
                    isFakeBoldText = true
                }
                val headerPaint = Paint().apply {
                    textSize = 12f
                    isFakeBoldText = true
                }
                val bodyPaint = Paint().apply {
                    textSize = 10f
                }

                // Draw Header
                canvas.drawText("SmartSpends Financial Report", 30f, 50f, titlePaint)
                canvas.drawText("Generated on: " + LocalDate.now().toString(), 30f, 75f, bodyPaint)

                // Draw Table Header
                canvas.drawText("Date", 30f, 120f, headerPaint)
                canvas.drawText("Description", 110f, 120f, headerPaint)
                canvas.drawText("Category", 310f, 120f, headerPaint)
                canvas.drawText("Type", 410f, 120f, headerPaint)
                canvas.drawText("Amount", 490f, 120f, headerPaint)

                var yPos = 145f
                val currencySymbol = when (_currencyCode.value) {
                    "INR" -> "Rs."
                    "USD" -> "$"
                    "EUR" -> "E"
                    else -> "Rs."
                }

                // Draw Table Rows
                for (tx in transactions) {
                    canvas.drawText(tx.date, 30f, yPos, bodyPaint)
                    canvas.drawText(tx.description.take(28), 110f, yPos, bodyPaint)
                    canvas.drawText(tx.category, 310f, yPos, bodyPaint)
                    canvas.drawText(tx.type, 410f, yPos, bodyPaint)
                    canvas.drawText("$currencySymbol${String.format(Locale.getDefault(), "%.2f", tx.amount)}", 490f, yPos, bodyPaint)
                    yPos += 20f
                    if (yPos > 800f) break // Avoid page overflow for page 1
                }

                pdfDocument.finishPage(page)

                context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                _statusMessage.value = "PDF report exported successfully!"
            } catch (e: Exception) {
                _statusMessage.value = "PDF export failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
