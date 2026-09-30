package org.example.common.config

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component

/**
 * Чтение динамической конфигурации студента из таблицы `config`.
 *
 * Каждый метод требует defaultValue: он возвращается, если ключа для студента нет
 * или значение не удалось разобрать, так что логика сервиса не зависит от заполненности таблицы.
 */
@Component
class DynamicConfig(
    private val jdbcClient: JdbcClient,
) {

    fun getString(login: String, key: String, defaultValue: String): String =
        get(login, key, defaultValue) { it }

    fun getInt(login: String, key: String, defaultValue: Int): Int =
        get(login, key, defaultValue) { it.trim().toIntOrNull() }

    fun getLong(login: String, key: String, defaultValue: Long): Long =
        get(login, key, defaultValue) { it.trim().toLongOrNull() }

    fun getDouble(login: String, key: String, defaultValue: Double): Double =
        get(login, key, defaultValue) { it.trim().toDoubleOrNull() }

    fun getBoolean(login: String, key: String, defaultValue: Boolean): Boolean =
        get(login, key, defaultValue) { it.trim().lowercase().toBooleanStrictOrNull() }

    private fun <T : Any> get(login: String, key: String, defaultValue: T, parse: (String) -> T?): T {
        val raw = jdbcClient.sql("SELECT value FROM config WHERE login = :login AND key = :key")
            .param("login", login)
            .param("key", key)
            .query(String::class.java)
            .optional()
            .orElse(null)
            ?: return defaultValue

        return parse(raw) ?: run {
            log.warn("Config value '{}' for login '{}' and key '{}' is malformed, using default '{}'", raw, login, key, defaultValue)
            defaultValue
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(DynamicConfig::class.java)
    }
}
