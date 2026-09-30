package org.example.city.balance

import org.example.common.auth.StudentTokenInterceptor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RestController

@RestController
class BalanceController(
    private val balanceRepository: BalanceRepository,
) {

    @GetMapping("/get_balance")
    fun getBalance(@RequestAttribute(StudentTokenInterceptor.LOGIN_ATTRIBUTE) login: String): BalanceResponse =
        BalanceResponse(balanceRepository.get(login))
}

data class BalanceResponse(val money: Long)
