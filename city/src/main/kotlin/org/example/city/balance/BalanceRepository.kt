package org.example.city.balance

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class BalanceRepository(
    private val jdbcClient: JdbcClient,
) {

    /** Баланс студента; 0, если записи ещё нет (студент зарегистрирован до появления таблицы balance). */
    fun get(login: String): Long =
        jdbcClient.sql("SELECT money FROM balance WHERE login = :login")
            .param("login", login)
            .query(Long::class.javaObjectType)
            .optional()
            .orElse(0L)

    /** Прибавляет [amount] к балансу и возвращает новое значение. */
    fun add(login: String, amount: Long): Long =
        jdbcClient.sql(
            """
            INSERT INTO balance (login, money) VALUES (:login, :amount)
            ON CONFLICT (login) DO UPDATE SET money = balance.money + EXCLUDED.money
            RETURNING money
            """.trimIndent()
        )
            .param("login", login)
            .param("amount", amount)
            .query(Long::class.javaObjectType)
            .single()
}
