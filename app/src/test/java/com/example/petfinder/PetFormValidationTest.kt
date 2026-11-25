package com.example.petfinder

import com.example.petfinder.data.local.Converters
import com.example.petfinder.ui.viewmodel.AgeUnit
import com.example.petfinder.ui.viewmodel.PetFormUiState
import com.example.petfinder.ui.viewmodel.PetFormViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetFormValidationTest {

    @Test
    fun validate_rejectsMissingRequiredFields() {
        val result = PetFormViewModel.validate(
            PetFormUiState(
                photoPaths = emptyList(),
                name = "",
                address = ""
            )
        )

        assertTrue(result.hasErrors)
        assertEquals("Adicione pelo menos 1 foto do animal.", result.photoError)
        assertEquals("Informe o nome do animal.", result.nameError)
        assertEquals("Informe a localização do animal.", result.addressError)
    }

    @Test
    fun validate_acceptsRequiredFieldsOnlyLeavingOptionalsBlank() {
        val result = PetFormViewModel.validate(
            PetFormUiState(
                photoPaths = listOf("/data/user/0/com.example.petfinder/files/pet_photos/thor.jpg"),
                name = "Thor",
                address = "R. Domingos de Morais, 1200",
                estimatedAgeValue = "",
                animalRg = ""
            )
        )

        assertFalse(result.hasErrors)
    }

    @Test
    fun converters_roundTripsAdditionalPhotoPaths() {
        val converters = Converters()
        val paths = listOf("/path/one.jpg", "/path/two.jpg")
        val serialized = converters.fromStringList(paths)
        assertEquals(paths, converters.toStringList(serialized))
        assertEquals(emptyList<String>(), converters.toStringList(""))
    }

    @Test
    fun resolveBreedFields_handlesPresetCustomAndBlankBreeds() {
        assertEquals(
            PetFormUiState.DEFAULT_BREED to "",
            PetFormViewModel.resolveBreedFields(null)
        )
        assertEquals(
            "Labrador" to "",
            PetFormViewModel.resolveBreedFields("Labrador")
        )
        assertEquals(
            PetFormUiState.OTHER_BREED to "SRD caramelo",
            PetFormViewModel.resolveBreedFields("SRD caramelo")
        )
    }

    @Test
    fun parseEstimatedAge_parsesYearsMonthsAndBlank() {
        assertEquals("3" to AgeUnit.ANOS, PetFormViewModel.parseEstimatedAge("3 anos"))
        assertEquals("1" to AgeUnit.ANOS, PetFormViewModel.parseEstimatedAge("1 ano"))
        assertEquals("6" to AgeUnit.MESES, PetFormViewModel.parseEstimatedAge("6 meses"))
        assertEquals("1" to AgeUnit.MESES, PetFormViewModel.parseEstimatedAge("1 mês"))
        assertEquals("" to AgeUnit.ANOS, PetFormViewModel.parseEstimatedAge(null))
    }
}
