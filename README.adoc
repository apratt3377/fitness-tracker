# microservices-fitness-tracker
Microservices based application to plan, track and analyze workouts.

## Overview

POC to experiment with microservice architecture design, AI tools, java spring boot and various databases. The goal is to implement a scalable, cloud native solution for users to track their strength training sessions.

## Architecture Diagram

![Architecture Diagram](arc42/images/out/whitebox-diagram.svg)

## Architecture Documentation

To design and plan the software architecture for this project, I used arc42 template. One of the goals of this project is to learn about architecture design, specifically microservices design, so [Fitness Tracker Design Document](arc42/arc42-template.adoc) contains all solution strategies and design decisions. Currently, it contains the pre-implementation architecture design for the entire system in the above diagram. As the implementation of this project progresses, any updates or new design decisions will be updated in this document.

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
