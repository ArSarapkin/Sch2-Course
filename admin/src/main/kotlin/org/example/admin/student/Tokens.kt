package org.example.admin.student

import java.security.SecureRandom
import java.util.Base64

object Tokens {

    private const val TOKEN_BYTES = 32

    private val random = SecureRandom()

    /** Случайный токен: 32 байта в base64url без паддинга. */
    fun generate(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
