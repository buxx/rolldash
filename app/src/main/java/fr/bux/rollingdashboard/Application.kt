package fr.bux.rollingdashboard

import android.app.Application
import androidx.work.Data
import androidx.work.PeriodicWorkRequest
import java.util.concurrent.TimeUnit

class RollingDashboardApplication : Application() {
    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { AppDatabase.getDatabase(this) }
    val character_repository by lazy { CharacterRepository(database.characterDao()) }
    val system_data_repository by lazy { SystemDataRepository(database.systemDataDao()) }
    val account_configuration_repository by lazy { AccountConfigurationRepository(database.accountConfigurationDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: RollingDashboardApplication
            private set
    }

    fun buildPeriodicGrabCharacterWorkRequest(now: Boolean = false): PeriodicWorkRequest {
        val data: Data = Data.Builder()
            .putString("NAME", "Rolling worker")
            .putBoolean("NOW", now)
            .build()

        return PeriodicWorkRequest.Builder(
            GrabCharacterWorker::class.java,
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS,
        ).setInputData(data).build()
    }
}
