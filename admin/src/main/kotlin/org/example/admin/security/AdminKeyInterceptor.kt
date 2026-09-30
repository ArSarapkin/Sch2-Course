package org.example.admin.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.example.common.auth.BearerToken
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.security.MessageDigest

/**
 * Пропускает запрос только с заголовком `Authorization: Bearer <app.admin.key>`.
 */
@Component
class AdminKeyInterceptor(
    @Value("\${app.admin.key}") adminKey: String,
) : HandlerInterceptor {

    private val adminKey = adminKey.toByteArray()

    init {
        require(adminKey.isNotBlank()) { "app.admin.key (ADMIN_KEY) must not be blank" }
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val provided = BearerToken.extract(request.getHeader(HttpHeaders.AUTHORIZATION))?.toByteArray()
        // сравнение за постоянное время, чтобы ключ нельзя было подобрать по времени ответа
        if (provided != null && MessageDigest.isEqual(provided, adminKey)) {
            return true
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        response.sendError(HttpStatus.UNAUTHORIZED.value())
        return false
    }
}
