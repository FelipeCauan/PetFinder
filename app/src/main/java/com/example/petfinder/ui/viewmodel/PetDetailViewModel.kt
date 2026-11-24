package com.example.petfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.SessionManager
import com.example.petfinder.data.local.dao.UserDao
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.data.model.AdoptionStatus
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

/**
 * ViewModel da TelaDetalhesAnimal: observa o pet selecionado, dados do responsável,
 * distância e verifica se o usuário logado é o dono do cadastro.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PetDetailViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val petRepository: PetRepository
) : ViewModel() {

    private val selectedPetId = MutableStateFlow<Long?>(null)
    private val feedbackState = MutableStateFlow<String?>(null)

    private val currentUserFlow = sessionManager.currentUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(null) else userDao.observeUserById(userId)
    }

    private val petFlow = selectedPetId.flatMapLatest { petId ->
        if (petId == null || petId <= 0L) flowOf(null) else petRepository.observePetById(petId)
    }

    private val ownerInfoFlow = petFlow.flatMapLatest { pet ->
        if (pet == null) {
            flowOf(null to emptyList<PetEntity>())
        } else {
            combine(
                userDao.observeUserById(pet.ownerId),
                petRepository.observePetsByOwner(pet.ownerId)
            ) { owner, ownerPets ->
                owner to ownerPets
            }
        }
    }

    val uiState: StateFlow<PetDetailUiState> = combine(
        petFlow,
        ownerInfoFlow,
        currentUserFlow,
        feedbackState
    ) { pet, (owner, ownerPets), currentUser, feedback ->
        if (pet == null) {
            PetDetailUiState(
                isLoading = false,
                pet = null,
                feedbackMessage = feedback
            )
        } else {
            val allPhotos = (listOf(pet.photoPath) + pet.additionalPhotoPaths)
                .filter { it.isNotBlank() }

            val distance = if (
                currentUser?.latitude != null &&
                currentUser.longitude != null &&
                pet.latitude != null &&
                pet.longitude != null
            ) {
                LocationHelper.calculateDistanceKm(
                    currentUser.latitude,
                    currentUser.longitude,
                    pet.latitude,
                    pet.longitude
                )
            } else {
                null
            }

            PetDetailUiState(
                isLoading = false,
                pet = pet,
                photoPaths = allPhotos,
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

    fun loadPet(petId: Long) {
        selectedPetId.value = petId
    }

    fun showFeedbackMessage(message: String) {
        feedbackState.update { message }
    }

    fun clearFeedbackMessage() {
        feedbackState.update { null }
    }
}

class PetDetailViewModelFactory(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val petRepository: PetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PetDetailViewModel::class.java)) {
            return PetDetailViewModel(userDao, sessionManager, petRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
