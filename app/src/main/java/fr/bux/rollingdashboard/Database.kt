package fr.bux.rollingdashboard

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Character::class, Character::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun accountConfigurationDao(): AccountConfigurationDao
}
