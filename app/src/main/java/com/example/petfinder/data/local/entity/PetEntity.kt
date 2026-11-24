package com.example.petfinder.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies

/**
 * Entidade Room que representa um animal cadastrado para adoção no PetFinder.
 */
@Entity(
    tableName = "pets",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["ownerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ownerId"])]
)
data class PetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerId: Long,
    val name: String,
    val species: PetSpecies,
    val sex: PetSex,
    val photoPath: String,
    val additionalPhotoPaths: List<String> = emptyList(),
    val animalRg: String? = null,
    val breed: String? = null,
    val estimatedAge: String? = null,
    val description: String? = null,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val size: PetSize,
    val status: AdoptionStatus = AdoptionStatus.DISPONIVEL,
    val createdAt: Long = System.currentTimeMillis()
)
