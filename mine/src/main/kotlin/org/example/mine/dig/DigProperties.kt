package org.example.mine.dig

import org.example.common.resource.ResourceType
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Настройки dig из application.yml (`app.dig`).
 *
 * @param banThreshold сколько 429 на dig студент может получить до временной блокировки (счёт с конца прошлой)
 * @param banDuration длительность временной блокировки dig
 * @param resourceChances вероятность выпадения каждого типа ресурса в процентах
 */
@ConfigurationProperties("app.dig")
data class DigProperties(
    val banThreshold: Int,
    val banDuration: Duration,
    val resourceChances: Map<ResourceType, Int>,
) {
    init {
        require(banThreshold >= 0) { "app.dig.ban-threshold must not be negative" }
        require(!banDuration.isNegative) { "app.dig.ban-duration must not be negative" }
        require(resourceChances.values.all { it >= 0 }) { "app.dig.resource-chances must not be negative" }
        require(resourceChances.values.sum() == 100) {
            "app.dig.resource-chances must sum to 100, got ${resourceChances.values.sum()}"
        }
    }
}
