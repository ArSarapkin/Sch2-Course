package org.example.common.auth

import java.security.MessageDigest

object TokenHash {

    /**
     * SHA-256 в hex (64 символа). Токены случайные и длинные, поэтому соль и медленный хеш не нужны,
     * а детерминированный хеш позволяет искать студента по токену.
     */
    fun of(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .toHexString()
}
