# **TeamCity Cloud: Remote Shell Executor**

A lightweight, scalable backend service designed to orchestrate and execute shell commands on remote virtual environments. This project simulates the core autoscaling behavior of CI/CD agents, dynamically provisioning executors based on user demand.

## **🚀 Architecture Overview**

To ensure scalability, isolation, and efficient resource allocation, this service leverages **Kubernetes Pods** as remote executors.

The application is built with **Kotlin** and **Spring Boot**, utilizing the Fabric8 Kubernetes Client to programmatically interact with the Kubernetes API.

### **System Workflow (Autoscaling Sequence)**

*(Source: Author's own diagram / Собственный рисунок)*

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
    Note over User, API: 1. Command Submission & Queuing  
    User->>API: POST /api/jobs {script, cpu}  
    activate API  
    API->>Service: submitJob(script, cpu)  
    activate Service  
    Service->>K8s: Request Pod creation  
    activate K8s  
    K8s-->>Service: Pod created (Pending)  
    deactivate K8s  
    Service-->>API: Job ID generated  
    deactivate Service  
    API-->>User: 202 Accepted {job_id, status: "QUEUED"}  
    deactivate API  
    end

    rect rgb(250, 250, 250)  
    Note over K8s, Pod: 2. Background Execution  
    K8s->>Pod: Container Startup  
    activate Pod  
    Note right of Pod: Pod transitions to Running.<br/>Isolated executor ready.  
    Pod->>Pod: Execute shell script  
    Pod-->>K8s: Script finished  
    deactivate Pod  
    Note right of K8s: Pod is terminated.<br/>Resources freed.  
    end

    rect rgb(250, 250, 250)  
    Note over User, K8s: 3. Status Polling  
    User->>API: GET /api/jobs/{id}  
    activate API  
    API->>Service: getJobStatus(id)  
    activate Service  
    Service->>K8s: Request Pod state  
    activate K8s  
    K8s-->>Service: Return Pod state  
    deactivate K8s  
    Service->>Service: Map K8s phase to Status  
    Service-->>API: Status: FINISHED  
    deactivate Service  
    API-->>User: 200 OK {status: "FINISHED"}  
    deactivate API  
    end
```

## **🛠 Tech Stack**

* **Language:** Kotlin  
* **Framework:** Spring Boot 3  
* **Orchestration:** Kubernetes (Minikube / Docker Desktop for local testing)  
* **K8s Integration:** Fabric8 Kubernetes Client

## **📦 API Documentation**

### **1\. Execute a Command**

Starts a new remote executor and runs the provided script.

**POST** /api/jobs

{  
  "script": "echo 'Hello from TeamCity Cloud\!' && sleep 5",  
  "cpuRequest": "500m"  
}

**Response (202 Accepted):**

{  
  "jobId": "123e4567-e89b-12d3-a456-426614174000",  
  "status": "QUEUED"  
}

### **2\. Check Execution Status**

Retrieves the current state of the execution.

**GET** /api/jobs/{jobId}

**Response (200 OK):**

{  
  "jobId": "123e4567-e89b-12d3-a456-426614174000",  
  "status": "IN\_PROGRESS"   
}

*(Status enum: QUEUED, IN\_PROGRESS, FINISHED)*

## **⚙️ Local Setup & Running**

### **Prerequisites**

* **Java 17+** installed.  
* **Minikube** or **Docker Desktop** with Kubernetes enabled.  
* **kubectl** configured and connected to your local cluster.

### **Steps to Run**

1. **Start your local Kubernetes cluster:**  
   minikube start

2. **Clone the repository:**  
   git clone \[https://github.com/YourUsername/teamcity-remote-executor.git\](https://github.com/YourUsername/teamcity-remote-executor.git)  
   cd teamcity-remote-executor

3. **Build and run the Spring Boot application:**  
   ./gradlew bootRun

   *(Note: The application will automatically use your local \~/.kube/config to authenticate with the cluster).*
