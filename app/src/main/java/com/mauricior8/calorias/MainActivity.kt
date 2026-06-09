package com.mauricior8.calorias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mauricior8.calorias.ui.MainViewModel
import com.mauricior8.calorias.ui.screen.MainScreen
import com.mauricior8.calorias.ui.theme.CaloriasTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as CaloriasApp
        MainViewModel.Factory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CaloriasTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
