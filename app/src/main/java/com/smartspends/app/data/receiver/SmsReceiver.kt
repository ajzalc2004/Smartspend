package com.smartspends.app.data.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.smartspends.app.data.database.TransactionDao
import com.smartspends.app.data.database.TransactionEntity
import com.smartspends.app.data.parser.SmsParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionDao: TransactionDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val sharedPref = context.getSharedPreferences("smartspends_prefs", Context.MODE_PRIVATE)
        val smsEnabled = sharedPref.getBoolean("sms_tracking_enabled", true)
        if (!smsEnabled) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            for (message in messages) {
                val body = message.messageBody
                val sender = message.originatingAddress
                val timestamp = message.timestampMillis

                val parsed = SmsParser.parse(body, sender, timestamp)
                if (parsed != null) {
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
                    transactionDao.insertTransaction(entity)

                    // Show notification if notifications are enabled
                    val notificationsEnabled = sharedPref.getBoolean("notifications_enabled", true)
                    if (notificationsEnabled) {
                        showTransactionNotification(context, parsed)
                    }
                }
            }
        }
    }

    private fun showTransactionNotification(context: Context, parsed: com.smartspends.app.data.parser.ParsedTransaction) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "smartspends_transactions"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Transaction Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when a new transaction is automatically detected from SMS"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val symbol = getCurrencySymbol(context)
        val title = if (parsed.type == "INCOME") "New Income Detected" else "New Expense Detected"
        val text = "${parsed.description}: $symbol${String.format(Locale.getDefault(), "%.2f", parsed.amount)} via ${parsed.transactionMode}"

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Fallback icon
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun getCurrencySymbol(context: Context): String {
        val sharedPref = context.getSharedPreferences("smartspends_prefs", Context.MODE_PRIVATE)
        val currencyCode = sharedPref.getString("currency_code", "INR") ?: "INR"
        return when (currencyCode) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "₹"
        }
    }
}
