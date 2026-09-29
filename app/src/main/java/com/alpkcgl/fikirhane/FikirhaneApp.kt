package com.alpkcgl.fikirhane

import android.app.Application
import android.content.Context
import com.alpkcgl.fikirhane.data.api.FikirhaneApi
import com.alpkcgl.fikirhane.data.api.HttpClientFactory
import com.alpkcgl.fikirhane.data.image.ImagePreparer
import com.alpkcgl.fikirhane.data.repository.AuthRepository
import com.alpkcgl.fikirhane.data.repository.ModerationRepository
import com.alpkcgl.fikirhane.data.repository.NotificationsRepository
import com.alpkcgl.fikirhane.data.repository.PostsRepository
import com.alpkcgl.fikirhane.data.repository.UsersRepository
import com.alpkcgl.fikirhane.data.session.SharedPreferencesSessionStore
import io.ktor.client.engine.okhttp.OkHttp

/** Manuel DI: uygulama ömrü boyunca yaşayan bağımlılıklar. */
class AppContainer(context: Context) {
    private val http = HttpClientFactory.create(
        baseUrl = BuildConfig.API_BASE_URL,
        engine = OkHttp.create(),
        enableLogging = BuildConfig.DEBUG,
    )
    private val api = FikirhaneApi(http)

    val authRepository = AuthRepository(api, SharedPreferencesSessionStore(context))
    val postsRepository = PostsRepository(api, authRepository)
    val usersRepository = UsersRepository(api, authRepository)
    val notificationsRepository = NotificationsRepository(api, authRepository)
    val moderationRepository = ModerationRepository(api, authRepository, postsRepository)
    val imagePreparer = ImagePreparer(context.contentResolver)
}

class FikirhaneApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
