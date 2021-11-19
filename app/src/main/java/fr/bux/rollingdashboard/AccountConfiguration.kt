package fr.bux.rollingdashboard

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity
data class AccountConfiguration(
    @PrimaryKey val server_address: String,
    @ColumnInfo(name = "user_name") val user_name: String,
    @ColumnInfo(name = "password") val password: String,  // FIXME : secure storage
    @ColumnInfo(name = "notify_hungry") val notify_hungry: Boolean,
    @ColumnInfo(name = "notify_thirsty") val notify_thirsty: Boolean,
    @ColumnInfo(name = "notify_ap") val notify_ap: Boolean,
    @ColumnInfo(name = "network_grab_each") val network_grab_each: Int,
)

@Dao
interface AccountConfigurationDao {
    @Query("SELECT * FROM accountconfiguration LIMIT 1")
    fun get(): Flow<AccountConfiguration>

    @Query("DELETE FROM accountconfiguration")
    fun clear()

    @Insert
    fun insert(account_configuration: AccountConfiguration)
}
