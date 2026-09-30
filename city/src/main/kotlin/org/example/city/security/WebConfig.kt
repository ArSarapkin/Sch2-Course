package org.example.city.security

import org.example.common.auth.StudentTokenInterceptor
import org.example.common.auth.StudentTokenService
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val jdbcClient: JdbcClient,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(StudentTokenInterceptor(jdbcClient, StudentTokenService.CITY))
            .addPathPatterns("/**")
            .excludePathPatterns("/error")
    }
}
