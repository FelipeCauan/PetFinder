
package com.example.petfinder

import com.example.petfinder.ui.telas.TelaInicial
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.petfinder.ui.theme.PetFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PetFinderTheme {
                TelaInicial()
            }
        }
    }
}
