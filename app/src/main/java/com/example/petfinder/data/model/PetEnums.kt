package com.example.petfinder.data.model

enum class PetSpecies(val label: String, val emoji: String) {
    CACHORRO("Cachorro", "🐶"),
    GATO("Gato", "🐱"),
    OUTRO("Outro", "🐰")
}

enum class PetSex(val label: String) {
    MACHO("Macho"),
    FEMEA("Fêmea")
}

enum class PetSize(val label: String) {
    PEQUENO("Pequeno"),
    MEDIO("Médio"),
    GRANDE("Grande")
}

enum class AdoptionStatus(val label: String) {
    DISPONIVEL("Disponível"),
    EM_PROCESSO("Em processo"),
    ADOTADO("Adotado")
}
