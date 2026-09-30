package org.example.common

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

/**
 * Подключает бины common в сервис: `@Import(CommonConfiguration::class)` на классе приложения.
 */
@Configuration
@ComponentScan
class CommonConfiguration
