package com.jetbrains.teamcity.teamcityexecutor

import com.jetbrains.teamcity.teamcityexecutor.service.JobService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for the core helper logic inside JobService.
 *
 * These tests focus on:
 * - job ID normalization
 * - Kubernetes phase to UI status mapping
 * - validation of job IDs
 * - validation of CPU values
 */
class JobServiceLogicTest {

    private val jobService = JobService()

    /**
     * Verifies that the helper removes the worker-pod prefix
     * and returns the logical job ID only.
     */
    @Test
    fun `normalizeJobId should remove worker-pod prefix`() {
        assertEquals("web-task-1", jobService.normalizeJobId("worker-pod-web-task-1"))
    }

    /**
     * Verifies that a clean job ID remains unchanged.
     */
    @Test
    fun `normalizeJobId should keep clean job id unchanged`() {
        assertEquals("web-task-1", jobService.normalizeJobId("web-task-1"))
    }

    /**
     * Verifies that Kubernetes Pending phase is mapped
     * to the QUEUED business status.
     */
    @Test
    fun `mapPodStatus should map Pending to QUEUED`() {
        assertEquals("QUEUED", jobService.mapPodStatus("Pending"))
    }

    /**
     * Verifies that Kubernetes Running phase is mapped
     * to the IN_PROGRESS business status.
     */
    @Test
    fun `mapPodStatus should map Running to IN_PROGRESS`() {
        assertEquals("IN_PROGRESS", jobService.mapPodStatus("Running"))
    }

    /**
     * Verifies that Kubernetes Succeeded phase is mapped
     * to the FINISHED business status.
     */
    @Test
    fun `mapPodStatus should map Succeeded to FINISHED`() {
        assertEquals("FINISHED", jobService.mapPodStatus("Succeeded"))
    }

    /**
     * Verifies that Kubernetes Failed phase is mapped
     * to the FAILED business status.
     */
    @Test
    fun `mapPodStatus should map Failed to FAILED`() {
        assertEquals("FAILED", jobService.mapPodStatus("Failed"))
    }

    /**
     * Verifies that uppercase characters are rejected
     * in job identifiers.
     */
    @Test
    fun `validateJobId should reject uppercase characters`() {
        assertThrows(IllegalArgumentException::class.java) {
            jobService.validateJobId("webTask1000")
        }
    }

    /**
     * Verifies that extremely large CPU values are rejected
     * by the demo validation rules.
     */
    @Test
    fun `validateCpu should reject values above demo limit`() {
        assertThrows(IllegalArgumentException::class.java) {
            jobService.validateCpu("1000")
        }
    }

    /**
     * Verifies that zero CPU is rejected.
     */
    @Test
    fun `validateCpu should reject zero`() {
        assertThrows(IllegalArgumentException::class.java) {
            jobService.validateCpu("0")
        }
    }

    /**
     * Verifies that a normal CPU value is accepted.
     */
    @Test
    fun `validateCpu should accept normal value`() {
        jobService.validateCpu("1")
    }
}