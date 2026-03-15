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
## Job Lifecycle

```text
QUEUED -> IN_PROGRESS -> FINISHED
                     -> FAILED

Suggested meaning of states:

QUEUED — request accepted, executor Pod is being scheduled or started

IN_PROGRESS — Pod is running and the script is executing

FINISHED — execution completed successfully

FAILED — pod creation or script execution failed

REST API
Submit a job

POST /api/jobs/start

Request body:

{
  "id": "web-task-101",
  "script": "echo Hello && sleep 5 && echo Done",
  "cpu": "1"
}

Example success response:

✅ Job web-task-101 successfully submitted with CPU=1.
List jobs

GET /api/jobs/list

Example response:

[
  {
    "id": "web-task-101",
    "name": "worker-pod-web-task-101",
    "status": "FINISHED"
  }
]
Get job logs

GET /api/jobs/{id}/logs

Example response:

Hello
Done
Delete a job

DELETE /api/jobs/{id}

Example response:

✅ Job web-task-101 has been deleted from the cluster.
Validation Rules
Job ID

Allowed format:

lowercase letters

numbers

hyphens

Example valid values:

web-task-101

job-1

Example invalid values:

webTask101

Job_1

task 1

CPU

Allowed values:

numeric only

greater than 0

less than or equal to 8

Examples:

0.5

1

2

Technology Stack

Language: Kotlin

Framework: Spring Boot 3

Executor model: ephemeral Kubernetes Pods

Kubernetes client: Fabric8 Kubernetes Client

Frontend: HTML + JavaScript + Bootstrap 5

Build tool: Gradle

Tested Environment

This project was tested with:

Windows 11 host machine

IntelliJ IDEA

Ubuntu virtual machine

MicroK8s

remote cluster access through a valid kubeconfig

Local Development
Prerequisites

Java 17+

Gradle or Gradle Wrapper

access to a Kubernetes cluster

valid kubeconfig

tested with MicroK8s on Ubuntu VM

Run locally

Windows PowerShell:

$env:KUBECONFIG="C:\Users\YOUR_USER\.kube\config"
.\gradlew.bat bootRun

Open:

http://localhost:8080
Example Demo Job

Job ID: web-task-200

CPU: 1

Script:

echo Hello && sleep 3 && echo Done
Current Capabilities

The current MVP supports:

creating a temporary executor Pod

executing a user-provided shell script

assigning CPU resources

status tracking

log retrieval

manual executor deletion

input validation on both frontend and backend

unit and controller tests

Limitations

Current limitations of this MVP:

requires an existing Kubernetes cluster

does not run in a fully standalone mode

does not keep persistent job history

does not reuse idle executors

does not implement a real autoscaler

uses popup-based log display instead of live log streaming

Possible Future Improvements

Potential extensions include:

maintain a small warm pool of pre-created executors

reuse idle pods for faster startup

add autoscaling heuristics based on queue length

collect metrics for provisioning latency and executor utilization

improve log streaming and overall UX

persist job history and execution metadata

Notes

This project uses Kubernetes Pods instead of full virtual machines.

This is a valid remote executor model for a lightweight MVP.

The general autoscaling idea remains similar to VM-based provisioning systems, but Pods offer faster startup and simpler demo deployment.

Documentation

Additional technical documentation can be placed in:

docs/PROJECT_DOCUMENTATION.md
Author

Developed as a test task / internship project focused on remote execution and Kubernetes-based executor orchestration.
