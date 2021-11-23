package fr.bux.rollingdashboard

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity
data class Character(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "action_points") val action_points: Float,
    @ColumnInfo(name = "hungry") val hungry: Boolean,
    @ColumnInfo(name = "thirsty") val thirsty: Boolean,
    @ColumnInfo(name = "last_refresh") val last_refresh: Long,
)

@Dao
interface CharacterDao {
    @Query("SELECT * FROM character LIMIT 1")
    fun flow(): Flow<Character>

    @Query("SELECT * FROM character LIMIT 1")
    fun get(): Character

    // Before insert a character this method must be called to clear the table
    @Query("DELETE FROM character")
    fun clear()

    @Insert
    fun insert(character: Character)
}
