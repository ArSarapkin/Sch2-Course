package org.example.city.sell

import org.example.common.resource.ResourceType
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Настройки продажи из application.yml (`app.sell`).
 *
 * @param prices цена продажи ресурса каждого типа
 */
@ConfigurationProperties("app.sell")
data class SellProperties(
    val prices: Map<ResourceType, Long>,
) {
    init {
        val missing = ResourceType.entries - prices.keys
        require(missing.isEmpty()) { "app.sell.prices has no price for $missing" }
        require(prices.values.all { it >= 0 }) { "app.sell.prices must not be negative" }
    }

    fun priceOf(type: ResourceType): Long = prices.getValue(type)
}
