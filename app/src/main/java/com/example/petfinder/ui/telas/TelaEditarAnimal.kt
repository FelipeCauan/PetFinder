package com.example.petfinder.ui.telas

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.ui.components.PetAdditionalInfoSection
import com.example.petfinder.ui.components.PetEditPhotoSection
import com.example.petfinder.ui.components.PetFinderPrimaryButton
import com.example.petfinder.ui.components.PetFinderTextField
import com.example.petfinder.ui.components.PetLocationSection
import com.example.petfinder.ui.components.PetSegmentedSelector
import com.example.petfinder.ui.components.PhotoSourceDialog
import com.example.petfinder.ui.theme.PetFinderTheme
import com.example.petfinder.ui.viewmodel.PetFormUiState
import com.example.petfinder.ui.viewmodel.PetFormViewModel
import com.example.petfinder.util.LocationHelper
import com.example.petfinder.util.PhotoStorageHelper
import kotlinx.coroutines.launch
import java.io.File

/**
 * TelaEditarAnimal stateful conectada ao PetFormViewModel.
 */
@Composable
fun TelaEditarAnimal(
    petId: Long,
    viewModel: PetFormViewModel,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onDeleteSuccess: () -> Unit,
    onSessionExpired: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationHelper = remember(context) { LocationHelper(context) }

    var pendingCameraFilePath by rememberSaveable { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val savedPath = PhotoStorageHelper.copyUriToInternalStorage(context, uri)
                if (savedPath != null) {
                    viewModel.addPhotoPath(savedPath)
                } else {
                    viewModel.showFeedbackMessage("Não foi possível carregar a imagem selecionada.")
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val filePath = pendingCameraFilePath
        pendingCameraFilePath = null
        if (success && filePath != null) {
            coroutineScope.launch {
                val savedPath = PhotoStorageHelper.moveFileToInternalStorage(context, File(filePath))
                if (savedPath != null) {
                    viewModel.addPhotoPath(savedPath)
                } else {
                    viewModel.showFeedbackMessage("Não foi possível salvar a foto capturada.")
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fillLocationFromGps(locationHelper)
        } else {
            viewModel.showFeedbackMessage("Permissão de localização negada. Digite o endereço.")
        }
    }

    LaunchedEffect(petId) {
        viewModel.loadPetForEdit(
            petId = petId,
            onNotFound = onNavigateBack
        )
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedbackMessage()
        }
    }

    TelaEditarAnimalContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onPickFromGallery = {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onTakePhoto = {
            val (uri, tempFile) = PhotoStorageHelper.createTempCameraUri(context)
            pendingCameraFilePath = tempFile.absolutePath
            cameraLauncher.launch(uri)
        },
        onRemovePhotoAt = viewModel::removePhotoAt,
        onMovePhoto = viewModel::movePhoto,
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
        onUseCurrentLocationClick = {
            if (locationHelper.hasLocationPermission()) {
                viewModel.fillLocationFromGps(locationHelper)
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        onSaveClick = {
            viewModel.submit(
                context = context,
                locationHelper = locationHelper,
                onSuccess = onSaveSuccess,
                onSessionExpired = onSessionExpired
            )
        },
        onConfirmDelete = {
            viewModel.deletePet(
                context = context,
                onDeleted = onDeleteSuccess,
                onSessionExpired = onSessionExpired
            )
        },
        modifier = modifier
    )
}

/**
 * Conteúdo stateless da TelaEditarAnimal (State Hoisting).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaEditarAnimalContent(
    state: PetFormUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhotoAt: (Int) -> Unit,
    onMovePhoto: (Int, Int) -> Unit,
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
    onSaveClick: () -> Unit,
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
            onDismissRequest = {
                if (!state.isDeleting) showDeleteDialog = false
            },
            title = { Text("Excluir cadastro?") },
            text = {
                Text(
                    "Essa ação removerá permanentemente o cadastro de $petNameLabel. Deseja continuar?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = {
                        showDeleteDialog = false
                        onConfirmDelete()
                    }
                ) {
                    Text(
                        text = "Excluir",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = { showDeleteDialog = false }
                ) {
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
                        text = "Editar animal",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isBusy) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            PetFinderPrimaryButton(
                                text = "Salvar alterações",
                                onClick = onSaveClick,
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
            PetEditPhotoSection(
                photoPaths = state.photoPaths,
                onAddPhotoClick = { showPhotoSourceDialog = true },
                onRemovePhotoAt = onRemovePhotoAt,
                onMovePhoto = onMovePhoto,
                errorText = state.photoError,
                enabled = !state.isBusy
            )

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

            PetFinderTextField(
                label = "Nome",
                isRequired = true,
                value = state.name,
                onValueChange = onNameChange,
                placeholder = "Thor",
                errorText = state.nameError,
                enabled = !state.isBusy,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            PetSegmentedSelector(
                label = "Espécie",
                options = PetSpecies.entries,
                selectedOption = state.species,
                optionLabel = { species ->
                    when (species) {
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
                addressError = state.addressError,
                isFetchingLocation = state.isFetchingLocation,
                enabled = !state.isBusy,
                onAddressChange = onAddressChange,
                onUseCurrentLocationClick = onUseCurrentLocationClick,
                showPrivacyHint = false
            )

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                enabled = !state.isBusy,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (state.isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Excluir cadastro",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TelaEditarAnimalPreview() {
    PetFinderTheme {
        TelaEditarAnimalContent(
            state = PetFormUiState(
                editingPetId = 1L,
                name = "Thor",
                estimatedAgeValue = "3",
                address = "R. Domingos de Morais, 1200",
                status = AdoptionStatus.DISPONIVEL
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onPickFromGallery = {},
            onTakePhoto = {},
            onRemovePhotoAt = {},
            onMovePhoto = { _, _ -> },
            onStatusSelected = {},
            onNameChange = {},
            onSpeciesSelected = {},
            onSexSelected = {},
            onSizeSelected = {},
            onBreedOptionSelected = {},
            onCustomBreedChange = {},
            onEstimatedAgeChange = {},
            onToggleAgeUnit = {},
            onAnimalRgChange = {},
            onDescriptionChange = {},
            onAddressChange = {},
            onUseCurrentLocationClick = {},
            onSaveClick = {},
            onConfirmDelete = {}
        )
    }
}
