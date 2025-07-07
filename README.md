# Capition
1. [Introduction](#introduction)
2. [Techinal overview](#technical-overview)
    1. [Microservice architecture](#micorservices-archiecture)
    2. [DDD](#ddd)
    3. [Spring Boot](#spring-boot)
    4. [Spring Cloud Data Flow](#spring-cloud-data-flow)
3. [Steps](#steps)  
    1. [Activity diagram](#1-activity-diagagram)
    2. [Persistence level](#2-persistence-level)
    3. [Class Diagram](#3-class-diagram)
# Introduction

This project demonstrates the use of **Spring Cloud Data Flow** with **Spring Boot** to simulate the data flow of an online order system.  
It models the lifecycle of an order — from placement to delivery — through a series of microservices connected in a data pipeline.

The application is designed using a **microservice architecture** and follows **Domain-Driven Design (DDD)** principles, making it both modular and scalable.  
It serves as a practical example of building distributed systems using modern cloud-native tools.

# Technical Overview

## Micorservices archiecture
# Microservices Architecture – Technical Overview (Martin Fowler)

**Microservices Architecture (MSA)** is an architectural style that structures an application as a collection of loosely coupled, independently deployable services. Each service is responsible for a distinct business capability and communicates with others using lightweight protocols, typically HTTP or messaging systems.

---

### Key Characteristics (Fowler & Lewis, 2014):

- **Componentization via Services**  
  Each part of the system is a separate service, which can be deployed independently and developed in isolation.

- **Business-Oriented Organization**  
  Teams are organized around business capabilities rather than technical layers (e.g., “Order Management” instead of “Database Layer”).

- **Independent Deployment**  
  Each service can be deployed without affecting the others, enabling continuous delivery and rapid iterations.

- **Decentralized Data Management**  
  Each service manages its own database or data store, promoting autonomy and avoiding shared database bottlenecks.

- **Built for Failure**  
  Microservices embrace the reality of failure. They use patterns like circuit breakers and retries to build fault-tolerant systems.

- **Infrastructure Automation**  
  Continuous integration, automated testing, and deployment pipelines are essential to manage the complexity of microservices.

---

### Common Trade-offs:

While microservices offer agility and scalability, they introduce challenges in distributed systems, such as:

- Increased operational complexity
- Need for service discovery and orchestration
- Eventual consistency (vs. strong consistency)

---

## DDD
Domain-Driven Design (DDD) is an approach for developing complex applications by continuously aligning implementation with an evolving model of core business concepts. It emphasizes collaboration between domain experts and developers to build a shared understanding.

## Key Principles

- **Focus on Core Domain and Domain Logic:** Design centers on business workflows decomposed into multiple interconnected models to manage complexity.
- **Bounded Context:** Large systems are divided into clearly defined contexts, each with its own model and team, preventing confusion and reducing errors.
- **Ubiquitous Language:** A common language shared between developers and domain experts ensures clear communication and consistent use of domain concepts.
- **Context Mapping:** Mapping relationships and data flow between bounded contexts maintains system integrity and clarifies dependencies.

## Challenges Addressed

DDD manages domain complexity to keep software understandable, modifiable, and extensible, independent of infrastructure quality.

## DDD and Microservices

- Each microservice should encapsulate a single bounded context.
- Communication between services must use the ubiquitous language.
- System-wide context mapping helps avoid tight coupling and supports maintainability.

## Spring Boot
Spring Boot is a framework that simplifies the development of production-ready Spring applications. It provides:

- **Auto-configuration:** Automatically configures Spring applications based on dependencies.
- **Standalone applications:** Allows creating executable JARs with embedded servers.
- **Opinionated defaults:** Reduces boilerplate configuration and speeds up development.
- **Production-ready features:** Includes metrics, health checks, and externalized configuration.

Spring Boot is widely used for building microservices and RESTful APIs due to its simplicity and robustness.

## Spring Cloud Data Flow
Spring Cloud Data Flow is a toolkit for building data integration and real-time data processing pipelines using microservices. It enables:

- **Orchestration of data pipelines:** Supports batch and streaming data workflows.
- **Composable microservices:** Allows assembling pre-built or custom services into complex workflows.
- **Multiple runtime options:** Supports local, Kubernetes, and cloud platforms.
- **Monitoring and management:** Provides dashboards and REST APIs for pipeline control.

Spring Cloud Data Flow is ideal for event-driven architectures and scalable data processing.

# Steps
## 1. Activity diagagram
Respecting DDD priciples we start with the activity diagram that repesents the bussiness flow.

![Activity Diagram](/Images/ActivityDiagram.png)

## 2. Define microservices
In this step, based on the activity diagram we define de the microservices needed for this application.  

### 1. Client microservice
This microservice communicates with the user interface. It receives data from the UI and creates a command. It acts as the **source** in the command data flow pipeline.

#### Responsibilities

- **`receiveCommand`**  
  This method receives data from the graphical user interface via a **RabbitMQ queue**. The user interface writes the necessary data into the queue, and the microservice consumes it in real time.

- **`comandaProdus`**  
  This method sends the received and processed command to the next microservice in the pipeline using **Kafka**, acting as a producer.

#### Technologies Used

- RabbitMQ (for receiving messages from the UI)
- Spring Cloud Stream
- Kotlin / Spring Boot

#### Architecture Role

- Acts as the **entry point** for the auction pipeline.
- Bridges communication between the UI and the distributed system.

#### SOLID Principles Applied

- **Single Responsibility Principle**: Each method handles exactly one concern (input vs. output).
- **Dependency Inversion Principle**: The microservice depends on **messaging systems** (RabbitMQ, Kafka) rather than tightly coupled modules.



## 3. Persistence level
Since we also need a persistence layer, a DBMS that supports SQL has been chosen. The properties and the relationships between these entities can be modeled using an entity-relationship diagram.

An entity represents a mutable object: it can change its properties without changing its identity. For example, a Product is an entity: the product is unique and will not change its identity (what uniquely distinguishes it) once it has been established. However, the price, description, and other specific attributes can be changed as often as needed.  

The entities that emerge from an initial analysis of the modeled business flow would be the following:
- user_data
- Command
- Facturare
- Depozit 
- Product

![ER Diagram](/Images/ER.png)

## 4. Class Diagram
According to the domain services identified above, the class diagram is designed to highlight the implementation methods for the business requirements as well.

![Class Diagram](/Images/ClassDiagram.drawio.png)

# Bibliography

- Martin Fowler & James Lewis. ["Microservices"](https://martinfowler.com/articles/microservices.html), March 2014.

- [DDD Community](https://dddcommunity.org/)

- [Spring Boot Documentation](https://spring.io/projects/spring-boot) 

- [Spring Cloud Data Flow Documentation](https://spring.io/projects/spring-cloud-dataflow)