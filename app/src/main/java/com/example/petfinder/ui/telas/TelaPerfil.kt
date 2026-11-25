package com.example.petfinder.ui.telas

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.petfinder.data.local.entity.PetEntity
import com.example.petfinder.data.model.AdoptionStatus
import com.example.petfinder.data.model.PetSpecies
import com.example.petfinder.ui.components.BottomNavTab
import com.example.petfinder.ui.components.NavegacaoInferior
import com.example.petfinder.ui.components.PetLocalImage
import com.example.petfinder.ui.components.formatPhone
import com.example.petfinder.ui.viewmodel.ProfileUiState
import com.example.petfinder.ui.viewmodel.ProfileViewModel

/**
 * TelaPerfil stateful conectada ao ProfileViewModel.
 */
@Composable
fun TelaPerfil(
    viewModel: ProfileViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToRegisterPet: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onLogout: () -> Unit,
    onPetClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedbackMessage()
        }
    }

    TelaPerfilContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onPetClick = onPetClick,
        onEditProfileClick = onNavigateToEditProfile,
        onLogout = {
            viewModel.logout()
            onLogout()
        },
        onBottomTabSelected = { tab ->
            when (tab) {
                BottomNavTab.HOME -> onNavigateToHome()
                BottomNavTab.PROFILE -> Unit
                BottomNavTab.REGISTER_PET -> onNavigateToRegisterPet()
            }
        },
        modifier = modifier
    )
}

/**
 * Conteúdo stateless da TelaPerfil (State Hoisting).
 */
@Composable
fun TelaPerfilContent(
    state: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onPetClick: (Long) -> Unit,
    onEditProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onBottomTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val fundo = Color(0xFFFAF8FC)
    var infoDialogTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var infoDialogBody by rememberSaveable { mutableStateOf<String?>(null) }

    if (infoDialogTitle != null && infoDialogBody != null) {
        AlertDialog(
            onDismissRequest = {
                infoDialogTitle = null
                infoDialogBody = null
            },
            title = { Text(infoDialogTitle.orEmpty()) },
            text = { Text(infoDialogBody.orEmpty()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        infoDialogTitle = null
                        infoDialogBody = null
                    }
                ) {
                    Text("Fechar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        infoDialogTitle = null
                        infoDialogBody = null
                        onEditProfileClick()
                    }
                ) {
                    Text("Editar")
                }
            }
        )
    }

    val user = state.user
    val displayName = user?.fullName ?: "Usuário"
    val displayEmail = user?.email ?: "—"
    val displayLocation = user?.shortLocationLabel ?: "Localização não informada"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavegacaoInferior(
                selectedTab = BottomNavTab.PROFILE,
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
            Text(
                text = "Perfil",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF292431)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                PetLocalImage(
                    photoPath = user?.photoPath,
                    contentDescription = "Foto de perfil",
                    fallbackEmoji = "👩🏻",
                    fallbackEmojiSize = 38.sp,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF292431)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = displayEmail,
                        fontSize = 12.sp,
                        color = Color(0xFF898493)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "📍 $displayLocation",
                        fontSize = 12.sp,
                        color = Color(0xFF898493)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onEditProfileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE9DDF8),
                    contentColor = Color(0xFF43266F)
                )
            ) {
                Text(
                    text = "✎  Editar perfil",
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            EstatisticasPerfil(
                registeredCount = state.registeredCount,
                adoptedCount = state.adoptedCount
            )

            Spacer(modifier = Modifier.height(24.dp))
            MeusAnimais(
                pets = state.myPets,
                onPetClick = onPetClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            SecaoConta(
                onPersonalDataClick = {
                    infoDialogTitle = "Dados pessoais e CPF"
                    infoDialogBody = if (user != null) {
                        val phoneLabel = user.phone.takeIf { it.isNotBlank() }?.let { formatPhone(it) }
                            ?: "Não informado"
                        "Nome: ${user.fullName}\nE-mail: ${user.email}\nCPF: ${formatCpf(user.cpf)}\nTelefone: $phoneLabel"
                    } else {
                        "Nenhum usuário autenticado."
                    }
                },
                onAddressClick = {
                    infoDialogTitle = "Endereço"
                    infoDialogBody = user?.formattedAddress ?: "Endereço não cadastrado."
                },
                onLogoutClick = onLogout
            )
        }
    }
}

@Composable
fun EstatisticasPerfil(
    registeredCount: Int,
    adoptedCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFFAF8FC),
                RoundedCornerShape(20.dp)
            )
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = registeredCount.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cadastrados",
                fontSize = 11.sp,
                color = Color(0xFF898493)
            )
        }

        VerticalDivider(
            modifier = Modifier.height(42.dp),
            color = Color(0xFFDDD5E8)
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = adoptedCount.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Adotados",
                fontSize = 11.sp,
                color = Color(0xFF898493)
            )
        }
    }
}

@Composable
fun CardMeuAnimal(
    nome: String,
    detalhes: String,
    status: String,
    emoji: String,
    photoPath: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column {
            PetLocalImage(
                photoPath = photoPath,
                contentDescription = "Foto de $nome",
                fallbackEmoji = emoji,
                fallbackEmojiSize = 48.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = nome,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF292431)
                )

                Text(
                    text = detalhes,
                    fontSize = 11.sp,
                    color = Color(0xFF898493)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = status,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (status == AdoptionStatus.ADOTADO.label) {
                        Color(0xFF388E3C)
                    } else {
                        Color(0xFF7044B5)
                    }
                )
            }
        }
    }
}

@Composable
fun MeusAnimais(
    pets: List<PetEntity>,
    onPetClick: (Long) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Meus animais",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF292431)
            )

            Text(
                text = "${pets.size} cadastrados",
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
                Text(
                    text = "Você ainda não cadastrou nenhum animal.",
                    fontSize = 13.sp,
                    color = Color(0xFF898493),
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pets, key = { it.id }) { pet ->
                    val ageOrSize = pet.estimatedAge?.takeIf { it.isNotBlank() } ?: pet.size.label
                    val emoji = when (pet.species) {
                        PetSpecies.CACHORRO -> "🐶"
                        PetSpecies.GATO -> "🐱"
                        PetSpecies.OUTRO -> "🐰"
                    }
                    CardMeuAnimal(
                        nome = pet.name,
                        detalhes = "$ageOrSize • ${pet.sex.label}",
                        status = pet.status.label,
                        emoji = emoji,
                        photoPath = pet.photoPath,
                        onClick = { onPetClick(pet.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun OpcaoConta(
    icone: ImageVector,
    titulo: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = Color(0xFF686174),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = titulo,
            fontSize = 13.sp,
            color = Color(0xFF514B5A),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Abrir",
            tint = Color(0xFF686174)
        )
    }
}

@Composable
fun SecaoConta(
    onPersonalDataClick: () -> Unit,
    onAddressClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column {
        Text(
            text = "Conta",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF292431)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFFF1EDF8),
                    RoundedCornerShape(18.dp)
                )
        ) {
            OpcaoConta(
                icone = Icons.Default.Person,
                titulo = "Dados pessoais e CPF",
                onClick = onPersonalDataClick
            )

            HorizontalDivider(color = Color(0xFFDDD5E8))

            OpcaoConta(
                icone = Icons.Default.LocationOn,
                titulo = "Endereço",
                onClick = onAddressClick
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onLogoutClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = Color(0xFFE53935)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Sair da conta",
                color = Color(0xFFE53935),
                fontSize = 13.sp
            )
        }
    }
}

private fun formatCpf(cpfDigits: String): String {
    if (cpfDigits.length != 11) return cpfDigits
    return "${cpfDigits.substring(0, 3)}.${cpfDigits.substring(3, 6)}." +
        "${cpfDigits.substring(6, 9)}-${cpfDigits.substring(9, 11)}"
}
