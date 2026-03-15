plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}
group = "com.jetbrains.teamcity"
version = "0.0.1-SNAPSHOT"
description = "teamcity-executor"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Standard library for creating REST Controllers (Step 1 & 2 on your diagram)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Allows Kotlin to work smoothly with JSON data
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Reflection support for Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // CRITICAL: This is the "brain" for Kubernetes interaction.
    // It allows your code to talk to MicroK8s and create Pods.
    implementation("io.fabric8:kubernetes-client:6.10.0")

    // Tool for testing your application
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Generates interactive API documentation (Swagger UI).
   // Access it at http://localhost:9090/swagger-ui.html to test your endpoints without Postman.
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
}
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
