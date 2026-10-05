package org.example.admin.student

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class StudentRepository(
    private val jdbcClient: JdbcClient,
) {

    /**
     * Создаёт студента вместе с нулевым балансом.
     * @return false, если студент с таким логином уже есть.
     */
    @Transactional
    fun insert(login: String, name: String, lastname: String, mineTokenHash: String, cityTokenHash: String): Boolean {
        val inserted = jdbcClient.sql(
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
        if (!inserted) {
            return false
        }

        jdbcClient.sql("INSERT INTO balance (login, money) VALUES (:login, 0)")
            .param("login", login)
            .update()
        return true
    }

    fun exists(login: String): Boolean =
        jdbcClient.sql("SELECT EXISTS (SELECT 1 FROM student WHERE login = :login)")
            .param("login", login)
            .query(Boolean::class.javaObjectType)
            .single()

    fun findLogins(): List<String> =
        jdbcClient.sql("SELECT login FROM student ORDER BY login")
            .query(String::class.java)
            .list()
            .filterNotNull()
}
