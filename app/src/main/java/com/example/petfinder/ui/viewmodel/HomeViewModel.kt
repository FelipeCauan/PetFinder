package com.example.petfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.SessionManager
import com.example.petfinder.data.local.dao.UserDao
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
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
    val selectedSex: PetSex? = null,
    val selectedSize: PetSize? = null,
    val selectedStatus: AdoptionStatus? = null,
    val maxDistanceKm: Double? = null,
    val hasLocationReference: Boolean = false,
    val activeFiltersCount: Int = 0,
    val pets: List<PetListItem> = emptyList(),
    val isRefreshingLocation: Boolean = false,
    val feedbackMessage: String? = null
)

private data class HomeFilterState(
    val searchQuery: String = "",
    val selectedSpecies: PetSpecies? = null,
    val selectedSex: PetSex? = null,
    val selectedSize: PetSize? = null,
    val selectedStatus: AdoptionStatus? = null,
    val maxDistanceKm: Double? = null,
    val gpsLocationLabel: String? = null,
    val isRefreshingLocation: Boolean = false,
    val feedbackMessage: String? = null
)

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
        filterState,
        sessionManager.temporaryLocation
    ) { user, allPets, filters, tempLocation ->
        val firstName = user?.fullName?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Visitante"
        val locationLabel = filters.gpsLocationLabel ?: user?.shortLocationLabel ?: "Localização não informada"
        val refLat = tempLocation?.latitude ?: user?.latitude
        val refLon = tempLocation?.longitude ?: user?.longitude

        val pets = filterAndSortPets(
            allPets = allPets,
            searchQuery = filters.searchQuery,
            selectedSpecies = filters.selectedSpecies,
            selectedSex = filters.selectedSex,
            selectedSize = filters.selectedSize,
            selectedStatus = filters.selectedStatus,
            maxDistanceKm = filters.maxDistanceKm,
            refLat = refLat,
            refLon = refLon
        )

        val activeCount = listOf(
            filters.selectedSex, filters.selectedSize, filters.selectedStatus, filters.maxDistanceKm
        ).count { it != null }

        HomeUiState(
            userFirstName = firstName,
            userPhotoPath = user?.photoPath,
            userLocationLabel = locationLabel,
            searchQuery = filters.searchQuery,
            selectedSpecies = filters.selectedSpecies,
            selectedSex = filters.selectedSex,
            selectedSize = filters.selectedSize,
            selectedStatus = filters.selectedStatus,
            maxDistanceKm = filters.maxDistanceKm,
            hasLocationReference = refLat != null && refLon != null,
            activeFiltersCount = activeCount,
            pets = pets,
            isRefreshingLocation = filters.isRefreshingLocation,
            feedbackMessage = filters.feedbackMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChange(query: String) = filterState.update { it.copy(searchQuery = query) }
    fun onSpeciesSelected(species: PetSpecies?) = filterState.update { it.copy(selectedSpecies = species) }

    fun applyAdvancedFilters(sex: PetSex?, size: PetSize?, status: AdoptionStatus?, maxDistanceKm: Double?) {
        filterState.update {
            it.copy(selectedSex = sex, selectedSize = size, selectedStatus = status, maxDistanceKm = maxDistanceKm)
        }
    }

    fun clearAdvancedFilters() {
        filterState.update {
            it.copy(selectedSex = null, selectedSize = null, selectedStatus = null, maxDistanceKm = null)
        }
    }

    fun refreshLocation(locationHelper: LocationHelper) {
        viewModelScope.launch {
            filterState.update { it.copy(isRefreshingLocation = true, feedbackMessage = null) }
            locationHelper.getCurrentAddress()
                .onSuccess { captured ->
                    val label = listOfNotNull(captured.city, captured.uf).takeIf { it.isNotEmpty() }?.joinToString(", ")
                    sessionManager.setTemporaryLocation(captured.latitude, captured.longitude)
                    filterState.update {
                        it.copy(
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

    fun showFeedbackMessage(message: String) = filterState.update { it.copy(feedbackMessage = message) }
    fun clearFeedbackMessage() = filterState.update { it.copy(feedbackMessage = null) }

    fun resetState() {
        filterState.value = HomeFilterState()
        sessionManager.clearTemporaryLocation()
    }

    companion object {
        internal fun filterAndSortPets(
            allPets: List<PetEntity>,
            searchQuery: String,
            selectedSpecies: PetSpecies?,
            selectedSex: PetSex?,
            selectedSize: PetSize?,
            selectedStatus: AdoptionStatus?,
            maxDistanceKm: Double?,
            refLat: Double?,
            refLon: Double?
        ): List<PetListItem> {
            val query = searchQuery.trim()
            val hasRefLocation = refLat != null && refLon != null

            return allPets
                .asSequence()
                .filter { pet ->
                    pet.status != AdoptionStatus.ADOTADO &&
                        (selectedSpecies == null || pet.species == selectedSpecies) &&
                        (selectedSex == null || pet.sex == selectedSex) &&
                        (selectedSize == null || pet.size == selectedSize) &&
                        (selectedStatus == null || pet.status == selectedStatus) &&
                        (query.isEmpty() ||
                            pet.name.contains(query, ignoreCase = true) ||
                            pet.breed?.contains(query, ignoreCase = true) == true ||
                            pet.species.label.contains(query, ignoreCase = true))
                }
                .map { pet ->
                    val dist = if (hasRefLocation && pet.latitude != null && pet.longitude != null) {
                        LocationHelper.calculateDistanceKm(refLat!!, refLon!!, pet.latitude, pet.longitude)
                    } else null
                    PetListItem(pet = pet, distanceKm = dist)
                }
                .filter { item ->
                    maxDistanceKm == null || !hasRefLocation || (item.distanceKm != null && item.distanceKm <= maxDistanceKm)
                }
                .toList()
                .let { list -> if (hasRefLocation) list.sortedBy { it.distanceKm ?: Double.MAX_VALUE } else list }
        }
    }
}
