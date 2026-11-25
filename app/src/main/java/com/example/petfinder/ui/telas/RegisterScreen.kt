package com.example.petfinder.ui.telas

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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.ui.components.CepVisualTransformation
import com.example.petfinder.ui.components.CpfVisualTransformation
import com.example.petfinder.ui.components.PetFinderPasswordField
import com.example.petfinder.ui.components.PetFinderPrimaryButton
import com.example.petfinder.ui.components.PetFinderTextField
import com.example.petfinder.ui.components.PhoneVisualTransformation
import com.example.petfinder.ui.components.rememberGpsRequester
import com.example.petfinder.ui.viewmodel.AuthViewModel
import com.example.petfinder.ui.viewmodel.RegisterUiState
import com.example.petfinder.util.LocationHelper

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

    val requestGps = rememberGpsRequester(
        locationHelper = locationHelper,
        onPermissionGranted = { viewModel.fillAddressFromGps(locationHelper) },
        onPermissionDenied = { viewModel.showFeedbackMessage("Permissão de localização negada. Preencha o endereço manualmente.") }
    )

    LaunchedEffect(authState.feedbackMessage) {
        val message = authState.feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearFeedbackMessage()
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
        onPhoneChange = viewModel::onRegisterPhoneChange,
        onPasswordChange = viewModel::onRegisterPasswordChange,
        onCepChange = viewModel::onRegisterCepChange,
        onStreetChange = viewModel::onRegisterStreetChange,
        onNumberChange = viewModel::onRegisterNumberChange,
        onCityChange = viewModel::onRegisterCityChange,
        onUfChange = viewModel::onRegisterUfChange,
        onAcceptedTermsChange = viewModel::onAcceptedTermsChange,
        onUseGpsClick = requestGps,
        onSubmit = { viewModel.submitRegister(onRegisterSuccess) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterContent(
    state: RegisterUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onCpfChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
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
    var showTermsDialog by rememberSaveable { mutableStateOf(false) }
    val cpfTransformation = remember { CpfVisualTransformation() }
    val phoneTransformation = remember { PhoneVisualTransformation() }
    val cepTransformation = remember { CepVisualTransformation() }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Termos de uso e Privacidade") },
            text = {
                Text(
                    "Seus dados cadastrais (nome, e-mail, CPF, telefone e endereço) são armazenados " +
                        "localmente no dispositivo apenas para identificar tutores e adotantes " +
                        "responsáveis no PetFinder."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Entendi") }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Criar conta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar para login")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                errorText = state.errors[AuthViewModel.KEY_FULL_NAME],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderTextField(
                label = "E-mail",
                value = state.email,
                onValueChange = onEmailChange,
                placeholder = "seu@email.com",
                errorText = state.errors[AuthViewModel.KEY_EMAIL],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderTextField(
                label = "CPF",
                value = state.cpf,
                onValueChange = onCpfChange,
                placeholder = "000.000.000-00",
                visualTransformation = cpfTransformation,
                errorText = state.errors[AuthViewModel.KEY_CPF],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderTextField(
                label = "Telefone para contato",
                value = state.phone,
                onValueChange = onPhoneChange,
                placeholder = "(00) 00000-0000",
                visualTransformation = phoneTransformation,
                errorText = state.errors[AuthViewModel.KEY_PHONE],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderPasswordField(
                label = "Senha",
                trailingHint = "Mínimo de 8 caracteres",
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = "Crie uma senha",
                errorText = state.errors[AuthViewModel.KEY_PASSWORD],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
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
                        .clickable(enabled = !state.isLoading && !state.isFetchingLocation, onClick = onUseGpsClick)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    if (state.isFetchingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Outlined.MyLocation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Usar GPS", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PetFinderTextField(
                label = "CEP",
                value = state.cep,
                onValueChange = onCepChange,
                placeholder = "00000-000",
                visualTransformation = cepTransformation,
                errorText = state.errors[AuthViewModel.KEY_CEP],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PetFinderTextField(
                    label = "Rua",
                    value = state.street,
                    onValueChange = onStreetChange,
                    placeholder = "Rua, avenida",
                    errorText = state.errors[AuthViewModel.KEY_STREET],
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(2.2f)
                )
                PetFinderTextField(
                    label = "Número",
                    value = state.number,
                    onValueChange = onNumberChange,
                    placeholder = "N°",
                    errorText = state.errors[AuthViewModel.KEY_NUMBER],
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PetFinderTextField(
                    label = "Cidade",
                    value = state.city,
                    onValueChange = onCityChange,
                    placeholder = "Cidade",
                    errorText = state.errors[AuthViewModel.KEY_CITY],
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(2.4f)
                )
                PetFinderTextField(
                    label = "UF",
                    value = state.uf,
                    onValueChange = onUfChange,
                    placeholder = "SP",
                    errorText = state.errors[AuthViewModel.KEY_UF],
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!state.isLoading) onSubmit() }),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.acceptedTerms, onCheckedChange = onAcceptedTermsChange, enabled = !state.isLoading)
                    val termsText = buildAnnotatedString {
                        append("Li e aceito os ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) { append("Termos de uso") }
                        append(" e a ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)) { append("Política de privacidade") }
                        append(".")
                    }
                    Text(
                        text = termsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !state.isLoading) { showTermsDialog = true }
                            .padding(vertical = 4.dp)
                    )
                }
                state.errors[AuthViewModel.KEY_TERMS]?.let { termsErr ->
                    Text(
                        text = termsErr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            PetFinderPrimaryButton(text = "Criar conta", onClick = onSubmit, isLoading = state.isLoading)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
