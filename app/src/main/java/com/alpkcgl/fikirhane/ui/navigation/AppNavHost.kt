package com.alpkcgl.fikirhane.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.ui.auth.LoginScreen
import com.alpkcgl.fikirhane.ui.auth.RegisterScreen
import com.alpkcgl.fikirhane.ui.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data class LoginRoute(val info: String? = null, val email: String? = null)

@Serializable
data object RegisterRoute

@Serializable
data object HomeRoute

/**
 * Oturum durumuna göre yönlendirir: giriş yapılınca ana ekrana, çıkış yapılınca ya da
 * oturum sona erince giriş ekranına geçilir; geri tuşuyla önceki duruma dönülmez.
 */
@Composable
fun AppNavHost(authRepository: AuthRepository) {
    val navController = rememberNavController()
    val user by authRepository.currentUser.collectAsStateWithLifecycle()
    val loggedIn = user != null
    val startDestination: Any = remember { if (loggedIn) HomeRoute else LoginRoute() }

    LaunchedEffect(loggedIn) {
        val onHome = navController.currentDestination?.hasRoute<HomeRoute>() == true
        if (loggedIn && !onHome) navController.resetTo(HomeRoute)
        if (!loggedIn && onHome) navController.resetTo(LoginRoute())
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<LoginRoute> { entry ->
            val route = entry.toRoute<LoginRoute>()
            LoginScreen(
                onRegisterClick = { navController.navigate(RegisterRoute) },
                infoMessage = route.info,
                prefillEmail = route.email,
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(
                onLoginClick = { navController.popBackStack() },
                onConfirmationRequired = { email ->
                    navController.resetTo(
                        LoginRoute(
                            info = "Neredeyse tamam! $email adresine bir doğrulama bağlantısı gönderdik. " +
                                "E-postanı doğruladıktan sonra giriş yapabilirsin.",
                            email = email,
                        ),
                    )
                },
            )
        }
        composable<HomeRoute> { HomeScreen() }
    }
}

/** Geri yığınını temizleyip [route]'a gider. */
private fun NavHostController.resetTo(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
