package fr.bux.rollingdashboard

import android.app.Application

class RollingDashboardApplication : Application() {
    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { AppDatabase.getDatabase(this) }
    val character_repository by lazy { CharacterRepository(database.characterDao()) }
    val account_configuration_repository by lazy { AccountConfigurationRepository(database.accountConfigurationDao()) }
}
