package com.jetbrains.teamcity.teamcityexecutor.model

/**
 * Data class representing the incoming request to start a job.
 */
data class JobRequest(
    val id: String,
    val script: String,
    val cpu: String = "1"
)

/**
 * Data class for the API response showing job identity and its current state.
 */
data class JobResponse(val id: String, val status: JobStatus)

data class JobSummaryResponse(
    val id: String,
    val name: String,
    val status: String
)

/**
 * Enum defining the lifecycle states of a Kubernetes Pod.
 */
enum class JobStatus {
    QUEUED, IN_PROGRESS, FINISHED, FAILED
}