package org.example.mine.dig

import org.example.common.config.ConfigKeys
import org.example.common.config.DynamicConfig
import org.example.mine.resource.ResourceRepository
import org.example.mine.resource.ResourceTypeRandomizer
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class DigService(
    private val digLogRepository: DigLogRepository,
    private val resourceRepository: ResourceRepository,
    private val resourceTypeRandomizer: ResourceTypeRandomizer,
    private val dynamicConfig: DynamicConfig,
    private val transactionTemplate: TransactionTemplate,
) {

    /** Время последнего успешного dig по логину. Из БД читается только при старте, дальше живёт в памяти. */
    private val lastDigTimes = ConcurrentHashMap(digLogRepository.findLastDigTimes())

    fun dig(login: String): DigResult {
        val timeoutMs = dynamicConfig.getLong(login, ConfigKeys.DIG_TIMEOUT_MS, DEFAULT_DIG_TIMEOUT_MS)
        val now = Instant.now()

        // Проверка и резервирование времени атомарны по логину, чтобы параллельные запросы
        // одного студента не прошли оба. БД внутри compute не трогаем, чтобы не держать блокировку.
        var previous: Instant? = null
        var waitMs = 0L
        lastDigTimes.compute(login) { _, last ->
            previous = last
            val elapsedMs = last?.let { Duration.between(it, now).toMillis() } ?: Long.MAX_VALUE
            if (elapsedMs < timeoutMs) {
                waitMs = timeoutMs - elapsedMs
                last
            } else {
                now
            }
        }
        if (waitMs > 0) {
            return DigResult.TooEarly(waitMs)
        }

        val resource = UUID.randomUUID()
        try {
            transactionTemplate.executeWithoutResult {
                resourceRepository.insert(resource, resourceTypeRandomizer.next())
                digLogRepository.insert(now, login, resource)
            }
        } catch (e: Exception) {
            // dig не записан, поэтому возвращаем прежнее время, если его никто не успел обновить
            lastDigTimes.compute(login) { _, current -> if (current == now) previous else current }
            throw e
        }
        return DigResult.Success(resource)
    }

    private companion object {
        private const val DEFAULT_DIG_TIMEOUT_MS = 10_000L
    }
}

sealed interface DigResult {
    data class Success(val resource: UUID) : DigResult
    data class TooEarly(val waitMs: Long) : DigResult
}
