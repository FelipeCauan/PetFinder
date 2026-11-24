package com.example.petfinder.ui.telas

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.ui.components.PetFinderPrimaryButton
import com.example.petfinder.ui.components.PetFinderTextField
import com.example.petfinder.ui.components.PetFormFieldLabel
import com.example.petfinder.ui.components.PetPhotoPickerRow
import com.example.petfinder.ui.components.PetSegmentedSelector
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
    var breedMenuExpanded by remember { mutableStateOf(false) }

    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = { Text("Adicionar foto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showPhotoSourceDialog = false
                                onTakePhoto()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Tirar foto com a câmera")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showPhotoSourceDialog = false
                                onPickFromGallery()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Escolher da galeria")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoSourceDialog = false }) {
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
            Surface(
                color = MaterialTheme.colorScheme.background
            ) {
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

            PetFinderTextFieldWithRequiredLabel(
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

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Informações adicionais",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Opcional, mas ajuda na adoção.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column {
                PetFormFieldLabel(label = "Raça", isRequired = false)
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = breedMenuExpanded,
                    onExpandedChange = {
                        if (!state.isLoading) breedMenuExpanded = !breedMenuExpanded
                    }
                ) {
                    TextField(
                        value = state.selectedBreedOption,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Selecionar raça",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = breedMenuExpanded,
                        onDismissRequest = { breedMenuExpanded = false }
                    ) {
                        PetFormViewModel.BREED_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onBreedOptionSelected(option)
                                    breedMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (state.selectedBreedOption == PetFormUiState.OTHER_BREED) {
                PetFinderTextField(
                    label = "Qual raça?",
                    value = state.customBreed,
                    onValueChange = onCustomBreedChange,
                    placeholder = "Digite a raça",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    PetFormFieldLabel(label = "Idade presumida", isRequired = false)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = state.estimatedAgeValue,
                        onValueChange = onEstimatedAgeChange,
                        placeholder = {
                            Text(
                                text = "3",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        },
                        suffix = {
                            Text(
                                text = state.estimatedAgeUnit.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !state.isLoading, onClick = onToggleAgeUnit)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        },
                        singleLine = true,
                        enabled = !state.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                PetFinderTextField(
                    label = "RG Animal",
                    value = state.animalRg,
                    onValueChange = onAnimalRgChange,
                    placeholder = "Opcional",
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            Column {
                PetFormFieldLabel(label = "Sobre o animal", isRequired = false)
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    placeholder = {
                        Text(
                            text = "Conte um pouco sobre a personalidade, cuidados ou história (opcional)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PetFinderTextFieldWithRequiredLabel(
                    label = "Localização",
                    isRequired = true,
                    value = state.address,
                    onValueChange = onAddressChange,
                    placeholder = "R. Domingos de Morais, 1200",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    errorText = state.addressError,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            enabled = !state.isLoading && !state.isFetchingLocation,
                            onClick = onUseCurrentLocationClick
                        )
                        .padding(vertical = 4.dp)
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
                        text = "Usar minha localização atual",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "O endereço exato não é exibido. Os interessados veem só o bairro e a distância.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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

@Composable
private fun PetFinderTextFieldWithRequiredLabel(
    label: String,
    isRequired: Boolean,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    errorText: String? = null,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        PetFormFieldLabel(label = label, isRequired = isRequired)
        Spacer(modifier = Modifier.height(6.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            },
            leadingIcon = leadingIcon,
            singleLine = true,
            isError = errorText != null,
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
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
