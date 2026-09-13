package com.moneymanager.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val TRANSACTION_SUCCESS_KEY = booleanPreferencesKey("transaction_success_notifications")
    }

    val isTransactionSuccessEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[TRANSACTION_SUCCESS_KEY] ?: true // Default to true
    }

    suspend fun setTransactionSuccessEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[TRANSACTION_SUCCESS_KEY] = enabled
        }
    }
}
