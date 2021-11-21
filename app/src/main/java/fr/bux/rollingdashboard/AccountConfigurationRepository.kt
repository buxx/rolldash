package fr.bux.rollingdashboard

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class AccountConfigurationRepository(private val accountConfigurationDao: AccountConfigurationDao) {
    val accountConfiguration: Flow<AccountConfiguration?> = accountConfigurationDao.flow()

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun update(account_configuration: AccountConfiguration) {
        accountConfigurationDao.clear()
        accountConfigurationDao.insert(account_configuration)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun get() : AccountConfiguration? {
        return accountConfigurationDao.get()
    }
}
