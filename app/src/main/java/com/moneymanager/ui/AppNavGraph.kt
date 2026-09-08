package com.moneymanager.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.moneymanager.ui.screen.*
import com.moneymanager.ui.viewmodel.CategoryViewModel
import com.moneymanager.ui.viewmodel.FundViewModel

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { navController.navigate(Screen.OcrReview.route(it)) }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { navController.navigate(Screen.PdfReview.route(it)) }
    }
    val categoryViewModel: CategoryViewModel = hiltViewModel()
    val fundViewModel: FundViewModel = hiltViewModel()

    val categories by categoryViewModel.categories.collectAsState()
    val funds by fundViewModel.funds.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddTransaction = { navController.navigate(Screen.AddTransaction.withFund()) },
                    onAddIncome = { navController.navigate(Screen.AddTransaction.withFund(type = "INCOME")) },
                    onFundClick = { navController.navigate(Screen.FundDetail.route(it)) },
                    onTransfer = { navController.navigate(Screen.Transfer.route) },
                    onImportScreenshot = { imagePicker.launch("image/*") },
                    onImportPdf = { pdfPicker.launch("application/pdf") },
                    onTransactionClick = { navController.navigate(Screen.TransactionDetail.route(it)) },
                    categories = categories
                )
            }
            composable(Screen.Funds.route) {
                FundListScreen(
                    onAddFund = { navController.navigate(Screen.AddFund.edit()) },
                    onFundClick = { navController.navigate(Screen.FundDetail.route(it)) },
                    onEditFund = { navController.navigate(Screen.AddFund.edit(it)) }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionListScreen(categories = categories, funds = funds, onTransactionClick = { navController.navigate(Screen.TransactionDetail.route(it)) })
            }
            composable(Screen.AddTransaction.route) { backStack ->
                val fundId = backStack.arguments?.getString("fundId") ?: ""
                val initialType = backStack.arguments?.getString("type")?.let { runCatching { com.moneymanager.domain.model.TxnType.valueOf(it) }.getOrNull() }
                AddTransactionScreen(onBack = { navController.popBackStack() }, initialType = initialType)
            }
            composable(Screen.FundDetail.route) { backStack ->
                val fundId = backStack.arguments?.getString("fundId") ?: ""
                FundDetailScreen(
                    fundId = fundId,
                    categories = categories,
                    onBack = { navController.popBackStack() },
                    onAddTransaction = { navController.navigate(Screen.AddTransaction.withFund(it)) },
                    onTransactionClick = { navController.navigate(Screen.TransactionDetail.route(it)) }
                )
            }
            composable(Screen.TransactionDetail.route) { backStack ->
                TransactionDetailScreen(
                    transactionId = backStack.arguments?.getString("transactionId").orEmpty(),
                    funds = funds,
                    categories = categories,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AddFund.route) { backStack ->
                val fundId = backStack.arguments?.getString("fundId")?.takeIf { it.isNotBlank() }
                AddFundScreen(editFundId = fundId, onBack = { navController.popBackStack() })
            }
            composable(Screen.Transfer.route) {
                TransferScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.OcrReview.route) { backStack ->
                backStack.arguments?.getString("uri")?.let(Uri::parse)?.let { uri ->
                    ImageImportRoute(uri = uri, onBack = { navController.popBackStack() })
                }
            }
            composable(Screen.PdfReview.route) { backStack ->
                backStack.arguments?.getString("uri")?.let(Uri::parse)?.let { uri ->
                    PdfImportRoute(uri = uri, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
