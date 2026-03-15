
/**
 * TeamcityExecutorApplication
 * * This is the main entry point for the Spring Boot application.
 * It initializes the application context, starts the embedded Tomcat server,
 * and enables the auto-configuration of necessary components (Services, Controllers).
 * * The application serves as a bridge between the local web interface and the
 * remote Kubernetes cluster on the Ubuntu virtual machine.
 */
package com.jetbrains.teamcity.teamcityexecutor

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TeamcityExecutorApplication

fun main(args: Array<String>) {
    runApplication<TeamcityExecutorApplication>(*args)
}
