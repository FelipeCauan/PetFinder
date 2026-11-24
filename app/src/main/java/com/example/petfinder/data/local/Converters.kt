package com.example.petfinder.data.local

import androidx.room.TypeConverter
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies

/**
 * Conversores de tipos para persistir enums e listas no Room Database.
 */
class Converters {

    @TypeConverter
    fun fromPetSpecies(value: PetSpecies): String = value.name

    @TypeConverter
    fun toPetSpecies(value: String): PetSpecies =
        enumValueOrDefault(value, PetSpecies.OUTRO)

    @TypeConverter
    fun fromPetSex(value: PetSex): String = value.name

    @TypeConverter
    fun toPetSex(value: String): PetSex =
        enumValueOrDefault(value, PetSex.MACHO)

    @TypeConverter
    fun fromPetSize(value: PetSize): String = value.name

    @TypeConverter
    fun toPetSize(value: String): PetSize =
        enumValueOrDefault(value, PetSize.MEDIO)

    @TypeConverter
    fun fromAdoptionStatus(value: AdoptionStatus): String = value.name

    @TypeConverter
    fun toAdoptionStatus(value: String): AdoptionStatus =
        enumValueOrDefault(value, AdoptionStatus.DISPONIVEL)

    @TypeConverter
    fun fromStringList(paths: List<String>): String =
        paths.filter { it.isNotBlank() }.joinToString(separator = LIST_SEPARATOR)

    @TypeConverter
    fun toStringList(serialized: String): List<String> =
        if (serialized.isBlank()) {
            emptyList()
        } else {
            serialized.split(LIST_SEPARATOR).filter { it.isNotBlank() }
        }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
        runCatching { enumValueOf<T>(name) }.getOrDefault(default)

    companion object {
        private const val LIST_SEPARATOR = "\n"
    }
}
