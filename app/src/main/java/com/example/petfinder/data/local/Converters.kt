package com.example.petfinder.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStringList(paths: List<String>): String =
        paths.filter { it.isNotBlank() }.joinToString("\n")

    @TypeConverter
    fun toStringList(serialized: String): List<String> =
        if (serialized.isBlank()) emptyList()
        else serialized.split("\n").filter { it.isNotBlank() }
}
