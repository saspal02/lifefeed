# Personal Life API — Project Plan

## 1. Problem - What problem are we solving?

Personal data is scattered across multiple applications and services, such as GitHub, wearable devices, reading platforms, workout trackers, and bookmark managers. Each service maintains its own data format, interface, and access mechanisms, making it difficult to get a unified view of personal activities.

There is no convenient, centralized way to access, organize, and programmatically query this information across different services.

**Personal Life API solves this by providing a unified, self-hosted API that collects data from the services I already use, normalizes it into a common structure, and makes it accessible through consistent endpoints.**

The primary goal is to give individuals ownership and control over their personal data, independent of the applications that originally generated it.

Instead of manually checking multiple applications, users can retrieve their activities through a chronological life feed or query individual data sources through a single API.

The API also serves as a foundation for building personal dashboards, automations, analytics, and AI-powered tools that can consume personal data.

## 2. Users - Who is this for?

**Primary users:**
- Developers who use multiple digital services and want to centralize their personal activity data.
- Quantified-self enthusiasts who want to track and understand different aspects of their daily lives.
- Individuals who prefer self-hosted solutions and want greater control over their personal information.

**Secondary users:**
- Developers who want to build dashboards, automations, and AI applications using personal activity data.
- Open-source contributors interested in personal data ownership and aggregation platforms.

The initial version is designed for individual use rather than multi-user SaaS environments.

## 3. Features - What does the MVP need?

- **Data Source Integrations:** Connect external services such as GitHub, wearable platforms, reading applications, and workout trackers.
- **Data Synchronization:** Automatically fetch and refresh data from connected services at scheduled intervals.
- **Data Normalization:** Convert data from different sources into a consistent event structure.
- **Unified Life Feed:** Provide a chronological, reverse-ordered feed combining activities from all connected sources.
- **Source-Specific Endpoints:** Expose individual endpoints for sleep, GitHub activity, reading progress, and workouts.
- **Summary Endpoints:** Provide daily and weekly summaries of collected activities.
- **API Authentication:** Protect private endpoints using personal API key authentication.
- **API Documentation:** Generate interactive API documentation using OpenAPI and Swagger UI.
- **Self-Hosting:** Allow the application to be deployed on a personal server or VPS.

## 4. Data - What are we storing?

The application will store normalized personal activity data collected from connected services.

**Core data:**

- **Activity Events:** Unique ID, source, event type, timestamp, summary, and structured JSON data.
- **Connected Sources:** Source name, connection status, synchronization configuration, and last successful synchronization time.
- **Source Credentials:** Encrypted OAuth access tokens, refresh tokens, or other integration credentials where applicable.
- **Synchronization Records:** Last sync time, sync status, errors, and synchronization checkpoints.
- **API Credentials:** Personal API keys and their associated metadata.

**Examples of activity data:**

| Source | Data |
|---|---|
| GitHub | Commits, repositories, contribution activity |
| Wearable | Sleep duration, sleep score, activity metrics |
| Reading | Books, reading progress, pages completed |
| Workout | Exercise type, duration, calories, performance |

**Common event structure:**

- `id`
- `source`
- `type`
- `timestamp`
- `summary`
- `data` (JSON)

The original source data may also be retained where necessary for debugging, reprocessing, or preserving information that cannot be represented in the normalized structure.

## 5. Tech - What stack are we using?

**Backend**
- Java 25
- Spring Boot
- Spring Web (REST APIs)
- Spring RestClient (external API integrations)
- Spring Scheduling (periodic synchronization)
- Spring Data JPA / Hibernate (database persistence)
- SpringDoc OpenAPI (API documentation)

**Database**
- PostgreSQL
- JSON/JSONB for flexible source-specific event data.

**Architecture**
- Module-wise monolithic architecture.
- Separate packages for individual data sources and application capabilities.
- Common event model for normalized activities.
- REST-based communication with external services.
- No microservices or distributed architecture for the MVP.

**Development and Infrastructure**
- Maven
- Docker
- Docker Compose
- Environment-based configuration for credentials and application settings.

**Root package:** `com.saswat.lifefeed`

The GitHub integration uses Spring RestClient to communicate with the GitHub REST API. Its initial implementation focuses on fetching and returning normalized GitHub activity rather than introducing unnecessary persistence infrastructure into that integration.

## 6. Monetize - How will this make money?

There is no monetization strategy planned for the initial version.

The project is primarily a personal-use and open-source project focused on data ownership, self-hosting, and programmability.

Potential future directions, if the project evolves beyond personal use:

- Community-supported open-source development.
- Hosted deployment for users who do not want to manage their own infrastructure.
- Premium integrations or advanced data aggregation capabilities.

Monetization is explicitly outside the MVP scope.

## 7. UI/UX - How should this look and feel?

The MVP is API-first. A polished dashboard or mobile application is not required.

The primary interface will be the REST API, with interactive API documentation available through Swagger UI.

The API should be predictable, consistent, and easy to consume from:
- Terminal using curl.
- Custom dashboards and frontend applications.
- Automation workflows.
- AI agents and other programmatic clients.

**Future dashboard direction:**

A minimal personal activity dashboard that presents:
- A chronological life feed.
- Activity grouped by source and category.
- Daily and weekly summaries.
- Individual views for sleep, GitHub activity, reading, and workouts.

The interface should prioritize information clarity, simplicity, and useful data presentation rather than social engagement or unnecessary gamification.

## 8. Deployment - Where and how will this ship?

**Deployment approach:** Self-hosted application.

The application should be deployable on a personal VPS or home server and accessible remotely through a configured domain.

**Infrastructure:**
- Application: Spring Boot executable JAR.
- Database: PostgreSQL.
- Containerization: Docker and Docker Compose.
- Background processing: Spring scheduled tasks for periodic source synchronization.
- API documentation: Swagger UI / OpenAPI.

**Build command:**
`./mvnw clean package`

**Application start command:**
`java -jar target/lifefeed-*.jar`

**Required environment variables (initial):**
- `SPRING_PROFILES_ACTIVE`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `GITHUB_USERNAME`
- `GITHUB_TOKEN`
- `LIFEFEED_API_KEY`

Additional credentials will be introduced as new integrations are implemented.

**Database and storage:**
- PostgreSQL for normalized activity records and application metadata.
- Persistent database volumes for Docker deployments.

**Operational requirements:**
- Scheduled synchronization without requiring manual API requests.
- Health-check endpoint for application monitoring.
- Persistent storage across container restarts.
- Secure handling of API credentials and external service tokens.
- Database backup and recovery support.

**Hosting provider:** Not finalized. A VPS or personal home server is the intended deployment target.

## 9. Usage model and constraints

- **Usage:** Initially intended for personal use.
- **Tenancy:** Single-user, single-tenant application.
- **Deployment:** Self-hosted, with remote access where configured.
- **Architecture:** Single Spring Boot application with module-wise package separation.
- **Data ownership:** The individual controls the collected data and deployment environment.
- **External dependencies:** Integrations depend on the availability, permissions, rate limits, and access policies of third-party APIs.
- **Security:** Private personal data must not be exposed without appropriate authentication and access controls. API credentials and integration tokens must be protected.
- **Availability:** No enterprise-grade SLA or high-availability requirement for the initial version.
- **Scalability:** Designed for personal activity volumes, without introducing distributed infrastructure prematurely.
- **Privacy:** No social features, public user profiles, or unnecessary sharing of personal information.
- **Extensibility:** New data sources should be implementable without changing the common event contract or existing integrations unnecessarily.

**Explicit exclusions:**
- Multi-user registration and account management.
- Subscription and billing systems.
- Microservices and distributed infrastructure.
- Real-time event streaming.
- Advanced analytics and AI-generated insights.
- Writing data back to connected services.
- A polished frontend or mobile application in the initial MVP.