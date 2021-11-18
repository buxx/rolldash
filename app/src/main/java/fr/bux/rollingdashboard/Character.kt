package fr.bux.rollingdashboard

import androidx.room.*

@Entity
data class Character(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "action_points") val action_points: Float,
    @ColumnInfo(name = "hungry") val hungry: Boolean,
    @ColumnInfo(name = "thirsty") val thirsty: Boolean,
)

@Dao
interface CharacterDao {
    @Query("SELECT * FROM character LIMIT 1")
    fun get(id: String): Character

    @Query("DELETE FROM character")
    fun clear()

    @Insert
    fun insert(character: Character)
}
