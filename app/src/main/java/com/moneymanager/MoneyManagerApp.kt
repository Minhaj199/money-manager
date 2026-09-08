package com.moneymanager

import android.app.Application
import com.moneymanager.data.repository.CategoryRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MoneyManagerApp : Application() {
    @Inject lateinit var categoryRepository: CategoryRepository

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            categoryRepository.seedDefaults()
        }
    }
}
