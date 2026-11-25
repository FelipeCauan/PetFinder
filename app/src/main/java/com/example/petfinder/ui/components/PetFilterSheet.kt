package com.example.petfinder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize

private val DISTANCE_OPTIONS_KM: List<Pair<Double?, String>> = listOf(
    null to "Qualquer",
    5.0 to "Até 5 km",
    15.0 to "Até 15 km",
    30.0 to "Até 30 km",
    50.0 to "Até 50 km"
)

/**
 * Painel inferior de filtros avançados da TelaInicial (State Hoisting + Material 3).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PetFilterBottomSheet(
    initialSex: PetSex?,
    initialSize: PetSize?,
    initialStatus: AdoptionStatus?,
    initialMaxDistanceKm: Double?,
    hasLocationReference: Boolean,
    isRefreshingLocation: Boolean,
    onRequestLocation: () -> Unit,
    onApplyFilters: (PetSex?, PetSize?, AdoptionStatus?, Double?) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draftSexName by rememberSaveable { mutableStateOf(initialSex?.name) }
    var draftSizeName by rememberSaveable { mutableStateOf(initialSize?.name) }
    var draftStatusName by rememberSaveable { mutableStateOf(initialStatus?.name) }
    var draftMaxDistanceKm by rememberSaveable { mutableStateOf(initialMaxDistanceKm) }

    val selectedSex = draftSexName?.let { runCatching { PetSex.valueOf(it) }.getOrNull() }
    val selectedSize = draftSizeName?.let { runCatching { PetSize.valueOf(it) }.getOrNull() }
    val selectedStatus = draftStatusName?.let { runCatching { AdoptionStatus.valueOf(it) }.getOrNull() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Filtrar animais",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            FilterGroupSection(
                title = "Sexo",
                options = listOf(null to "Todos") + PetSex.entries.map { it to it.label },
                selectedOption = selectedSex,
                onSelect = { draftSexName = it?.name }
            )

            FilterGroupSection(
                title = "Porte",
                options = listOf(null to "Todos") + PetSize.entries.map { it to it.label },
                selectedOption = selectedSize,
                onSelect = { draftSizeName = it?.name }
            )

            FilterGroupSection(
                title = "Estado de adoção",
                options = listOf(null to "Todos") + AdoptionStatus.entries
                    .filter { it != AdoptionStatus.ADOTADO }
                    .map { it to it.label },
                selectedOption = selectedStatus,
                onSelect = { draftStatusName = it?.name }
            )

            FilterGroupSection(
                title = "Distância máxima (GPS)",
                options = DISTANCE_OPTIONS_KM,
                selectedOption = draftMaxDistanceKm,
                onSelect = { draftMaxDistanceKm = it }
            )

            if (draftMaxDistanceKm != null && !hasLocationReference) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Sua localização GPS ainda não foi definida para filtrar por distância.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = onRequestLocation,
                        enabled = !isRefreshingLocation
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRefreshingLocation) "Obtendo localização..." else "Atualizar localização GPS",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        draftSexName = null
                        draftSizeName = null
                        draftStatusName = null
                        draftMaxDistanceKm = null
                        onClearFilters()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Limpar",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        onApplyFilters(
                            selectedSex,
                            selectedSize,
                            selectedStatus,
                            draftMaxDistanceKm
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "Aplicar filtros",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> FilterGroupSection(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, label) ->
                val selected = value == selectedOption
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(value) },
                    label = {
                        Text(
                            text = label,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = null,
                    shape = RoundedCornerShape(50)
                )
            }
        }
    }
}
