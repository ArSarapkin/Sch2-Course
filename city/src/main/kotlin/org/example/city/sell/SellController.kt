package org.example.city.sell

import org.example.common.auth.StudentTokenInterceptor
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
class SellController(
    private val sellService: SellService,
) {

    @PostMapping("/sell")
    fun sell(
        @RequestAttribute(StudentTokenInterceptor.LOGIN_ATTRIBUTE) login: String,
        @RequestParam resource: UUID,
    ): SellResponse =
        sellService.sell(login, resource)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Resource $resource does not exist or is already sold")
}
