package com.moneymanager.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.moneymanager.R
import com.moneymanager.domain.model.TxnType
import com.moneymanager.ui.component.formatAmount
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "transaction_updates_channel"
        private const val CHANNEL_NAME = "Transaction Updates"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for successfully saved transactions"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTransactionSuccess(
        type: TxnType,
        amount: Double,
        fundName: String,
        oldBalance: Double,
        newBalance: Double,
        date: Long
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, abort notification
            return
        }

        val typeStr = if (type.name == "INCOME") "Income" else "Expense"
        val actionStr = if (type.name == "INCOME") "added to" else "spent from"
        
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(date))

        val title = "Success: $typeStr Saved"
        val message = "₹${formatAmount(amount)} $actionStr $fundName.\n" +
                "Prev Balance: ₹${formatAmount(oldBalance)}\n" +
                "New Balance: ₹${formatAmount(newBalance)}\n" +
                "Date: $dateStr"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Fallback icon
            .setContentTitle(title)
            .setContentText("₹${formatAmount(amount)} $actionStr $fundName")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID + date.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            // Handled missing permission gracefully
        }
    }
}
