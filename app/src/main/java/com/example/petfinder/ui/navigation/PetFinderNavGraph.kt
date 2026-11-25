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
    val goToLoginClearingBackstack: () -> Unit = {
        homeViewModel.resetState()
        navController.navigate(PetFinderRoutes.LOGIN) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    val openRegisterPet: () -> Unit = {
        petFormViewModel.resetForm()
        navController.navigate(PetFinderRoutes.REGISTER_PET) { launchSingleTop = true }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(PetFinderRoutes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(PetFinderRoutes.REGISTER) },
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
                onNavigateBack = { navController.popBackStack() },
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
                    navController.navigate(PetFinderRoutes.PROFILE) { launchSingleTop = true }
                },
                onNavigateToRegisterPet = openRegisterPet,
                onPetClick = { petId -> navController.navigate(PetFinderRoutes.petDetailRoute(petId)) }
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
                onNavigateToRegisterPet = openRegisterPet,
                onNavigateToEditProfile = {
                    navController.navigate(PetFinderRoutes.EDIT_PROFILE) { launchSingleTop = true }
                },
                onPetClick = { petId -> navController.navigate(PetFinderRoutes.petDetailRoute(petId)) },
                onLogout = goToLoginClearingBackstack
            )
        }

        composable(PetFinderRoutes.EDIT_PROFILE) {
            TelaEditarPerfil(
                viewModel = editProfileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = {
                    profileViewModel.showFeedbackMessage("Perfil atualizado com sucesso!")
                    navController.popBackStack()
                },
                onAccountDeleted = {
                    authViewModel.showFeedbackMessage("Conta excluída com sucesso.")
                    goToLoginClearingBackstack()
                },
                onSessionExpired = goToLoginClearingBackstack
            )
        }

        composable(PetFinderRoutes.REGISTER_PET) {
            TelaCadastroAnimal(
                viewModel = petFormViewModel,
                petId = null,
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    homeViewModel.showFeedbackMessage("Animal publicado para adoção!")
                    navController.navigate(PetFinderRoutes.HOME) {
                        popUpTo(PetFinderRoutes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSessionExpired = goToLoginClearingBackstack
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
                onNavigateBack = { navController.popBackStack() },
                onEditPetClick = { id ->
                    navController.navigate(PetFinderRoutes.editPetRoute(id)) { launchSingleTop = true }
                }
            )
        }

        composable(
            route = PetFinderRoutes.EDIT_PET,
            arguments = listOf(navArgument("petId") { type = NavType.LongType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getLong("petId") ?: -1L
            TelaCadastroAnimal(
                viewModel = petFormViewModel,
                petId = petId,
                onNavigateBack = { navController.popBackStack() },
                onSuccess = {
                    petDetailViewModel.showFeedbackMessage("Alterações salvas!")
                    navController.popBackStack()
                },
                onDeleted = {
                    navController.popBackStack(route = PetFinderRoutes.PET_DETAIL, inclusive = true)
                    when (navController.currentBackStackEntry?.destination?.route) {
                        PetFinderRoutes.PROFILE -> profileViewModel.showFeedbackMessage("Cadastro excluído com sucesso.")
                        else -> homeViewModel.showFeedbackMessage("Cadastro excluído com sucesso.")
                    }
                },
                onSessionExpired = goToLoginClearingBackstack
            )
        }
    }
}
