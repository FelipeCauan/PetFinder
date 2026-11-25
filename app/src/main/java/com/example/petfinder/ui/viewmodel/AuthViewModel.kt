package com.example.petfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.util.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val errors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false
)

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val cpf: String = "",
    val phone: String = "",
    val password: String = "",
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val city: String = "",
    val uf: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val acceptedTerms: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val isFetchingLocation: Boolean = false
)

data class AuthUiState(
    val login: LoginUiState = LoginUiState(),
    val register: RegisterUiState = RegisterUiState(),
    val feedbackMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onLoginEmailChange(value: String) = updateLogin {
        copy(email = value, errors = errors - KEY_EMAIL)
    }

    fun onLoginPasswordChange(value: String) = updateLogin {
        copy(password = value, errors = errors - KEY_PASSWORD)
    }

    fun submitLogin(onSuccess: (UserEntity) -> Unit) {
        val state = _uiState.value.login
        val errors = validateLogin(state)
        if (errors.isNotEmpty()) {
            updateLogin { copy(errors = errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(login = it.login.copy(isLoading = true), feedbackMessage = null) }
            authRepository.login(state.email, state.password)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            login = it.login.copy(isLoading = false, password = ""),
                            feedbackMessage = "Bem-vindo(a), ${user.fullName}!"
                        )
                    }
                    onSuccess(user)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            login = it.login.copy(isLoading = false),
                            feedbackMessage = error.message ?: "Não foi possível entrar."
                        )
                    }
                }
        }
    }

    fun onRegisterFullNameChange(value: String) = updateRegister {
        copy(fullName = value, errors = errors - KEY_FULL_NAME)
    }

    fun onRegisterEmailChange(value: String) = updateRegister {
        copy(email = value, errors = errors - KEY_EMAIL)
    }

    fun onRegisterCpfChange(value: String) = updateRegister {
        copy(cpf = value.filter { it.isDigit() }.take(11), errors = errors - KEY_CPF)
    }

    fun onRegisterPhoneChange(value: String) = updateRegister {
        copy(phone = value.filter { it.isDigit() }.take(11), errors = errors - KEY_PHONE)
    }

    fun onRegisterPasswordChange(value: String) = updateRegister {
        copy(password = value, errors = errors - KEY_PASSWORD)
    }

    fun onRegisterCepChange(value: String) = updateRegister {
        copy(cep = value.filter { it.isDigit() }.take(8), errors = errors - KEY_CEP)
    }

    fun onRegisterStreetChange(value: String) = updateRegister {
        copy(street = value, errors = errors - KEY_STREET)
    }

    fun onRegisterNumberChange(value: String) = updateRegister {
        copy(number = value, errors = errors - KEY_NUMBER)
    }

    fun onRegisterCityChange(value: String) = updateRegister {
        copy(city = value, errors = errors - KEY_CITY)
    }

    fun onRegisterUfChange(value: String) = updateRegister {
        copy(uf = value.filter { it.isLetter() }.take(2).uppercase(), errors = errors - KEY_UF)
    }

    fun onAcceptedTermsChange(accepted: Boolean) = updateRegister {
        copy(acceptedTerms = accepted, errors = errors - KEY_TERMS)
    }

    fun fillAddressFromGps(locationHelper: LocationHelper) {
        viewModelScope.launch {
            updateRegister { copy(isFetchingLocation = true) }
            _uiState.update { it.copy(feedbackMessage = null) }

            locationHelper.getCurrentAddress()
                .onSuccess { captured ->
                    val current = _uiState.value.register
                    val hasAddress = listOf(captured.cep, captured.street, captured.city, captured.uf)
                        .any { !it.isNullOrBlank() }

                    _uiState.update {
                        it.copy(
                            register = current.copy(
                                isFetchingLocation = false,
                                latitude = captured.latitude,
                                longitude = captured.longitude,
                                cep = captured.cep?.takeIf { c -> c.length == 8 } ?: current.cep,
                                street = captured.street?.takeIf { s -> s.isNotBlank() } ?: current.street,
                                number = captured.number?.takeIf { n -> n.isNotBlank() } ?: current.number,
                                city = captured.city?.takeIf { c -> c.isNotBlank() } ?: current.city,
                                uf = captured.uf?.takeIf { u -> u.length == 2 } ?: current.uf,
                                errors = current.errors - setOf(KEY_CEP, KEY_STREET, KEY_CITY, KEY_UF)
                            ),
                            feedbackMessage = if (hasAddress) {
                                "Endereço preenchido pelo GPS!"
                            } else {
                                "Localização GPS salva! Complete os campos do endereço."
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            register = it.register.copy(isFetchingLocation = false),
                            feedbackMessage = error.message ?: "Não foi possível obter a localização."
                        )
                    }
                }
        }
    }

    fun submitRegister(onSuccess: (UserEntity) -> Unit) {
        val state = _uiState.value.register
        val errors = validateRegister(state)
        if (errors.isNotEmpty()) {
            updateRegister { copy(errors = errors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(register = it.register.copy(isLoading = true), feedbackMessage = null) }
            authRepository.register(
                fullName = state.fullName,
                email = state.email,
                password = state.password,
                cpf = state.cpf,
                phone = state.phone,
                cep = state.cep,
                street = state.street,
                number = state.number,
                city = state.city,
                uf = state.uf,
                latitude = state.latitude,
                longitude = state.longitude
            ).onSuccess { user ->
                _uiState.update {
                    it.copy(
                        register = RegisterUiState(),
                        login = LoginUiState(email = user.email),
                        feedbackMessage = "Conta criada com sucesso! Bem-vindo(a), ${user.fullName}."
                    )
                }
                onSuccess(user)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        register = it.register.copy(isLoading = false),
                        feedbackMessage = error.message ?: "Não foi possível criar a conta."
                    )
                }
            }
        }
    }

    fun clearErrors() {
        _uiState.update {
            it.copy(
                login = it.login.copy(errors = emptyMap()),
                register = it.register.copy(errors = emptyMap())
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun showFeedbackMessage(message: String) {
        _uiState.update { it.copy(feedbackMessage = message) }
    }

    private inline fun updateLogin(transform: LoginUiState.() -> LoginUiState) {
        _uiState.update { it.copy(login = it.login.transform()) }
    }

    private inline fun updateRegister(transform: RegisterUiState.() -> RegisterUiState) {
        _uiState.update { it.copy(register = it.register.transform()) }
    }

    companion object {
        const val KEY_FULL_NAME = "fullName"
        const val KEY_EMAIL = "email"
        const val KEY_CPF = "cpf"
        const val KEY_PHONE = "phone"
        const val KEY_PASSWORD = "password"
        const val KEY_CEP = "cep"
        const val KEY_STREET = "street"
        const val KEY_NUMBER = "number"
        const val KEY_CITY = "city"
        const val KEY_UF = "uf"
        const val KEY_TERMS = "terms"

        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        internal fun validateEmail(email: String): String? {
            val trimmed = email.trim()
            return when {
                trimmed.isEmpty() -> "Informe o e-mail."
                !EMAIL_REGEX.matches(trimmed) -> "E-mail inválido."
                else -> null
            }
        }

        internal fun validateLogin(state: LoginUiState): Map<String, String> = buildMap {
            validateEmail(state.email)?.let { put(KEY_EMAIL, it) }
            when {
                state.password.isEmpty() -> put(KEY_PASSWORD, "Informe a senha.")
                state.password.length < 8 -> put(KEY_PASSWORD, "A senha deve ter pelo menos 8 caracteres.")
            }
        }

        internal fun validateRegister(state: RegisterUiState): Map<String, String> = buildMap {
            if (state.fullName.trim().length < 3) put(KEY_FULL_NAME, "Informe o nome completo.")
            validateEmail(state.email)?.let { put(KEY_EMAIL, it) }
            if (state.cpf.filter { it.isDigit() }.length != 11) put(KEY_CPF, "O CPF deve conter 11 dígitos.")
            if (state.phone.filter { it.isDigit() }.length !in 10..11) {
                put(KEY_PHONE, "Informe o telefone com DDD (10 ou 11 dígitos).")
            }
            when {
                state.password.isEmpty() -> put(KEY_PASSWORD, "Crie uma senha.")
                state.password.length < 8 -> put(KEY_PASSWORD, "Mínimo de 8 caracteres.")
            }
            if (state.cep.filter { it.isDigit() }.length != 8) put(KEY_CEP, "CEP inválido.")
            if (state.street.trim().isEmpty()) put(KEY_STREET, "Informe a rua.")
            if (state.number.trim().isEmpty()) put(KEY_NUMBER, "Informe o N°.")
            if (state.city.trim().isEmpty()) put(KEY_CITY, "Informe a cidade.")
            if (state.uf.trim().filter { it.isLetter() }.length != 2) put(KEY_UF, "UF.")
            if (!state.acceptedTerms) {
                put(KEY_TERMS, "Você precisa aceitar os Termos de uso e a Política de privacidade.")
            }
        }
    }
}
