package org.example.admin.student

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.example.common.auth.TokenHash
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class StudentController(
    private val studentRepository: StudentRepository,
) {

    /**
     * Регистрирует студента и возвращает его токены. В БД сохраняются только хеши,
     * поэтому токены можно получить лишь из этого ответа.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): RegisterResponse {
        val mineToken = Tokens.generate()
        val cityToken = Tokens.generate()

        val inserted = studentRepository.insert(
            login = request.login,
            name = request.name,
            lastname = request.lastname,
            mineTokenHash = TokenHash.of(mineToken),
            cityTokenHash = TokenHash.of(cityToken),
        )
        if (!inserted) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Student '${request.login}' is already registered")
        }

        return RegisterResponse(mineToken, cityToken)
    }
}

data class RegisterRequest(
    @field:NotBlank @field:Size(max = 64) val login: String,
    @field:NotBlank @field:Size(max = 128) val name: String,
    @field:NotBlank @field:Size(max = 128) val lastname: String,
)

data class RegisterResponse(
    @get:JsonProperty("mine_token") val mineToken: String,
    @get:JsonProperty("city_token") val cityToken: String,
)
