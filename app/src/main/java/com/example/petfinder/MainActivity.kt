package com.example.petfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petfinder.data.local.PetFinderDatabase
import com.example.petfinder.data.local.SessionManager
import com.example.petfinder.data.repository.AuthRepository
import com.example.petfinder.ui.navigation.PetFinderNavGraph
import com.example.petfinder.ui.theme.PetFinderTheme
import com.example.petfinder.ui.viewmodel.AuthViewModel
import com.example.petfinder.ui.viewmodel.AuthViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PetFinderDatabase.getInstance(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        val authRepository = AuthRepository(database.userDao(), sessionManager)
        val authFactory = AuthViewModelFactory(authRepository)

        setContent {
            PetFinderTheme {
                val authViewModel: AuthViewModel = viewModel(factory = authFactory)
                PetFinderNavGraph(authViewModel = authViewModel)
            }
        }
    }
}
