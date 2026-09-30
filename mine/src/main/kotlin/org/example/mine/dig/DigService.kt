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
    private val digProperties: DigProperties,
) {

    /**
     * Состояние dig по логину. Время последнего успешного dig читается из БД только при старте,
     * счётчик 429 и блокировки живут только в памяти.
     */
    private val states = ConcurrentHashMap(digLogRepository.findLastDigTimes().mapValues { DigState(lastDig = it.value) })

    fun dig(login: String): DigResult {
        val timeoutMs = dynamicConfig.getLong(login, ConfigKeys.DIG_TIMEOUT_MS, DEFAULT_DIG_TIMEOUT_MS)
        val banThreshold = digProperties.banThreshold
        val banDurationMs = digProperties.banDuration.toMillis()
        val now = Instant.now()

        // Проверка и обновление состояния атомарны по логину, чтобы параллельные запросы
        // одного студента не прошли оба. БД внутри compute не трогаем, чтобы не держать блокировку.
        var previousLastDig: Instant? = null
        var rejection: DigResult? = null
        states.compute(login) { _, current ->
            val state = current ?: DigState()
            previousLastDig = state.lastDig

            val banLeftMs = state.bannedUntil?.let { Duration.between(now, it).toMillis() } ?: 0L
            if (banLeftMs > 0) {
                rejection = DigResult.Banned(banLeftMs)
                return@compute state
            }
            // блокировка истекла — начинаем считать 429 заново
            val active = if (state.bannedUntil != null) state.copy(earlyAttempts = 0, bannedUntil = null) else state

            val elapsedMs = active.lastDig?.let { Duration.between(it, now).toMillis() } ?: Long.MAX_VALUE
            if (elapsedMs >= timeoutMs) {
                return@compute active.copy(lastDig = now)
            }

            val earlyAttempts = active.earlyAttempts + 1
            if (earlyAttempts > banThreshold) {
                rejection = DigResult.Banned(banDurationMs)
                active.copy(earlyAttempts = earlyAttempts, bannedUntil = now.plusMillis(banDurationMs))
            } else {
                rejection = DigResult.TooEarly(timeoutMs - elapsedMs)
                active.copy(earlyAttempts = earlyAttempts)
            }
        }
        rejection?.let { return it }

        val resource = UUID.randomUUID()
        try {
            transactionTemplate.executeWithoutResult {
                resourceRepository.insert(resource, resourceTypeRandomizer.next())
                digLogRepository.insert(now, login, resource)
            }
        } catch (e: Exception) {
            // dig не записан, поэтому возвращаем прежнее время, если его никто не успел обновить
            states.computeIfPresent(login) { _, state ->
                if (state.lastDig == now) state.copy(lastDig = previousLastDig) else state
            }
            throw e
        }
        return DigResult.Success(resource)
    }

    private data class DigState(
        val lastDig: Instant? = null,
        /** 429 с конца последней блокировки; успешный dig счётчик не сбрасывает. */
        val earlyAttempts: Int = 0,
        val bannedUntil: Instant? = null,
    )

    private companion object {
        private const val DEFAULT_DIG_TIMEOUT_MS = 10_000L
    }
}

sealed interface DigResult {
    data class Success(val resource: UUID) : DigResult
    data class TooEarly(val waitMs: Long) : DigResult
    data class Banned(val waitMs: Long) : DigResult
}
