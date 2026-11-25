package com.example.petfinder.ui.telas

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.ui.components.PetAdditionalInfoSection
import com.example.petfinder.ui.components.PetFinderPrimaryButton
import com.example.petfinder.ui.components.PetFinderTextField
import com.example.petfinder.ui.components.PetLocationSection
import com.example.petfinder.ui.components.PetPhotoPickerRow
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
 * TelaCadastroAnimal stateful conectada ao PetFormViewModel.
 */
@Composable
fun TelaCadastroAnimal(
    viewModel: PetFormViewModel,
    onNavigateBack: () -> Unit,
    onPublishSuccess: () -> Unit,
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

    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedbackMessage()
        }
    }

    TelaCadastroAnimalContent(
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
        onSubmit = {
            viewModel.submit(
                context = context,
                locationHelper = locationHelper,
                onSuccess = onPublishSuccess,
                onSessionExpired = onSessionExpired
            )
        },
        modifier = modifier
    )
}

/**
 * Conteúdo stateless da TelaCadastroAnimal (State Hoisting).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCadastroAnimalContent(
    state: PetFormUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onPickFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhotoAt: (Int) -> Unit,
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
    modifier: Modifier = Modifier
) {
    var showPhotoSourceDialog by rememberSaveable { mutableStateOf(false) }

    if (showPhotoSourceDialog) {
        PhotoSourceDialog(
            title = "Adicionar foto",
            onDismiss = { showPhotoSourceDialog = false },
            onTakePhoto = onTakePhoto,
            onPickFromGallery = onPickFromGallery
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
                        text = "Cadastrar animal",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !state.isLoading) {
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
                            text = "Publicar para adoção",
                            onClick = onSubmit,
                            isLoading = state.isLoading
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PetPhotoPickerRow(
                photoPaths = state.photoPaths,
                onAddPhotoClick = { showPhotoSourceDialog = true },
                onRemovePhotoAt = onRemovePhotoAt,
                errorText = state.photoError,
                enabled = !state.isLoading
            )

            PetFinderTextField(
                label = "Nome",
                isRequired = true,
                value = state.name,
                onValueChange = onNameChange,
                placeholder = "Thor",
                errorText = state.nameError,
                enabled = !state.isLoading,
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
                enabled = !state.isLoading
            )

            PetSegmentedSelector(
                label = "Sexo",
                options = PetSex.entries,
                selectedOption = state.sex,
                optionLabel = { it.label },
                onOptionSelected = onSexSelected,
                enabled = !state.isLoading
            )

            PetSegmentedSelector(
                label = "Porte",
                options = PetSize.entries,
                selectedOption = state.size,
                optionLabel = { it.label },
                onOptionSelected = onSizeSelected,
                enabled = !state.isLoading
            )

            PetAdditionalInfoSection(
                state = state,
                enabled = !state.isLoading,
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
                enabled = !state.isLoading,
                onAddressChange = onAddressChange,
                onUseCurrentLocationClick = onUseCurrentLocationClick,
                showPrivacyHint = true
            )

            val requiredLegend = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("* ")
                }
                append("Campos obrigatórios")
            }
            Text(
                text = requiredLegend,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TelaCadastroAnimalPreview() {
    PetFinderTheme {
        TelaCadastroAnimalContent(
            state = PetFormUiState(
                name = "Thor",
                estimatedAgeValue = "3",
                address = "R. Domingos de Morais, 1200"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onPickFromGallery = {},
            onTakePhoto = {},
            onRemovePhotoAt = {},
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
            onSubmit = {}
        )
    }
}
