# User Service

The User Service is the core identity and account management component of the Fitness Tracker ecosystem. It handles user lifecycle management, credential storage and role-based access control.

## Prerequisites

Ensure you have the following installed:
* **Java 21+**
* **Maven 3.9+**
* **Docker Desktop**
* **kinD** (Kubernetes in Docker)
* **kubectl**
* **PostgreSQL**

## Local Development (No Kubernetes)

Run these commands to build and test the service on your host machine.

### 1. Database Configuration
The service is configured to connect to PostgreSQL. For local testing without a live Postgres instance, ensure the `src/test/resources/application.properties` is configured for **H2 In-Memory**.

### 2. Local Build
```bash
# Compile and package (skipping tests if DB is unavailable)
mvn clean package

# Run the application
java -jar -Dspring.profiles.active=local target/userservice-0.0.1-SNAPSHOT.jar
```

## 3. Kind Deployment

### 3.1 Build Image

```bash
# Build image with 'latest' tag
docker build -t user-service:latest .
```

### 3.2 Load image into Kind
```bash
# Sideload the image into the nodes
kind load docker-image user-service:latest --name fitness-cluster
```

### 3.3 Deployment
```bash

# Deployment
kubectl apply -f k8s/deployment.yaml

# Setup ingress
kubectl apply -f k8s/ingress.yaml

# Check pod status
kubectl get pods -l app=user-service

# Stream logs
kubectl logs -l app=user-service -f

# Port forwarding for local testing on port 8080
kubectl port-forward service/user-service 8080:80
```

## 4. API Documentation & Testing
Swagger UI at http://localhost:8080/swagger-ui.html

### 4.1 API Endpoints
| Method | Endpoint | Description | Access |
|:--- |:--- |:--- |:--- |
| **GET** | `/api/users/me` | Retrieve current user profile using `X-User-Id` header. | User Facing |
| **POST** | `/internal/users/authenticate` | Verifies credentials; returns ID and Roles for JWT generation. | Internal |
| **POST** | `/internal/users/create` | Creates a new user record (triggered during registration). | Internal |
| **GET** | `/api/admin/users` | Lists all registered users with passwords stripped out. | Admin |
| **DELETE** | `/api/admin/users/{id}` | Permanently removes (Soft-Delete) a user account by UUID. | Admin |