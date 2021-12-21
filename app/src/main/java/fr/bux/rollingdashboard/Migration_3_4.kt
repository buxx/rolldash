package fr.bux.rollingdashboard

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object: Migration(3,4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Character ADD COLUMN alive INTEGER NOT NULL DEFAULT 0")
    }
}