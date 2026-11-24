package com.example.petfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.SessionManager
import com.example.petfinder.data.local.dao.UserDao
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.data.repository.PetRepository
import com.example.petfinder.util.LocationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PetListItem(
    val pet: PetEntity,
    val distanceKm: Double? = null
)

data class HomeUiState(
    val userFirstName: String = "",
    val userPhotoPath: String? = null,
    val userLocationLabel: String = "Localização não informada",
    val searchQuery: String = "",
    val selectedSpecies: PetSpecies? = null,
    val pets: List<PetListItem> = emptyList(),
    val isRefreshingLocation: Boolean = false,
    val feedbackMessage: String? = null
)

private data class HomeFilterState(
    val searchQuery: String = "",
    val selectedSpecies: PetSpecies? = null,
    val gpsLat: Double? = null,
    val gpsLon: Double? = null,
    val gpsLocationLabel: String? = null,
    val isRefreshingLocation: Boolean = false,
    val feedbackMessage: String? = null
)

/**
 * ViewModel da TelaInicial: combina usuário logado, lista de pets do Room, filtros e GPS.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val petRepository: PetRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(HomeFilterState())

    private val currentUserFlow = sessionManager.currentUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(null) else userDao.observeUserById(userId)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        currentUserFlow,
        petRepository.observeAllPets(),
        filterState
    ) { user, allPets, filters ->
        val firstName = user?.fullName
            ?.trim()
            ?.substringBefore(" ")
            ?.takeIf { it.isNotBlank() }
            ?: "Visitante"

        val userCityUf = user?.shortLocationLabel
        val locationLabel = filters.gpsLocationLabel ?: userCityUf ?: "Localização não informada"

        val refLat = filters.gpsLat ?: user?.latitude
        val refLon = filters.gpsLon ?: user?.longitude

        val filteredPets = allPets
            .filter { pet ->
                val matchesSpecies = filters.selectedSpecies == null || pet.species == filters.selectedSpecies
                val query = filters.searchQuery.trim()
                val matchesQuery = query.isEmpty() ||
                    pet.name.contains(query, ignoreCase = true) ||
                    (pet.breed?.contains(query, ignoreCase = true) == true) ||
                    pet.species.label.contains(query, ignoreCase = true)
                matchesSpecies && matchesQuery
            }
            .map { pet ->
                val dist = if (refLat != null && refLon != null && pet.latitude != null && pet.longitude != null) {
                    LocationHelper.calculateDistanceKm(refLat, refLon, pet.latitude, pet.longitude)
                } else {
                    null
                }
                PetListItem(pet = pet, distanceKm = dist)
            }
            .let { list ->
                if (refLat != null && refLon != null) {
                    list.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
                } else {
                    list
                }
            }

        HomeUiState(
            userFirstName = firstName,
            userPhotoPath = user?.photoPath,
            userLocationLabel = locationLabel,
            searchQuery = filters.searchQuery,
            selectedSpecies = filters.selectedSpecies,
            pets = filteredPets,
            isRefreshingLocation = filters.isRefreshingLocation,
            feedbackMessage = filters.feedbackMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChange(query: String) {
        filterState.update { it.copy(searchQuery = query) }
    }

    fun onSpeciesSelected(species: PetSpecies?) {
        filterState.update { it.copy(selectedSpecies = species) }
    }

    fun refreshLocation(locationHelper: LocationHelper) {
        viewModelScope.launch {
            filterState.update { it.copy(isRefreshingLocation = true, feedbackMessage = null) }
            locationHelper.getCurrentAddress()
                .onSuccess { captured ->
                    val label = listOfNotNull(captured.city, captured.uf)
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(", ")
                    filterState.update {
                        it.copy(
                            gpsLat = captured.latitude,
                            gpsLon = captured.longitude,
                            gpsLocationLabel = label ?: it.gpsLocationLabel,
                            isRefreshingLocation = false,
                            feedbackMessage = "Localização GPS atualizada."
                        )
                    }
                }
                .onFailure { error ->
                    filterState.update {
                        it.copy(
                            isRefreshingLocation = false,
                            feedbackMessage = error.message ?: "Não foi possível obter a localização."
                        )
                    }
                }
        }
    }

    fun showFeedbackMessage(message: String) {
        filterState.update { it.copy(feedbackMessage = message) }
    }

    fun clearFeedbackMessage() {
        filterState.update { it.copy(feedbackMessage = null) }
    }

    fun resetState() {
        filterState.value = HomeFilterState()
    }
}

class HomeViewModelFactory(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val petRepository: PetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(userDao, sessionManager, petRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
