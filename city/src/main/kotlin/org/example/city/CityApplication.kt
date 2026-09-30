package org.example.city

import org.example.common.CommonConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(CommonConfiguration::class)
class CityApplication

fun main(args: Array<String>) {
    runApplication<CityApplication>(*args)
}
