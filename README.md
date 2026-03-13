# TeamCity Cloud: Remote Shell Executor

A lightweight, scalable backend service designed to orchestrate and execute shell commands on remote virtual environments. This project simulates the core autoscaling behavior of CI/CD agents, dynamically provisioning executors based on user demand.

## 🚀 Architecture Overview
To ensure scalability, isolation, and efficient resource allocation, this service leverages **Kubernetes Pods** as remote executors. 

The application is built with **Kotlin** and **Spring Boot**, utilizing the `Fabric8 Kubernetes Client` to programmatically interact with the Kubernetes API.

### System Workflow (Autoscaling Sequence)

```mermaid
sequenceDiagram
    autonumber
    actor User as Client (Developer)
    
    box rgb(255, 253, 238) Spring Boot Service
        participant API as JobController
        participant Service as JobService
    end
    
    box rgb(238, 247, 255) Kubernetes Cluster
        participant K8s as K8s API (Fabric8)
        participant Pod as Pod (Remote Executor)
    end

    rect rgb(250, 250, 250)
    note right of User: 1. Command Submission & Queuing
    User->>API: POST /api/jobs {script: "echo Hello", cpu: "1"}
    activate API
    API->>Service: submitJob(script, cpu)
    activate Service
    Service->>K8s: Request Pod creation (Allocate CPU)
    activate K8s
    K8s-->>Service: Pod successfully created (Pending)
    deactivate K8s
    Service-->>API: Job ID generated (e.g., "123")
    deactivate Service
    API-->>User: HTTP 202 Accepted {job_id: "123", status: "QUEUED"}
    deactivate API
    end

    rect rgb(250, 250, 250)
    note right of User: 2. Background Execution (Autoscaling)
    K8s->>Pod: Container Startup (Allocate Resources)
    activate Pod
    Note right of Pod: Pod transitions to Running state.<br/>This acts as our isolated executor.
    Pod->>Pod: Execute shell script
    Pod-->>K8s: Script execution finished (Success / Failure)
    deactivate Pod
    Note right of K8s: Pod is terminated.<br/>Cluster resources are automatically freed.
    end

    rect rgb(250, 250, 250)
    note right of User: 3. Status Polling by Client
    User->>API: GET /api/jobs/123
    activate API
    API->>Service: getJobStatus("123")
    activate Service
    Service->>K8s: Request state of Pod associated with Job "123"
    activate K8s
    K8s-->>Service: Return Pod state (e.g., Succeeded)
    deactivate K8s
    Service->>Service: Map K8s phase to Application status
    Service-->>API: Status: FINISHED
    deactivate Service
    API-->>User: HTTP 200 OK {job_id: "123", status: "FINISHED"}
    deactivate API
    end
```

    🛠 Tech Stack
Language: Kotlin

Framework: Spring Boot 3

Orchestration: Kubernetes (Minikube / Docker Desktop)

K8s Integration: Fabric8 Kubernetes Client

📦 API Documentation
1. Execute a Command
Starts a new remote executor and runs the provided script.

POST /api/jobs
{
  "script": "echo 'Hello from TeamCity Cloud!' && sleep 5",
  "cpuRequest": "500m"
}
Response (202 Accepted):
{
  "jobId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "QUEUED"
}

2. Check Execution Status
Retrieves the current state of the execution.

GET /api/jobs/{jobId}

Response (200 OK):
{
  "jobId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "IN_PROGRESS" 
}
(Status enum: QUEUED, IN_PROGRESS, FINISHED)

⚙️ Local Setup & Running
Prerequisites
Java 17+ installed.

Minikube or Docker Desktop with Kubernetes enabled.

kubectl configured and connected to your local cluster.

Steps to Run
Start your local Kubernetes cluster:
