package fr.bux.rollingdashboard

import androidx.annotation.WorkerThread
import kotlinx.coroutines.flow.Flow

class CharacterRepository(private val characterDao: CharacterDao) {
    val character: Flow<Character> = characterDao.flow()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun update(character: Character) {
        characterDao.clear()
        characterDao.insert(character)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun get() : Character? {
        return characterDao.get()
    }
}
