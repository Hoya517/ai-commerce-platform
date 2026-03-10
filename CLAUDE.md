# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
./gradlew build          # Compile and build
./gradlew bootRun        # Run the application
./gradlew test           # Run all tests
./gradlew test --tests "com.hoya.aicommerce.SomeTest"  # Run a single test class
./gradlew clean          # Clean build artifacts
```

## Tech Stack

- **Java 21**, **Spring Boot 4.0.3**, **Gradle 9.3.1**
- **Spring Data JPA** (Hibernate) for persistence
- **Spring MVC** for REST APIs
- **SpringDoc OpenAPI 3.0.2** — Swagger UI auto-generated from controller annotations
- **Lombok** — use `@Getter`, `@Builder`, `@RequiredArgsConstructor`, etc. to reduce boilerplate
- **JUnit 5** for testing (`@SpringBootTest` for integration, plain JUnit for unit tests)

## Architecture

This is a Spring Boot backend designed as a learning platform for MSA (microservices) and Spring AI integration. The base package is `com.hoya.aicommerce`.

Standard layered structure expected as the project grows:
- **Controller** — REST endpoints, annotated with `@RestController`
- **Service** — business logic, `@Service`
- **Repository** — Spring Data JPA interfaces, `@Repository`
- **Entity** — JPA entities, `@Entity`
- **DTO** — request/response objects (use Lombok `@Builder`, `@Data`)

OpenAPI docs are auto-generated — document controllers with SpringDoc annotations (`@Operation`, `@Tag`) when adding endpoints. Swagger UI is accessible at `/swagger-ui.html` when the app is running.

## Configuration

`application.properties` currently has only `spring.application.name`. Database and other environment-specific settings go here (or in `application-{profile}.properties`).
