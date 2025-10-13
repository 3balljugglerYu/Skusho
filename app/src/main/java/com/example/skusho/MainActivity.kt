package com.example.skusho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.skusho.data.preferences.AppPreferences
import com.example.skusho.ui.home.HomeScreen
import com.example.skusho.ui.onboarding.OnboardingScreen
import com.example.skusho.ui.settings.SettingsScreen
import com.example.skusho.ui.theme.SkushoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkushoTheme {
                SkushoApp()
            }
        }
    }
}

@Composable
fun SkushoApp() {
    val navController = rememberNavController()
    val appPreferences = AppPreferences(androidx.compose.ui.platform.LocalContext.current)
    val onboardingCompleted by appPreferences.onboardingCompleted.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    
    NavHost(
        navController = navController,
        startDestination = if (onboardingCompleted) "home" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    scope.launch {
                        appPreferences.setOnboardingCompleted(true)
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable("home") {
            HomeScreen(
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}