/**
 * JobController handles incoming HTTP requests and routes them to the JobService.
 * This class serves as the main entry point for the REST API of the application.
 *
 * It provides administrative control over Kubernetes worker pods, allowing users to:
 * - List all job-related pods in the cluster.
 * - Manually trigger a new task execution via URL.
 * - Retrieve execution output (logs) for audit or debugging.
 * - Clean up and terminate jobs once they are no longer required.
 */
package com.jetbrains.teamcity.teamcityexecutor.controller

import com.jetbrains.teamcity.teamcityexecutor.service.JobService
import org.springframework.web.bind.annotation.* // Wildcard import for all web annotations
import com.jetbrains.teamcity.teamcityexecutor.model.JobRequest

@RestController
@RequestMapping("/api/jobs")
class JobController(private val jobService: JobService) {

    /**
     * Retrieves a summary list of all current jobs from the cluster.
     */
    @GetMapping("/list")
    fun listJobs() = jobService.getAllJobs()

    /**
     * Triggers the creation of a new worker pod based on the request body.
     */
    @PostMapping("/start")
    fun startJob(@RequestBody request: JobRequest) =
        jobService.createAndRunJob(request.id, request.script, request.cpu)

    /**
     * Fetches the command output/logs from a specific job pod.
     */
    @GetMapping("/{id}/logs")
    fun getLogs(@PathVariable id: String) = jobService.getJobLogs(id)

    /**
     * Removes the job and its associated pod from the Kubernetes environment.
     */
    @DeleteMapping("/{id}")
    fun deleteJob(@PathVariable id: String) = jobService.deleteJob(id)
}