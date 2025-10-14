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
import com.example.skusho.domain.usecase.settings.GetOnboardingCompletedUseCase
import com.example.skusho.domain.usecase.settings.SetOnboardingCompletedUseCase
import com.example.skusho.ui.home.HomeScreen
import com.example.skusho.ui.onboarding.OnboardingScreen
import com.example.skusho.ui.settings.SettingsScreen
import com.example.skusho.ui.theme.SkushoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var getOnboardingCompletedUseCase: GetOnboardingCompletedUseCase
    
    @Inject
    lateinit var setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkushoTheme {
                SkushoApp(
                    getOnboardingCompletedUseCase = getOnboardingCompletedUseCase,
                    setOnboardingCompletedUseCase = setOnboardingCompletedUseCase
                )
            }
        }
    }
}

@Composable
fun SkushoApp(
    getOnboardingCompletedUseCase: GetOnboardingCompletedUseCase,
    setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase
) {
    val navController = rememberNavController()
    val onboardingCompleted by getOnboardingCompletedUseCase().collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    
    NavHost(
        navController = navController,
        startDestination = if (onboardingCompleted) "home" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    scope.launch {
                        setOnboardingCompletedUseCase(true)
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
                },
                onRestartTutorial = {
                    scope.launch {
                        setOnboardingCompletedUseCase(false)
                        navController.navigate("onboarding") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
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
