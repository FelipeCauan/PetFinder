package com.example.petfinder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.petfinder.ui.screens.LoginScreen
import com.example.petfinder.ui.screens.RegisterScreen
import com.example.petfinder.ui.viewmodel.AuthViewModel

object PetFinderRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
}

/**
 * Grafo de navegação principal do PetFinder.
 */
@Composable
fun PetFinderNavGraph(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = PetFinderRoutes.LOGIN,
        modifier = modifier
    ) {
        composable(PetFinderRoutes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(PetFinderRoutes.REGISTER)
                },
                onLoginSuccess = {
                    // Próxima etapa: navegar para a tela inicial de pets para adoção.
                }
            )
        }

        composable(PetFinderRoutes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.popBackStack(
                        route = PetFinderRoutes.LOGIN,
                        inclusive = false
                    )
                }
            )
        }
    }
}
