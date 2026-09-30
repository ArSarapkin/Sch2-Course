package org.example.mine

import org.example.common.CommonConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(CommonConfiguration::class)
@ConfigurationPropertiesScan
class MineApplication

fun main(args: Array<String>) {
    runApplication<MineApplication>(*args)
}
