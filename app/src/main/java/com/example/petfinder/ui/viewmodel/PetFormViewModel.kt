package com.example.petfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.data.repository.PetRepository
import com.example.petfinder.util.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class AgeUnit(val label: String) {
    ANOS("anos"),
    MESES("meses")
}

data class PetFormUiState(
    val editingPetId: Long? = null,
    val photoPaths: List<String> = emptyList(),
    val name: String = "",
    val species: PetSpecies = PetSpecies.CACHORRO,
    val sex: PetSex = PetSex.MACHO,
    val size: PetSize = PetSize.MEDIO,
    val selectedBreedOption: String = DEFAULT_BREED,
    val customBreed: String = "",
    val estimatedAgeValue: String = "",
    val estimatedAgeUnit: AgeUnit = AgeUnit.ANOS,
    val animalRg: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoError: String? = null,
    val nameError: String? = null,
    val addressError: String? = null,
    val isLoading: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val feedbackMessage: String? = null
) {
    companion object {
        const val DEFAULT_BREED = "SRD (sem raça definida)"
        const val OTHER_BREED = "Outra (informar)"
        const val MAX_PHOTOS = 6
    }
}

/**
 * ViewModel da TelaCadastroAnimal: gerencia validações, fotos, GPS e persistência no Room.
 */
class PetFormViewModel(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetFormUiState())
    val uiState: StateFlow<PetFormUiState> = _uiState.asStateFlow()

    fun addPhotoPath(path: String) {
        _uiState.update { state ->
            if (state.photoPaths.size >= PetFormUiState.MAX_PHOTOS || path.isBlank()) {
                state
            } else {
                state.copy(
                    photoPaths = state.photoPaths + path,
                    photoError = null
                )
            }
        }
    }

    fun removePhotoAt(index: Int) {
        _uiState.update { state ->
            if (index !in state.photoPaths.indices) return@update state
            val updated = state.photoPaths.toMutableList().apply { removeAt(index) }
            state.copy(photoPaths = updated)
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onSpeciesSelected(species: PetSpecies) {
        _uiState.update { it.copy(species = species) }
    }

    fun onSexSelected(sex: PetSex) {
        _uiState.update { it.copy(sex = sex) }
    }

    fun onSizeSelected(size: PetSize) {
        _uiState.update { it.copy(size = size) }
    }

    fun onBreedOptionSelected(option: String) {
        _uiState.update {
            it.copy(
                selectedBreedOption = option,
                customBreed = if (option == PetFormUiState.OTHER_BREED) it.customBreed else ""
            )
        }
    }

    fun onCustomBreedChange(value: String) {
        _uiState.update { it.copy(customBreed = value) }
    }

    fun onEstimatedAgeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(2)
        _uiState.update { it.copy(estimatedAgeValue = digits) }
    }

    fun onToggleAgeUnit() {
        _uiState.update { state ->
            val next = if (state.estimatedAgeUnit == AgeUnit.ANOS) {
                AgeUnit.MESES
            } else {
                AgeUnit.ANOS
            }
            state.copy(estimatedAgeUnit = next)
        }
    }

    fun onAnimalRgChange(value: String) {
        _uiState.update { it.copy(animalRg = value) }
    }

    fun onAddressChange(value: String) {
        _uiState.update { it.copy(address = value, addressError = null) }
    }

    fun fillLocationFromGps(locationHelper: LocationHelper) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingLocation = true, feedbackMessage = null) }
            locationHelper.getCurrentAddress()
                .onSuccess { captured ->
                    val streetPart = listOfNotNull(
                        captured.street?.takeIf { it.isNotBlank() },
                        captured.number?.takeIf { it.isNotBlank() }
                    ).joinToString(", ")

                    val cityPart = listOfNotNull(
                        captured.city?.takeIf { it.isNotBlank() },
                        captured.uf?.takeIf { it.isNotBlank() }
                    ).joinToString(" - ")

                    val formattedAddress = listOf(streetPart, cityPart)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
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
                            address = formattedAddress,
                            latitude = captured.latitude,
                            longitude = captured.longitude,
                            addressError = null,
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

    fun submit(
        onSuccess: () -> Unit,
        onSessionExpired: () -> Unit = {}
    ) {
        val current = _uiState.value
        val validation = validate(current)
        if (validation.hasErrors) {
            _uiState.update {
                it.copy(
                    photoError = validation.photoError,
                    nameError = validation.nameError,
                    addressError = validation.addressError,
                    feedbackMessage = "Preencha todos os campos obrigatórios."
                )
            }
            return
        }

        val resolvedBreed = when (current.selectedBreedOption) {
            PetFormUiState.OTHER_BREED -> current.customBreed.trim().ifBlank { null }
            else -> current.selectedBreedOption
        }

        val resolvedAge = current.estimatedAgeValue.trim().takeIf { it.isNotEmpty() }?.let { num ->
            val unitLabel = if (num == "1") {
                if (current.estimatedAgeUnit == AgeUnit.ANOS) "ano" else "mês"
            } else {
                current.estimatedAgeUnit.label
            }
            "$num $unitLabel"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, feedbackMessage = null) }
            val loggedUser = authRepository.getLoggedInUser()
            if (loggedUser == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = "Sessão inválida ou expirada. Faça login novamente."
                    )
                }
                onSessionExpired()
                return@launch
            }

            val pet = PetEntity(
                id = current.editingPetId ?: 0L,
                ownerId = loggedUser.id,
                name = current.name.trim(),
                species = current.species,
                sex = current.sex,
                photoPath = current.photoPaths.first(),
                additionalPhotoPaths = current.photoPaths.drop(1),
                animalRg = current.animalRg.trim().ifBlank { null },
                breed = resolvedBreed,
                estimatedAge = resolvedAge,
                address = current.address.trim(),
                latitude = current.latitude,
                longitude = current.longitude,
                size = current.size,
                status = AdoptionStatus.DISPONIVEL
            )

            runCatching {
                petRepository.insertPet(pet)
            }.onSuccess {
                _uiState.value = PetFormUiState()
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = error.message ?: "Erro ao salvar animal no banco local."
                    )
                }
            }
        }
    }

    fun resetForm() {
        _uiState.value = PetFormUiState()
    }

    fun showFeedbackMessage(message: String) {
        _uiState.update { it.copy(feedbackMessage = message) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    internal data class ValidationResult(
        val photoError: String? = null,
        val nameError: String? = null,
        val addressError: String? = null
    ) {
        val hasErrors: Boolean
            get() = photoError != null || nameError != null || addressError != null
    }

    companion object {
        val BREED_OPTIONS = listOf(
            PetFormUiState.DEFAULT_BREED,
            "Vira-lata",
            "Labrador",
            "Golden Retriever",
            "Poodle",
            "Pinscher",
            "Shih-tzu",
            "Siamês",
            "Persa",
            "Maine Coon",
            PetFormUiState.OTHER_BREED
        )

        internal fun validate(state: PetFormUiState): ValidationResult {
            val photoError = if (state.photoPaths.isEmpty()) {
                "Adicione pelo menos 1 foto do animal."
            } else {
                null
            }
            val nameError = if (state.name.trim().isEmpty()) {
                "Informe o nome do animal."
            } else {
                null
            }
            val addressError = if (state.address.trim().length < 3) {
                "Informe a localização do animal."
            } else {
                null
            }
            return ValidationResult(
                photoError = photoError,
                nameError = nameError,
                addressError = addressError
            )
        }
    }
}

class PetFormViewModelFactory(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PetFormViewModel::class.java)) {
            return PetFormViewModel(authRepository, petRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
