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
  This responsability receives data from the graphical user interface via a **RabbitMQ queue**. The user interface writes the necessary data into the queue, and the microservice consumes it in real time.

- **`comandaProdus`**  
  This functionality sends the received and processed command to the next microservice in the pipeline using **Kafka**, acting as a producer.

#### Technologies Used

- RabbitMQ (for receiving messages from the UI)
- Spring Cloud Stream
- Kotlin / Spring Boot

#### Architecture Role

- Acts as the **entry point** for the command pipeline.
- Bridges communication between the UI and the distributed system.

#### SOLID Principles Applied

- **Single Responsibility Principle**: The microservice is responsible for a single business capability – handling and forwarding commands. It does not mix unrelated concerns like inventory management or delivery processing.
- **Interface Segregation Principle**: The microservice exposes only the operations necessary for handling commands.
- **Inversion of Control**: The microservice delegates external communication (e.g., saving to DB) using message brokers like RabbitMQ instead of direct calls.

### 2. Comanda Microservice

This microservice is responsible for receiving and forwarding command data to the warehouse. The command data is received from the Client microservice.

#### Responsibilities

- **`sendMessage`**  
  Sends data to be stored in the database. The database is managed by another microservice that will be discussed later.

- **`receiveCommand`**  
  Receives command data from the client, calls `sendMessage`, and then forwards the data further in the pipeline.

#### Technologies Used

- RabbitMQ (for sending data to the Database Microservice)
- Spring Cloud Stream
- Kotlin / Spring Boot

#### Architecture Role

- Acts as a processor in the command pipeline
- Forwards the command data further in the system

#### SOLID Principles Applied

- **Single Responsibility Principle**: The microservice is responsible for a single business capability – handling and forwarding commands. It does not mix unrelated concerns like inventory management or delivery processing.
- **Interface Segregation Principle**: The microservice exposes only the operations necessary for handling commands.
- **Inversion of Control**: The microservice delegates external communication (e.g., saving to DB) using message brokers like RabbitMQ instead of direct calls.

### 3. Depozit Microservice

This microservice implements the warehouse logic. It communicates with the Producer Microservice and the DB Microservice. The communication with the DB Microservice is used to monitor commands and product stock. The Producer Microservice represents an external entity that supplies products to the warehouse, based on incoming commands.

#### Responsibilities

- **`sendMessage`**  
  Sends messages to the DB Microservice and the Producer Microservice.

- **`receiveMessage`**  
  Receives messages from the DB Microservice and the Producer Microservice.

- **`processCommand`**  
  Handles command processing. It checks whether the current stock is sufficient to fulfill a command.  
  - If stock is sufficient, it updates the stock.
  - If stock is low, it reject the command.
  - Finally, it forwards the processed command to the next step in the pipeline.

#### Technologies Used

- RabbitMQ (for sending and receiving data to/from the DB Microservice and the Producer Microservice)
- Spring Cloud Stream
- Kotlin / Spring Boot

#### Architecture Role

- Acts as a processor in the command pipeline  
- Manages warehouse stock and supplier communication

#### SOLID Principles Applied

- **Single Responsibility Principle**: The microservice is responsible for a single business domain – warehouse operations (including stock checking and supplier coordination). It does not handle unrelated concerns like payment or client management.
- **Interface Segregation Principle**: The microservice exposes only the operations needed for warehouse logic.
- **Inversion of Control**: Communication with external systems is decoupled via message brokers (RabbitMQ).

### 4. Facturare microservie

This microservice is responsible for generating the invoice for a command. It receives data from the Depozit microservice, creates the invoice, saves it to the database, and then forwards the command data to the Livrare microservice.

#### Responsibilities

- **`sendMessage`**  
  Sends messages and requests to the DBMicroservice.

- **`receiveMessage`**  
  Receives responses from the DBMicroservice for previously sent requests.

- **`generateInvoice`**  
  Creates the invoice and stores it in the database. After that, it sends the command data to the Livrare microservice.

#### Technologies Used

- RabbitMQ (for sending and receiving data to/from the DBMicroservice)
- Spring Cloud Stream
- Kotlin / Spring Boot

#### Architecture Role

- Acts as a billing processor in the command pipeline  
- Responsible for generating and storing invoices before delivery

#### SOLID Principles Applied

- **Single Responsibility Principle**: The microservice is responsible for a single business concern – invoice generation. It does not handle unrelated tasks like stock or delivery management.
- **Interface Segregation Principle**: Only invoice-related operations are exposed, ensuring minimal and focused APIs.
- **Inversion of Control**: Communication with external services (e.g., DBMicroservice) is done through asynchronous messaging using RabbitMQ.

### 5. Livrare Microservie

This is the last microservice in the pipeline. This microservice simulates the delivery if the command was successfully completed. If the stock was not sufficient, it displays a message indicating that the command was rejected.

#### Responsibilities

- **`sendMessage`**  
  Sends a message or request to the DBMicroservice.

- **`receiveMessage`**  
  Receives a message from the DBMicroservice.

- **`expediereComanda`**  
  If the command was accepted, it simulates the delivery process by displaying a confirmation message.  
  If the command was rejected, it displays a message explaining the reason for the rejection.

#### Technologies Used

- RabbitMQ (for sending and receiving data to/from the DBMicroservice)
- Spring Cloud Stream
- Kotlin / Spring Boot

#### Architecture Role

- Acts as the final processor in the command pipeline  
- Responsible for simulating delivery if the command was successful, or showing a rejection message otherwise

#### SOLID Principles Applied

- **Single Responsibility Principle**: The microservice handles a single business task – managing the final step of the command, whether it's delivery or rejection.
- **Interface Segregation Principle**: It exposes only delivery-related operations, keeping its interface clean and focused.
- **Inversion of Control**: Communication with external systems (e.g., DBMicroservice) is handled through message brokers like RabbitMQ.

### 6. DBMicroservice

This microservice is not a direct step in the command processing pipeline. Its responsibility is to ensure data persistence. It handles reading from and writing to the database, based on messages it receives from other microservices.

#### Responsibilities

- **`sendMessage`**  
  Sends messages to other microservices when needed.

- **`receiveMessage`**  
  Receives messages from other microservices with instructions for database operations.

- **`update`**  
  Updates specific columns in a database table.

- **`insert`**  
  Inserts data into the database.

- **`read`**  
  Reads data from a specified table.

#### Technologies Used

- RabbitMQ (for sending and receiving data to/from other microservices)
- Kotlin / Spring Boot

#### Architecture Role

- Provides partial implementation of CRUD operations through message-based interaction  
- Ensures persistent storage of all command-related and system data

#### SOLID Principles Applied

- **Single Responsibility Principle**: The microservice handles one clear concern — managing database operations.
- **Interface Segregation Principle**: It exposes only database-related functionality, keeping its interface clean and decoupled.
- **Inversion of Control**: All interactions with this service are triggered by external messages, following the principles of asynchronous messaging with RabbitMQ.

#### 7. Producer Microservice

This microservice does not directly participate in the main command pipeline. It simulates a producer that manufactures products which are requested by the warehouse (Depozit microservice) to replenish its stock.

#### Responsibilities

- **`sendMessage`**  
  Sends a message to the Depozit microservice when the products are ready for delivery.

- **`receiveMessage`**  
  Receives messages from the Depozit microservice. These messages contain the product requests (commands) from the warehouse.

- **`processCommand`**  
  Simulates the time required to produce the products based on the received request.

#### Technologies Used

- RabbitMQ (for sending and receiving data to/from other microservices)
- Kotlin / Spring Boot

#### Architecture Role

- Provides products to the warehouse when requested  
- Acts as a passive participant triggered by warehouse demands

#### SOLID Principles Applied

- **Single Responsibility Principle**: The microservice is responsible for one task — producing products for the warehouse.
- **Interface Segregation Principle**: It exposes only the necessary operations related to production.
- **Inversion of Control**: It reacts to external messages (from Depozit microservice) using RabbitMQ instead of direct method calls.

![Microservice Diagram](/Images/microservice.jpg)
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