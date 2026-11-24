package com.example.petfinder

import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.ui.viewmodel.AuthViewModel
import com.example.petfinder.ui.viewmodel.LoginUiState
import com.example.petfinder.ui.viewmodel.RegisterUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val result = AuthViewModel.validateLogin(
            LoginUiState(
                email = "invalido",
                password = "123"
            )
        )

        assertTrue(result.hasErrors)
        assertEquals("E-mail inválido.", result.emailError)
        assertEquals("A senha deve ter pelo menos 8 caracteres.", result.passwordError)
    }

    @Test
    fun validateRegister_acceptsValidInputs() {
        val result = AuthViewModel.validateRegister(
            RegisterUiState(
                fullName = "Mariana Costa",
                email = "mariana.costa@email.com",
                cpf = "12345678901",
                password = "password123",
                cep = "01310100",
                street = "Av. Paulista",
                number = "1000",
                city = "São Paulo",
                uf = "SP",
                acceptedTerms = true
            )
        )

        assertFalse(result.hasErrors)
    }

    @Test
    fun validateRegister_rejectsMissingFieldsAndUnacceptedTerms() {
        val result = AuthViewModel.validateRegister(
            RegisterUiState(
                fullName = "",
                email = "mariana@email.com",
                cpf = "12345",
                password = "123",
                cep = "123",
                street = "",
                number = "",
                city = "",
                uf = "S",
                acceptedTerms = false
            )
        )

        assertTrue(result.hasErrors)
        assertEquals("Informe o nome completo.", result.fullNameError)
        assertEquals("O CPF deve conter 11 dígitos.", result.cpfError)
        assertEquals("Mínimo de 8 caracteres.", result.passwordError)
        assertEquals("CEP inválido.", result.cepError)
        assertEquals("Informe a rua.", result.streetError)
        assertEquals("Informe o N°.", result.numberError)
        assertEquals("Informe a cidade.", result.cityError)
        assertEquals("UF.", result.ufError)
        assertEquals(
            "Você precisa aceitar os Termos de uso e a Política de privacidade.",
            result.termsError
        )
    }
}
