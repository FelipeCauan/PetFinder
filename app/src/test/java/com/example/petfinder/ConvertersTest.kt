package com.example.petfinder

import com.example.petfinder.data.local.Converters
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun roundTripConversions_preserveAllEnums() {
        PetSpecies.entries.forEach { species ->
            assertEquals(species, converters.toPetSpecies(converters.fromPetSpecies(species)))
        }
        PetSex.entries.forEach { sex ->
            assertEquals(sex, converters.toPetSex(converters.fromPetSex(sex)))
        }
        PetSize.entries.forEach { size ->
            assertEquals(size, converters.toPetSize(converters.fromPetSize(size)))
        }
        AdoptionStatus.entries.forEach { status ->
            assertEquals(
                status,
                converters.toAdoptionStatus(converters.fromAdoptionStatus(status))
            )
        }
    }

    @Test
    fun unknownEnumStrings_fallBackSafely() {
        assertEquals(PetSpecies.OUTRO, converters.toPetSpecies("DESCONHECIDO"))
        assertEquals(PetSex.MACHO, converters.toPetSex("DESCONHECIDO"))
        assertEquals(PetSize.MEDIO, converters.toPetSize("DESCONHECIDO"))
        assertEquals(AdoptionStatus.DISPONIVEL, converters.toAdoptionStatus("DESCONHECIDO"))
    }
}
