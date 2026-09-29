package com.alpkcgl.fikirhane.data.repository

import com.alpkcgl.fikirhane.data.api.ApiException
import com.alpkcgl.fikirhane.data.api.FikirhaneApi
import com.alpkcgl.fikirhane.data.model.PublicUser
import com.alpkcgl.fikirhane.data.model.RegisterRequest
import com.alpkcgl.fikirhane.data.model.RegisterResponse
import com.alpkcgl.fikirhane.data.model.SessionTokens
import com.alpkcgl.fikirhane.data.session.SessionStore
import com.alpkcgl.fikirhane.data.session.StoredSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Kayıt sonucu: oturum açıldı mı, yoksa e-posta doğrulaması mı bekleniyor? */
sealed interface RegisterResult {
    data class LoggedIn(val user: PublicUser) : RegisterResult
    data class ConfirmationRequired(val email: String) : RegisterResult
}

/**
 * Oturumun tek sahibi. Token'ları saklar, süresi dolmak üzereyken yeniler ve
 * oturumdaki kullanıcıyı [currentUser] ile yayınlar (null = çıkış yapılmış).
 */
class AuthRepository(
    private val api: FikirhaneApi,
    private val store: SessionStore,
    private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 },
) {
    private var session: StoredSession? = store.load()
    private val refreshMutex = Mutex()

    private val _currentUser = MutableStateFlow(session?.user)
    val currentUser: StateFlow<PublicUser?> = _currentUser.asStateFlow()

    suspend fun login(email: String, password: String): PublicUser {
        val result = api.login(email.trim().lowercase(), password)
        setSession(StoredSession(result.session, result.user))
        return result.user
    }

    suspend fun register(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): RegisterResult {
        val normalizedEmail = email.trim().lowercase()
        val result: RegisterResponse = api.register(
            RegisterRequest(
                email = normalizedEmail,
                password = password,
                username = username.trim().lowercase(),
                displayName = displayName.trim(),
            ),
        )
        val tokens = result.session
        return if (tokens != null) {
            setSession(StoredSession(tokens, result.user))
            RegisterResult.LoggedIn(result.user)
        } else {
            RegisterResult.ConfirmationRequired(normalizedEmail)
        }
    }

    suspend fun resendConfirmation(email: String): String =
        api.resendConfirmation(email.trim().lowercase()).message

    fun logout() {
        session = null
        store.clear()
        _currentUser.value = null
    }

    /** Kullanıcı bilgisini sunucudan tazeler (ör. rol değiştiyse). */
    suspend fun refreshUser(): PublicUser {
        val user = authorized { token -> api.me(token).user }
        session?.let { setSession(it.copy(user = user)) }
        return user
    }

    /**
     * Giriş gerektiren bir API çağrısını geçerli bir access token ile çalıştırır.
     * Token süresi dolmak üzereyse önce yenilenir; sunucu yine de 401 dönerse bir kez
     * yenileyip tekrar denenir. Yenileme reddedilirse oturum kapatılır.
     */
    suspend fun <T> authorized(block: suspend (accessToken: String) -> T): T {
        val current = session ?: throw notLoggedIn()
        val token = if (isExpiringSoon(current.tokens)) {
            refreshTokens(current.tokens.accessToken).accessToken
        } else {
            current.tokens.accessToken
        }
        return try {
            block(token)
        } catch (e: ApiException) {
            if (!e.isUnauthorized) throw e
            block(refreshTokens(token).accessToken)
        }
    }

    /**
     * Aynı anda birden fazla istek yenileme isterse yalnızca biri sunucuya gider.
     * [staleAccessToken]: isteğin kullandığı (artık geçersiz sayılan) access token; bu arada
     * başka bir istek yenilediyse yeni token'lar doğrudan kullanılır.
     */
    private suspend fun refreshTokens(staleAccessToken: String): SessionTokens = refreshMutex.withLock {
        val current = session ?: throw notLoggedIn()
        val alreadyRefreshed = current.tokens.accessToken != staleAccessToken &&
            !isExpiringSoon(current.tokens)
        if (alreadyRefreshed) return current.tokens

        val tokens = try {
            api.refresh(current.tokens.refreshToken).session
        } catch (e: ApiException) {
            // Ağ hatasında oturum korunur; sunucu reddettiyse kullanıcı yeniden giriş yapmalı.
            if (e.isUnauthorized) logout()
            throw e
        }
        setSession(current.copy(tokens = tokens))
        tokens
    }

    private fun isExpiringSoon(tokens: SessionTokens): Boolean {
        val expiresAt = tokens.expiresAt ?: return false
        return expiresAt - nowSeconds() < REFRESH_MARGIN_SECONDS
    }

    private fun setSession(value: StoredSession) {
        session = value
        store.save(value)
        _currentUser.value = value.user
    }

    private fun notLoggedIn() = ApiException(401, "Giriş yapman gerekiyor")

    private companion object {
        /** Token'ın süresi dolmadan bu kadar saniye önce yenilenir. */
        const val REFRESH_MARGIN_SECONDS = 60L
    }
}
