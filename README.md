# TeamCity Cloud: Remote Shell Executor

A lightweight backend service that executes shell commands on a **temporary remote executor implemented as a Kubernetes Pod** and tracks the execution lifecycle.

The project models the core idea behind autoscaling build agents in **TeamCity Cloud**: when a user submits a job, the service provisions an isolated executor, waits until it becomes ready, runs the script, and exposes the job status through a REST API.

---

## Overview

The service is implemented in **Kotlin** with **Spring Boot** and uses **Kubernetes Pods** as remote executors.

A user can:

- submit a shell script to be executed
- specify the required resources for the executor, for example CPU count
- check the execution status: `QUEUED`, `IN_PROGRESS`, `FINISHED`, or `FAILED`
- inspect execution logs
- terminate the executor pod manually

The service:

- creates a new remote executor Pod for each submitted job
- waits until the Pod is initialized and ready
- executes the shell script inside the container
- updates and exposes the current job status
- allows the Pod to be deleted after execution to free resources

---

## Architecture

This implementation uses a **Kubernetes Pod** as the remote executor.

Main components:

- **JobController** — REST API for submitting jobs, retrieving logs, listing jobs, and deleting executors
- **JobService** — application logic for validation, lifecycle management, status mapping, and Kubernetes interaction
- **Kubernetes API Client (Fabric8)** — integration layer used to create, inspect, and delete Pods
- **Remote Executor Pod** — temporary Pod where the shell script is executed

---

## System Workflow

```mermaid
sequenceDiagram
    autonumber
    actor User as Client (Developer)

    box "Spring Boot Service" #fffdee
        participant API as JobController
        participant Service as JobService
    end

    box "Kubernetes Cluster" #eef7ff
        participant K8s as K8s API (Fabric8)
        participant Pod as Pod (Remote Executor)
    end

    rect rgb(250, 250, 250)
        Note over User, API: 1. Job Submission
        User->>API: POST /api/jobs/start {id, script, cpu}
        activate API
        API->>Service: createAndRunJob(id, script, cpu)
        activate Service
        Service->>Service: validateJobId(id)
        Service->>Service: validateCpu(cpu)
        Service->>K8s: Request Pod creation
        activate K8s
        K8s-->>Service: Pod created (Pending)
        deactivate K8s
        Service-->>API: Job submitted
        deactivate Service
        API-->>User: 200 OK
        deactivate API
    end

    rect rgb(250, 250, 250)
        Note over K8s, Pod: 2. Background Execution
        K8s->>Pod: Container startup
        activate Pod
        Note right of Pod: Pod transitions to Running.<br/>Executor becomes available.
        Pod->>Pod: Execute shell script
        Pod-->>K8s: Script finished
        deactivate Pod
        Note right of K8s: Pod completes or fails.<br/>Resources can be freed later.
    end

    rect rgb(250, 250, 250)
        Note over User, K8s: 3. Status Polling
        User->>API: GET /api/jobs/list
        activate API
        API->>Service: getAllJobs()
        activate Service
        Service->>K8s: Request Pod states
        activate K8s
        K8s-->>Service: Return Pod phases
        deactivate K8s
        Service->>Service: Map K8s phase to UI status
        Service-->>API: QUEUED / IN_PROGRESS / FINISHED / FAILED
        deactivate Service
        API-->>User: 200 OK
        deactivate API
    end

    rect rgb(250, 250, 250)
        Note over User, K8s: 4. Log Retrieval
        User->>API: GET /api/jobs/{id}/logs
        activate API
        API->>Service: getJobLogs(id)
        activate Service
        Service->>K8s: Request container logs
        activate K8s
        K8s-->>Service: Return logs
        deactivate K8s
        Service-->>API: Logs
        deactivate Service
        API-->>User: 200 OK
        deactivate API
    end
