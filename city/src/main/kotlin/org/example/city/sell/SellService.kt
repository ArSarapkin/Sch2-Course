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

    /** Продаёт ресурс; null, если ресурса нет. Удаление ресурса и начисление денег в одной транзакции. */
    @Transactional
    fun sell(login: String, resource: UUID): SellResponse? {
        val type = resourceRepository.delete(resource) ?: return null
        val price = sellProperties.priceOf(type)
        val money = balanceRepository.add(login, price)
        return SellResponse(type.name, price, money)
    }
}

data class SellResponse(val type: String, val price: Long, val money: Long)
