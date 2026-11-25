package com.example.petfinder

import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.ui.viewmodel.EditProfileUiState
import com.example.petfinder.ui.viewmodel.EditProfileViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditProfileValidationTest {

    @Test
    fun validate_acceptsValidProfileWithoutPasswordChange() {
        val errors = EditProfileViewModel.validate(
            EditProfileUiState(
                fullName = "Mariana Costa",
                email = "mariana.costa@email.com",
                cpf = "12345678900",
                phone = "85999998888",
                cep = "04101300",
                street = "R. Vergueiro",
                number = "3185",
                city = "São Paulo",
                uf = "SP",
                newPassword = "",
                confirmPassword = ""
            )
        )

        assertTrue(errors.isEmpty())
        assertNull(errors[EditProfileViewModel.KEY_NEW_PASSWORD])
        assertNull(errors[EditProfileViewModel.KEY_CONFIRM_PASSWORD])
    }

    @Test
    fun validate_rejectsInvalidFieldsAndMismatchedPassword() {
        val errors = EditProfileViewModel.validate(
            EditProfileUiState(
                fullName = "Ma",
                email = "email-invalido",
                cpf = "12345",
                phone = "123",
                cep = "123",
                street = "",
                number = "",
                city = "",
                uf = "S",
                newPassword = "123",
                confirmPassword = "456"
            )
        )

        assertTrue(errors.isNotEmpty())
        assertEquals("Informe o nome completo.", errors[EditProfileViewModel.KEY_FULL_NAME])
        assertEquals("E-mail inválido.", errors[EditProfileViewModel.KEY_EMAIL])
        assertEquals("O CPF deve conter 11 dígitos.", errors[EditProfileViewModel.KEY_CPF])
        assertEquals("Informe o telefone com DDD (10 ou 11 dígitos).", errors[EditProfileViewModel.KEY_PHONE])
        assertEquals("CEP inválido.", errors[EditProfileViewModel.KEY_CEP])
        assertEquals("Informe a rua.", errors[EditProfileViewModel.KEY_STREET])
        assertEquals("Informe o N°.", errors[EditProfileViewModel.KEY_NUMBER])
        assertEquals("Informe a cidade.", errors[EditProfileViewModel.KEY_CITY])
        assertEquals("UF.", errors[EditProfileViewModel.KEY_UF])
        assertEquals("A nova senha deve ter pelo menos 8 caracteres.", errors[EditProfileViewModel.KEY_NEW_PASSWORD])
        assertEquals("As senhas não coincidem.", errors[EditProfileViewModel.KEY_CONFIRM_PASSWORD])
    }

    @Test
    fun validate_acceptsMatchingNewPassword() {
        val errors = EditProfileViewModel.validate(
            EditProfileUiState(
                fullName = "Mariana Costa",
                email = "mariana.costa@email.com",
                cpf = "12345678900",
                phone = "11988887777",
                cep = "13010100",
                street = "Rua das Flores",
                number = "100",
                city = "Campinas",
                uf = "SP",
                newPassword = "novaSenha123",
                confirmPassword = "novaSenha123"
            )
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun userEntity_formatsAddressAndShortLabel() {
        val user = UserEntity(
            fullName = "Mariana Costa",
            email = "mariana@email.com",
            passwordHash = "hash",
            cpf = "12345678900",
            phone = "85999998888",
            cep = "04101300",
            street = "R. Vergueiro",
            number = "3185",
            city = "São Paulo",
            uf = "SP"
        )

        assertEquals("R. Vergueiro, 3185 - São Paulo/SP (CEP 04101300)", user.formattedAddress)
        assertEquals("São Paulo, SP", user.shortLocationLabel)
    }
}
