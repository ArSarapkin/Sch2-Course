package org.example.mine.dig

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class DigLogRepository(
    private val jdbcClient: JdbcClient,
) {

    fun insert(time: Instant, login: String, resource: UUID) {
        jdbcClient.sql("INSERT INTO dig_log (time, login, resourse) VALUES (:time, :login, :resource)")
            .param("time", OffsetDateTime.ofInstant(time, ZoneOffset.UTC))
            .param("login", login)
            .param("resource", resource)
            .update()
    }

    /** Время последнего dig каждого студента. */
    fun findLastDigTimes(): Map<String, Instant> =
        jdbcClient.sql("SELECT login, MAX(time) AS last_time FROM dig_log GROUP BY login")
            .query { rs, _ -> rs.getString("login") to rs.getObject("last_time", OffsetDateTime::class.java).toInstant() }
            .list()
            .toMap()
}
