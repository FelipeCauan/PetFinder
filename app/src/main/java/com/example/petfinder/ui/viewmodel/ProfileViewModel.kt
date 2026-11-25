package com.example.petfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.dao.PetDao
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val user: UserEntity? = null,
    val myPets: List<PetEntity> = emptyList(),
    val registeredCount: Int = 0,
    val adoptedCount: Int = 0,
    val feedbackMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    petDao: PetDao
) : ViewModel() {

    private val feedbackState = MutableStateFlow<String?>(null)

    private val userAndPetsFlow = authRepository.observeCurrentUser().flatMapLatest { user ->
        if (user == null) flowOf(null to emptyList())
        else petDao.observePetsByOwner(user.id).map { pets -> user to pets }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        userAndPetsFlow,
        feedbackState
    ) { (user, myPets), feedback ->
        ProfileUiState(
            user = user,
            myPets = myPets,
            registeredCount = myPets.size,
            adoptedCount = myPets.count { it.status == AdoptionStatus.ADOTADO },
            feedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ProfileUiState()
    )

    fun showFeedbackMessage(message: String) { feedbackState.value = message }
    fun clearFeedbackMessage() { feedbackState.value = null }
    fun logout() = authRepository.logout()
}
