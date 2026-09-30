package org.example.common.auth

object BearerToken {

    private const val PREFIX = "Bearer "

    /** Токен из значения заголовка `Authorization: Bearer <token>`, либо null, если заголовка нет или он другого вида. */
    fun extract(authorizationHeader: String?): String? =
        authorizationHeader
            ?.takeIf { it.startsWith(PREFIX, ignoreCase = true) }
            ?.substring(PREFIX.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}
