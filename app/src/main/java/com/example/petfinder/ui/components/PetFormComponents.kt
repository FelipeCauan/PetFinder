package com.example.petfinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.petfinder.ui.viewmodel.PetFormUiState
import com.example.petfinder.ui.viewmodel.PetFormViewModel

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

        ExposedDropdownMenuBox(
            expanded = breedMenuExpanded,
            onExpandedChange = { if (enabled) breedMenuExpanded = !breedMenuExpanded }
        ) {
            PetFinderTextField(
                label = "Raça",
                value = state.selectedBreedOption,
                onValueChange = {},
                placeholder = "",
                readOnly = true,
                enabled = enabled,
                trailingIcon = {
                    Icon(Icons.Outlined.KeyboardArrowDown, "Selecionar raça", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                textFieldModifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
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
            PetFinderTextField(
                label = "Idade presumida",
                value = state.estimatedAgeValue,
                onValueChange = onEstimatedAgeChange,
                placeholder = "3",
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
                enabled = enabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f)
            )

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

        PetFinderTextField(
            label = "Sobre o animal",
            value = state.description,
            onValueChange = onDescriptionChange,
            placeholder = "Conte um pouco sobre a personalidade, cuidados ou história (opcional)",
            singleLine = false,
            minLines = 3,
            maxLines = 5,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
        )
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
