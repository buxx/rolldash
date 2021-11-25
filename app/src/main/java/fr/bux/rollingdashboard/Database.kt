package fr.bux.rollingdashboard

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SystemData::class, Character::class, AccountConfiguration::class], version = 1)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

}
