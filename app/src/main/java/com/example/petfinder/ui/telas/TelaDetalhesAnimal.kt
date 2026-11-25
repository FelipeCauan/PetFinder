package com.example.petfinder.ui.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.local.entity.UserEntity
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSex
import com.example.petfinder.data.model.PetSize
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.ui.components.PetLocalImage
import com.example.petfinder.ui.theme.PetFinderTheme
import com.example.petfinder.ui.viewmodel.PetDetailUiState
import com.example.petfinder.ui.viewmodel.PetDetailViewModel
import java.util.Locale

/**
 * TelaDetalheAnimal stateful conectada ao PetDetailViewModel.
 */
@Composable
fun TelaDetalheAnimal(
    petId: Long,
    viewModel: PetDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditPetClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(petId) {
        viewModel.loadPet(petId)
    }

    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedbackMessage()
        }
    }

    TelaDetalheAnimalContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onEditOwnerClick = { onEditPetClick(petId) },
        modifier = modifier
    )
}

/**
 * Conteúdo stateless da TelaDetalheAnimal (State Hoisting).
 */
@Composable
fun TelaDetalheAnimalContent(
    state: PetDetailUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onEditOwnerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdoptDialog by rememberSaveable { mutableStateOf(false) }

    val pet = state.pet
    val owner = state.owner

    if (showAdoptDialog && pet != null) {
        AlertDialog(
            onDismissRequest = { showAdoptDialog = false },
            title = { Text("Contato para adoção") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Para combinar a adoção de ${pet.name}, entre em contato com o responsável:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Responsável: ${owner?.fullName ?: "Não informado"}",
                        fontWeight = FontWeight.Medium
                    )
                    Text("E-mail: ${owner?.email ?: "Não informado"}")
                    Text(
                        text = "Cidade: ${owner?.shortLocationLabel ?: pet.address}"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdoptDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (pet != null) {
                BarraAdocao(
                    isOwner = state.isOwner,
                    isAdopted = pet.status == AdoptionStatus.ADOTADO,
                    onClick = {
                        if (state.isOwner) {
                            onEditOwnerClick()
                        } else {
                            showAdoptDialog = true
                        }
                    }
                )
            }
        },
        containerColor = Color(0xFFFAF8FC)
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF7044B5))
            }
            return@Scaffold
        }

        if (pet == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Animal não encontrado.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF292431)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onNavigateBack) {
                        Text("Voltar")
                    }
                }
            }
            return@Scaffold
        }

        val speciesLabel = when (pet.species) {
            PetSpecies.CACHORRO -> "Cão"
            PetSpecies.GATO -> "Gato"
            PetSpecies.OUTRO -> "Outro"
        }
        val speciesEmoji = when (pet.species) {
            PetSpecies.CACHORRO -> "🐶"
            PetSpecies.GATO -> "🐱"
            PetSpecies.OUTRO -> "🐰"
        }
        val ageLabel = pet.estimatedAge?.takeIf { it.isNotBlank() } ?: "Não inf."
        val breedLabel = pet.breed?.takeIf { it.isNotBlank() } ?: "SRD (sem raça definida)"
        val locationText = buildString {
            append(pet.address)
            state.distanceKm?.let { km ->
                append(String.format(Locale("pt", "BR"), " · %.1f km", km))
            }
        }

        val (statusBg, statusFg) = when (pet.status) {
            AdoptionStatus.DISPONIVEL -> Color(0xFFC8F5DF) to Color(0xFF075C3B)
            AdoptionStatus.EM_PROCESSO -> Color(0xFFFFF3CD) to Color(0xFF7C5800)
            AdoptionStatus.ADOTADO -> Color(0xFFE9DDF8) to Color(0xFF43266F)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAF8FC))
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // Área da fotografia
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color(0xFFE9DDF8))
            ) {
                val photos = state.photoPaths
                if (photos.size <= 1) {
                    PetLocalImage(
                        photoPath = photos.firstOrNull(),
                        contentDescription = "Foto de ${pet.name}",
                        fallbackEmoji = speciesEmoji,
                        fallbackEmojiSize = 110.sp,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val pagerState = rememberPagerState(pageCount = { photos.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        PetLocalImage(
                            photoPath = photos[page],
                            contentDescription = "Foto ${page + 1} de ${pet.name}",
                            fallbackEmoji = speciesEmoji,
                            fallbackEmojiSize = 110.sp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(photos.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.45f)
                                        }
                                    )
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                        .background(Color.White, CircleShape)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF292431)
                    )
                }
            }

            // Painel de informações
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color(0xFFFAF8FC),
                        RoundedCornerShape(
                            topStart = 28.dp,
                            topEnd = 28.dp
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Nome e status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pet.name,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF292431),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "● ${pet.status.label}",
                        fontSize = 12.sp,
                        color = statusFg,
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }

                Text(
                    text = breedLabel,
                    fontSize = 15.sp,
                    color = Color(0xFF898493)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Localização
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF7044B5)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = locationText,
                        fontSize = 14.sp,
                        color = Color(0xFF292431)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Características
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Caracteristica("Espécie", speciesLabel)
                    Caracteristica("Sexo", pet.sex.label)
                    Caracteristica("Porte", pet.size.label)
                    Caracteristica("Idade", ageLabel)
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Descrição
                Text(
                    text = "Sobre ${pet.name}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF292431)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = pet.description?.takeIf { it.isNotBlank() }
                        ?: "Nenhuma descrição adicional informada.",
                    fontSize = 15.sp,
                    color = Color(0xFF686174),
                    lineHeight = 23.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = Color(0xFFDDD5E8))

                Spacer(modifier = Modifier.height(12.dp))

                // Identificação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RG Animal",
                        color = Color(0xFF898493)
                    )

                    Text(
                        text = pet.animalRg?.takeIf { it.isNotBlank() } ?: "Não informado",
                        color = Color(0xFF292431)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = Color(0xFFDDD5E8))

                Spacer(modifier = Modifier.height(24.dp))

                // Responsável
                val ownerName = owner?.fullName ?: "Responsável"
                val ownerInitials = computeInitials(ownerName)
                val ownerSummary = if (state.ownerAdoptedCount > 0) {
                    "Responsável · ${state.ownerAdoptedCount} animais doados"
                } else {
                    "Responsável · ${state.ownerPetsCount} cadastrados"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFF1EDF8),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!owner?.photoPath.isNullOrBlank()) {
                        PetLocalImage(
                            photoPath = owner?.photoPath,
                            contentDescription = "Foto de $ownerName",
                            fallbackEmoji = "👤",
                            fallbackEmojiSize = 22.sp,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    Color(0xFFE9DDF8),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ownerInitials,
                                color = Color(0xFF43266F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = ownerName,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF292431)
                        )

                        Text(
                            text = ownerSummary,
                            fontSize = 11.sp,
                            color = Color(0xFF898493)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.Caracteristica(
    titulo: String,
    valor: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .background(
                Color(0xFFF1EDF8),
                RoundedCornerShape(22.dp)
            )
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = titulo,
            fontSize = 11.sp,
            color = Color(0xFF898493)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = valor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF292431)
        )
    }
}

@Composable
fun BarraAdocao(
    isOwner: Boolean,
    isAdopted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAF8FC))
            .navigationBarsPadding()
            .padding(
                horizontal = 20.dp,
                vertical = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onClick,
            enabled = isOwner || !isAdopted,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7044B5)
            )
        ) {
            Text(
                text = when {
                    isOwner -> "Editar esse animal"
                    isAdopted -> "Animal já adotado"
                    else -> "Quero adotar"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun computeInitials(fullName: String): String {
    val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "PF"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

@Preview(showBackground = true)
@Composable
fun TelaDetalheAnimalPreview() {
    PetFinderTheme {
        TelaDetalheAnimalContent(
            state = PetDetailUiState(
                isLoading = false,
                pet = PetEntity(
                    id = 1L,
                    ownerId = 1L,
                    name = "Bento",
                    species = PetSpecies.CACHORRO,
                    sex = PetSex.MACHO,
                    photoPath = "",
                    animalRg = "SP-2291-0457",
                    breed = "Beagle",
                    estimatedAge = "9 anos",
                    description = "Calmo e muito carinhoso. Foi resgatado na rua há dois meses, já está castrado e vacinado.",
                    address = "Vila Mariana, São Paulo",
                    size = PetSize.MEDIO,
                    status = AdoptionStatus.DISPONIVEL
                ),
                owner = UserEntity(
                    id = 1L,
                    fullName = "Ana Ribeiro",
                    email = "ana@email.com",
                    passwordHash = "",
                    cpf = "12345678901",
                    cep = "04110000",
                    street = "Rua Domingos de Morais",
                    number = "1200",
                    city = "São Paulo",
                    uf = "SP"
                ),
                ownerAdoptedCount = 6,
                isOwner = false,
                distanceKm = 2.3
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onEditOwnerClick = {}
        )
    }
}
