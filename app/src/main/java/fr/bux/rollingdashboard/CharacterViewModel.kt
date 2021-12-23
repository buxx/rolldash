package fr.bux.rollingdashboard

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class CharacterViewModel(private val repository: CharacterRepository) : ViewModel() {

    // Using LiveData and caching what allCharacters returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val character: LiveData<Character> = repository.character.asLiveData()

    fun update(character: Character) = viewModelScope.launch {
        repository.update(character)
    }

    suspend fun get(): Character? {
        return repository.get()
    }
}

class CharacterViewModelFactory(private val repository: CharacterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CharacterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CharacterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
