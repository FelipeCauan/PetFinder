package com.example.petfinder.data.repository

import com.example.petfinder.data.local.dao.PetDao
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.model.AdoptionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repositório responsável pelas operações de consulta e manipulação de animais para adoção.
 */
class PetRepository(
    private val petDao: PetDao
) {

    fun observeAllPets(): Flow<List<PetEntity>> = petDao.observeAllPets()

    fun observePetsByOwner(ownerId: Long): Flow<List<PetEntity>> =
        petDao.observePetsByOwner(ownerId)

    fun observePetById(petId: Long): Flow<PetEntity?> = petDao.observePetById(petId)

    suspend fun getPetById(petId: Long): PetEntity? = petDao.getPetById(petId)

    suspend fun insertPet(pet: PetEntity): Long = petDao.insertPet(pet)

    suspend fun updatePet(pet: PetEntity) = petDao.updatePet(pet)

    suspend fun updateAdoptionStatus(petId: Long, status: AdoptionStatus) =
        petDao.updateAdoptionStatus(petId, status)

    suspend fun deletePet(pet: PetEntity) = petDao.deletePet(pet)

    suspend fun deletePetById(petId: Long) = petDao.deletePetById(petId)
}
