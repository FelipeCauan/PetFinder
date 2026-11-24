package com.example.petfinder.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavTab(
    val label: String,
    val icon: ImageVector
) {
    HOME("Início", Icons.Default.Home),
    REGISTER_PET("Cadastrar", Icons.Default.Add),
    PROFILE("Perfil", Icons.Default.Person)
}

/**
 * Barra de navegação inferior compartilhada e stateless (State Hoisting).
 */
@Composable
fun NavegacaoInferior(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White
    ) {
        BottomNavTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
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
