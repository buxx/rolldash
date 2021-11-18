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
    @Query("SELECT * FROM character WHERE id = :id LIMIT 1")
    fun get(id: String): Character

    @Insert
    fun insert(character: Character)
}
