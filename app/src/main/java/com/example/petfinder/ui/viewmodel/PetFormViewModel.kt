package com.example.petfinder.ui.viewmodel

import android.content.Context
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
import com.example.petfinder.util.PhotoStorageHelper
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
    val originalOwnerId: Long? = null,
    val originalCreatedAt: Long? = null,
    val originalPhotoPaths: List<String> = emptyList(),
    val initialAddress: String? = null,
    val gpsAddress: String? = null,
    val photoPaths: List<String> = emptyList(),
    val status: AdoptionStatus = AdoptionStatus.DISPONIVEL,
    val name: String = "",
    val species: PetSpecies = PetSpecies.CACHORRO,
    val sex: PetSex = PetSex.MACHO,
    val size: PetSize = PetSize.MEDIO,
    val selectedBreedOption: String = DEFAULT_BREED,
    val customBreed: String = "",
    val estimatedAgeValue: String = "",
    val estimatedAgeUnit: AgeUnit = AgeUnit.ANOS,
    val animalRg: String = "",
    val description: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoError: String? = null,
    val nameError: String? = null,
    val addressError: String? = null,
    val isLoadingPet: Boolean = false,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isFetchingLocation: Boolean = false,
    val feedbackMessage: String? = null
) {
    val isEditMode: Boolean
        get() = editingPetId != null

    val isBusy: Boolean
        get() = isLoading || isDeleting

    companion object {
        const val DEFAULT_BREED = "SRD (sem raça definida)"
        const val OTHER_BREED = "Outra (informar)"
        const val MAX_PHOTOS = 6
    }
}

/**
 * ViewModel compartilhado por TelaCadastroAnimal e TelaEditarAnimal:
 * gerencia validações, fotos (incluindo reordenação), status de adoção, GPS e persistência no Room.
 */
class PetFormViewModel(
    private val authRepository: AuthRepository,
    private val petRepository: PetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PetFormUiState())
    val uiState: StateFlow<PetFormUiState> = _uiState.asStateFlow()

    fun loadPetForEdit(petId: Long, onNotFound: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPet = true, feedbackMessage = null) }
            val pet = petRepository.getPetById(petId)
            if (pet == null) {
                _uiState.value = PetFormUiState()
                onNotFound()
                return@launch
            }

            val photos = (listOf(pet.photoPath) + pet.additionalPhotoPaths)
                .filter { it.isNotBlank() }
                .take(PetFormUiState.MAX_PHOTOS)

            val (breedOption, customBreed) = resolveBreedFields(pet.breed)
            val (ageValue, ageUnit) = parseEstimatedAge(pet.estimatedAge)

            _uiState.value = PetFormUiState(
                editingPetId = pet.id,
                originalOwnerId = pet.ownerId,
                originalCreatedAt = pet.createdAt,
                originalPhotoPaths = photos,
                initialAddress = pet.address,
                photoPaths = photos,
                status = pet.status,
                name = pet.name,
                species = pet.species,
                sex = pet.sex,
                size = pet.size,
                selectedBreedOption = breedOption,
                customBreed = customBreed,
                estimatedAgeValue = ageValue,
                estimatedAgeUnit = ageUnit,
                animalRg = pet.animalRg.orEmpty(),
                description = pet.description.orEmpty(),
                address = pet.address,
                latitude = pet.latitude,
                longitude = pet.longitude,
                isLoadingPet = false
            )
        }
    }

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

    fun movePhoto(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            if (fromIndex !in state.photoPaths.indices ||
                toIndex !in state.photoPaths.indices ||
                fromIndex == toIndex
            ) {
                return@update state
            }
            val updated = state.photoPaths.toMutableList()
            val moved = updated.removeAt(fromIndex)
            updated.add(toIndex, moved)
            state.copy(photoPaths = updated)
        }
    }

    fun onStatusSelected(status: AdoptionStatus) {
        _uiState.update { it.copy(status = status) }
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

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
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
                            gpsAddress = formattedAddress,
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
        context: Context? = null,
        locationHelper: LocationHelper? = null,
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

            if (current.isEditMode && current.originalOwnerId != null && current.originalOwnerId != loggedUser.id) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = "Você não tem permissão para editar este cadastro."
                    )
                }
                return@launch
            }

            val trimmedAddress = current.address.trim()
            val matchesGps = current.gpsAddress != null && trimmedAddress == current.gpsAddress.trim()
            val matchesInitial = current.initialAddress != null && trimmedAddress == current.initialAddress.trim()
            val (resolvedLat, resolvedLon) = when {
                matchesGps || matchesInitial -> current.latitude to current.longitude
                else -> locationHelper?.forwardGeocode(trimmedAddress) ?: (null to null)
            }

            val pet = PetEntity(
                id = current.editingPetId ?: 0L,
                ownerId = current.originalOwnerId ?: loggedUser.id,
                name = current.name.trim(),
                species = current.species,
                sex = current.sex,
                photoPath = current.photoPaths.first(),
                additionalPhotoPaths = current.photoPaths.drop(1),
                animalRg = current.animalRg.trim().ifBlank { null },
                breed = resolvedBreed,
                estimatedAge = resolvedAge,
                description = current.description.trim().ifBlank { null },
                address = trimmedAddress,
                latitude = resolvedLat,
                longitude = resolvedLon,
                size = current.size,
                status = if (current.isEditMode) current.status else AdoptionStatus.DISPONIVEL,
                createdAt = current.originalCreatedAt ?: System.currentTimeMillis()
            )

            runCatching {
                if (current.isEditMode) {
                    petRepository.updatePet(pet)
                } else {
                    petRepository.insertPet(pet)
                }
            }.onSuccess {
                if (context != null && current.isEditMode) {
                    val removedPhotos = current.originalPhotoPaths.filter { it !in current.photoPaths }
                    if (removedPhotos.isNotEmpty()) {
                        PhotoStorageHelper.deleteLocalPhotos(context, removedPhotos)
                    }
                }
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

    fun deletePet(
        context: Context,
        onDeleted: () -> Unit,
        onSessionExpired: () -> Unit = {}
    ) {
        val current = _uiState.value
        val petId = current.editingPetId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, feedbackMessage = null) }
            val loggedUser = authRepository.getLoggedInUser()
            if (loggedUser == null) {
                _uiState.update { it.copy(isDeleting = false) }
                onSessionExpired()
                return@launch
            }

            if (current.originalOwnerId != null && current.originalOwnerId != loggedUser.id) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        feedbackMessage = "Você não tem permissão para excluir este cadastro."
                    )
                }
                return@launch
            }

            runCatching {
                petRepository.deletePetById(petId)
            }.onSuccess {
                val filesToDelete = (current.originalPhotoPaths + current.photoPaths).distinct()
                PhotoStorageHelper.deleteLocalPhotos(context, filesToDelete)
                _uiState.value = PetFormUiState()
                onDeleted()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        feedbackMessage = error.message ?: "Não foi possível excluir o cadastro."
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
        private val AGE_REGEX = Regex("^(\\d{1,2})\\s*(ano|anos|mês|mes|meses)?$", RegexOption.IGNORE_CASE)

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

        internal fun resolveBreedFields(breed: String?): Pair<String, String> {
            val trimmed = breed?.trim().orEmpty()
            return when {
                trimmed.isEmpty() -> PetFormUiState.DEFAULT_BREED to ""
                trimmed in BREED_OPTIONS && trimmed != PetFormUiState.OTHER_BREED -> trimmed to ""
                else -> PetFormUiState.OTHER_BREED to trimmed
            }
        }

        internal fun parseEstimatedAge(estimatedAge: String?): Pair<String, AgeUnit> {
            val trimmed = estimatedAge?.trim().orEmpty()
            if (trimmed.isEmpty()) return "" to AgeUnit.ANOS
            val match = AGE_REGEX.matchEntire(trimmed) ?: return "" to AgeUnit.ANOS
            val digits = match.groupValues[1]
            val unitRaw = match.groupValues[2].lowercase()
            val unit = if (unitRaw.startsWith("m")) AgeUnit.MESES else AgeUnit.ANOS
            return digits to unit
        }

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
