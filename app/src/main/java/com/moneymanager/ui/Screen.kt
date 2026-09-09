package com.moneymanager.ui

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Funds : Screen("funds")
    object Transactions : Screen("transactions")
    object AddTransaction : Screen("add_transaction?fundId={fundId}&type={type}&transactionId={transactionId}") {
        fun withFund(fundId: String = "", type: String = "") = "add_transaction?fundId=$fundId&type=$type&transactionId="
        fun edit(transactionId: String) = "add_transaction?fundId=&type=&transactionId=$transactionId"
    }
    object FundDetail : Screen("fund/{fundId}") {
        fun route(fundId: String) = "fund/$fundId"
    }
    object TransactionDetail : Screen("transaction/{transactionId}") {
        fun route(transactionId: String) = "transaction/$transactionId"
    }
    object AddFund : Screen("add_fund?fundId={fundId}") {
        fun edit(fundId: String = "") = "add_fund?fundId=$fundId"
    }
    object Transfer : Screen("transfer")
    object BackupRestore : Screen("backup_restore")
    object OcrReview : Screen("ocr_review?uri={uri}") {
        fun route(uri: Uri) = "ocr_review?uri=${Uri.encode(uri.toString())}"
    }
    object PdfReview : Screen("pdf_review?uri={uri}") {
        fun route(uri: Uri) = "pdf_review?uri=${Uri.encode(uri.toString())}"
    }
}

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Default.Home),
    BottomNavItem(Screen.Funds, "Funds", Icons.Default.Wallet),
    BottomNavItem(Screen.Transactions, "History", Icons.Default.List),
)
