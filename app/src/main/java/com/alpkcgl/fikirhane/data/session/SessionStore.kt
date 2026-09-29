package com.alpkcgl.fikirhane.data.session

import android.content.Context
import androidx.core.content.edit
import com.alpkcgl.fikirhane.data.api.ApiJson
import com.alpkcgl.fikirhane.data.model.PublicUser
import com.alpkcgl.fikirhane.data.model.SessionTokens
import kotlinx.serialization.Serializable

/** Kalıcı oturum: token'lar ve oturumdaki kullanıcı. */
@Serializable
data class StoredSession(val tokens: SessionTokens, val user: PublicUser)

interface SessionStore {
    fun load(): StoredSession?
    fun save(session: StoredSession)
    fun clear()
}

/**
 * Oturumu uygulamaya özel SharedPreferences'ta tutar (diğer uygulamalar erişemez;
 * manifest'te yedekleme kapalı olduğundan cihaz yedeğine de girmez).
 */
class SharedPreferencesSessionStore(context: Context) : SessionStore {
    private val prefs = context.getSharedPreferences("fikirhane.session", Context.MODE_PRIVATE)

    override fun load(): StoredSession? =
        prefs.getString(KEY, null)?.let {
            // Biçim değiştiyse (eski sürüm) oturum yok sayılır; kullanıcı yeniden giriş yapar.
            runCatching { ApiJson.decodeFromString<StoredSession>(it) }.getOrNull()
        }

    override fun save(session: StoredSession) {
        prefs.edit { putString(KEY, ApiJson.encodeToString(session)) }
    }

    override fun clear() {
        prefs.edit { remove(KEY) }
    }

    private companion object {
        const val KEY = "session"
    }
}
