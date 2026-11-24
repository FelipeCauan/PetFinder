package com.example.petfinder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.petfinder.ui.telas.LoginScreen
import com.example.petfinder.ui.telas.RegisterScreen
import com.example.petfinder.ui.telas.TelaCadastroAnimal
import com.example.petfinder.ui.telas.TelaInicial
import com.example.petfinder.ui.telas.TelaPerfil
import com.example.petfinder.ui.viewmodel.AuthViewModel
import com.example.petfinder.ui.viewmodel.HomeViewModel
import com.example.petfinder.ui.viewmodel.PetFormViewModel
import com.example.petfinder.ui.viewmodel.ProfileViewModel

object PetFinderRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val REGISTER_PET = "register_pet"
}

/**
 * Grafo de navegação principal do PetFinder.
 */
@Composable
fun PetFinderNavGraph(
    startDestination: String,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    petFormViewModel: PetFormViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(PetFinderRoutes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(PetFinderRoutes.REGISTER)
                },
                onLoginSuccess = {
                    navController.navigate(PetFinderRoutes.HOME) {
                        popUpTo(PetFinderRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
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
                    navController.navigate(PetFinderRoutes.HOME) {
                        popUpTo(PetFinderRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(PetFinderRoutes.HOME) {
            TelaInicial(
                viewModel = homeViewModel,
                onNavigateToProfile = {
                    navController.navigate(PetFinderRoutes.PROFILE) {
                        launchSingleTop = true
                    }
                },
                onNavigateToRegisterPet = {
                    petFormViewModel.resetForm()
                    navController.navigate(PetFinderRoutes.REGISTER_PET) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(PetFinderRoutes.PROFILE) {
            TelaPerfil(
                viewModel = profileViewModel,
                onNavigateToHome = {
                    navController.navigate(PetFinderRoutes.HOME) {
                        popUpTo(PetFinderRoutes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRegisterPet = {
                    petFormViewModel.resetForm()
                    navController.navigate(PetFinderRoutes.REGISTER_PET) {
                        launchSingleTop = true
                    }
                },
                onLogout = {
                    navController.navigate(PetFinderRoutes.LOGIN) {
                        popUpTo(PetFinderRoutes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(PetFinderRoutes.REGISTER_PET) {
            TelaCadastroAnimal(
                viewModel = petFormViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPublishSuccess = {
                    homeViewModel.showFeedbackMessage("Animal publicado para adoção!")
                    navController.navigate(PetFinderRoutes.HOME) {
                        popUpTo(PetFinderRoutes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSessionExpired = {
                    navController.navigate(PetFinderRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
