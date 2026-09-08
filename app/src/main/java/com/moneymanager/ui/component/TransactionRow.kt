package com.moneymanager.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.domain.model.Transaction
import com.moneymanager.domain.model.TxnType
import com.moneymanager.ui.theme.ExpenseRed
import com.moneymanager.ui.theme.ExpenseRedContainer
import com.moneymanager.ui.theme.IncomeGreen
import com.moneymanager.ui.theme.IncomeGreenContainer
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionRow(
    txn: Transaction,
    fundName: String,
    categoryIcon: String,
    onClick: () -> Unit = {}
) {
    val isIncome = txn.type == TxnType.INCOME
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val amountPrefix = if (isIncome) "+" else "-"
    val label = txn.merchant.ifBlank { txn.description.ifBlank { "Transaction" } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isIncome) IncomeGreenContainer else ExpenseRedContainer,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(categoryIcon, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                "$fundName · ${formatDate(txn.date)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        Text(
            "$amountPrefix₹${formatAmount(txn.amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}

fun formatDate(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 86_400_000 -> "Today"
        diff < 172_800_000 -> "Yesterday"
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
    }
}
