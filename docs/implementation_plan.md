# Transaction Success Notification Implementation Plan

This plan details the implementation of a success notification feature that alerts the user when a transaction is successfully saved, while providing a toggle in the settings to enable/disable it.

## User Review Required
> [!IMPORTANT]
> The app currently doesn't have a dedicated generic "Settings" screen; it uses the `BackupRestoreScreen` as the primary settings destination when the user taps the Settings gear icon on the Home screen. I propose renaming the screen to `SettingsScreen` and adding the "Notifications" toggle there, alongside the existing Backup & Restore and Data Management options. Please confirm if this is acceptable or if you'd prefer a completely separate screen.

## Proposed Changes

### Configuration & State Management
#### [NEW] `app/src/main/java/com/moneymanager/data/preferences/NotificationPreferences.kt`
- Create a new DataStore preference class `NotificationPreferences` to store the boolean state of the transaction success notifications.
- Expose a `Flow<Boolean>` for observing the state and a suspend function to toggle it.
- Default value will be set to `true`.

#### [NEW] `app/src/main/java/com/moneymanager/util/NotificationHelper.kt`
- Create a helper class injected via Hilt.
- Responsibilities:
  - Create the notification channel ("Transaction Updates").
  - Dispatch the local push notification using `NotificationManagerCompat`.
  - Provide a function `showTransactionSuccess(type, amount, fundName, oldBalance, newBalance, date)`.
- Use the app icon and appropriate text formatting.

### UI & Navigation
#### [MODIFY] `app/src/main/java/com/moneymanager/ui/AppNavGraph.kt`
- Inject the new `NotificationPreferences` and update the Settings route. 
- The `Screen.BackupRestore.route` can be kept or aliased, but we will pass the new toggle state down to the screen.

#### [MODIFY] `app/src/main/java/com/moneymanager/ui/screen/BackupRestoreScreen.kt` (or Rename to `SettingsScreen.kt`)
- Add a "Notifications" section.
- Add a Switch/Toggle for "Transaction Success Notifications".
- Connect the Switch to the `NotificationPreferences` via a new or existing ViewModel.
- Ask for `POST_NOTIFICATIONS` permission (Android 13+) if the user tries to turn it on (or it can be requested gracefully).

### Business Logic
#### [MODIFY] `app/src/main/java/com/moneymanager/ui/viewmodel/AddTransactionViewModel.kt`
- Inject `NotificationPreferences`, `NotificationHelper`, and `TransactionDao`.
- In the `save()` method:
  - Check if notifications are enabled via the DataStore.
  - Fetch the old balance of the selected fund (using `TransactionDao.totalIncome/totalExpense` + `FundEntity.startingBalance`).
  - Proceed with the existing transaction saving logic (`saveUseCase`).
  - Fetch the updated new balance of the selected fund.
  - Trigger `NotificationHelper.showTransactionSuccess(...)` using the computed old and new balances.
- Ensure the notification is sent *only* upon successful save without duplicate errors.

## Verification Plan
### Automated Tests
- No automated UI tests, rely on manual verification.

### Manual Verification
1. Launch the app and go to Settings (Backup & Restore screen).
2. Verify the new "Transaction Success Notifications" toggle is present and defaults to ON.
3. Add a new Income or Expense transaction.
4. Verify that an in-app push notification is displayed immediately after saving.
5. Verify the notification content matches the requirements: Type, Amount, Fund Name, Previous Balance, Updated Balance, Date/Time.
6. Toggle the setting to OFF in Settings.
7. Add another transaction and verify NO notification is shown.
8. Restart the app and verify the toggle state is persisted.
