/**
 * TeamcityExecutorApplicationTests
 *
 * This is the foundational integration test for the Spring Boot application.
 * Its primary responsibility is to verify that the Spring Application Context
 * loads successfully without any fatal errors.
 *
 * If there are missing dependencies, conflicting configurations, or broken beans
 * (like JobService or JobController), this test will fail immediately. 
 * It acts as the first line of defense before deploying the application.
 */
package com.jetbrains.teamcity.teamcityexecutor

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class TeamcityExecutorApplicationTests {

    @Test
    fun contextLoads() {
        // If the application context fails to load, this test will fail automatically.
        // No assertion is needed here.
    }
}