package org.example.common.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Пропускает запрос только с заголовком `Authorization: Bearer <токен студента>` для сервиса [service]
 * и кладёт логин студента в атрибут запроса [LOGIN_ATTRIBUTE].
 */
class StudentTokenInterceptor(
    private val jdbcClient: JdbcClient,
    service: StudentTokenService,
) : HandlerInterceptor {

    private val findLoginSql = "SELECT login FROM student WHERE ${service.hashColumn} = :hash"

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val login = BearerToken.extract(request.getHeader(HttpHeaders.AUTHORIZATION))
            ?.let { findLogin(TokenHash.of(it)) }
        if (login != null) {
            request.setAttribute(LOGIN_ATTRIBUTE, login)
            return true
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        response.sendError(HttpStatus.UNAUTHORIZED.value())
        return false
    }

    private fun findLogin(tokenHash: String): String? =
        jdbcClient.sql(findLoginSql)
            .param("hash", tokenHash)
            .query(String::class.java)
            .optional()
            .orElse(null)

    companion object {
        /** Используется в контроллерах как `@RequestAttribute(LOGIN_ATTRIBUTE) login: String`. */
        const val LOGIN_ATTRIBUTE = "studentLogin"
    }
}

/** Сервис, для которого выдан токен, и колонка таблицы student с его хешем. */
enum class StudentTokenService(val hashColumn: String) {
    MINE("mine_token_hash"),
    CITY("city_token_hash"),
}
