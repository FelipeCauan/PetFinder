package com.example.petfinder.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val phone: String = "",
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
                phone = user.phone,
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

    fun onAddressChange(value: String) = _uiState.update {
        it.copy(addressText = value, errors = it.errors - KEY_ADDRESS)
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
                    val nextStreet = captured.street?.takeIf { it.isNotBlank() } ?: current.street
                    val nextNumber = captured.number?.takeIf { it.isNotBlank() } ?: ""
                    val nextCity = captured.city?.takeIf { it.isNotBlank() } ?: current.city
                    val nextUf = captured.uf?.takeIf { it.length == 2 } ?: current.uf
                    val nextCep = captured.cep?.takeIf { it.length == 8 } ?: current.cep

                    val streetPart = listOf(nextStreet, nextNumber).filter { it.isNotBlank() }.joinToString(", ")
                    val cityPart = listOf(nextCity, nextUf).filter { it.isNotBlank() }.joinToString(", ")
                    val formatted = listOf(streetPart, cityPart)
                        .filter { it.isNotBlank() }
                        .joinToString(" — ")
                        .ifBlank { String.format(Locale.US, "GPS: %.4f, %.4f", captured.latitude, captured.longitude) }

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
                            errors = it.errors - KEY_ADDRESS,
                            isFetchingLocation = false,
                            feedbackMessage = "Localização capturada pelo GPS."
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

            val trimmedAddress = current.addressText.trim()
            val matchesGps = current.gpsAddressText != null && trimmedAddress == current.gpsAddressText.trim()
            val matchesInitial = trimmedAddress == current.initialAddressText.trim()

            val resolved = when {
                matchesGps -> ParsedAddress(current.cep, current.street, current.number, current.city, current.uf)
                matchesInitial -> ParsedAddress(loggedUser.cep, loggedUser.street, loggedUser.number, loggedUser.city, loggedUser.uf)
                else -> parseAddressText(trimmedAddress)
            }

            val (lat, lon) = when {
                matchesGps -> current.latitude to current.longitude
                matchesInitial -> loggedUser.latitude to loggedUser.longitude
                else -> locationHelper.forwardGeocode(trimmedAddress) ?: (null to null)
            }

            authRepository.updateProfile(
                userId = loggedUser.id,
                fullName = current.fullName,
                email = current.email,
                cpf = current.cpf,
                phone = current.phone,
                cep = resolved.cep,
                street = resolved.street,
                number = resolved.number,
                city = resolved.city,
                uf = resolved.uf,
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

            val ownedPets = petRepository.getPetsByOwner(loggedUser.id)
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

    internal data class ParsedAddress(
        val cep: String,
        val street: String,
        val number: String,
        val city: String,
        val uf: String
    )

    companion object {
        const val KEY_FULL_NAME = "fullName"
        const val KEY_EMAIL = "email"
        const val KEY_CPF = "cpf"
        const val KEY_PHONE = "phone"
        const val KEY_ADDRESS = "address"
        const val KEY_NEW_PASSWORD = "newPassword"
        const val KEY_CONFIRM_PASSWORD = "confirmPassword"

        private val CEP_REGEX = Regex("(?:\\(?CEP\\s*)?(\\d{5})-?(\\d{3})\\)?", RegexOption.IGNORE_CASE)

        internal fun validate(state: EditProfileUiState): Map<String, String> = buildMap {
            if (state.fullName.trim().length < 3) put(KEY_FULL_NAME, "Informe o nome completo.")
            AuthViewModel.validateEmail(state.email)?.let { put(KEY_EMAIL, it) }
            if (state.cpf.filter { it.isDigit() }.length != 11) put(KEY_CPF, "O CPF deve conter 11 dígitos.")
            if (state.phone.filter { it.isDigit() }.length !in 10..11) {
                put(KEY_PHONE, "Informe o telefone com DDD (10 ou 11 dígitos).")
            }
            if (state.addressText.trim().length < 3) put(KEY_ADDRESS, "Informe o endereço.")

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

        internal fun parseAddressText(raw: String): ParsedAddress {
            val cepMatch = CEP_REGEX.find(raw)
            val cepDigits = cepMatch?.let { "${it.groupValues[1]}${it.groupValues[2]}" }.orEmpty()
            val clean = CEP_REGEX.replace(raw, "").trim().trimEnd(',', '-', '—', ' ')

            val parts = clean.split(" — ", " - ", limit = 2)
            val left = parts[0].trim()
            val right = parts.getOrNull(1)?.trim().orEmpty()

            val streetParts = left.split(",", limit = 2).map { it.trim() }
            val street = streetParts[0]
            val number = streetParts.getOrNull(1).orEmpty()

            val regionParts = right.split(",", "/", "-").map { it.trim() }.filter { it.isNotEmpty() }
            val last = regionParts.lastOrNull().orEmpty()
            val uf = if (last.length == 2 && last.all { it.isLetter() }) last.uppercase() else ""
            val city = when {
                uf.isNotEmpty() && regionParts.size >= 2 -> regionParts[regionParts.size - 2]
                uf.isEmpty() -> last
                else -> ""
            }

            return ParsedAddress(cepDigits, street, number, city, uf)
        }
    }
}
