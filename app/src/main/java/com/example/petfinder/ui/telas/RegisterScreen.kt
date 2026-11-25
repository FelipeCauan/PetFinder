package com.example.petfinder.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.petfinder.ui.components.CpfVisualTransformation
import com.example.petfinder.ui.components.PetFinderPasswordField
import com.example.petfinder.ui.components.PetFinderPrimaryButton
import com.example.petfinder.ui.components.PetFinderTextField
import com.example.petfinder.ui.components.PhoneVisualTransformation
import com.example.petfinder.ui.components.UserAddressFieldsSection
import com.example.petfinder.ui.components.rememberFeedbackSnackbar
import com.example.petfinder.ui.components.rememberGpsRequester
import com.example.petfinder.ui.viewmodel.AuthViewModel
import com.example.petfinder.util.LocationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onRegisterSuccess: (UserEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.uiState.collectAsState()
    val state = authState.register
    val snackbarHostState = rememberFeedbackSnackbar(
        message = authState.feedbackMessage,
        onConsumed = viewModel::clearFeedbackMessage
    )
    val context = LocalContext.current
    val locationHelper = remember(context) { LocationHelper(context) }

    val requestGps = rememberGpsRequester(
        locationHelper = locationHelper,
        onPermissionGranted = { viewModel.fillAddressFromGps(locationHelper) },
        onPermissionDenied = { viewModel.showFeedbackMessage("Permissão de localização negada. Preencha o endereço manualmente.") }
    )

    var showTermsDialog by rememberSaveable { mutableStateOf(false) }

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
                    IconButton(
                        onClick = {
                            viewModel.clearErrors()
                            onNavigateBack()
                        },
                        enabled = !state.isLoading
                    ) {
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
                onValueChange = viewModel::onRegisterFullNameChange,
                placeholder = "Mariana Costa",
                errorText = state.errors[AuthViewModel.KEY_FULL_NAME],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderTextField(
                label = "E-mail",
                value = state.email,
                onValueChange = viewModel::onRegisterEmailChange,
                placeholder = "seu@email.com",
                errorText = state.errors[AuthViewModel.KEY_EMAIL],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderTextField(
                label = "CPF",
                value = state.cpf,
                onValueChange = viewModel::onRegisterCpfChange,
                placeholder = "000.000.000-00",
                visualTransformation = CpfVisualTransformation,
                errorText = state.errors[AuthViewModel.KEY_CPF],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderTextField(
                label = "Telefone para contato",
                value = state.phone,
                onValueChange = viewModel::onRegisterPhoneChange,
                placeholder = "(00) 00000-0000",
                visualTransformation = PhoneVisualTransformation,
                errorText = state.errors[AuthViewModel.KEY_PHONE],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(14.dp))

            PetFinderPasswordField(
                label = "Senha",
                trailingHint = "Mínimo de 8 caracteres",
                value = state.password,
                onValueChange = viewModel::onRegisterPasswordChange,
                placeholder = "Crie uma senha",
                errorText = state.errors[AuthViewModel.KEY_PASSWORD],
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(modifier = Modifier.height(24.dp))

            UserAddressFieldsSection(
                cep = state.cep,
                street = state.street,
                number = state.number,
                city = state.city,
                uf = state.uf,
                errors = state.errors,
                enabled = !state.isLoading,
                isFetchingLocation = state.isFetchingLocation,
                onCepChange = viewModel::onRegisterCepChange,
                onStreetChange = viewModel::onRegisterStreetChange,
                onNumberChange = viewModel::onRegisterNumberChange,
                onCityChange = viewModel::onRegisterCityChange,
                onUfChange = viewModel::onRegisterUfChange,
                onUseGpsClick = requestGps,
                ufImeAction = ImeAction.Done,
                ufKeyboardActions = KeyboardActions(onDone = {
                    if (!state.isLoading) viewModel.submitRegister(onRegisterSuccess)
                })
            )

            Spacer(modifier = Modifier.height(18.dp))

            Column {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.acceptedTerms,
                        onCheckedChange = viewModel::onAcceptedTermsChange,
                        enabled = !state.isLoading
                    )
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

            PetFinderPrimaryButton(
                text = "Criar conta",
                onClick = { viewModel.submitRegister(onRegisterSuccess) },
                isLoading = state.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
