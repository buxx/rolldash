package fr.bux.rollingdashboard

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class SystemDataViewModel(private val repository: SystemDataRepository) : ViewModel() {
    val systemData: LiveData<SystemData> = repository.systemData.asLiveData()
}

class SystemDataViewModelFactory(private val repository: SystemDataRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SystemDataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SystemDataViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
