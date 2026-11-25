package com.example.petfinder.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.dao.PetDao
import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.util.LocationHelper
import com.example.petfinder.util.PhotoStorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val userId: Long? = null,
    val photoPath: String? = null,
    val originalPhotoPath: String? = null,
    val fullName: String = "",
    val email: String = "",
    val cpf: String = "",
    val phone: String = "",
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val city: String = "",
    val uf: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hasGpsCoordinates: Boolean = false,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val errors: Map<String, String> = emptyMap(),
    val isLoadingUser: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val feedbackMessage: String? = null
) {
    val isBusy: Boolean get() = isSaving || isDeleting
}

class EditProfileViewModel(
    private val authRepository: AuthRepository,
    private val petDao: PetDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    fun loadCurrentUser(onSessionExpired: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUser = true, feedbackMessage = null) }
            val user = authRepository.getLoggedInUser()
            if (user == null) {
                _uiState.update { it.copy(isLoadingUser = false) }
                onSessionExpired()
                return@launch
            }
            _uiState.value = EditProfileUiState(
                userId = user.id,
                photoPath = user.photoPath,
                originalPhotoPath = user.photoPath,
                fullName = user.fullName,
                email = user.email,
                cpf = user.cpf,
                phone = user.phone,
                cep = user.cep,
                street = user.street,
                number = user.number,
                city = user.city,
                uf = user.uf,
                latitude = user.latitude,
                longitude = user.longitude,
                isLoadingUser = false
            )
        }
    }

    fun onPhotoSelected(path: String) {
        if (path.isNotBlank()) _uiState.update { it.copy(photoPath = path) }
    }

    fun onRemovePhoto() = _uiState.update { it.copy(photoPath = null) }

    fun onFullNameChange(value: String) = _uiState.update {
        it.copy(fullName = value, errors = it.errors - KEY_FULL_NAME)
    }

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value, errors = it.errors - KEY_EMAIL)
    }

    fun onCpfChange(value: String) = _uiState.update {
        it.copy(cpf = value.filter { c -> c.isDigit() }.take(11), errors = it.errors - KEY_CPF)
    }

    fun onPhoneChange(value: String) = _uiState.update {
        it.copy(phone = value.filter { c -> c.isDigit() }.take(11), errors = it.errors - KEY_PHONE)
    }

    fun onCepChange(value: String) = _uiState.update {
        it.copy(cep = value.filter { c -> c.isDigit() }.take(8), errors = it.errors - KEY_CEP)
    }

    fun onStreetChange(value: String) = _uiState.update {
        it.copy(street = value, hasGpsCoordinates = false, errors = it.errors - KEY_STREET)
    }

    fun onNumberChange(value: String) = _uiState.update {
        it.copy(number = value, hasGpsCoordinates = false, errors = it.errors - KEY_NUMBER)
    }

    fun onCityChange(value: String) = _uiState.update {
        it.copy(city = value, hasGpsCoordinates = false, errors = it.errors - KEY_CITY)
    }

    fun onUfChange(value: String) = _uiState.update {
        it.copy(uf = value.filter { c -> c.isLetter() }.take(2).uppercase(), hasGpsCoordinates = false, errors = it.errors - KEY_UF)
    }

    fun onNewPasswordChange(value: String) = _uiState.update {
        it.copy(newPassword = value, errors = it.errors - KEY_NEW_PASSWORD - KEY_CONFIRM_PASSWORD)
    }

    fun onConfirmPasswordChange(value: String) = _uiState.update {
        it.copy(confirmPassword = value, errors = it.errors - KEY_CONFIRM_PASSWORD)
    }

    fun fillLocationFromGps(locationHelper: LocationHelper) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true, feedbackMessage = null) }
            locationHelper.getCurrentAddress()
                .onSuccess { captured ->
                    val current = _uiState.value
                    _uiState.update {
                        it.copy(
                            cep = captured.cep?.takeIf { c -> c.length == 8 } ?: current.cep,
                            street = captured.street?.takeIf { s -> s.isNotBlank() } ?: current.street,
                            number = captured.number?.takeIf { n -> n.isNotBlank() } ?: current.number,
                            city = captured.city?.takeIf { c -> c.isNotBlank() } ?: current.city,
                            uf = captured.uf?.takeIf { u -> u.length == 2 } ?: current.uf,
                            latitude = captured.latitude,
                            longitude = captured.longitude,
                            hasGpsCoordinates = true,
                            errors = current.errors - setOf(KEY_CEP, KEY_STREET, KEY_CITY, KEY_UF),
                            isFetchingLocation = false,
                            feedbackMessage = "Endereço preenchido pelo GPS!"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isFetchingLocation = false,
                            feedbackMessage = error.message ?: "Não foi possível obter a localização."
                        )
                    }
                }
        }
    }

    fun saveChanges(
        context: Context,
        locationHelper: LocationHelper,
        onSuccess: () -> Unit,
        onSessionExpired: () -> Unit
    ) {
        val current = _uiState.value
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, feedbackMessage = null) }
            val loggedUser = authRepository.getLoggedInUser()
            if (loggedUser == null) {
                _uiState.update { it.copy(isSaving = false) }
                onSessionExpired()
                return@launch
            }

            val sameAddress = current.street.trim() == loggedUser.street.trim() &&
                current.number.trim() == loggedUser.number.trim() &&
                current.city.trim() == loggedUser.city.trim() &&
                current.uf.trim().equals(loggedUser.uf.trim(), ignoreCase = true)

            val (lat, lon) = when {
                current.hasGpsCoordinates -> current.latitude to current.longitude
                sameAddress -> loggedUser.latitude to loggedUser.longitude
                else -> {
                    val query = "${current.street.trim()}, ${current.number.trim()}, ${current.city.trim()} - ${current.uf.trim()}"
                    locationHelper.forwardGeocode(query) ?: (null to null)
                }
            }

            authRepository.updateProfile(
                userId = loggedUser.id,
                fullName = current.fullName,
                email = current.email,
                cpf = current.cpf,
                phone = current.phone,
                cep = current.cep,
                street = current.street,
                number = current.number,
                city = current.city,
                uf = current.uf,
                latitude = lat,
                longitude = lon,
                photoPath = current.photoPath,
                newPassword = current.newPassword.takeIf { it.isNotBlank() }
            ).onSuccess { updatedUser ->
                val oldPhoto = current.originalPhotoPath
                if (!oldPhoto.isNullOrBlank() && oldPhoto != updatedUser.photoPath) {
                    PhotoStorageHelper.deleteLocalPhotos(context, listOf(oldPhoto))
                }
                _uiState.update { it.copy(isSaving = false, newPassword = "", confirmPassword = "") }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        feedbackMessage = error.message ?: "Não foi possível salvar as alterações."
                    )
                }
            }
        }
    }

    fun deleteAccount(
        context: Context,
        onDeleted: () -> Unit,
        onSessionExpired: () -> Unit
    ) {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, feedbackMessage = null) }
            val loggedUser = authRepository.getLoggedInUser()
            if (loggedUser == null) {
                _uiState.update { it.copy(isDeleting = false) }
                onSessionExpired()
                return@launch
            }

            val ownedPets = petDao.getPetsByOwner(loggedUser.id)
            val filesToDelete = buildList {
                add(loggedUser.photoPath)
                add(current.photoPath)
                ownedPets.forEach {
                    add(it.photoPath)
                    addAll(it.additionalPhotoPaths)
                }
            }.distinct()

            authRepository.deleteAccount(loggedUser.id)
                .onSuccess {
                    PhotoStorageHelper.deleteLocalPhotos(context, filesToDelete)
                    _uiState.value = EditProfileUiState(isLoadingUser = false)
                    onDeleted()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            feedbackMessage = error.message ?: "Não foi possível excluir a conta."
                        )
                    }
                }
        }
    }

    fun showFeedbackMessage(message: String) = _uiState.update { it.copy(feedbackMessage = message) }
    fun clearFeedbackMessage() = _uiState.update { it.copy(feedbackMessage = null) }

    companion object {
        const val KEY_FULL_NAME = "fullName"
        const val KEY_EMAIL = "email"
        const val KEY_CPF = "cpf"
        const val KEY_PHONE = "phone"
        const val KEY_CEP = "cep"
        const val KEY_STREET = "street"
        const val KEY_NUMBER = "number"
        const val KEY_CITY = "city"
        const val KEY_UF = "uf"
        const val KEY_NEW_PASSWORD = "newPassword"
        const val KEY_CONFIRM_PASSWORD = "confirmPassword"

        internal fun validate(state: EditProfileUiState): Map<String, String> = buildMap {
            if (state.fullName.trim().length < 3) put(KEY_FULL_NAME, "Informe o nome completo.")
            AuthViewModel.validateEmail(state.email)?.let { put(KEY_EMAIL, it) }
            if (state.cpf.filter { it.isDigit() }.length != 11) put(KEY_CPF, "O CPF deve conter 11 dígitos.")
            if (state.phone.filter { it.isDigit() }.length !in 10..11) {
                put(KEY_PHONE, "Informe o telefone com DDD (10 ou 11 dígitos).")
            }
            if (state.cep.filter { it.isDigit() }.length != 8) put(KEY_CEP, "CEP inválido.")
            if (state.street.trim().isEmpty()) put(KEY_STREET, "Informe a rua.")
            if (state.number.trim().isEmpty()) put(KEY_NUMBER, "Informe o N°.")
            if (state.city.trim().isEmpty()) put(KEY_CITY, "Informe a cidade.")
            if (state.uf.trim().filter { it.isLetter() }.length != 2) put(KEY_UF, "UF.")

            val hasPassword = state.newPassword.isNotEmpty() || state.confirmPassword.isNotEmpty()
            if (hasPassword) {
                if (state.newPassword.length < 8) {
                    put(KEY_NEW_PASSWORD, "A nova senha deve ter pelo menos 8 caracteres.")
                }
                when {
                    state.confirmPassword.isEmpty() -> put(KEY_CONFIRM_PASSWORD, "Confirme a nova senha.")
                    state.newPassword != state.confirmPassword -> put(KEY_CONFIRM_PASSWORD, "As senhas não coincidem.")
                }
            }
        }
    }
}
