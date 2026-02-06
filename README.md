# microservices-fitness-tracker
Microservices based application to plan, track and analyze workouts.

## Overview

POC to experiment with microservice architecture design, AI tools, java spring boot and various databases. Goal is to implement a scalable, cloud native solution.

## Architecture Diagram

## Microservices

| **Service** | **Description** | **Tech**  |
|:------- |:----------- |:----- |
| API Gateway | Entrypoint for requests and user authentication | Spring Cloud Gateway |
| Discovery Service | Service discovery and registration | Kubernetes |
| Authentication Service | User authentication | Spring Security, Redis |
| User Service | Manages users | Spring Boot, PostgreSQL |
| Workout Service | Manages user workout routines | Spring Boot, PostgreSQL |
| Analytics Service | AI to summarize completed workout | python |
| Recommendation Service | AI to recommend next workout | python |
| Notification Service | Stores and alerts users to AI messages | Spring Boot, MongoDB, SSE |
| Frontend | Webapp for Users | React |
| SNS & SQS | Message broker | Localstack |


## Project Plan

The goal of this project is to refresh and experiment with different microservice concepts. Put theoretical architecture design into practice. Below is the project plan with learning goals.

Project plan table here: use ascii doc and embed?



## How to run locally

Install Localstack: https://github.com/localstack/localstack

1. Localstack setup

``` sh localstack-setup.sh ```

2. Start postgres
3. Start mongodb

## Future Functionality