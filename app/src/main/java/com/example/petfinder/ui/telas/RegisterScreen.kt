package com.example.petfinder.ui.telas

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.ui.components.CepVisualTransformation
import com.example.petfinder.ui.components.CpfVisualTransformation
import com.example.petfinder.ui.components.PetFinderPrimaryButton
import com.example.petfinder.ui.components.PetFinderTextField
import com.example.petfinder.ui.theme.PetFinderTheme
import com.example.petfinder.ui.viewmodel.AuthViewModel
import com.example.petfinder.ui.viewmodel.RegisterUiState
import com.example.petfinder.util.LocationHelper

/**
 * Tela de Cadastro stateful conectada ao AuthViewModel.
 */
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onRegisterSuccess: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val locationHelper = remember(context) { LocationHelper(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fillAddressFromGps(locationHelper)
        } else {
            viewModel.showFeedbackMessage("Permissão de localização negada. Preencha o endereço manualmente.")
        }
    }

    LaunchedEffect(authState.feedbackMessage) {
        val message = authState.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedbackMessage()
        }
    }

    RegisterContent(
        state = authState.register,
        snackbarHostState = snackbarHostState,
        onNavigateBack = {
            viewModel.clearErrors()
            onNavigateBack()
        },
        onFullNameChange = viewModel::onRegisterFullNameChange,
        onEmailChange = viewModel::onRegisterEmailChange,
        onCpfChange = viewModel::onRegisterCpfChange,
        onPasswordChange = viewModel::onRegisterPasswordChange,
        onCepChange = viewModel::onRegisterCepChange,
        onStreetChange = viewModel::onRegisterStreetChange,
        onNumberChange = viewModel::onRegisterNumberChange,
        onCityChange = viewModel::onRegisterCityChange,
        onUfChange = viewModel::onRegisterUfChange,
        onAcceptedTermsChange = viewModel::onAcceptedTermsChange,
        onUseGpsClick = {
            if (locationHelper.hasLocationPermission()) {
                viewModel.fillAddressFromGps(locationHelper)
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        onSubmit = { viewModel.submitRegister(onRegisterSuccess) },
        modifier = modifier
    )
}

/**
 * Conteúdo stateless da tela de Cadastro (State Hoisting).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterContent(
    state: RegisterUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onCpfChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onCepChange: (String) -> Unit,
    onStreetChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onUfChange: (String) -> Unit,
    onAcceptedTermsChange: (Boolean) -> Unit,
    onUseGpsClick: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    val cpfTransformation = remember { CpfVisualTransformation() }
    val cepTransformation = remember { CepVisualTransformation() }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Termos de uso e Privacidade") },
            text = {
                Text(
                    "Seus dados cadastrais (nome, e-mail, CPF e endereço) são armazenados " +
                        "localmente no dispositivo apenas para identificar tutores e adotantes " +
                        "responsáveis no PetFinder."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Entendi")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Criar conta",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isLoading) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar para login"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Seus dados ajudam protetores a conhecerem quem vai adotar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            PetFinderTextField(
                label = "Nome completo",
                value = state.fullName,
                onValueChange = onFullNameChange,
                placeholder = "Mariana Costa",
                errorText = state.fullNameError,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderTextField(
                label = "E-mail",
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = "seu@email.com",
                errorText = state.emailError,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderTextField(
                label = "CPF",
                value = state.cpf,
                onValueChange = onCpfChange,
                placeholder = "000.000.000-00",
                visualTransformation = cpfTransformation,
                errorText = state.cpfError,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderTextField(
                label = "Senha",
                trailingHint = "Mínimo de 8 caracteres",
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = "Crie uma senha",
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                "Ocultar senha"
                            } else {
                                "Mostrar senha"
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                errorText = state.passwordError,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Endereço",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            enabled = !state.isLoading && !state.isFetchingLocation,
                            onClick = onUseGpsClick
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    if (state.isFetchingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Usar GPS",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PetFinderTextField(
                label = "CEP",
                value = state.cep,
                onValueChange = onCepChange,
                placeholder = "00000-000",
                visualTransformation = cepTransformation,
                errorText = state.cepError,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PetFinderTextField(
                    label = "Rua",
                    value = state.street,
                    onValueChange = onStreetChange,
                    placeholder = "Rua, avenida",
                    errorText = state.streetError,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(2.2f)
                )
                PetFinderTextField(
                    label = "Número",
                    value = state.number,
                    onValueChange = onNumberChange,
                    placeholder = "N°",
                    errorText = state.numberError,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PetFinderTextField(
                    label = "Cidade",
                    value = state.city,
                    onValueChange = onCityChange,
                    placeholder = "Cidade",
                    errorText = state.cityError,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(2.4f)
                )
                PetFinderTextField(
                    label = "UF",
                    value = state.uf,
                    onValueChange = onUfChange,
                    placeholder = "SP",
                    errorText = state.ufError,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!state.isLoading) onSubmit()
                        }
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.acceptedTerms,
                        onCheckedChange = onAcceptedTermsChange,
                        enabled = !state.isLoading
                    )
                    val termsText = buildAnnotatedString {
                        append("Li e aceito os ")
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append("Termos de uso")
                        }
                        append(" e a ")
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append("Política de privacidade")
                        }
                        append(".")
                    }
                    Text(
                        text = termsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !state.isLoading) {
                                showTermsDialog = true
                            }
                            .padding(vertical = 4.dp)
                    )
                }
                if (state.termsError != null) {
                    Text(
                        text = state.termsError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PetFinderPrimaryButton(
                text = "Criar conta",
                onClick = onSubmit,
                isLoading = state.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    PetFinderTheme {
        RegisterContent(
            state = RegisterUiState(acceptedTerms = true),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onFullNameChange = {},
            onEmailChange = {},
            onCpfChange = {},
            onPasswordChange = {},
            onCepChange = {},
            onStreetChange = {},
            onNumberChange = {},
            onCityChange = {},
            onUfChange = {},
            onAcceptedTermsChange = {},
            onUseGpsClick = {},
            onSubmit = {}
        )
    }
}
