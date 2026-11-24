package com.example.petfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.SessionManager
import com.example.petfinder.data.local.dao.UserDao
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.data.repository.PetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val user: UserEntity? = null,
    val myPets: List<PetEntity> = emptyList(),
    val registeredCount: Int = 0,
    val adoptedCount: Int = 0
)

/**
 * ViewModel da TelaPerfil: observa o usuário logado e os animais cadastrados por ele no Room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val currentUserFlow = sessionManager.currentUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(null) else userDao.observeUserById(userId)
    }

    private val myPetsFlow = sessionManager.currentUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList()) else petRepository.observePetsByOwner(userId)
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        currentUserFlow,
        myPetsFlow
    ) { user, myPets ->
        ProfileUiState(
            user = user,
            myPets = myPets,
            registeredCount = myPets.size,
            adoptedCount = myPets.count { it.status == AdoptionStatus.ADOTADO }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ProfileUiState()
    )

    fun logout() {
        authRepository.logout()
    }
}

class ProfileViewModelFactory(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(userDao, sessionManager, authRepository, petRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
