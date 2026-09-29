package com.alpkcgl.fikirhane.core

/** Web sitesi ve API ile aynı kurallar; hatalı form sunucuya gönderilmeden yakalanır. */
object AuthValidation {
    private val USERNAME = Regex("^[a-z0-9_]{3,30}$")
    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun email(value: String): String? =
        if (EMAIL.matches(value.trim())) null else "Geçerli bir e-posta adresi gir"

    fun username(value: String): String? {
        val v = value.trim()
        return when {
            '@' in v -> "Buraya e-posta değil, kısa bir kullanıcı adı yaz (ör. ali_alp)"
            !USERNAME.matches(v) -> "3-30 karakter; yalnızca küçük harf, rakam ve _ kullanılabilir"
            else -> null
        }
    }

    fun displayName(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Görünen ad gerekli"
            v.length > 60 -> "Görünen ad en fazla 60 karakter olabilir"
            else -> null
        }
    }

    fun newPassword(value: String): String? = when {
        value.length < 8 -> "Şifre en az 8 karakter olmalı"
        value.length > 72 -> "Şifre en fazla 72 karakter olabilir"
        else -> null
    }

    fun passwordConfirm(password: String, confirm: String): String? =
        if (password == confirm) null else "Şifreler eşleşmiyor"
}
