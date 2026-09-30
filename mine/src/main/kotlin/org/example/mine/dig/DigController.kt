package org.example.mine.dig

import org.example.common.auth.StudentTokenInterceptor
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DigController(
    private val digService: DigService,
) {

    @PostMapping("/dig")
    fun dig(@RequestAttribute(StudentTokenInterceptor.LOGIN_ATTRIBUTE) login: String): ResponseEntity<Any> =
        when (val result = digService.dig(login)) {
            is DigResult.Success -> ResponseEntity.ok(DigResponse(result.resource))
            is DigResult.TooEarly -> tooManyRequests(
                result.waitMs,
                DigRejection("TOO_EARLY", "Dig timeout has not passed yet"),
            )
            is DigResult.Banned -> tooManyRequests(
                result.waitMs,
                DigRejection("BANNED", "Dig is temporarily blocked for too many early attempts"),
            )
        }

    private fun tooManyRequests(waitMs: Long, body: DigRejection): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            // Retry-After по стандарту в целых секундах, поэтому точное значение отдельно в мс
            .header(HttpHeaders.RETRY_AFTER, ((waitMs + 999) / 1000).toString())
            .header(RETRY_AFTER_MS, waitMs.toString())
            .body(body)

    private companion object {
        private const val RETRY_AFTER_MS = "Retry-After-Ms"
    }
}

data class DigResponse(val resource: UUID)

data class DigRejection(val reason: String, val message: String)
