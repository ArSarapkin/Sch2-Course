package org.example.admin.config

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class ConfigRepository(
    private val jdbcClient: JdbcClient,
) {

    /**
     * Записи таблицы config. Пустой [login] или [key] означает «без фильтра»;
     * логин ищется по вхождению без учёта регистра, ключ — точно.
     */
    fun find(login: String, key: String): List<ConfigEntry> =
        jdbcClient.sql(
            """
            SELECT login, key, value
            FROM config
            WHERE (:login = '' OR position(lower(:login) IN lower(login)) > 0)
              AND (:key = '' OR key = :key)
            ORDER BY login, key
            LIMIT $MAX_ROWS
            """.trimIndent()
        )
            .param("login", login)
            .param("key", key)
            .query { rs, _ -> ConfigEntry(rs.getString("login"), rs.getString("key"), rs.getString("value")) }
            .list()

    /** Создаёт значение или заменяет существующее. */
    fun upsert(entry: ConfigEntry) {
        jdbcClient.sql(
            """
            INSERT INTO config (login, key, value) VALUES (:login, :key, :value)
            ON CONFLICT (login, key) DO UPDATE SET value = EXCLUDED.value
            """.trimIndent()
        )
            .param("login", entry.login)
            .param("key", entry.key)
            .param("value", entry.value)
            .update()
    }

    /** @return false, если такой записи не было. */
    fun delete(login: String, key: String): Boolean =
        jdbcClient.sql("DELETE FROM config WHERE login = :login AND key = :key")
            .param("login", login)
            .param("key", key)
            .update() == 1

    private companion object {
        private const val MAX_ROWS = 1000
    }
}
