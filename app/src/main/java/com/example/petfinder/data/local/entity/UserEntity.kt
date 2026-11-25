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
    val phone: String = "",
    val cep: String,
    val street: String,
    val number: String,
    val city: String,
    val uf: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val photoPath: String? = null
) {
    val formattedAddress: String
        get() {
            val streetPart = listOf(street, number)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(", ")
            val cityUfPart = listOf(city.trim(), uf.trim())
                .filter { it.isNotEmpty() }
                .joinToString("/")
            val base = listOf(streetPart, cityUfPart)
                .filter { it.isNotEmpty() }
                .joinToString(" - ")
            val cepDigits = cep.filter { it.isDigit() }
            return when {
                base.isNotEmpty() && cepDigits.isNotEmpty() -> "$base (CEP $cepDigits)"
                base.isNotEmpty() -> base
                cepDigits.isNotEmpty() -> "CEP $cepDigits"
                else -> "Endereço não informado"
            }
        }

    val shortLocationLabel: String
        get() {
            val cityTrim = city.trim()
            val ufTrim = uf.trim()
            return when {
                cityTrim.isNotEmpty() && ufTrim.isNotEmpty() -> "$cityTrim, $ufTrim"
                cityTrim.isNotEmpty() -> cityTrim
                ufTrim.isNotEmpty() -> ufTrim
                street.trim().isNotEmpty() -> street.trim()
                else -> "Localização não informada"
            }
        }

    val editableAddressLine: String
        get() {
            val streetPart = listOf(street, number)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(", ")
            val cityUfPart = listOf(city.trim(), uf.trim())
                .filter { it.isNotEmpty() }
                .joinToString(", ")
            return listOf(streetPart, cityUfPart)
                .filter { it.isNotEmpty() }
                .joinToString(" — ")
        }
}
