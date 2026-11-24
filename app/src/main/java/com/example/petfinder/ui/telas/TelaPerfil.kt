package com.example.petfinder.ui.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider

@Composable
fun TelaPerfil() {

    val fundo = Color(0xFFFAF8FC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fundo)
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
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Color(0xFFE9DDF8),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👩🏻",
                    fontSize = 38.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {

                Text(
                    text = "Marina de Lima Freire",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF292431)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "marina.lima@gmail.com",
                    fontSize = 12.sp,
                    color = Color(0xFF898493)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "📍 Tabuleiro do Norte, Ceará",
                    fontSize = 12.sp,
                    color = Color(0xFF898493)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { },
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

        EstatisticasPerfil()

        Spacer(modifier = Modifier.height(24.dp))
        MeusAnimais()

        Spacer(modifier = Modifier.height(24.dp))
        SecaoConta()
    }
}

@Composable
fun EstatisticasPerfil() {

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

        // Animais cadastrados
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "3",
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

        // Animais adotados
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "1",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Adotados",
                fontSize = 11.sp,
                color = Color(0xFF898493)
            )
        }

        VerticalDivider(
            modifier = Modifier.height(42.dp),
            color = Color(0xFFDDD5E8)
        )

        // Animais favoritos
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "4",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Favoritos",
                fontSize = 11.sp,
                color = Color(0xFF898493)
            )
        }
    }
}


@Composable
fun CardMeuAnimal(
    nome: String,
    idade: String,
    status: String,
    emoji: String
) {

    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column {

            // Imagem provisória do animal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFFE9DDF8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 48.sp
                )
            }

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
                    text = idade,
                    fontSize = 11.sp,
                    color = Color(0xFF898493)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = status,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (status == "Adotado")
                        Color(0xFF388E3C)
                    else
                        Color(0xFF7044B5)
                )
            }
        }
    }
}


@Composable
fun MeusAnimais() {

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
                text = "Ver todos",
                fontSize = 12.sp,
                color = Color(0xFF7044B5)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.horizontalScroll(
                rememberScrollState()
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            CardMeuAnimal(
                nome = "Thor",
                idade = "2 anos • Macho",
                status = "Disponível",
                emoji = "🐶"
            )

            CardMeuAnimal(
                nome = "Mimi",
                idade = "1 ano • Fêmea",
                status = "Adotado",
                emoji = "🐱"
            )

            CardMeuAnimal(
                nome = "Pipoca",
                idade = "3 meses • Fêmea",
                status = "Disponível",
                emoji = "🐰"
            )
        }
    }
}


@Composable
fun OpcaoConta(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
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
fun SecaoConta() {

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
                titulo = "Dados pessoais e CPF"
            )

            HorizontalDivider(
                color = Color(0xFFDDD5E8)
            )

            OpcaoConta(
                icone = Icons.Default.LocationOn,
                titulo = "Endereço"
            )

            HorizontalDivider(
                color = Color(0xFFDDD5E8)
            )

            OpcaoConta(
                icone = Icons.Default.Notifications,
                titulo = "Notificações"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { }
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
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

@Preview(showBackground = true)
@Composable
fun TelaPerfilPreview() {
    TelaPerfil()
}
