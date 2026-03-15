/**
 * JobService provides high-level functionality to manage Kubernetes Pods within the cluster.
 *
 * This service is responsible for:
 * - Orchestrating the creation of worker Pods based on the Ubuntu image.
 * - Retrieving information about currently existing jobs.
 * - Fetching real-time execution logs from specific containers.
 * - Cleaning up and deleting completed or unnecessary Pods.
 *
 * It interacts with the Kubernetes API using the Fabric8 Kubernetes Client.
 */
package com.jetbrains.teamcity.teamcityexecutor.service

import com.jetbrains.teamcity.teamcityexecutor.model.JobSummaryResponse
import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.api.model.PodBuilder
import io.fabric8.kubernetes.client.ConfigBuilder
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.springframework.stereotype.Service

@Service
class JobService {

    // Use a custom client configuration for local/demo environments.
    // withTrustCerts(true) helps when the remote cluster uses a self-signed
    // certificate or a certificate that does not match the IP address.
    private val config = ConfigBuilder()
        .withTrustCerts(true)
        .build()

    // The client instance is initialized using the custom configuration.
    private val client = KubernetesClientBuilder()
        .withConfig(config)
        .build()

    private val namespace = "default"

    /**
     * Normalizes a user-provided job ID.
     * This prevents accidental duplication like:
     * worker-pod-worker-pod-web-task-12
     */
    internal fun normalizeJobId(jobId: String): String {
        return jobId.removePrefix("worker-pod-").trim()
    }

    /**
     * Builds the Kubernetes Pod name from the logical job ID.
     */
    private fun buildPodName(jobId: String): String {
        return "worker-pod-${normalizeJobId(jobId)}"
    }

    /**
     * Maps raw Kubernetes pod phases to simplified business statuses
     * used by the application UI.
     */
    internal fun mapPodStatus(phase: String?): String {
        return when (phase) {
            "Pending" -> "QUEUED"
            "Running" -> "IN_PROGRESS"
            "Succeeded", "Completed" -> "FINISHED"
            "Failed" -> "FAILED"
            else -> "UNKNOWN"
        }
    }

    /**
     * Validates the incoming job identifier.
     * Only lowercase letters, digits, and hyphens are allowed.
     */
    internal fun validateJobId(jobId: String) {
        val pattern = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$")
        require(pattern.matches(jobId)) {
            "Job ID must contain only lowercase letters, numbers, and hyphens."
        }
    }

    /**
     * Validates the requested CPU value for the demo environment.
     */
    internal fun validateCpu(cpu: String) {
        val cpuValue = cpu.toDoubleOrNull()
            ?: throw IllegalArgumentException("CPU value must be a valid number.")

        require(cpuValue > 0) {
            "CPU value must be greater than 0."
        }

        require(cpuValue <= 8) {
            "CPU value is too high for this demo. Please use a value between 0.1 and 8."
        }
    }

    /**
     * Creates and starts a new worker Pod in the Kubernetes cluster.
     *
     * The pod executes a user-provided shell script and applies the requested
     * CPU resources for the container.
     *
     * @param jobId A unique identifier used to name and track the job.
     * @param script A shell script that will be executed inside the container.
     * @param cpu The requested CPU value for the container, used for both
     * requests and limits.
     */
    fun createAndRunJob(jobId: String, script: String, cpu: String): String {
        val normalizedJobId = normalizeJobId(jobId)
        validateJobId(normalizedJobId)
        validateCpu(cpu)

        val podName = buildPodName(normalizedJobId)

        val pod: Pod = PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName("ubuntu-job")
            .withImage("ubuntu:22.04")
            // Execute the user-defined shell script inside the container.
            .withCommand("sh", "-c", script)
            // Apply CPU requests and limits based on the user input.
            .withNewResources()
            .addToRequests("cpu", io.fabric8.kubernetes.api.model.Quantity(cpu))
            .addToLimits("cpu", io.fabric8.kubernetes.api.model.Quantity(cpu))
            .endResources()
            .endContainer()
            // Ensures the pod does not restart automatically after completion.
            .withRestartPolicy("Never")
            .endSpec()
            .build()

        client.pods().inNamespace(namespace).resource(pod).create()

        return "✅ Job $normalizedJobId successfully submitted with CPU=$cpu."
    }

    /**
     * Retrieves a list of all worker pods created by this service.
     */
    fun getAllJobs(): List<JobSummaryResponse> {
        return client.pods().inNamespace(namespace).list().items
            .filter { it.metadata.name.startsWith("worker-pod-") }
            .map {
                JobSummaryResponse(
                    id = it.metadata.name.removePrefix("worker-pod-"),
                    name = it.metadata.name,
                    status = mapPodStatus(it.status?.phase)
                )
            }
    }

    /**
     * Fetches the logs from the worker Pod to see the output of the job.
     */
    fun getJobLogs(jobId: String): String {
        val normalizedJobId = normalizeJobId(jobId)
        val podName = buildPodName(normalizedJobId)

        val pod = client.pods().inNamespace(namespace).withName(podName).get()
            ?: return "❌ Pod $podName was not found."

        return try {
            client.pods().inNamespace(namespace).withName(podName).getLog()
        } catch (e: Exception) {
            "❌ Error fetching logs: ${e.message}\nCurrent status: ${pod.status?.phase ?: "Unknown"}"
        }
    }

    /**
     * Deletes the specified job/pod from the cluster.
     */
    fun deleteJob(jobId: String): String {
        val normalizedJobId = normalizeJobId(jobId)
        val podName = buildPodName(normalizedJobId)

        val result = client.pods().inNamespace(namespace).withName(podName).delete()

        return if (result.isNotEmpty()) {
            "✅ Job $normalizedJobId has been deleted from the cluster."
        } else {
            "❌ Pod $podName could not be deleted or does not exist."
        }
    }
}