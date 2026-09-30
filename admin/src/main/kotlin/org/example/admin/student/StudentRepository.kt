package org.example.admin.student

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository

@Repository
class StudentRepository(
    private val jdbcClient: JdbcClient,
) {

    /** @return false, если студент с таким логином уже есть. */
    fun insert(login: String, name: String, lastname: String, mineTokenHash: String, cityTokenHash: String): Boolean =
        jdbcClient.sql(
            """
            INSERT INTO student (login, name, lastname, mine_token_hash, city_token_hash)
            VALUES (:login, :name, :lastname, :mineTokenHash, :cityTokenHash)
            ON CONFLICT (login) DO NOTHING
            """.trimIndent()
        )
            .param("login", login)
            .param("name", name)
            .param("lastname", lastname)
            .param("mineTokenHash", mineTokenHash)
            .param("cityTokenHash", cityTokenHash)
            .update() == 1
}
