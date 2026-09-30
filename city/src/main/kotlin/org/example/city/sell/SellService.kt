package org.example.city.sell

import org.example.city.balance.BalanceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SellService(
    private val resourceRepository: ResourceRepository,
    private val balanceRepository: BalanceRepository,
    private val sellProperties: SellProperties,
) {

    /**
     * Продаёт ресурс; false, если ресурса нет (не существовал или уже продан).
     * Удаление ресурса и начисление денег в одной транзакции.
     */
    @Transactional
    fun sell(login: String, resource: UUID): Boolean {
        val type = resourceRepository.delete(resource) ?: return false
        balanceRepository.add(login, sellProperties.priceOf(type))
        return true
    }
}
