package com.alpkcgl.fikirhane.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `kullanıcı adı kuralları web ile aynı`() {
        assertNull(AuthValidation.username("ali_alp42"))
        assertNotNull(AuthValidation.username("ab"))
        assertNotNull(AuthValidation.username("Ali"))
        assertNotNull(AuthValidation.username("ali alp"))
        assertNotNull(AuthValidation.username("çağrı"))
        assertNotNull(AuthValidation.username("a".repeat(31)))
    }

    @Test
    fun `kullanıcı adına e-posta yazılırsa özel mesaj verir`() {
        assertEquals(
            "Buraya e-posta değil, kısa bir kullanıcı adı yaz (ör. ali_alp)",
            AuthValidation.username("ali@gmail.com"),
        )
    }

    @Test
    fun `e-posta, görünen ad ve şifre`() {
        assertNull(AuthValidation.email("ayse@example.com"))
        assertNotNull(AuthValidation.email("bozuk"))
        assertNull(AuthValidation.displayName("Ayşe"))
        assertNotNull(AuthValidation.displayName("   "))
        assertNotNull(AuthValidation.displayName("a".repeat(61)))
        assertNotNull(AuthValidation.newPassword("1234567"))
        assertNull(AuthValidation.newPassword("12345678"))
        assertNotNull(AuthValidation.newPassword("a".repeat(73)))
        assertNotNull(AuthValidation.passwordConfirm("12345678", "12345679"))
        assertNull(AuthValidation.passwordConfirm("12345678", "12345678"))
    }
}
