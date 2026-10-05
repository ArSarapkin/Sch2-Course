package org.example.admin.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.example.admin.student.StudentRepository
import org.example.common.config.ConfigKeys
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Просмотр и изменение динамического конфига (таблица config) для админ-панели.
 */
@RestController
@RequestMapping("/config")
class ConfigController(
    private val configRepository: ConfigRepository,
    private val studentRepository: StudentRepository,
) {

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "") login: String,
        @RequestParam(defaultValue = "") key: String,
    ): List<ConfigEntry> = configRepository.find(login.trim(), key.trim())

    /** Ключи, которые можно задавать. */
    @GetMapping("/keys")
    fun keys(): List<String> = ConfigKeys.ALL

    /** Создаёт значение или заменяет существующее. */
    @PutMapping
    fun put(@Valid @RequestBody entry: ConfigEntry): ConfigEntry {
        if (entry.key !in ConfigKeys.ALL) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown config key '${entry.key}'")
        }
        if (!studentRepository.exists(entry.login)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Student '${entry.login}' is not registered")
        }
        configRepository.upsert(entry)
        return entry
    }

    /** Удаляет значение; после этого сервисы берут для студента значение по умолчанию. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@RequestParam login: String, @RequestParam key: String) {
        if (!configRepository.delete(login, key)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "No config '$key' for student '$login'")
        }
    }
}

data class ConfigEntry(
    @field:NotBlank val login: String,
    @field:NotBlank val key: String,
    @field:NotBlank val value: String,
)
