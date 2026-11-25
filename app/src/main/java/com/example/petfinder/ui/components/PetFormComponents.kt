package com.example.petfinder.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.outlined.DeleteOutline
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.petfinder.ui.viewmodel.PetFormUiState
import com.example.petfinder.ui.viewmodel.PetFormViewModel
import com.example.petfinder.util.PhotoStorageHelper

@Composable
fun PetFormFieldLabel(
    label: String,
    isRequired: Boolean = false,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        append(label)
        if (isRequired) {
            append(" ")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("*") }
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@Composable
fun <T> PetSegmentedSelector(
    label: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    isRequired: Boolean = true,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PetFormFieldLabel(label = label, isRequired = isRequired)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color.White else Color.Transparent,
                    shadowElevation = if (isSelected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = enabled) { onOptionSelected(option) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PetPhotosSection(
    photoPaths: List<String>,
    isEditMode: Boolean,
    onAddPhotoClick: () -> Unit,
    onRemovePhotoAt: (Int) -> Unit,
    onMovePhoto: (Int, Int) -> Unit,
    errorText: String?,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PetFormFieldLabel(label = "Fotos", isRequired = true)
            Text(
                text = if (isEditMode) "Arraste para reordenar" else "Até ${PetFormUiState.MAX_PHOTOS} fotos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isEditMode) {
            EditModePhotoRow(
                photoPaths = photoPaths,
                enabled = enabled,
                onRemovePhotoAt = onRemovePhotoAt,
                onMovePhoto = onMovePhoto
            )
            if (photoPaths.size < PetFormUiState.MAX_PHOTOS) {
                Spacer(modifier = Modifier.height(12.dp))
                DashedAddPhotoCard(
                    cornerRadius = 20.dp,
                    borderColor = borderColor,
                    enabled = enabled,
                    onClick = onAddPhotoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.PhotoCamera, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Adicionar foto",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                itemsIndexed(photoPaths) { index, path ->
                    PetThumbnailCard(
                        path = path,
                        index = index,
                        size = 116.dp,
                        enabled = enabled,
                        showDragHandle = false,
                        onRemove = { onRemovePhotoAt(index) }
                    )
                }
                if (photoPaths.size < PetFormUiState.MAX_PHOTOS) {
                    item {
                        val width = if (photoPaths.isEmpty()) 220.dp else 188.dp
                        DashedAddPhotoCard(
                            cornerRadius = 16.dp,
                            borderColor = borderColor,
                            enabled = enabled,
                            onClick = onAddPhotoClick,
                            modifier = Modifier
                                .width(width)
                                .height(116.dp)
                                .padding(0.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, null, tint = primaryColor, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Adicionar foto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Câmera ou galeria",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 6.dp, top = 6.dp)
            )
        }
    }
}

@Composable
private fun EditModePhotoRow(
    photoPaths: List<String>,
    enabled: Boolean,
    onRemovePhotoAt: (Int) -> Unit,
    onMovePhoto: (Int, Int) -> Unit
) {
    if (photoPaths.isEmpty()) return

    val thumbSize = 104.dp
    val spacing = 10.dp
    val stepPx = with(LocalDensity.current) { (thumbSize + spacing).toPx() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val latestCount by rememberUpdatedState(photoPaths.size)

    val onDragStep: (PointerInputChange, Float) -> Unit = { change, dragX ->
        change.consume()
        val active = draggingIndex
        if (active != null) {
            dragOffsetX += dragX
            if (dragOffsetX >= stepPx * 0.55f && active < latestCount - 1) {
                onMovePhoto(active, active + 1)
                draggingIndex = active + 1
                dragOffsetX -= stepPx
            } else if (dragOffsetX <= -stepPx * 0.55f && active > 0) {
                onMovePhoto(active, active - 1)
                draggingIndex = active - 1
                dragOffsetX += stepPx
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        photoPaths.forEachIndexed { index, path ->
            val isDragging = draggingIndex == index
            val currentIndex by rememberUpdatedState(index)
            val resetDrag = {
                draggingIndex = null
                dragOffsetX = 0f
            }

            PetThumbnailCard(
                path = path,
                index = index,
                size = thumbSize,
                enabled = enabled,
                showDragHandle = enabled,
                onRemove = { onRemovePhotoAt(index) },
                dragHandleModifier = Modifier.pointerInput(enabled, path) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { draggingIndex = currentIndex; dragOffsetX = 0f },
                        onDragEnd = resetDrag,
                        onDragCancel = resetDrag,
                        onDrag = { change, delta -> onDragStep(change, delta.x) }
                    )
                },
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationX = if (isDragging) dragOffsetX else 0f
                        val scale = if (isDragging) 1.04f else 1f
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerInput(enabled, path) {
                        if (!enabled) return@pointerInput
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggingIndex = currentIndex; dragOffsetX = 0f },
                            onDragEnd = resetDrag,
                            onDragCancel = resetDrag,
                            onDrag = { change, delta -> onDragStep(change, delta.x) }
                        )
                    }
            )
        }
    }
}

@Composable
private fun PetThumbnailCard(
    path: String,
    index: Int,
    size: Dp,
    enabled: Boolean,
    showDragHandle: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        PetLocalImage(
            photoPath = path,
            contentDescription = "Foto ${index + 1} do animal",
            fallbackEmoji = "🐾",
            modifier = Modifier.fillMaxSize()
        )

        if (showDragHandle) {
            Box(
                modifier = dragHandleModifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DragIndicator, "Arrastar para reordenar", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        if (index == 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("Capa", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }

        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, "Remover foto", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun DashedAddPhotoCard(
    cornerRadius: Dp,
    borderColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f))
                )
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun PetLocalImage(
    photoPath: String?,
    contentDescription: String?,
    fallbackEmoji: String,
    modifier: Modifier = Modifier,
    fallbackEmojiSize: TextUnit = 48.sp
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = photoPath) {
        value = photoPath?.takeIf { it.isNotBlank() }?.let { PhotoStorageHelper.loadImageBitmap(it) }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(bitmap = loaded, contentDescription = contentDescription, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(text = fallbackEmoji, fontSize = fallbackEmojiSize)
        }
    }
}

@Composable
fun PhotoSourceDialog(
    title: String,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    onRemovePhoto: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogActionRow(Icons.Outlined.PhotoCamera, "Tirar foto com a câmera", MaterialTheme.colorScheme.primary) {
                    onDismiss()
                    onTakePhoto()
                }
                DialogActionRow(Icons.Outlined.PhotoLibrary, "Escolher da galeria", MaterialTheme.colorScheme.primary) {
                    onDismiss()
                    onPickFromGallery()
                }
                if (onRemovePhoto != null) {
                    DialogActionRow(Icons.Outlined.DeleteOutline, "Remover foto atual", MaterialTheme.colorScheme.error) {
                        onDismiss()
                        onRemovePhoto()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, color = if (tint == MaterialTheme.colorScheme.error) tint else Color.Unspecified)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetAdditionalInfoSection(
    state: PetFormUiState,
    enabled: Boolean,
    onBreedOptionSelected: (String) -> Unit,
    onCustomBreedChange: (String) -> Unit,
    onEstimatedAgeChange: (String) -> Unit,
    onToggleAgeUnit: () -> Unit,
    onAnimalRgChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var breedMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
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
                onExpandedChange = { if (enabled) breedMenuExpanded = !breedMenuExpanded }
            ) {
                TextField(
                    value = state.selectedBreedOption,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    enabled = enabled,
                    trailingIcon = {
                        Icon(Icons.Outlined.KeyboardArrowDown, "Selecionar raça", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = petFormFieldColors(),
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
                enabled = enabled,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                PetFormFieldLabel(label = "Idade presumida", isRequired = false)
                Spacer(modifier = Modifier.height(6.dp))
                TextField(
                    value = state.estimatedAgeValue,
                    onValueChange = onEstimatedAgeChange,
                    placeholder = { Text("3", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)) },
                    suffix = {
                        Text(
                            text = state.estimatedAgeUnit.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = enabled, onClick = onToggleAgeUnit)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    },
                    singleLine = true,
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(16.dp),
                    colors = petFormFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PetFinderTextField(
                label = "RG Animal",
                value = state.animalRg,
                onValueChange = onAnimalRgChange,
                placeholder = "Opcional",
                enabled = enabled,
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
                enabled = enabled,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(16.dp),
                colors = petFormFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PetLocationSection(
    address: String,
    addressError: String?,
    isFetchingLocation: Boolean,
    enabled: Boolean,
    onAddressChange: (String) -> Unit,
    onUseCurrentLocationClick: () -> Unit,
    showPrivacyHint: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PetFinderTextField(
            label = "Localização",
            isRequired = true,
            value = address,
            onValueChange = onAddressChange,
            placeholder = "R. Domingos de Morais, 1200",
            leadingIcon = {
                Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            errorText = addressError,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled && !isFetchingLocation, onClick = onUseCurrentLocationClick)
                .padding(vertical = 4.dp)
        ) {
            if (isFetchingLocation) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            } else {
                Icon(Icons.Outlined.MyLocation, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Usar minha localização atual",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (showPrivacyHint) {
            Text(
                text = "O endereço exato não é exibido. Os interessados veem só o bairro e a distância.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun petFormFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent
)
