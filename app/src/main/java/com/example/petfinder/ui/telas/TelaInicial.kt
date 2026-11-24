package com.example.petfinder.ui.telas

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.ui.components.BottomNavTab
import com.example.petfinder.ui.components.NavegacaoInferior
import com.example.petfinder.ui.theme.PetFinderTheme
import com.example.petfinder.ui.viewmodel.HomeUiState
import com.example.petfinder.ui.viewmodel.HomeViewModel
import com.example.petfinder.ui.viewmodel.PetListItem
import com.example.petfinder.util.LocationHelper
import java.util.Locale

/**
 * TelaInicial stateful conectada ao HomeViewModel.
 */
@Composable
fun TelaInicial(
    viewModel: HomeViewModel,
    onNavigateToProfile: () -> Unit,
    onPetClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val locationHelper = remember(context) { LocationHelper(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshLocation(locationHelper)
        } else {
            viewModel.showFeedbackMessage("Permissão de localização negada.")
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedbackMessage()
        }
    }

    TelaInicialContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSpeciesSelected = viewModel::onSpeciesSelected,
        onRefreshLocationClick = {
            if (locationHelper.hasLocationPermission()) {
                viewModel.refreshLocation(locationHelper)
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        onPetClick = onPetClick,
        onBottomTabSelected = { tab ->
            when (tab) {
                BottomNavTab.HOME -> Unit
                BottomNavTab.PROFILE -> onNavigateToProfile()
                BottomNavTab.REGISTER_PET -> {
                    viewModel.showFeedbackMessage("Cadastro de animais será conectado na próxima etapa.")
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Conteúdo stateless da TelaInicial (State Hoisting).
 */
@Composable
fun TelaInicialContent(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String) -> Unit,
    onSpeciesSelected: (PetSpecies?) -> Unit,
    onRefreshLocationClick: () -> Unit,
    onPetClick: (Long) -> Unit,
    onBottomTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val fundo = Color(0xFFFAF8FC)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavegacaoInferior(
                selectedTab = BottomNavTab.HOME,
                onTabSelected = onBottomTabSelected
            )
        },
        containerColor = fundo
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(fundo)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Cabecalho(
                userName = state.userFirstName,
                locationLabel = state.userLocationLabel,
                isRefreshingLocation = state.isRefreshingLocation,
                onLocationClick = onRefreshLocationClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Adote um novo amigo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF292431)
            )

            Spacer(modifier = Modifier.height(20.dp))
            BarraPesquisa(
                searchQuery = state.searchQuery,
                onSearchQueryChange = onSearchQueryChange
            )

            Spacer(modifier = Modifier.height(24.dp))
            Categorias(
                selectedSpecies = state.selectedSpecies,
                onSpeciesSelected = onSpeciesSelected
            )

            Spacer(modifier = Modifier.height(24.dp))
            AnimaisProximos(
                pets = state.pets,
                onPetClick = onPetClick
            )
        }
    }
}

@Composable
fun Cabecalho(
    userName: String,
    locationLabel: String,
    isRefreshingLocation: Boolean,
    onLocationClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    Color(0xFFE9DDF8),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "👩🏻",
                fontSize = 25.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Olá, $userName",
                fontSize = 13.sp,
                color = Color(0xFF898493)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = !isRefreshingLocation, onClick = onLocationClick)
                    .padding(vertical = 2.dp)
            ) {
                if (isRefreshingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0xFF7044B5)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Text(
                        text = "📍",
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = locationLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF292431)
                )

                Text(
                    text = " ⌄",
                    fontSize = 16.sp,
                    color = Color(0xFF898493)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    Color(0xFFF0EAF8),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🔔",
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun BarraPesquisa(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .background(
                    Color.White,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔍",
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { campo ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Buscar um animal...",
                            color = Color(0xFF898493),
                            fontSize = 13.sp
                        )
                    }
                    campo()
                }
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    Color(0xFF7044B5),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "☷",
                color = Color.White,
                fontSize = 25.sp
            )
        }
    }
}

@Composable
fun Categorias(
    selectedSpecies: PetSpecies?,
    onSpeciesSelected: (PetSpecies?) -> Unit
) {
    val categorias = listOf(
        null to "Todos",
        PetSpecies.CACHORRO to "🐶 Cães",
        PetSpecies.GATO to "🐱 Gatos",
        PetSpecies.OUTRO to "🐰 Outros"
    )

    Column {
        Text(
            text = "Categorias",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF292431)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categorias.forEach { (species, label) ->
                val ativa = selectedSpecies == species

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            color = if (ativa) Color(0xFF7044B5) else Color.White,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onSpeciesSelected(species) }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = label,
                        color = if (ativa) Color.White else Color(0xFF514B5A),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CardAnimal(
    nome: String,
    detalhes: String,
    distancia: String,
    emoji: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFE9DDF8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 55.sp
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = nome,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF292431)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = detalhes,
                    fontSize = 12.sp,
                    color = Color(0xFF898493)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = distancia,
                    fontSize = 12.sp,
                    color = Color(0xFF7044B5),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AnimaisProximos(
    pets: List<PetListItem>,
    onPetClick: (Long) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Perto de você",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF292431)
            )

            Text(
                text = "${pets.size} encontrados",
                fontSize = 12.sp,
                color = Color(0xFF7044B5)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (pets.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🐾", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nenhum animal cadastrado ainda",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF292431)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Os animais cadastrados para adoção aparecerão aqui.",
                        fontSize = 12.sp,
                        color = Color(0xFF898493)
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pets, key = { it.pet.id }) { item ->
                    val pet = item.pet
                    val ageLabel = pet.estimatedAge?.takeIf { it.isNotBlank() } ?: pet.size.label
                    val distanceLabel = item.distanceKm?.let { km ->
                        String.format(Locale("pt", "BR"), "%.1f km de você", km)
                    } ?: pet.status.label

                    CardAnimal(
                        nome = pet.name,
                        detalhes = "$ageLabel • ${pet.sex.label}",
                        distancia = distanceLabel,
                        emoji = speciesEmoji(pet.species),
                        onClick = { onPetClick(pet.id) }
                    )
                }
            }
        }
    }
}

private fun speciesEmoji(species: PetSpecies): String = when (species) {
    PetSpecies.CACHORRO -> "🐶"
    PetSpecies.GATO -> "🐱"
    PetSpecies.OUTRO -> "🐰"
}

@Preview(showBackground = true)
@Composable
fun TelaInicialPreview() {
    PetFinderTheme {
        TelaInicialContent(
            state = HomeUiState(
                userFirstName = "Marina",
                userLocationLabel = "Tabuleiro do Norte, CE"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSearchQueryChange = {},
            onSpeciesSelected = {},
            onRefreshLocationClick = {},
            onPetClick = {},
            onBottomTabSelected = {}
        )
    }
}
