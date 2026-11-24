package com.example.petfinder.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidade Room que representa um usuário cadastrado no aplicativo PetFinder.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val cpf: String,
    val cep: String,
    val street: String,
    val number: String,
    val city: String,
    val uf: String,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val formattedAddress: String
        get() = "$street, $number - $city/$uf (CEP $cep)"
}
