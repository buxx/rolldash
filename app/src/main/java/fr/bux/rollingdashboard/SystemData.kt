package fr.bux.rollingdashboard

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity
data class SystemData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "last_try_refresh") var last_try_refresh: Long,
    @ColumnInfo(name = "current_grab_error") val current_grab_error: String?,
)

@Dao
interface SystemDataDao {
    @Query("SELECT * FROM SystemData LIMIT 1")
    fun flow(): Flow<SystemData>

    @Query("SELECT * FROM SystemData LIMIT 1")
    fun get(): SystemData?

    @Insert
    fun insert(system: SystemData)

    @Update
    fun update(system: SystemData)

    @Query("UPDATE SystemData SET current_grab_error=:error")
    fun updateCurrentGrabError(error: String?);
}
