package org.example.city.sell

import org.example.common.auth.StudentTokenInterceptor
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class SellController(
    private val sellService: SellService,
) {

    /** Отвечает только, продан ли ресурс; тип, цену и баланс не раскрывает. */
    @PostMapping("/sell")
    fun sell(
        @RequestAttribute(StudentTokenInterceptor.LOGIN_ATTRIBUTE) login: String,
        @RequestParam resource: UUID,
    ): SellResponse =
        SellResponse(sellService.sell(login, resource))
}

data class SellResponse(val sold: Boolean)
