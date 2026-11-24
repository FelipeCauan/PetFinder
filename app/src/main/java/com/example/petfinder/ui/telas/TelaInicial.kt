package com.example.petfinder.ui.telas

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.tooling.preview.Preview
import com.example.petfinder.ui.theme.PetFinderTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.filled.Add


@Composable
fun TelaInicial() {

    val fundo = Color(0xFFFAF8FC)

    Scaffold(
        bottomBar = {
            NavegacaoInferior()
        },
        containerColor = fundo
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(fundo)
                .padding(paddingValues)
                .padding(20.dp)
        ) {

            Cabecalho()

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Adote um novo amigo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))
            BarraPesquisa()

            Spacer(modifier = Modifier.height(24.dp))
            Categorias()

            Spacer(modifier = Modifier.height(24.dp))
            AnimaisProximos()
        }
    }
}

@Composable
fun Cabecalho() {

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
                text = "Olá, Marina",
                fontSize = 13.sp,
                color = Color(0xFF898493)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "📍",
                    fontSize = 12.sp
                )

                Text(
                    text = "Tabuleiro do Norte, Ceará",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF292431)
                )

                Text(
                    text = "⌄",
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
fun BarraPesquisa() {

    var textoBusca by remember {
        mutableStateOf("")
    }

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
                value = textoBusca,
                onValueChange = { textoBusca = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { campo ->
                    if (textoBusca.isEmpty()) {
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
fun Categorias() {

    val categorias = listOf(
        "Todos",
        "🐶 Cães",
        "🐱 Gatos",
        "🐰 Outros"
    )

    var selecionada by remember {
        mutableStateOf("Todos")
    }

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

            categorias.forEach { categoria ->

                val ativa = selecionada == categoria

                Box(
                    modifier = Modifier
                        .background(
                            color = if (ativa)
                                Color(0xFF7044B5)
                            else
                                Color.White,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable {
                            selecionada = categoria
                        }
                        .padding(
                            horizontal = 18.dp,
                            vertical = 10.dp
                        )
                ) {

                    Text(
                        text = categoria,
                        color = if (ativa)
                            Color.White
                        else
                            Color(0xFF514B5A),
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
    idade: String,
    distancia: String,
    emoji: String
) {

    Card(
        modifier = Modifier.width(220.dp),
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
                    text = idade,
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
fun AnimaisProximos() {

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

            CardAnimal(
                nome = "Luna",
                idade = "2 anos • Fêmea",
                distancia = "1,5 km de você",
                emoji = "🐶"
            )

            CardAnimal(
                nome = "Oliver",
                idade = "1 ano • Macho",
                distancia = "2 km de você",
                emoji = "🐱"
            )

            CardAnimal(
                nome = "Pipoca",
                idade = "3 meses • Fêmea",
                distancia = "3 km de você",
                emoji = "🐰"
            )
        }
    }
}


@Composable
fun NavegacaoInferior() {

    var selecionada by remember {
        mutableStateOf("Início")
    }

    val itens = listOf(
        "Início",
        "Favoritos",
        "Cadastrar",
        "Perfil"
    )

    val icones = listOf(
        Icons.Default.Home,
        Icons.Default.Favorite,
        Icons.Default.Add,
        Icons.Default.Person
    )

    NavigationBar(
        containerColor = Color.White
    ) {

        itens.forEachIndexed { index, item ->

            NavigationBarItem(
                selected = selecionada == item,

                onClick = {
                    selecionada = item
                },

                icon = {
                    Icon(
                        imageVector = icones[index],
                        contentDescription = item
                    )
                },

                label = {
                    Text(item)
                },

                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF7044B5),
                    selectedTextColor = Color(0xFF7044B5),
                    indicatorColor = Color(0xFFE9DDF8),
                    unselectedIconColor = Color(0xFF898493),
                    unselectedTextColor = Color(0xFF898493)
                )
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun TelaInicialPreview() {
    PetFinderTheme {
        TelaInicial()
    }
}