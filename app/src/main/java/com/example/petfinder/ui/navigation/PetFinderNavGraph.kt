package com.example.petfinder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.petfinder.ui.telas.LoginScreen
import com.example.petfinder.ui.telas.RegisterScreen
import com.example.petfinder.ui.telas.TelaCadastroAnimal
import com.example.petfinder.ui.telas.TelaDetalheAnimal
import com.example.petfinder.ui.telas.TelaEditarAnimal
import com.example.petfinder.ui.telas.TelaEditarPerfil
import com.example.petfinder.ui.telas.TelaInicial
import com.example.petfinder.ui.telas.TelaPerfil
import com.example.petfinder.ui.viewmodel.AuthViewModel
import com.example.petfinder.ui.viewmodel.EditProfileViewModel
import com.example.petfinder.ui.viewmodel.HomeViewModel
import com.example.petfinder.ui.viewmodel.PetDetailViewModel
import com.example.petfinder.ui.viewmodel.PetFormViewModel
import com.example.petfinder.ui.viewmodel.ProfileViewModel

object PetFinderRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val REGISTER_PET = "register_pet"
    const val EDIT_PET = "edit_pet/{petId}"
    const val PET_DETAIL = "pet_detail/{petId}"

    fun petDetailRoute(petId: Long): String = "pet_detail/$petId"
    fun editPetRoute(petId: Long): String = "edit_pet/$petId"
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
    editProfileViewModel: EditProfileViewModel,
    petFormViewModel: PetFormViewModel,
    petDetailViewModel: PetDetailViewModel,
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
                },
                onPetClick = { petId ->
                    navController.navigate(PetFinderRoutes.petDetailRoute(petId))
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
                onNavigateToEditProfile = {
                    navController.navigate(PetFinderRoutes.EDIT_PROFILE) {
                        launchSingleTop = true
                    }
                },
                onPetClick = { petId ->
                    navController.navigate(PetFinderRoutes.petDetailRoute(petId))
                },
                onLogout = {
                    homeViewModel.resetState()
                    navController.navigate(PetFinderRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(PetFinderRoutes.EDIT_PROFILE) {
            TelaEditarPerfil(
                viewModel = editProfileViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    profileViewModel.showFeedbackMessage("Perfil atualizado com sucesso!")
                    navController.popBackStack()
                },
                onAccountDeleted = {
                    homeViewModel.resetState()
                    authViewModel.showFeedbackMessage("Conta excluída com sucesso.")
                    navController.navigate(PetFinderRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSessionExpired = {
                    homeViewModel.resetState()
                    navController.navigate(PetFinderRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
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

        composable(
            route = PetFinderRoutes.PET_DETAIL,
            arguments = listOf(navArgument("petId") { type = NavType.LongType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getLong("petId") ?: -1L
            TelaDetalheAnimal(
                petId = petId,
                viewModel = petDetailViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditPetClick = { id ->
                    navController.navigate(PetFinderRoutes.editPetRoute(id)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = PetFinderRoutes.EDIT_PET,
            arguments = listOf(navArgument("petId") { type = NavType.LongType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getLong("petId") ?: -1L
            TelaEditarAnimal(
                petId = petId,
                viewModel = petFormViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    petDetailViewModel.showFeedbackMessage("Alterações salvas!")
                    navController.popBackStack()
                },
                onDeleteSuccess = {
                    navController.popBackStack(
                        route = PetFinderRoutes.PET_DETAIL,
                        inclusive = true
                    )
                    when (navController.currentBackStackEntry?.destination?.route) {
                        PetFinderRoutes.PROFILE -> profileViewModel.showFeedbackMessage("Cadastro excluído com sucesso.")
                        else -> homeViewModel.showFeedbackMessage("Cadastro excluído com sucesso.")
                    }
                },
                onSessionExpired = {
                    homeViewModel.resetState()
                    navController.navigate(PetFinderRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
