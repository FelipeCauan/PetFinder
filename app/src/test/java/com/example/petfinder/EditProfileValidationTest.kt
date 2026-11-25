package com.example.petfinder

import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.ui.viewmodel.EditProfileUiState
import com.example.petfinder.ui.viewmodel.EditProfileViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditProfileValidationTest {

    @Test
    fun validate_acceptsValidProfileWithoutPasswordChange() {
        val result = EditProfileViewModel.validate(
            EditProfileUiState(
                fullName = "Mariana Costa",
                email = "mariana.costa@email.com",
                cpf = "12345678900",
                phone = "85999998888",
                addressText = "R. Vergueiro, 3185 — Vila Mariana, São Paulo, SP",
                newPassword = "",
                confirmPassword = ""
            )
        )

        assertFalse(result.hasErrors)
        assertNull(result.newPasswordError)
        assertNull(result.confirmPasswordError)
    }

    @Test
    fun validate_rejectsInvalidFieldsAndMismatchedPassword() {
        val result = EditProfileViewModel.validate(
            EditProfileUiState(
                fullName = "Ma",
                email = "email-invalido",
                cpf = "12345",
                phone = "123",
                addressText = "ab",
                newPassword = "123",
                confirmPassword = "456"
            )
        )

        assertTrue(result.hasErrors)
        assertEquals("Informe o nome completo.", result.fullNameError)
        assertEquals("E-mail inválido.", result.emailError)
        assertEquals("O CPF deve conter 11 dígitos.", result.cpfError)
        assertEquals("Informe o telefone com DDD (10 ou 11 dígitos).", result.phoneError)
        assertEquals("Informe o endereço.", result.addressError)
        assertEquals("A nova senha deve ter pelo menos 8 caracteres.", result.newPasswordError)
        assertEquals("As senhas não coincidem.", result.confirmPasswordError)
    }

    @Test
    fun validate_acceptsMatchingNewPassword() {
        val result = EditProfileViewModel.validate(
            EditProfileUiState(
                fullName = "Mariana Costa",
                email = "mariana.costa@email.com",
                cpf = "12345678900",
                phone = "11988887777",
                addressText = "Rua das Flores, 100 — Campinas, SP",
                newPassword = "novaSenha123",
                confirmPassword = "novaSenha123"
            )
        )

        assertFalse(result.hasErrors)
    }

    @Test
    fun parseAddressText_extractsStreetNumberCityUfAndCep() {
        val parsed = EditProfileViewModel.parseAddressText(
            "R. Vergueiro, 3185 — Vila Mariana, São Paulo/SP (CEP 04101-300)"
        )

        assertEquals("R. Vergueiro", parsed.street)
        assertEquals("3185", parsed.number)
        assertEquals("São Paulo", parsed.city)
        assertEquals("SP", parsed.uf)
        assertEquals("04101300", parsed.cep)
    }

    @Test
    fun userEntity_shortLocationLabelAndFormattedAddressHandlePartialFields() {
        val freeFormUser = UserEntity(
            fullName = "Mariana Costa",
            email = "mariana@email.com",
            passwordHash = "hash",
            cpf = "12345678900",
            cep = "",
            street = "Vila Mariana, São Paulo",
            number = "",
            city = "",
            uf = ""
        )

        assertEquals("Vila Mariana, São Paulo", freeFormUser.shortLocationLabel)
        assertEquals("Vila Mariana, São Paulo", freeFormUser.formattedAddress)
    }
}
