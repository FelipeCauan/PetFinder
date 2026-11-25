package com.example.petfinder

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petfinder.data.local.PetFinderDatabase
import com.example.petfinder.data.local.SessionManager
import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.data.repository.PetRepository
import com.example.petfinder.ui.navigation.PetFinderNavGraph
import com.example.petfinder.ui.navigation.PetFinderRoutes
import com.example.petfinder.ui.theme.PetFinderTheme
import com.example.petfinder.ui.viewmodel.AuthViewModel
import com.example.petfinder.ui.viewmodel.EditProfileViewModel
import com.example.petfinder.ui.viewmodel.HomeViewModel
import com.example.petfinder.ui.viewmodel.PetDetailViewModel
import com.example.petfinder.ui.viewmodel.PetFormViewModel
import com.example.petfinder.ui.viewmodel.ProfileViewModel
import com.example.petfinder.ui.viewmodel.simpleViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        val database = PetFinderDatabase.getInstance(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        val userDao = database.userDao()
        val authRepository = AuthRepository(userDao, sessionManager)
        val petRepository = PetRepository(database.petDao())

        val hasValidSession = runBlocking(Dispatchers.IO) { authRepository.getLoggedInUser() != null }
        val startDestination = if (hasValidSession) PetFinderRoutes.HOME else PetFinderRoutes.LOGIN

        setContent {
            PetFinderTheme {
                val authViewModel: AuthViewModel = viewModel(
                    factory = simpleViewModelFactory { AuthViewModel(authRepository) }
                )
                val homeViewModel: HomeViewModel = viewModel(
                    factory = simpleViewModelFactory { HomeViewModel(userDao, sessionManager, petRepository) }
                )
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = simpleViewModelFactory { ProfileViewModel(userDao, sessionManager, authRepository, petRepository) }
                )
                val editProfileViewModel: EditProfileViewModel = viewModel(
                    factory = simpleViewModelFactory { EditProfileViewModel(authRepository, petRepository) }
                )
                val petFormViewModel: PetFormViewModel = viewModel(
                    factory = simpleViewModelFactory { PetFormViewModel(authRepository, petRepository) }
                )
                val petDetailViewModel: PetDetailViewModel = viewModel(
                    factory = simpleViewModelFactory { PetDetailViewModel(userDao, sessionManager, petRepository) }
                )

                PetFinderNavGraph(
                    startDestination = startDestination,
                    authViewModel = authViewModel,
                    homeViewModel = homeViewModel,
                    profileViewModel = profileViewModel,
                    editProfileViewModel = editProfileViewModel,
                    petFormViewModel = petFormViewModel,
                    petDetailViewModel = petDetailViewModel
                )
            }
        }
    }
}
