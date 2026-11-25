package com.example.petfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.SessionManager
import com.example.petfinder.data.local.dao.PetDao
import com.example.petfinder.data.local.dao.UserDao
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.util.LocationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class PetDetailUiState(
    val isLoading: Boolean = true,
    val pet: PetEntity? = null,
    val photoPaths: List<String> = emptyList(),
    val owner: UserEntity? = null,
    val ownerPetsCount: Int = 0,
    val ownerAdoptedCount: Int = 0,
    val isOwner: Boolean = false,
    val distanceKm: Double? = null,
    val feedbackMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class PetDetailViewModel(
    authRepository: AuthRepository,
    private val userDao: UserDao,
    sessionManager: SessionManager,
    private val petDao: PetDao
) : ViewModel() {

    private val selectedPetId = MutableStateFlow<Long?>(null)
    private val feedbackState = MutableStateFlow<String?>(null)

    private val petFlow = selectedPetId.flatMapLatest { petId ->
        if (petId == null || petId <= 0L) flowOf(null) else petDao.observePetById(petId)
    }

    private val ownerInfoFlow = petFlow.flatMapLatest { pet ->
        if (pet == null) flowOf(null to emptyList())
        else combine(userDao.observeUserById(pet.ownerId), petDao.observePetsByOwner(pet.ownerId)) { o, p -> o to p }
    }

    val uiState: StateFlow<PetDetailUiState> = combine(
        petFlow,
        ownerInfoFlow,
        authRepository.observeCurrentUser(),
        sessionManager.temporaryLocation,
        feedbackState
    ) { pet, (owner, ownerPets), currentUser, tempLocation, feedback ->
        if (pet == null) {
            PetDetailUiState(isLoading = false, feedbackMessage = feedback)
        } else {
            val refLat = tempLocation?.latitude ?: currentUser?.latitude
            val refLon = tempLocation?.longitude ?: currentUser?.longitude
            val distance = if (refLat != null && refLon != null && pet.latitude != null && pet.longitude != null) {
                LocationHelper.calculateDistanceKm(refLat, refLon, pet.latitude, pet.longitude)
            } else null

            PetDetailUiState(
                isLoading = false,
                pet = pet,
                photoPaths = (listOf(pet.photoPath) + pet.additionalPhotoPaths).filter { it.isNotBlank() },
                owner = owner,
                ownerPetsCount = ownerPets.size,
                ownerAdoptedCount = ownerPets.count { it.status == AdoptionStatus.ADOTADO },
                isOwner = currentUser != null && pet.ownerId == currentUser.id,
                distanceKm = distance,
                feedbackMessage = feedback
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PetDetailUiState()
    )

    fun loadPet(petId: Long) { selectedPetId.value = petId }
    fun showFeedbackMessage(message: String) { feedbackState.value = message }
    fun clearFeedbackMessage() { feedbackState.value = null }
}
