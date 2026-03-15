Project Documentation: TeamCity Remote Shell Executor
1. Architectural Vision & Tech Stack Rationale
The core objective of this project is to simulate the behavior of autoscaling CI/CD build agents, similar to TeamCity Cloud. To achieve this efficiently within an MVP scope, the architecture relies on ephemeral containers rather than full virtual machines.

Here is the rationale behind the chosen technology stack:

Kotlin & Spring Boot 3: Kotlin was chosen for its conciseness, expressiveness, and built-in null safety, which significantly reduces boilerplate code compared to standard Java. Spring Boot 3 serves as the foundational framework, providing a robust and production-ready environment for building RESTful APIs rapidly.

Hosted Kubernetes Pods vs. Virtual Machines (EC2): While the task allowed any cloud platform, Kubernetes Pods were selected as the remote executors. Unlike EC2 instances which take minutes to provision, Pods start in a matter of seconds. They provide excellent process isolation, are highly resource-efficient, and their lifecycle is natively managed by the Kubernetes Control Plane. This makes them an ideal fit for temporary, isolated workload execution.

Fabric8 Kubernetes Client: Interacting with the Kubernetes API requires a reliable client. Fabric8 was preferred over the official official Kubernetes Java client due to its highly intuitive Fluent Builder API (e.g., PodBuilder()) and seamless integration with JVM-based applications.

Vanilla JS + Bootstrap 5 (Frontend): To demonstrate the backend API without over-engineering the client side, a lightweight Vanilla JavaScript frontend was implemented. It uses basic fetch API for asynchronous polling and Bootstrap 5 for clean, responsive UI components, keeping the focus entirely on the backend orchestration.

2. Project Structure & Core Components
The application follows a clean, layered architecture separating HTTP routing from business logic and infrastructure interactions.

JobController.kt (Presentation Layer): Serves as the REST API entry point. It defines the contract for external clients, handling HTTP GET, POST, and DELETE requests for job creation, status retrieval, log fetching, and pod termination.

JobService.kt (Business & Infrastructure Layer): The core engine of the application. It acts as a bridge between the Spring Boot application and the Kubernetes cluster. It is responsible for translating user requests into Kubernetes resource definitions (Pods), deploying them via the Fabric8 client, mapping complex K8s phases (like Pending or Succeeded) into simplified business statuses, and retrieving container logs.

JobModels.kt (Data Layer): Contains Data Transfer Objects (DTOs) such as JobRequest and JobResponse. It also defines the JobStatus enum (QUEUED, RUNNING, COMPLETED, FAILED) to ensure a strict contract between the backend and frontend.

index.html: The client-side dashboard that interacts with the JobController. It implements a 5-second interval polling mechanism to dynamically update the UI with real-time job statuses and provides action buttons to run tasks or fetch logs.

Вот продолжение твоей документации, оформленное в строгом Markdown. Просто скопируй этот блок и вставь его сразу после второго раздела в твой файл PROJECT_DOCUMENTATION.md.

3. Testing Strategy
To ensure reliability and maintainability, the application includes a suite of automated tests focusing on different layers of the system.

Unit Testing (TeamcityExecutorApplicationTests & Service layer): The business logic is isolated and tested independently. By mocking the Fabric8 Kubernetes Client, we can simulate various Kubernetes cluster states (e.g., Pod creation success, retrieval of logs, and mapping K8s phases like Pending or Succeeded to application-specific statuses) without needing a live cluster during the CI/CD build phase.

Integration Testing (JobControllerTest): The REST API layer is tested using Spring's MockMvc. These tests validate the HTTP contracts, ensuring that endpoints correctly accept valid payloads, reject malformed requests (e.g., invalid job IDs or missing CPU parameters), and return the expected JSON structures and HTTP status codes (200 OK, 400 Bad Request, etc.).

4. Constraints & Known Limitations
Building an MVP requires deliberate trade-offs. The following constraints were accepted for this iteration:

Stateless Backend (No Database): Currently, the Spring Boot application is entirely stateless. It relies on the Kubernetes API as the single source of truth. If a Pod is deleted (either manually via the UI or by K8s garbage collection), all metadata and execution logs for that job are permanently lost.

Security & RCE By Design: The service is built to execute arbitrary shell scripts. In the current local implementation, Pods run with default permissions. In a production environment, this is a severe security risk. There are currently no strict Kubernetes SecurityContexts (e.g., runAsNonRoot: true, allowPrivilegeEscalation: false) applied to the worker pods.

Local Certificate Bypassing: To facilitate smooth local development with a remote MicroK8s virtual machine, the Fabric8 client is configured with .withTrustCerts(true). This bypasses strict TLS certificate validation. This is strictly a development hack and is an anti-pattern for production.

Lack of True Queuing: The UI displays a QUEUED status, but this simply maps to the Kubernetes Pending phase (waiting for the scheduler). If a user submits 1,000 jobs simultaneously, the API will immediately attempt to create 1,000 Pods, potentially overwhelming the cluster's control plane. There is no external message broker handling backpressure.

5. Future Improvements & Scalability
If this MVP were to evolve into a production-grade Enterprise solution, the following architectural upgrades would be prioritized:

Message Broker Integration (RabbitMQ / Apache Kafka): To handle high throughput and burst traffic, incoming execution requests should be published to a message queue. Worker nodes or the Spring Boot service would asynchronously consume these messages and provision K8s Pods only when cluster resources are actually available, preventing API server exhaustion.

Persistent Storage (PostgreSQL): Introducing a relational database would decouple job history from the Kubernetes cluster lifecycle. Logs, execution times, and exit codes would be saved to the database upon job completion, allowing for Pod cleanup without losing audit trails.

WebSockets / Server-Sent Events (SSE): The current frontend uses HTTP polling every 5 seconds to fetch status updates. Replacing this with WebSockets or SSE would drastically reduce network overhead and allow for real-time log streaming directly from the container to the user's browser.

Graceful Degradation & Timeouts: Users could submit infinite loops (e.g., sleep 99999). Utilizing Kubernetes activeDeadlineSeconds in the Pod spec would ensure that hanging or malicious scripts are forcefully terminated after a predefined timeout.

Hardened Network Policies: Worker pods should be isolated using Kubernetes Network Policies, blocking outbound internet access (unless explicitly required) and preventing horizontal traversal within the cluster.

6. Edge Cases Handled
Despite being an MVP, several edge cases are proactively managed to ensure system stability:

Job ID Normalization: To prevent K8s naming validation errors and UI glitches (such as duplicated prefixes like worker-pod-worker-pod-web-task-12), the backend sanitizes and normalizes incoming Job IDs before interacting with the Fabric8 client.

Resource Limitation: CPU requests are strictly capped to ensure a single runaway job cannot monopolize the entire node's resources.

Graceful Error Recovery: Interactions with the Kubernetes API (such as fetching logs from a Pod that might still be initializing or has crashed) are wrapped in try-catch blocks. Instead of crashing the Spring Boot application with a 500 Internal Server Error, the service gracefully catches Fabric8 exceptions and returns a readable error message to the client.
