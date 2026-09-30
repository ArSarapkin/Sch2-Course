package org.example.mine.resource

import org.example.common.resource.ResourceType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class ResourceRepository(
    private val jdbcClient: JdbcClient,
) {

    fun insert(uuid: UUID, type: ResourceType) {
        jdbcClient.sql("INSERT INTO resource (uuid, type) VALUES (:uuid, :type)")
            .param("uuid", uuid)
            .param("type", type.name)
            .update()
    }
}
