package org.example.city.sell

import org.example.common.resource.ResourceType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class ResourceRepository(
    private val jdbcClient: JdbcClient,
) {

    /**
     * Удаляет ресурс и возвращает его тип, либо null, если ресурса нет (не существовал или уже продан).
     * Удаление атомарно, поэтому при параллельных продажах одного ресурса тип получит только одна.
     */
    fun delete(uuid: UUID): ResourceType? =
        jdbcClient.sql("DELETE FROM resource WHERE uuid = :uuid RETURNING type")
            .param("uuid", uuid)
            .query(String::class.java)
            .optional()
            .map(ResourceType::valueOf)
            .orElse(null)
}
