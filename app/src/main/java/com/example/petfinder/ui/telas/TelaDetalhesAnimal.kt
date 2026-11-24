
package com.example.petfinder.ui.telas

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold

@Composable
fun TelaDetalheAnimal() {

    Scaffold(
        bottomBar = {
            BarraAdocao()
        },
        containerColor = Color(0xFFFAF8FC)
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAF8FC))
                .padding(paddingValues)
        ) {

            // Área da fotografia
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color(0xFFD8C9B8))
            ) {

                Text(
                    text = "🐶",
                    fontSize = 110.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .padding(16.dp)
                        .size(44.dp)
                        .background(Color.White, CircleShape)
                        .align(Alignment.TopStart)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Compartilhar"
                        )
                    }

                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Favoritar",
                            tint = Color(0xFF7044B5)
                        )
                    }
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
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                // Nome e status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bento",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "● Disponível",
                        fontSize = 12.sp,
                        color = Color(0xFF075C3B),
                        modifier = Modifier
                            .background(
                                Color(0xFFC8F5DF),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            )
                    )
                }

                Text(
                    text = "Beagle",
                    fontSize = 15.sp,
                    color = Color(0xFF898493)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Localização
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF7044B5)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Vila Mariana, São Paulo · 2,3 km",
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Características
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Caracteristica("Espécie", "Cão")
                    Caracteristica("Sexo", "Macho")
                    Caracteristica("Porte", "Médio")
                    Caracteristica("Idade", "9 anos")
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Descrição
                Text(
                    text = "Sobre o Bento",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Calmo e muito carinhoso. Foi resgatado na rua há dois meses, já está castrado e vacinado. Convive bem com outros cães e gosta de passeios curtos.",
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
                        text = "SP-2291-0457"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = Color(0xFFDDD5E8))

                Spacer(modifier = Modifier.height(24.dp))

                // Responsável
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

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                Color(0xFFE9DDF8),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("AR", color = Color(0xFF43266F))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Ana Ribeiro",
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = "Responsável · 6 animais doados",
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
            .padding(vertical = 14.dp),
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
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BarraAdocao() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAF8FC))
            .padding(
                horizontal = 20.dp,
                vertical = 14.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Botão de favorito
        IconButton(
            onClick = { },
            modifier = Modifier
                .size(54.dp)
                .background(
                    Color(0xFFFAF8FC),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "Favoritar",
                tint = Color(0xFF292431)
            )
        }

        // Botão de adoção
        Button(
            onClick = { },
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7044B5)
            )
        ) {
            Text(
                text = "Quero adotar",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TelaDetalheAnimalPreview() {
    TelaDetalheAnimal()
}
