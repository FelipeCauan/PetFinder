package com.example.petfinder

import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.ui.viewmodel.AuthViewModel
import com.example.petfinder.ui.viewmodel.LoginUiState
import com.example.petfinder.ui.viewmodel.RegisterUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun hashPassword_isDeterministicAndNotPlaintext() {
        val hash1 = AuthRepository.hashPassword("segredo123")
        val hash2 = AuthRepository.hashPassword("segredo123")
        val hashOther = AuthRepository.hashPassword("outraSenha")

        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
        assertNotEquals("segredo123", hash1)
        assertNotEquals(hash1, hashOther)
    }

    @Test
    fun validateLogin_rejectsInvalidEmailAndShortPassword() {
        val errors = AuthViewModel.validateLogin(
            LoginUiState(
                email = "invalido",
                password = "123"
            )
        )

        assertTrue(errors.isNotEmpty())
        assertEquals("E-mail inválido.", errors[AuthViewModel.KEY_EMAIL])
        assertEquals("A senha deve ter pelo menos 8 caracteres.", errors[AuthViewModel.KEY_PASSWORD])
    }

    @Test
    fun validateRegister_acceptsValidInputs() {
        val errors = AuthViewModel.validateRegister(
            RegisterUiState(
                fullName = "Mariana Costa",
                email = "mariana.costa@email.com",
                cpf = "12345678901",
                phone = "85999998888",
                password = "password123",
                cep = "01310100",
                street = "Av. Paulista",
                number = "1000",
                city = "São Paulo",
                uf = "SP",
                acceptedTerms = true
            )
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun validateRegister_rejectsMissingFieldsAndUnacceptedTerms() {
        val errors = AuthViewModel.validateRegister(
            RegisterUiState(
                fullName = "",
                email = "mariana@email.com",
                cpf = "12345",
                phone = "123",
                password = "123",
                cep = "123",
                street = "",
                number = "",
                city = "",
                uf = "S",
                acceptedTerms = false
            )
        )

        assertTrue(errors.isNotEmpty())
        assertEquals("Informe o nome completo.", errors[AuthViewModel.KEY_FULL_NAME])
        assertEquals("O CPF deve conter 11 dígitos.", errors[AuthViewModel.KEY_CPF])
        assertEquals("Informe o telefone com DDD (10 ou 11 dígitos).", errors[AuthViewModel.KEY_PHONE])
        assertEquals("Mínimo de 8 caracteres.", errors[AuthViewModel.KEY_PASSWORD])
        assertEquals("CEP inválido.", errors[AuthViewModel.KEY_CEP])
        assertEquals("Informe a rua.", errors[AuthViewModel.KEY_STREET])
        assertEquals("Informe o N°.", errors[AuthViewModel.KEY_NUMBER])
        assertEquals("Informe a cidade.", errors[AuthViewModel.KEY_CITY])
        assertEquals("UF.", errors[AuthViewModel.KEY_UF])
        assertEquals(
            "Você precisa aceitar os Termos de uso e a Política de privacidade.",
            errors[AuthViewModel.KEY_TERMS]
        )
    }
}
