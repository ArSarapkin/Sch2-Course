package org.example.mine.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.common.auth.BearerToken
import org.example.common.auth.TokenHash
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Пропускает запрос только с заголовком `Authorization: Bearer <mine_token>` студента
 * и кладёт его логин в атрибут запроса [LOGIN_ATTRIBUTE].
 */
@Component
class StudentTokenInterceptor(
    private val jdbcClient: JdbcClient,
) : HandlerInterceptor {

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

    private fun findLogin(mineTokenHash: String): String? =
        jdbcClient.sql("SELECT login FROM student WHERE mine_token_hash = :hash")
            .param("hash", mineTokenHash)
            .query(String::class.java)
            .optional()
            .orElse(null)

    companion object {
        /** Используется в контроллерах как `@RequestAttribute(LOGIN_ATTRIBUTE) login: String`. */
        const val LOGIN_ATTRIBUTE = "studentLogin"
    }
}
