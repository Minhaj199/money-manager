package com.moneymanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moneymanager.ui.AppNavGraph
import com.moneymanager.ui.theme.MoneyManagerTheme
import com.moneymanager.ui.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by themeViewModel.isDark.collectAsState()
            MoneyManagerTheme(darkTheme = isDark) {
                AppNavGraph(
                    isDark = isDark,
                    onToggleTheme = themeViewModel::toggle
                )
            }
        }
    }
}
