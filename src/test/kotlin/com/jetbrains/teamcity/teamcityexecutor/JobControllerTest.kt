/**
 * JobControllerTest
 *
 * This class contains controller-level tests for the JobController REST API.
 * The Kubernetes-dependent JobService is mocked so that the tests validate
 * only the web layer behavior, request handling, and HTTP status codes.
 */
package com.jetbrains.teamcity.teamcityexecutor

import com.jetbrains.teamcity.teamcityexecutor.service.JobService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var jobService: JobService

    /**
     * Verifies that a valid job start request is accepted by the API.
     * The service is mocked, so no real Kubernetes interaction happens.
     */
    @Test
    fun `should accept valid job start request`() {
        val requestBody = """
            {
              "id": "web-task-500",
              "script": "echo Hello && echo Done",
              "cpu": "1"
            }
        """.trimIndent()

        `when`(jobService.createAndRunJob("web-task-500", "echo Hello && echo Done", "1"))
            .thenReturn("✅ Job web-task-500 successfully submitted with CPU=1.")

        mockMvc.perform(
            post("/api/jobs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(content().string("✅ Job web-task-500 successfully submitted with CPU=1."))
    }

    /**
     * Verifies that the API rejects an invalid CPU value.
     * Validation is handled before any real Kubernetes action is needed.
     */
    @Test
    fun `should reject invalid cpu value`() {
        val requestBody = """
            {
              "id": "web-task-501",
              "script": "echo Hello",
              "cpu": "1000"
            }
        """.trimIndent()

        `when`(jobService.createAndRunJob("web-task-501", "echo Hello", "1000"))
            .thenThrow(IllegalArgumentException("CPU value is too high for this demo. Please use a value between 0.1 and 8."))

        mockMvc.perform(
            post("/api/jobs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    /**
     * Verifies that the API rejects an invalid job identifier.
     */
    @Test
    fun `should reject invalid job id`() {
        val requestBody = """
            {
              "id": "webTask501",
              "script": "echo Hello",
              "cpu": "1"
            }
        """.trimIndent()

        `when`(jobService.createAndRunJob("webTask501", "echo Hello", "1"))
            .thenThrow(IllegalArgumentException("Job ID must contain only lowercase letters, numbers, and hyphens."))

        mockMvc.perform(
            post("/api/jobs/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
    }

    /**
     * Verifies that the logs endpoint returns a response body.
     */
    @Test
    fun `should return logs for job`() {
        `when`(jobService.getJobLogs("web-task-500"))
            .thenReturn("Hello\nDone")

        mockMvc.perform(get("/api/jobs/web-task-500/logs"))
            .andExpect(status().isOk)
            .andExpect(content().string("Hello\nDone"))
    }

    /**
     * Verifies that the delete endpoint returns a success message.
     */
    @Test
    fun `should delete job`() {
        `when`(jobService.deleteJob("web-task-500"))
            .thenReturn("✅ Job web-task-500 has been deleted from the cluster.")

        mockMvc.perform(delete("/api/jobs/web-task-500"))
            .andExpect(status().isOk)
            .andExpect(content().string("✅ Job web-task-500 has been deleted from the cluster."))
    }
}