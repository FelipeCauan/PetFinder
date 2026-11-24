package com.example.petfinder.data.model

/**
 * Espécie do animal cadastrado para adoção.
 */
enum class PetSpecies(val label: String) {
    CACHORRO("Cachorro"),
    GATO("Gato"),
    OUTRO("Outro")
}

/**
 * Sexo do animal.
 */
enum class PetSex(val label: String) {
    MACHO("Macho"),
    FEMEA("Fêmea")
}

/**
 * Porte físico do animal.
 */
enum class PetSize(val label: String) {
    PEQUENO("Pequeno"),
    MEDIO("Médio"),
    GRANDE("Grande")
}

/**
 * Estado atual do processo de adoção do animal.
 */
enum class AdoptionStatus(val label: String) {
    DISPONIVEL("Disponível"),
    EM_PROCESSO("Em processo"),
    ADOTADO("Adotado")
}
