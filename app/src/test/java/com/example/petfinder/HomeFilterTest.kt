package com.example.petfinder

import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.ui.viewmodel.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFilterTest {

    private val samplePets = listOf(
        PetEntity(
            id = 1L,
            ownerId = 1L,
            name = "Thor",
            species = PetSpecies.CACHORRO,
            sex = PetSex.MACHO,
            photoPath = "/tmp/thor.jpg",
            address = "Russas, CE",
            latitude = -4.9403,
            longitude = -37.9753,
            size = PetSize.MEDIO,
            status = AdoptionStatus.DISPONIVEL
        ),
        PetEntity(
            id = 2L,
            ownerId = 1L,
            name = "Mia",
            species = PetSpecies.GATO,
            sex = PetSex.FEMEA,
            photoPath = "/tmp/mia.jpg",
            address = "Tabuleiro do Norte, CE",
            latitude = -5.2528,
            longitude = -38.1286,
            size = PetSize.PEQUENO,
            status = AdoptionStatus.EM_PROCESSO
        ),
        PetEntity(
            id = 3L,
            ownerId = 2L,
            name = "Bob",
            species = PetSpecies.CACHORRO,
            sex = PetSex.MACHO,
            photoPath = "/tmp/bob.jpg",
            address = "Fortaleza, CE",
            latitude = -3.7172,
            longitude = -38.5433,
            size = PetSize.GRANDE,
            status = AdoptionStatus.ADOTADO
        )
    )

    @Test
    fun filterAndSortPets_filtersBySexSizeAndStatus() {
        val result = HomeViewModel.filterAndSortPets(
            allPets = samplePets,
            searchQuery = "",
            selectedSpecies = null,
            selectedSex = PetSex.FEMEA,
            selectedSize = PetSize.PEQUENO,
            selectedStatus = AdoptionStatus.EM_PROCESSO,
            maxDistanceKm = null,
            refLat = null,
            refLon = null
        )

        assertEquals(1, result.size)
        assertEquals("Mia", result.first().pet.name)
    }

    @Test
    fun filterAndSortPets_filtersByMaxDistanceAndSortsByNearest() {
        // Referência em Russas-CE (-4.9403, -37.9753): Thor = 0 km, Mia ~ 38 km, Bob ~ 150 km
        val result = HomeViewModel.filterAndSortPets(
            allPets = samplePets,
            searchQuery = "",
            selectedSpecies = null,
            selectedSex = null,
            selectedSize = null,
            selectedStatus = null,
            maxDistanceKm = 50.0,
            refLat = -4.9403,
            refLon = -37.9753
        )

        assertEquals(2, result.size)
        assertEquals("Thor", result[0].pet.name)
        assertEquals("Mia", result[1].pet.name)
        assertTrue((result[0].distanceKm ?: 1.0) < 0.1)
    }

    @Test
    fun filterAndSortPets_excludesAdoptedPets() {
        val result = HomeViewModel.filterAndSortPets(
            allPets = samplePets,
            searchQuery = "",
            selectedSpecies = null,
            selectedSex = null,
            selectedSize = null,
            selectedStatus = null,
            maxDistanceKm = null,
            refLat = null,
            refLon = null
        )

        assertEquals(2, result.size)
        assertTrue(result.none { it.pet.status == AdoptionStatus.ADOTADO })
    }
}
