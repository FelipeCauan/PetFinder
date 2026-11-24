package com.example.petfinder.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.data.repository.PetRepository
import com.example.petfinder.util.LocationHelper
import com.example.petfinder.util.PhotoStorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class EditProfileUiState(
    val userId: Long? = null,
    val photoPath: String? = null,
    val originalPhotoPath: String? = null,
    val fullName: String = "",
    val email: String = "",
    val cpf: String = "",
    val addressText: String = "",
    val initialAddressText: String = "",
    val gpsAddressText: String? = null,
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val city: String = "",
    val uf: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val newPassword: String = "",
    val confirmPassword: String = "",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val cpfError: String? = null,
    val addressError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoadingUser: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val feedbackMessage: String? = null
) {
    val isBusy: Boolean
        get() = isSaving || isDeleting
}

/**
 * ViewModel responsável pela tela de edição de perfil e exclusão da conta do usuário.
 */
class EditProfileViewModel(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository
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
            val initialAddress = user.editableAddressLine
            _uiState.value = EditProfileUiState(
                userId = user.id,
                photoPath = user.photoPath,
                originalPhotoPath = user.photoPath,
                fullName = user.fullName,
                email = user.email,
                cpf = user.cpf,
                addressText = initialAddress,
                initialAddressText = initialAddress,
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
        if (path.isBlank()) return
        _uiState.update { it.copy(photoPath = path) }
    }

    fun onRemovePhoto() {
        _uiState.update { it.copy(photoPath = null) }
    }

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value, fullNameError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onCpfChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(11)
        _uiState.update { it.copy(cpf = digits, cpfError = null) }
    }

    fun onAddressChange(value: String) {
        _uiState.update { it.copy(addressText = value, addressError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                newPassword = value,
                newPasswordError = null,
                confirmPasswordError = null
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun fillLocationFromGps(locationHelper: LocationHelper) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true, feedbackMessage = null) }
            locationHelper.getCurrentAddress()
                .onSuccess { captured ->
                    val current = _uiState.value
                    val nextStreet = captured.street?.takeIf { it.isNotBlank() } ?: current.street
                    val nextNumber = captured.number?.takeIf { it.isNotBlank() } ?: ""
                    val nextCity = captured.city?.takeIf { it.isNotBlank() } ?: current.city
                    val nextUf = captured.uf?.takeIf { it.length == 2 } ?: current.uf
                    val nextCep = captured.cep?.takeIf { it.length == 8 } ?: current.cep

                    val streetPart = listOf(nextStreet, nextNumber)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    val cityPart = listOf(nextCity, nextUf)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    val formatted = listOf(streetPart, cityPart)
                        .filter { it.isNotBlank() }
                        .joinToString(" — ")
                        .ifBlank {
                            String.format(
                                Locale.US,
                                "GPS: %.4f, %.4f",
                                captured.latitude,
                                captured.longitude
                            )
                        }

                    _uiState.update {
                        it.copy(
                            addressText = formatted,
                            gpsAddressText = formatted,
                            cep = nextCep,
                            street = nextStreet,
                            number = nextNumber,
                            city = nextCity,
                            uf = nextUf,
                            latitude = captured.latitude,
                            longitude = captured.longitude,
                            addressError = null,
                            isFetchingLocation = false,
                            feedbackMessage = "Endereço atualizado pelo GPS!"
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
        val validation = validate(current)
        if (validation.hasErrors) {
            _uiState.update {
                it.copy(
                    fullNameError = validation.fullNameError,
                    emailError = validation.emailError,
                    cpfError = validation.cpfError,
                    addressError = validation.addressError,
                    newPasswordError = validation.newPasswordError,
                    confirmPasswordError = validation.confirmPasswordError
                )
            }
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

            val trimmedAddress = current.addressText.trim()
            val matchesGps = current.gpsAddressText != null &&
                trimmedAddress == current.gpsAddressText.trim()
            val matchesInitial = trimmedAddress == current.initialAddressText.trim()

            val resolvedAddress = when {
                matchesGps -> ParsedAddress(
                    cep = current.cep,
                    street = current.street,
                    number = current.number,
                    city = current.city,
                    uf = current.uf
                )
                matchesInitial -> ParsedAddress(
                    cep = loggedUser.cep,
                    street = loggedUser.street,
                    number = loggedUser.number,
                    city = loggedUser.city,
                    uf = loggedUser.uf
                )
                else -> parseAddressText(trimmedAddress)
            }

            val (resolvedLat, resolvedLon) = when {
                matchesGps -> current.latitude to current.longitude
                matchesInitial -> loggedUser.latitude to loggedUser.longitude
                else -> locationHelper.forwardGeocode(trimmedAddress) ?: (null to null)
            }

            authRepository.updateProfile(
                userId = loggedUser.id,
                fullName = current.fullName,
                email = current.email,
                cpf = current.cpf,
                cep = resolvedAddress.cep,
                street = resolvedAddress.street,
                number = resolvedAddress.number,
                city = resolvedAddress.city,
                uf = resolvedAddress.uf,
                latitude = resolvedLat,
                longitude = resolvedLon,
                photoPath = current.photoPath,
                newPassword = current.newPassword.takeIf { it.isNotBlank() }
            ).onSuccess { updatedUser ->
                val oldPhoto = current.originalPhotoPath
                if (!oldPhoto.isNullOrBlank() && oldPhoto != updatedUser.photoPath) {
                    PhotoStorageHelper.deleteLocalPhotos(context, listOf(oldPhoto))
                }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        newPassword = "",
                        confirmPassword = ""
                    )
                }
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

            val ownedPets = petRepository.getPetsByOwner(loggedUser.id)
            val filesToDelete = buildList {
                add(loggedUser.photoPath)
                add(current.photoPath)
                ownedPets.forEach { pet ->
                    add(pet.photoPath)
                    addAll(pet.additionalPhotoPaths)
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

    fun showFeedbackMessage(message: String) {
        _uiState.update { it.copy(feedbackMessage = message) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    internal data class ParsedAddress(
        val cep: String,
        val street: String,
        val number: String,
        val city: String,
        val uf: String
    )

    internal data class ValidationResult(
        val fullNameError: String? = null,
        val emailError: String? = null,
        val cpfError: String? = null,
        val addressError: String? = null,
        val newPasswordError: String? = null,
        val confirmPasswordError: String? = null
    ) {
        val hasErrors: Boolean
            get() = listOf(
                fullNameError,
                emailError,
                cpfError,
                addressError,
                newPasswordError,
                confirmPasswordError
            ).any { it != null }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private val CEP_REGEX = Regex("(?:\\(?CEP\\s*)?(\\d{5})-?(\\d{3})\\)?", RegexOption.IGNORE_CASE)
        private val TRAILING_UF_REGEX = Regex("^(.*?)[/,\\-]\\s*([A-Za-z]{2})$")
        private val STREET_NUMBER_REGEX = Regex("^(.+?),\\s*(\\d+[A-Za-z0-9\\-]*|S/N|s/n)$")

        internal fun validate(state: EditProfileUiState): ValidationResult {
            val fullNameError = if (state.fullName.trim().length < 3) {
                "Informe o nome completo."
            } else {
                null
            }

            val trimmedEmail = state.email.trim()
            val emailError = when {
                trimmedEmail.isEmpty() -> "Informe o e-mail."
                !EMAIL_REGEX.matches(trimmedEmail) -> "E-mail inválido."
                else -> null
            }

            val cpfDigits = state.cpf.filter { it.isDigit() }
            val cpfError = if (cpfDigits.length != 11) {
                "O CPF deve conter 11 dígitos."
            } else {
                null
            }

            val addressError = if (state.addressText.trim().length < 3) {
                "Informe o endereço."
            } else {
                null
            }

            val hasAnyPasswordInput = state.newPassword.isNotEmpty() || state.confirmPassword.isNotEmpty()
            val newPasswordError = if (hasAnyPasswordInput && state.newPassword.length < 8) {
                "A nova senha deve ter pelo menos 8 caracteres."
            } else {
                null
            }

            val confirmPasswordError = when {
                hasAnyPasswordInput && state.confirmPassword.isEmpty() -> "Confirme a nova senha."
                hasAnyPasswordInput && state.newPassword != state.confirmPassword -> "As senhas não coincidem."
                else -> null
            }

            return ValidationResult(
                fullNameError = fullNameError,
                emailError = emailError,
                cpfError = cpfError,
                addressError = addressError,
                newPasswordError = newPasswordError,
                confirmPasswordError = confirmPasswordError
            )
        }

        internal fun parseAddressText(raw: String): ParsedAddress {
            val cepMatch = CEP_REGEX.find(raw)
            val cepDigits = cepMatch?.let { "${it.groupValues[1]}${it.groupValues[2]}" }.orEmpty()
            val withoutCep = CEP_REGEX.replace(raw, "").trim().trimEnd(',', '-', '—', ' ')

            val separator = when {
                withoutCep.contains(" — ") -> " — "
                withoutCep.contains(" - ") -> " - "
                else -> null
            }

            if (separator != null) {
                val splitIdx = withoutCep.lastIndexOf(separator)
                val left = withoutCep.substring(0, splitIdx).trim()
                val right = withoutCep.substring(splitIdx + separator.length).trim()

                val streetMatch = STREET_NUMBER_REGEX.matchEntire(left)
                val street = streetMatch?.groupValues?.get(1)?.trim() ?: left
                val number = streetMatch?.groupValues?.get(2)?.trim().orEmpty()

                val (city, uf) = parseCityAndUf(right)
                return ParsedAddress(
                    cep = cepDigits,
                    street = street,
                    number = number,
                    city = city,
                    uf = uf
                )
            }

            val streetMatch = STREET_NUMBER_REGEX.matchEntire(withoutCep)
            return if (streetMatch != null) {
                ParsedAddress(
                    cep = cepDigits,
                    street = streetMatch.groupValues[1].trim(),
                    number = streetMatch.groupValues[2].trim(),
                    city = "",
                    uf = ""
                )
            } else {
                ParsedAddress(
                    cep = cepDigits,
                    street = withoutCep,
                    number = "",
                    city = "",
                    uf = ""
                )
            }
        }

        private fun parseCityAndUf(regionText: String): Pair<String, String> {
            val trimmed = regionText.trim()
            val ufMatch = TRAILING_UF_REGEX.matchEntire(trimmed)
            if (ufMatch != null) {
                val beforeUf = ufMatch.groupValues[1].trim()
                val uf = ufMatch.groupValues[2].uppercase()
                val city = beforeUf.substringAfterLast(",").trim()
                return city to uf
            }
            val city = trimmed.substringAfterLast(",").trim()
            return city to ""
        }
    }
}

class EditProfileViewModelFactory(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            return EditProfileViewModel(authRepository, petRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
