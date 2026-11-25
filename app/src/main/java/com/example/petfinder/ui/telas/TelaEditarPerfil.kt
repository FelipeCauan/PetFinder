package com.example.petfinder.ui.telas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petfinder.ui.components.CpfVisualTransformation
import com.example.petfinder.ui.components.PetFinderPasswordField
import com.example.petfinder.ui.components.PetFinderPrimaryButton
import com.example.petfinder.ui.components.PetFinderTextField
import com.example.petfinder.ui.components.PetLocalImage
import com.example.petfinder.ui.components.PhoneVisualTransformation
import com.example.petfinder.ui.components.PhotoSourceDialog
import com.example.petfinder.ui.components.UserAddressFieldsSection
import com.example.petfinder.ui.components.rememberFeedbackSnackbar
import com.example.petfinder.ui.components.rememberGpsRequester
import com.example.petfinder.ui.components.rememberPhotoPickerActions
import com.example.petfinder.ui.viewmodel.EditProfileViewModel
import com.example.petfinder.util.LocationHelper
import com.example.petfinder.util.PhotoStorageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaEditarPerfil(
    viewModel: EditProfileViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onAccountDeleted: () -> Unit,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = rememberFeedbackSnackbar(
        message = state.feedbackMessage,
        onConsumed = viewModel::clearFeedbackMessage
    )
    val context = LocalContext.current
    val locationHelper = remember(context) { LocationHelper(context) }

    val photoActions = rememberPhotoPickerActions(
        dirName = PhotoStorageHelper.USER_PHOTOS_DIR,
        filePrefix = "user",
        onPhotoSaved = viewModel::onPhotoSelected,
        onError = viewModel::showFeedbackMessage
    )
    val requestGps = rememberGpsRequester(
        locationHelper = locationHelper,
        onPermissionGranted = { viewModel.fillLocationFromGps(locationHelper) },
        onPermissionDenied = { viewModel.showFeedbackMessage("Permissão de localização negada. Preencha o endereço manualmente.") }
    )

    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser(onSessionExpired = onSessionExpired)
    }

    var showPhotoSourceDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAccountDialog by rememberSaveable { mutableStateOf(false) }

    if (showPhotoSourceDialog) {
        PhotoSourceDialog(
            title = "Alterar foto de perfil",
            onDismiss = { showPhotoSourceDialog = false },
            onTakePhoto = photoActions.takePhoto,
            onPickFromGallery = photoActions.pickFromGallery,
            onRemovePhoto = if (!state.photoPath.isNullOrBlank()) viewModel::onRemovePhoto else null
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isDeleting) showDeleteAccountDialog = false },
            title = { Text("Excluir minha conta?") },
            text = {
                Text("Essa ação apagará permanentemente sua conta e todos os animais cadastrados por você. Não é possível desfazer.")
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount(
                            context = context,
                            onDeleted = onAccountDeleted,
                            onSessionExpired = onSessionExpired
                        )
                    }
                ) {
                    Text("Excluir conta", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(enabled = !state.isDeleting, onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancelar")
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
                title = { Text("Editar perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isBusy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (!state.isLoadingUser) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            PetFinderPrimaryButton(
                                text = "Salvar alterações",
                                onClick = {
                                    viewModel.saveChanges(
                                        context = context,
                                        locationHelper = locationHelper,
                                        onSuccess = onSaveSuccess,
                                        onSessionExpired = onSessionExpired
                                    )
                                },
                                isLoading = state.isSaving,
                                enabled = !state.isBusy
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoadingUser) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable(enabled = !state.isBusy) { showPhotoSourceDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    PetLocalImage(
                        photoPath = state.photoPath,
                        contentDescription = "Foto de perfil",
                        fallbackEmoji = "👩🏻",
                        fallbackEmojiSize = 44.sp,
                        modifier = Modifier
                            .size(94.dp)
                            .clip(CircleShape)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, "Alterar foto", tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Alterar foto",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !state.isBusy) { showPhotoSourceDialog = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            PetFinderTextField(
                label = "Nome completo",
                isRequired = true,
                value = state.fullName,
                onValueChange = viewModel::onFullNameChange,
                placeholder = "Mariana Costa",
                errorText = state.errors[EditProfileViewModel.KEY_FULL_NAME],
                enabled = !state.isBusy,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )

            PetFinderTextField(
                label = "E-mail",
                isRequired = true,
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "mariana.costa@email.com",
                errorText = state.errors[EditProfileViewModel.KEY_EMAIL],
                enabled = !state.isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )

            PetFinderTextField(
                label = "CPF",
                isRequired = true,
                trailingHint = "Não é exibido publicamente",
                value = state.cpf,
                onValueChange = viewModel::onCpfChange,
                placeholder = "000.000.000-00",
                visualTransformation = CpfVisualTransformation,
                errorText = state.errors[EditProfileViewModel.KEY_CPF],
                enabled = !state.isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
            )

            PetFinderTextField(
                label = "Telefone para contato",
                isRequired = true,
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                placeholder = "(00) 00000-0000",
                visualTransformation = PhoneVisualTransformation,
                errorText = state.errors[EditProfileViewModel.KEY_PHONE],
                enabled = !state.isBusy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
            )

            UserAddressFieldsSection(
                cep = state.cep,
                street = state.street,
                number = state.number,
                city = state.city,
                uf = state.uf,
                errors = state.errors,
                enabled = !state.isBusy,
                isFetchingLocation = state.isFetchingLocation,
                onCepChange = viewModel::onCepChange,
                onStreetChange = viewModel::onStreetChange,
                onNumberChange = viewModel::onNumberChange,
                onCityChange = viewModel::onCityChange,
                onUfChange = viewModel::onUfChange,
                onUseGpsClick = requestGps,
                subtitle = "Usado para calcular a distância dos animais.",
                isRequired = true,
                ufImeAction = ImeAction.Next
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Senha",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Deixe em branco para manter a senha atual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                PetFinderPasswordField(
                    label = "Nova senha",
                    value = state.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    placeholder = "Mínimo 8 caracteres",
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    errorText = state.errors[EditProfileViewModel.KEY_NEW_PASSWORD],
                    enabled = !state.isBusy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                PetFinderPasswordField(
                    label = "Confirmar nova senha",
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    placeholder = "Repita a senha",
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    errorText = state.errors[EditProfileViewModel.KEY_CONFIRM_PASSWORD],
                    enabled = !state.isBusy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Conta",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = !state.isBusy) { showDeleteAccountDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                        } else {
                            Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Excluir minha conta",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Remove sua conta e todos os animais cadastrados por você.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
