import org.springframework.boot.gradle.plugin.SpringBootPlugin

// Библиотека общего кода для сервисов; своего приложения и точки входа нет
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-jdbc")
    implementation("org.slf4j:slf4j-api")
}

kotlin {
    jvmToolchain(22)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
