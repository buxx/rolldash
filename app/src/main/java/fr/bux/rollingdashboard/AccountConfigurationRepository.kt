package fr.bux.rollingdashboard

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class AccountConfigurationRepository(private val accountConfigurationDao: AccountConfigurationDao) {
    val accountConfiguration: Flow<AccountConfiguration> = accountConfigurationDao.get()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun update(account_configuration: AccountConfiguration) {
        accountConfigurationDao.clear()
        accountConfigurationDao.insert(account_configuration)
    }
}
