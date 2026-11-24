package com.example.petfinder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false
)

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val cpf: String = "",
    val password: String = "",
    val cep: String = "",
    val street: String = "",
    val number: String = "",
    val city: String = "",
    val uf: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val acceptedTerms: Boolean = false,
    val fullNameError: String? = null,
    val emailError: String? = null,
    val cpfError: String? = null,
    val passwordError: String? = null,
    val cepError: String? = null,
    val streetError: String? = null,
    val numberError: String? = null,
    val cityError: String? = null,
    val ufError: String? = null,
    val termsError: String? = null,
    val isLoading: Boolean = false,
    val isFetchingLocation: Boolean = false
)

data class AuthUiState(
    val login: LoginUiState = LoginUiState(),
    val register: RegisterUiState = RegisterUiState(),
    val authenticatedUser: UserEntity? = null,
    val feedbackMessage: String? = null
)

/**
 * ViewModel compartilhado entre LoginScreen e RegisterScreen.
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // --- Campos de Login ---

    fun onLoginEmailChange(value: String) {
        _uiState.update { it.copy(login = it.login.copy(email = value, emailError = null)) }
    }

    fun onLoginPasswordChange(value: String) {
        _uiState.update { it.copy(login = it.login.copy(password = value, passwordError = null)) }
    }

    fun submitLogin(onSuccess: (UserEntity) -> Unit) {
        val loginState = _uiState.value.login
        val validation = validateLogin(loginState)
        if (validation.hasErrors) {
            _uiState.update {
                it.copy(
                    login = it.login.copy(
                        emailError = validation.emailError,
                        passwordError = validation.passwordError
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(login = it.login.copy(isLoading = true), feedbackMessage = null)
            }
            authRepository.login(loginState.email, loginState.password)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            login = it.login.copy(isLoading = false, password = ""),
                            authenticatedUser = user,
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

    // --- Campos de Cadastro ---

    fun onRegisterFullNameChange(value: String) {
        _uiState.update {
            it.copy(register = it.register.copy(fullName = value, fullNameError = null))
        }
    }

    fun onRegisterEmailChange(value: String) {
        _uiState.update {
            it.copy(register = it.register.copy(email = value, emailError = null))
        }
    }

    fun onRegisterCpfChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(11)
        _uiState.update {
            it.copy(register = it.register.copy(cpf = digits, cpfError = null))
        }
    }

    fun onRegisterPasswordChange(value: String) {
        _uiState.update {
            it.copy(register = it.register.copy(password = value, passwordError = null))
        }
    }

    fun onRegisterCepChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(8)
        _uiState.update {
            it.copy(register = it.register.copy(cep = digits, cepError = null))
        }
    }

    fun onRegisterStreetChange(value: String) {
        _uiState.update {
            it.copy(register = it.register.copy(street = value, streetError = null))
        }
    }

    fun onRegisterNumberChange(value: String) {
        _uiState.update {
            it.copy(register = it.register.copy(number = value, numberError = null))
        }
    }

    fun onRegisterCityChange(value: String) {
        _uiState.update {
            it.copy(register = it.register.copy(city = value, cityError = null))
        }
    }

    fun onRegisterUfChange(value: String) {
        val letters = value.filter { it.isLetter() }.take(2).uppercase()
        _uiState.update {
            it.copy(register = it.register.copy(uf = letters, ufError = null))
        }
    }

    fun onAcceptedTermsChange(accepted: Boolean) {
        _uiState.update {
            it.copy(register = it.register.copy(acceptedTerms = accepted, termsError = null))
        }
    }

    fun fillAddressFromGps(locationHelper: LocationHelper) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    register = it.register.copy(isFetchingLocation = true),
                    feedbackMessage = null
                )
            }
            locationHelper.getCurrentAddress()
                .onSuccess { captured ->
                    val current = _uiState.value.register
                    val hasAnyAddressField = listOf(
                        captured.cep,
                        captured.street,
                        captured.city,
                        captured.uf
                    ).any { !it.isNullOrBlank() }

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
                                cepError = null,
                                streetError = null,
                                cityError = null,
                                ufError = null
                            ),
                            feedbackMessage = if (hasAnyAddressField) {
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
        val registerState = _uiState.value.register
        val validation = validateRegister(registerState)
        if (validation.hasErrors) {
            _uiState.update {
                it.copy(
                    register = it.register.copy(
                        fullNameError = validation.fullNameError,
                        emailError = validation.emailError,
                        cpfError = validation.cpfError,
                        passwordError = validation.passwordError,
                        cepError = validation.cepError,
                        streetError = validation.streetError,
                        numberError = validation.numberError,
                        cityError = validation.cityError,
                        ufError = validation.ufError,
                        termsError = validation.termsError
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(register = it.register.copy(isLoading = true), feedbackMessage = null)
            }
            authRepository.register(
                fullName = registerState.fullName,
                email = registerState.email,
                password = registerState.password,
                cpf = registerState.cpf,
                cep = registerState.cep,
                street = registerState.street,
                number = registerState.number,
                city = registerState.city,
                uf = registerState.uf,
                latitude = registerState.latitude,
                longitude = registerState.longitude
            )
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            register = RegisterUiState(),
                            login = LoginUiState(email = user.email),
                            authenticatedUser = user,
                            feedbackMessage = "Conta criada com sucesso! Bem-vindo(a), ${user.fullName}."
                        )
                    }
                    onSuccess(user)
                }
                .onFailure { error ->
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
        _uiState.update { state ->
            state.copy(
                login = state.login.copy(emailError = null, passwordError = null),
                register = state.register.copy(
                    fullNameError = null,
                    emailError = null,
                    cpfError = null,
                    passwordError = null,
                    cepError = null,
                    streetError = null,
                    numberError = null,
                    cityError = null,
                    ufError = null,
                    termsError = null
                )
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun showFeedbackMessage(message: String) {
        _uiState.update { it.copy(feedbackMessage = message) }
    }

    internal data class LoginValidationResult(
        val emailError: String? = null,
        val passwordError: String? = null
    ) {
        val hasErrors: Boolean
            get() = emailError != null || passwordError != null
    }

    internal data class RegisterValidationResult(
        val fullNameError: String? = null,
        val emailError: String? = null,
        val cpfError: String? = null,
        val passwordError: String? = null,
        val cepError: String? = null,
        val streetError: String? = null,
        val numberError: String? = null,
        val cityError: String? = null,
        val ufError: String? = null,
        val termsError: String? = null
    ) {
        val hasErrors: Boolean
            get() = listOf(
                fullNameError,
                emailError,
                cpfError,
                passwordError,
                cepError,
                streetError,
                numberError,
                cityError,
                ufError,
                termsError
            ).any { it != null }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        private fun validateEmail(email: String): String? {
            val trimmed = email.trim()
            return when {
                trimmed.isEmpty() -> "Informe o e-mail."
                !EMAIL_REGEX.matches(trimmed) -> "E-mail inválido."
                else -> null
            }
        }

        internal fun validateLogin(state: LoginUiState): LoginValidationResult {
            val passwordError = when {
                state.password.isEmpty() -> "Informe a senha."
                state.password.length < 8 -> "A senha deve ter pelo menos 8 caracteres."
                else -> null
            }
            return LoginValidationResult(
                emailError = validateEmail(state.email),
                passwordError = passwordError
            )
        }

        internal fun validateRegister(state: RegisterUiState): RegisterValidationResult {
            val fullNameError = if (state.fullName.trim().length < 3) {
                "Informe o nome completo."
            } else {
                null
            }
            val cpfDigits = state.cpf.filter { it.isDigit() }
            val cpfError = if (cpfDigits.length != 11) {
                "O CPF deve conter 11 dígitos."
            } else {
                null
            }
            val passwordError = when {
                state.password.isEmpty() -> "Crie uma senha."
                state.password.length < 8 -> "Mínimo de 8 caracteres."
                else -> null
            }
            val cepDigits = state.cep.filter { it.isDigit() }
            val cepError = if (cepDigits.length != 8) {
                "CEP inválido."
            } else {
                null
            }
            val streetError = if (state.street.trim().isEmpty()) "Informe a rua." else null
            val numberError = if (state.number.trim().isEmpty()) "Informe o N°." else null
            val cityError = if (state.city.trim().isEmpty()) "Informe a cidade." else null
            val ufLetters = state.uf.trim().filter { it.isLetter() }
            val ufError = if (ufLetters.length != 2) "UF." else null
            val termsError = if (!state.acceptedTerms) {
                "Você precisa aceitar os Termos de uso e a Política de privacidade."
            } else {
                null
            }

            return RegisterValidationResult(
                fullNameError = fullNameError,
                emailError = validateEmail(state.email),
                cpfError = cpfError,
                passwordError = passwordError,
                cepError = cepError,
                streetError = streetError,
                numberError = numberError,
                cityError = cityError,
                ufError = ufError,
                termsError = termsError
            )
        }
    }
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
