package fr.bux.rollingdashboard

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity
data class Character(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "alive") val alive: Boolean,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "action_points") val action_points: Float,
    @ColumnInfo(name = "hungry") val hungry: Boolean,
    @ColumnInfo(name = "thirsty") val thirsty: Boolean,
    @ColumnInfo(name = "tired") val tired: Boolean,
    @ColumnInfo(name = "exhausted") val exhausted: Boolean,
    @ColumnInfo(name = "last_refresh") val last_refresh: Long,
    @ColumnInfo(name = "avatar_uuid") val avatar_uuid: String?,
)

@Dao
interface CharacterDao {
    @Query("SELECT * FROM character LIMIT 1")
    fun flow(): Flow<Character>

    @Query("SELECT * FROM character LIMIT 1")
    fun get(): Character?

    @Insert
    fun insert(character: Character)

    @Query("UPDATE Character SET alive=0")
    fun setDead()

    @Update
    fun update(character: Character)
}


@Serializable
data class CharacterInfo(
    val name: String,
    val action_points: Float,
    val max_action_points: Float,
    val is_attack_ready: Boolean,
    val is_defend_ready: Boolean,
    val is_exhausted: Boolean,
    val is_hunger: Boolean,
    val is_thirsty: Boolean,
    val is_tired: Boolean,
    val is_vulnerable: Boolean,
    val avatar_uuid: String?,
)
