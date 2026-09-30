package org.example.mine.dig

import org.example.mine.security.StudentTokenInterceptor
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
    fun dig(@RequestAttribute(StudentTokenInterceptor.LOGIN_ATTRIBUTE) login: String): ResponseEntity<DigResponse> =
        when (val result = digService.dig(login)) {
            is DigResult.Success -> ResponseEntity.ok(DigResponse(result.resource))
            is DigResult.TooEarly -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                // Retry-After по стандарту в целых секундах, поэтому точное значение отдельно в мс
                .header(HttpHeaders.RETRY_AFTER, ((result.waitMs + 999) / 1000).toString())
                .header(RETRY_AFTER_MS, result.waitMs.toString())
                .build()
        }

    private companion object {
        private const val RETRY_AFTER_MS = "Retry-After-Ms"
    }
}

data class DigResponse(val resource: UUID)
