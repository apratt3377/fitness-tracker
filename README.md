# Fitness Tracker: Event-Driven Microservices
A cloud-native, scalable Proof-of-Concept (POC) designed to track strength training sessions. Leverages an event-driven architecture and AI-powered insights.

## System Architecture
This project follows a white-box design using the **arc42 template**.
- **Architecture & Design Decisions:** [Detailed Design Document](https://apratt3377.github.io/fitness-tracker-architecture-diagram/) 
- **Core Pattern:** Event-Driven via SNS/SQS.

![Architecture Diagram](assets/whitebox-diagram.svg)


## Technology Stack & Service Map

| **Service** | **Responsibility** | **Stack**  | **Data Store** |
|:------- |:----------- |:----- |
| **API Gateway** | Request Routing & Auth | Spring Cloud Gateway | - |
| **Auth Service** | User Auth and Session Management | Spring Security | Redis |
| **User Service** | Account Management | Spring Boot | PostgreSQL |
| **Workout Management Service** | Workout Planning & History | Spring Boot | PostgreSQL |
| **Analytics Service** | AI Workout Summaries | python | LLM API |
| **Notification Service** | SSE Real-time updates | Spring Boot | MongoDB |
| **Frontend** | Webapp for Users | TBD | - |
| **SNS & SQS** | Message broker | Localstack (AWS) | - |

## AI-Assisted Development
This project serves as a sandbox for **AI-Augmented Engineering** I'm evaluating different AI coding tools in order to:
- **Accelerate Boilerplate:** Rapidly generate boilerplate code for Spring Boot (Java) and FastAPI (Python) services
- **Unit Testing:** Leveraging AI to generate high-coverage test suites with 70% coverage
- **Refactoring:** Garner feedback for code performance and readability

## Local Setup

### 1. Prerequisites
- Docker Desktop
- [Kind CLI](https://kind.sigs.k8s.io/)
- Kubectl

### 2. Spin up Persistent Infrastructure
```bash
cd setup
docker-compose up -d
```

### 3. Initialize Kubernetes & Network Bridge
Since the DBs and LocalStack live outside Kind, we use a custom bridge script to establish connectivity.


```bash
chmod +x ./setup-cluster.sh
 ./setup-cluster.sh 
 ```

 ### 4. Verify Connectivity

Ensure all infrastructure components are reachable from within the cluster

```bash
 ./setup-cluster-test.sh 
 ```

## Roadmap & Evolution

- [x] **Phase 1L** Initial [arc42 Design Strategy](https://apratt3377.github.io/fitness-tracker-architecture-diagram/)

- [x] **Phase 2:** Local Hybrid Infrastructure & Network Bridging

- [] **Phase 3:** Service Implementation (User & Workout Management Services)

- [] **Phase 4:** Analytics Layer & Event-Driven Integration (SNS/SQS, Analytics & Notification Service)

- [] **Phase 5:** Frontend built with Codeium

Detailed status tracking is available in the [Project Plan](docs/project-plan.md)