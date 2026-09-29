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
import com.alpkcgl.fikirhane.ui.editor.EditorScreen
import com.alpkcgl.fikirhane.ui.home.HomeScreen
import com.alpkcgl.fikirhane.ui.posts.MyPostsScreen
import com.alpkcgl.fikirhane.ui.posts.PostDetailScreen
import kotlinx.serialization.Serializable

@Serializable
data class LoginRoute(val info: String? = null, val email: String? = null)

@Serializable
data object RegisterRoute

@Serializable
data object HomeRoute

@Serializable
data class PostRoute(val slug: String)

/** slug null: yeni yazı; dolu: o yazıyı düzenle. */
@Serializable
data class EditorRoute(val slug: String? = null)

@Serializable
data object MyPostsRoute

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
        val destination = navController.currentDestination
        val onAuthScreen = destination == null ||
            destination.hasRoute<LoginRoute>() || destination.hasRoute<RegisterRoute>()
        if (loggedIn && onAuthScreen) navController.resetTo(HomeRoute)
        if (!loggedIn && !onAuthScreen) navController.resetTo(LoginRoute())
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
        composable<HomeRoute> {
            HomeScreen(
                onPostClick = { navController.navigate(PostRoute(it)) },
                onWrite = { navController.navigate(EditorRoute()) },
                onMyPosts = { navController.navigate(MyPostsRoute) },
            )
        }
        composable<PostRoute> {
            PostDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(EditorRoute(it)) },
            )
        }
        composable<EditorRoute> {
            EditorScreen(
                onBack = { navController.popBackStack() },
                onPublished = { slug ->
                    // Editör yığından çıkarılır; geri tuşu yazıdan önceki ekrana döner.
                    navController.navigate(PostRoute(slug)) {
                        popUpTo<EditorRoute> { inclusive = true }
                        // Düzenlemeye yazı sayfasından gelindiyse eski kopya da kaldırılır.
                        launchSingleTop = true
                    }
                },
            )
        }
        composable<MyPostsRoute> {
            MyPostsScreen(
                onBack = { navController.popBackStack() },
                onPostClick = { navController.navigate(PostRoute(it)) },
            )
        }
    }
}

/** Geri yığınını temizleyip [route]'a gider. */
private fun NavHostController.resetTo(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
