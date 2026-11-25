package com.example.petfinder.ui.telas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.ui.components.PetAdditionalInfoSection
import com.example.petfinder.ui.components.PetFinderPrimaryButton
import com.example.petfinder.ui.components.PetFinderTextField
import com.example.petfinder.ui.components.PetLocationSection
import com.example.petfinder.ui.components.PetPhotosSection
import com.example.petfinder.ui.components.PetSegmentedSelector
import com.example.petfinder.ui.components.PhotoSourceDialog
import com.example.petfinder.ui.components.rememberGpsRequester
import com.example.petfinder.ui.components.rememberPhotoPickerActions
import com.example.petfinder.ui.viewmodel.PetFormUiState
import com.example.petfinder.ui.viewmodel.PetFormViewModel
import com.example.petfinder.util.LocationHelper

@Composable
fun TelaCadastroAnimal(
    viewModel: PetFormViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    petId: Long? = null,
    onDeleted: () -> Unit = {},
    onSessionExpired: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val locationHelper = remember(context) { LocationHelper(context) }

    val photoActions = rememberPhotoPickerActions(
        onPhotoSaved = viewModel::addPhotoPath,
        onError = viewModel::showFeedbackMessage
    )
    val requestGps = rememberGpsRequester(
        locationHelper = locationHelper,
        onPermissionGranted = { viewModel.fillLocationFromGps(locationHelper) },
        onPermissionDenied = { viewModel.showFeedbackMessage("Permissão de localização negada. Digite o endereço.") }
    )

    LaunchedEffect(petId) {
        if (petId != null && petId > 0L) {
            viewModel.loadPetForEdit(petId = petId, onNotFound = onNavigateBack)
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearFeedbackMessage()
    }

    TelaCadastroAnimalContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onPickFromGallery = photoActions.pickFromGallery,
        onTakePhoto = photoActions.takePhoto,
        onRemovePhotoAt = viewModel::removePhotoAt,
        onStatusSelected = viewModel::onStatusSelected,
        onNameChange = viewModel::onNameChange,
        onSpeciesSelected = viewModel::onSpeciesSelected,
        onSexSelected = viewModel::onSexSelected,
        onSizeSelected = viewModel::onSizeSelected,
        onBreedOptionSelected = viewModel::onBreedOptionSelected,
        onCustomBreedChange = viewModel::onCustomBreedChange,
        onEstimatedAgeChange = viewModel::onEstimatedAgeChange,
        onToggleAgeUnit = viewModel::onToggleAgeUnit,
        onAnimalRgChange = viewModel::onAnimalRgChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onAddressChange = viewModel::onAddressChange,
        onUseCurrentLocationClick = requestGps,
        onSubmit = {
            viewModel.submit(
                context = context,
                locationHelper = locationHelper,
                onSuccess = onSuccess,
                onSessionExpired = onSessionExpired
            )
        },
        onConfirmDelete = {
            viewModel.deletePet(
                context = context,
                onDeleted = onDeleted,
                onSessionExpired = onSessionExpired
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCadastroAnimalContent(
    state: PetFormUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhotoAt: (Int) -> Unit,
    onStatusSelected: (AdoptionStatus) -> Unit,
    onNameChange: (String) -> Unit,
    onSpeciesSelected: (PetSpecies) -> Unit,
    onSexSelected: (PetSex) -> Unit,
    onSizeSelected: (PetSize) -> Unit,
    onBreedOptionSelected: (String) -> Unit,
    onCustomBreedChange: (String) -> Unit,
    onEstimatedAgeChange: (String) -> Unit,
    onToggleAgeUnit: () -> Unit,
    onAnimalRgChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onUseCurrentLocationClick: () -> Unit,
    onSubmit: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPhotoSourceDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    if (showPhotoSourceDialog) {
        PhotoSourceDialog(
            title = "Adicionar foto",
            onDismiss = { showPhotoSourceDialog = false },
            onTakePhoto = onTakePhoto,
            onPickFromGallery = onPickFromGallery
        )
    }

    if (showDeleteDialog) {
        val petNameLabel = state.name.trim().ifBlank { "este animal" }
        AlertDialog(
            onDismissRequest = { if (!state.isDeleting) showDeleteDialog = false },
            title = { Text("Excluir cadastro?") },
            text = { Text("Essa ação removerá permanentemente o cadastro de $petNameLabel. Deseja continuar?") },
            confirmButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = {
                        showDeleteDialog = false
                        onConfirmDelete()
                    }
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(enabled = !state.isDeleting, onClick = { showDeleteDialog = false }) {
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
                title = {
                    Text(
                        text = if (state.isEditMode) "Editar animal" else "Cadastrar animal",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isBusy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (!state.isLoadingPet) {
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
                                text = if (state.isEditMode) "Salvar alterações" else "Publicar para adoção",
                                onClick = onSubmit,
                                isLoading = state.isLoading,
                                enabled = !state.isBusy
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoadingPet) {
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PetPhotosSection(
                photoPaths = state.photoPaths,
                onAddPhotoClick = { showPhotoSourceDialog = true },
                onRemovePhotoAt = onRemovePhotoAt,
                errorText = state.errors[PetFormViewModel.KEY_PHOTO],
                enabled = !state.isBusy
            )

            if (state.isEditMode) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PetSegmentedSelector(
                        label = "Estado de adoção",
                        options = AdoptionStatus.entries,
                        selectedOption = state.status,
                        optionLabel = { it.label },
                        onOptionSelected = onStatusSelected,
                        enabled = !state.isBusy
                    )
                    Text(
                        text = "Em “Em processo”, o animal continua visível, mas marcado como em negociação.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PetFinderTextField(
                label = "Nome",
                isRequired = true,
                value = state.name,
                onValueChange = onNameChange,
                placeholder = "Thor",
                errorText = state.errors[PetFormViewModel.KEY_NAME],
                enabled = !state.isBusy,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )

            PetSegmentedSelector(
                label = "Espécie",
                options = PetSpecies.entries,
                selectedOption = state.species,
                optionLabel = {
                    when (it) {
                        PetSpecies.CACHORRO -> "Cão"
                        PetSpecies.GATO -> "Gato"
                        PetSpecies.OUTRO -> "Outro"
                    }
                },
                onOptionSelected = onSpeciesSelected,
                enabled = !state.isBusy
            )

            PetSegmentedSelector(
                label = "Sexo",
                options = PetSex.entries,
                selectedOption = state.sex,
                optionLabel = { it.label },
                onOptionSelected = onSexSelected,
                enabled = !state.isBusy
            )

            PetSegmentedSelector(
                label = "Porte",
                options = PetSize.entries,
                selectedOption = state.size,
                optionLabel = { it.label },
                onOptionSelected = onSizeSelected,
                enabled = !state.isBusy
            )

            PetAdditionalInfoSection(
                state = state,
                enabled = !state.isBusy,
                onBreedOptionSelected = onBreedOptionSelected,
                onCustomBreedChange = onCustomBreedChange,
                onEstimatedAgeChange = onEstimatedAgeChange,
                onToggleAgeUnit = onToggleAgeUnit,
                onAnimalRgChange = onAnimalRgChange,
                onDescriptionChange = onDescriptionChange
            )

            PetLocationSection(
                address = state.address,
                addressError = state.errors[PetFormViewModel.KEY_ADDRESS],
                isFetchingLocation = state.isFetchingLocation,
                enabled = !state.isBusy,
                onAddressChange = onAddressChange,
                onUseCurrentLocationClick = onUseCurrentLocationClick,
                showPrivacyHint = !state.isEditMode
            )

            if (state.isEditMode) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !state.isBusy,
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                    } else {
                        Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(19.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excluir cadastro", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            } else {
                val requiredLegend = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("* ") }
                    append("Campos obrigatórios")
                }
                Text(requiredLegend, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
