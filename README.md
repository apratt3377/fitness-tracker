# microservices-fitness-tracker
Microservices based application to plan, track and analyze workouts.

## Overview

POC to experiment with microservice architecture design, AI tools, java spring boot and various databases. The goal is to implement a scalable, cloud native solution for users to track their strength training sessions.

## Architecture Diagram

![Architecture Diagram](assets/whitebox-diagram.svg)

## Architecture Documentation

To design and plan the software architecture for this project, I used arc42 template. One of the goals of this project is to learn about architecture design, specifically microservices design, so [Fitness Tracker Design Document](https://apratt3377.github.io/fitness-tracker-architecture-diagram/) contains all solution strategies and design decisions. Currently, it contains the pre-implementation architecture design for the entire system in the above diagram. As the implementation of this project progresses, any updates or new design decisions will be updated in this document.

## Microservices

| **Service** | **Description** | **Tech**  |
|:------- |:----------- |:----- |
| API Gateway | Entrypoint for requests and user authentication | Spring Cloud Gateway |
| Authentication Service | User authentication | Spring Security, Redis |
| User Service | Manages users | Spring Boot, PostgreSQL |
| Workout Service | Manages user workout routines | Spring Boot, PostgreSQL |
| Analytics Service | AI to summarize completed workout | python |
| Notification Service | Stores and alerts users to AI messages | Spring Boot, MongoDB, SSE |
| Frontend | Webapp for Users | React |
| SNS & SQS | Message broker | Localstack |

## Setup

### Prerequisites
1. docker
2. kind

#### Database & Localstack

``` 
    cd setup
    docker-compose up -d
```

#### Kind Cluster

Because we run the database containers and localstack externally from the kubernetes cluster, there's some additional networking steps to make sure they are accessible from inside kubernetes pods where the microservices will be running. A setup script was created for this.

``` ./setup-cluster.sh ```

A test script is provided to check connectivity between the kind cluster and docker.

``` ./setup-cluster-test.sh ```


## Project Plan

1. The first phase of this project is to develop an initial design document: [Fitness Tracker Design Document](https://apratt3377.github.io/fitness-tracker-architecture-diagram/). This has been completed.

2. The second phase is to set up the local infrastructure needed for this project. This includes localstack for AWS services, PostgreSQL, MongoDB, and Redis servers and a local kubernetes cluster. This has been completed.

3. The 3rd phase is to implement the User Service with assistance from Codeium. This phase will also involve deploying this service to the Kubernetes cluster and making sure it can connect with its Postgres DB.

The remainder of the Project plan and status can be found here: [Project Plan](docs/project-plan.adoc)