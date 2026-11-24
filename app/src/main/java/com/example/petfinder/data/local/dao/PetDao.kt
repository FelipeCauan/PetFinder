package com.example.petfinder.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.model.AdoptionStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO responsável pelas operações CRUD completas de animais no Room.
 */
@Dao
interface PetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity): Long

    @Update
    suspend fun updatePet(pet: PetEntity)

    @Delete
    suspend fun deletePet(pet: PetEntity)

    @Query("DELETE FROM pets WHERE id = :petId")
    suspend fun deletePetById(petId: Long)

    @Query("SELECT * FROM pets ORDER BY createdAt DESC")
    fun observeAllPets(): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun observePetsByOwner(ownerId: Long): Flow<List<PetEntity>>

    @Query("SELECT * FROM pets WHERE ownerId = :ownerId")
    suspend fun getPetsByOwner(ownerId: Long): List<PetEntity>

    @Query("SELECT * FROM pets WHERE id = :petId LIMIT 1")
    fun observePetById(petId: Long): Flow<PetEntity?>

    @Query("SELECT * FROM pets WHERE id = :petId LIMIT 1")
    suspend fun getPetById(petId: Long): PetEntity?

    @Query("UPDATE pets SET status = :status WHERE id = :petId")
    suspend fun updateAdoptionStatus(petId: Long, status: AdoptionStatus)
}
