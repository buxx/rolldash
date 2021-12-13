package fr.bux.rollingdashboard

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class AccountConfigurationViewModel(private val repository: AccountConfigurationRepository) : ViewModel() {

    // Using LiveData and caching what allAccountConfigurations returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val accountConfiguration: LiveData<AccountConfiguration?> = repository.accountConfiguration.asLiveData()

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(account_configuration: AccountConfiguration) = viewModelScope.launch {
        repository.update(account_configuration)
    }

    suspend fun get(): AccountConfiguration? {
        return repository.get()
    }

    fun isEntryValid(server_address: String, user_name: String, password: String): Boolean {
        if (server_address.isBlank() || user_name.isBlank() || password.isBlank()) {
            return false
        }
        return true
    }

}

class AccountConfigurationViewModelFactory(private val repository: AccountConfigurationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountConfigurationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountConfigurationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
