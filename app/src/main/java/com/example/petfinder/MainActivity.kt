package com.example.petfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.example.petfinder.ui.viewmodel.AuthViewModelFactory
import com.example.petfinder.ui.viewmodel.HomeViewModel
import com.example.petfinder.ui.viewmodel.HomeViewModelFactory
import com.example.petfinder.ui.viewmodel.ProfileViewModel
import com.example.petfinder.ui.viewmodel.ProfileViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PetFinderDatabase.getInstance(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        val userDao = database.userDao()
        val authRepository = AuthRepository(userDao, sessionManager)
        val petRepository = PetRepository(database.petDao())

        val authFactory = AuthViewModelFactory(authRepository)
        val homeFactory = HomeViewModelFactory(userDao, sessionManager, petRepository)
        val profileFactory = ProfileViewModelFactory(userDao, sessionManager, authRepository, petRepository)

        val startDestination = if (sessionManager.getUserId() != null) {
            PetFinderRoutes.HOME
        } else {
            PetFinderRoutes.LOGIN
        }

        setContent {
            PetFinderTheme {
                val authViewModel: AuthViewModel = viewModel(factory = authFactory)
                val homeViewModel: HomeViewModel = viewModel(factory = homeFactory)
                val profileViewModel: ProfileViewModel = viewModel(factory = profileFactory)

                PetFinderNavGraph(
                    startDestination = startDestination,
                    authViewModel = authViewModel,
                    homeViewModel = homeViewModel,
                    profileViewModel = profileViewModel
                )
            }
        }
    }
}
