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
import com.alpkcgl.fikirhane.ui.admin.AdminScreen
import com.alpkcgl.fikirhane.ui.auth.LoginScreen
import com.alpkcgl.fikirhane.ui.auth.RegisterScreen
import com.alpkcgl.fikirhane.ui.discover.SearchScreen
import com.alpkcgl.fikirhane.ui.discover.TopicScreen
import com.alpkcgl.fikirhane.ui.editor.EditorScreen
import com.alpkcgl.fikirhane.ui.home.HomeScreen
import com.alpkcgl.fikirhane.ui.notifications.NotificationsScreen
import com.alpkcgl.fikirhane.ui.posts.MyPostsScreen
import com.alpkcgl.fikirhane.ui.posts.PostDetailScreen
import com.alpkcgl.fikirhane.ui.profile.EditProfileScreen
import com.alpkcgl.fikirhane.ui.profile.ProfileScreen
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

@Serializable
data class ProfileRoute(val username: String)

@Serializable
data object EditProfileRoute

/** Kategori (slug) ya da etiket (tag) sayfası; biri dolu olur. */
@Serializable
data class TopicRoute(val slug: String? = null, val tag: String? = null)

@Serializable
data object SearchRoute

@Serializable
data object NotificationsRoute

@Serializable
data object AdminRoute

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

    val back: () -> Unit = { navController.popBackStack() }
    val openPost: (String) -> Unit = { navController.navigate(PostRoute(it)) }
    val openProfile: (String) -> Unit = { navController.navigate(ProfileRoute(it)) }

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
                onLoginClick = back,
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
                onPostClick = openPost,
                onWrite = { navController.navigate(EditorRoute()) },
                onMyPosts = { navController.navigate(MyPostsRoute) },
                onProfile = openProfile,
                onSearch = { navController.navigate(SearchRoute) },
                onNotifications = { navController.navigate(NotificationsRoute) },
                onCategory = { navController.navigate(TopicRoute(slug = it)) },
                onAdmin = { navController.navigate(AdminRoute) },
            )
        }
        composable<PostRoute> {
            PostDetailScreen(
                onBack = back,
                onEdit = { navController.navigate(EditorRoute(it)) },
                onProfile = openProfile,
                onCategory = { navController.navigate(TopicRoute(slug = it)) },
                onTag = { navController.navigate(TopicRoute(tag = it)) },
            )
        }
        composable<EditorRoute> {
            EditorScreen(
                onBack = back,
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
        composable<MyPostsRoute> { MyPostsScreen(onBack = back, onPostClick = openPost) }
        composable<ProfileRoute> {
            ProfileScreen(
                onBack = back,
                onPostClick = openPost,
                onEditProfile = { navController.navigate(EditProfileRoute) },
            )
        }
        composable<EditProfileRoute> { EditProfileScreen(onBack = back) }
        composable<TopicRoute> { TopicScreen(onBack = back, onPostClick = openPost) }
        composable<SearchRoute> { SearchScreen(onBack = back, onPostClick = openPost, onProfile = openProfile) }
        composable<NotificationsRoute> {
            NotificationsScreen(onBack = back, onPostClick = openPost, onProfile = openProfile)
        }
        composable<AdminRoute> { AdminScreen(onBack = back, onPostClick = openPost, onProfile = openProfile) }
    }
}

/** Geri yığınını temizleyip [route]'a gider. */
private fun NavHostController.resetTo(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
