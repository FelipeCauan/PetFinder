package com.example.petfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.SessionManager
import com.example.petfinder.data.local.dao.PetDao
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.util.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PetListItem(
    val pet: PetEntity,
    val distanceKm: Double? = null
)

data class PetFilters(
    val searchQuery: String = "",
    val selectedSpecies: PetSpecies? = null,
    val selectedSex: PetSex? = null,
    val selectedSize: PetSize? = null,
    val selectedStatus: AdoptionStatus? = null,
    val maxDistanceKm: Double? = null
) {
    val activeCount: Int
        get() = listOf(selectedSex, selectedSize, selectedStatus, maxDistanceKm).count { it != null }
}

data class HomeUiState(
    val userFirstName: String = "",
    val userPhotoPath: String? = null,
    val userLocationLabel: String = "Localização não informada",
    val filters: PetFilters = PetFilters(),
    val hasLocationReference: Boolean = false,
    val pets: List<PetListItem> = emptyList(),
    val isRefreshingLocation: Boolean = false,
    val feedbackMessage: String? = null
)

private data class HomeSessionOverlay(
    val filters: PetFilters = PetFilters(),
    val gpsLocationLabel: String? = null,
    val isRefreshingLocation: Boolean = false,
    val feedbackMessage: String? = null
)

class HomeViewModel(
    authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    petDao: PetDao
) : ViewModel() {

    private val overlayState = MutableStateFlow(HomeSessionOverlay())

    val uiState: StateFlow<HomeUiState> = combine(
        authRepository.observeCurrentUser(),
        petDao.observeAllPets(),
        overlayState,
        sessionManager.temporaryLocation
    ) { user, allPets, overlay, tempLocation ->
        val firstName = user?.fullName?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Visitante"
        val locationLabel = overlay.gpsLocationLabel ?: user?.shortLocationLabel ?: "Localização não informada"
        val refLat = tempLocation?.latitude ?: user?.latitude
        val refLon = tempLocation?.longitude ?: user?.longitude
        val f = overlay.filters

        val pets = filterAndSortPets(
            allPets = allPets,
            searchQuery = f.searchQuery,
            selectedSpecies = f.selectedSpecies,
            selectedSex = f.selectedSex,
            selectedSize = f.selectedSize,
            selectedStatus = f.selectedStatus,
            maxDistanceKm = f.maxDistanceKm,
            refLat = refLat,
            refLon = refLon
        )

        HomeUiState(
            userFirstName = firstName,
            userPhotoPath = user?.photoPath,
            userLocationLabel = locationLabel,
            filters = f,
            hasLocationReference = refLat != null && refLon != null,
            pets = pets,
            isRefreshingLocation = overlay.isRefreshingLocation,
            feedbackMessage = overlay.feedbackMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChange(query: String) =
        overlayState.update { it.copy(filters = it.filters.copy(searchQuery = query)) }

    fun onSpeciesSelected(species: PetSpecies?) =
        overlayState.update { it.copy(filters = it.filters.copy(selectedSpecies = species)) }

    fun applyAdvancedFilters(sex: PetSex?, size: PetSize?, status: AdoptionStatus?, maxDistanceKm: Double?) {
        overlayState.update {
            it.copy(
                filters = it.filters.copy(
                    selectedSex = sex,
                    selectedSize = size,
                    selectedStatus = status,
                    maxDistanceKm = maxDistanceKm
                )
            )
        }
    }

    fun clearAdvancedFilters() {
        overlayState.update {
            it.copy(
                filters = it.filters.copy(
                    selectedSex = null,
                    selectedSize = null,
                    selectedStatus = null,
                    maxDistanceKm = null
                )
            )
        }
    }

    fun refreshLocation(locationHelper: LocationHelper) {
        viewModelScope.launch {
            overlayState.update { it.copy(isRefreshingLocation = true, feedbackMessage = null) }
            locationHelper.getCurrentAddress()
                .onSuccess { captured ->
                    val label = listOfNotNull(captured.city, captured.uf).takeIf { it.isNotEmpty() }?.joinToString(", ")
                    sessionManager.setTemporaryLocation(captured.latitude, captured.longitude)
                    overlayState.update {
                        it.copy(
                            gpsLocationLabel = label ?: it.gpsLocationLabel,
                            isRefreshingLocation = false,
                            feedbackMessage = "Localização GPS atualizada."
                        )
                    }
                }
                .onFailure { error ->
                    overlayState.update {
                        it.copy(
                            isRefreshingLocation = false,
                            feedbackMessage = error.message ?: "Não foi possível obter a localização."
                        )
                    }
                }
        }
    }

    fun showFeedbackMessage(message: String) = overlayState.update { it.copy(feedbackMessage = message) }
    fun clearFeedbackMessage() = overlayState.update { it.copy(feedbackMessage = null) }

    fun resetState() {
        overlayState.value = HomeSessionOverlay()
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
                .let { list -> if (hasRefLocation) list.sortedBy { it.distanceKm ?: Double.MAX_VALUE } else list }
        }
    }
}
