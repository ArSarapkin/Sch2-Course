package org.example.mine.resource

import org.example.common.resource.ResourceType
import org.example.mine.dig.DigProperties
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom

/** Выбирает тип ресурса случайно по вероятностям из [DigProperties]. */
@Component
class ResourceTypeRandomizer(
    properties: DigProperties,
) {

    private val chances = properties.resourceChances.entries
        .filter { it.value > 0 }
        .map { it.key to it.value }

    fun next(): ResourceType {
        var roll = ThreadLocalRandom.current().nextInt(100)
        for ((type, chance) in chances) {
            roll -= chance
            if (roll < 0) {
                return type
            }
        }
        error("Unreachable: resource chances sum to 100")
    }
}
