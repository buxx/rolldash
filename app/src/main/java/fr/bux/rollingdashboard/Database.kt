package fr.bux.rollingdashboard

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SystemData::class, Character::class, AccountConfiguration::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun systemDataDao(): SystemDataDao
    abstract fun characterDao(): CharacterDao
    abstract fun accountConfigurationDao(): AccountConfigurationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}
