package fr.bux.rollingdashboard

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity
data class SystemData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "last_try_refresh") var last_try_refresh: Long,
)

@Dao
interface SystemDataDao {
    @Query("SELECT * FROM SystemData LIMIT 1")
    fun get(): SystemData?

    @Insert
    fun insert(system: SystemData)

    @Update
    fun update(system: SystemData)
}
