package org.example.mine.dig

import org.example.common.resource.ResourceType
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Настройки dig из application.yml (`app.dig`).
 *
 * @param resourceChances вероятность выпадения каждого типа ресурса в процентах
 */
@ConfigurationProperties("app.dig")
data class DigProperties(
    val resourceChances: Map<ResourceType, Int>,
) {
    init {
        require(resourceChances.values.all { it >= 0 }) { "app.dig.resource-chances must not be negative" }
        require(resourceChances.values.sum() == 100) {
            "app.dig.resource-chances must sum to 100, got ${resourceChances.values.sum()}"
        }
    }
}
