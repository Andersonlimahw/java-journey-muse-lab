# Architecture — java-journey-muse-lab

Trip-planner API (`POST /trips` and friends) as a **modular monolith**:
one deployable, five vertical slices, one shared kernel.
Full rationale: [ADR 0001](adr/0001-modular-monolith-vertical-slices.md),
events slice: [ADR 0002](adr/0002-trip-events-slice.md),
Java 26 upgrade: [ADR 0003](adr/0003-java-26-spring-boot-4-upgrade.md).

## System context (C4 L1)

```mermaid
flowchart LR
    U["Traveler<br/>(Postman / frontend)"] -->|HTTP JSON :8080| API["java-journey-muse-lab<br/>Spring Boot 4.1.1 · Java 26"]
    API -->|JDBC embedded| DB[("H2 in-memory<br/>Flyway V1–V5")]
```

## Containers (C4 L2 — still one container, sliced inside)

```mermaid
flowchart TB
    API["journey-0.1.0.jar<br/>single Spring Boot app"]
    API --> T["slice trips"]
    API --> P["slice participants"]
    API --> A["slice activities"]
    API --> L["slice links"]
    API --> E["slice events"]
    API --> K["shared kernel"]
    T & P & A & L & E --> DB[("H2")]
```

## Slice ownership

| Slice | Package | Endpoints | Tables |
|---|---|---|---|
| trips | `modules.trips` | `POST/GET /trips`, `GET /trips/{id}`, `GET /trips/{id}/overview`, `PUT /trips/{id}`, `PATCH /trips/{id}/confirmation`, `POST /trips/{id}/invite`, `GET /trips/{id}/participants` | `trips` |
| participants | `modules.participants` | `GET/POST /participants`, `GET/PUT/DELETE /participants/{id}`, `POST /participants/{id}/confirm` | `participants` |
| activities | `modules.activities` | `POST/GET /trips/{id}/activities`, `GET/PUT/DELETE /trips/{id}/activities/{activityId}` | `activities` |
| links | `modules.links` | `POST/GET /trips/{id}/links`, `GET/PUT/DELETE /trips/{id}/links/{linkId}` | `links` |
| events | `modules.events` | `POST/GET /trips/{id}/events`, `GET/PUT/DELETE /trips/{id}/events/{eventId}` | `trip_events` |
| shared kernel | `shared` | `ApiError`, `GlobalExceptionHandler` (no business logic) | — |

Dependency rule: slices talk to each other **only via service public APIs**
(`TripController → ParticipantService`, never `→ ParticipantRepository`).
Trip-scoped slices load the `Trip` first (404 if missing), then delegate.

## Data model

```mermaid
erDiagram
    TRIPS ||--o{ PARTICIPANTS : "has (trip_id, cascade)"
    TRIPS ||--o{ ACTIVITIES : "has (trip_id, cascade)"
    TRIPS ||--o{ LINKS : "has (trip_id, cascade)"
    TRIPS ||--o{ TRIP_EVENTS : "has (trip_id, cascade)"
    TRIPS {
        uuid id PK
        string destination
        timestamp starts_at
        timestamp ends_at
        boolean is_confirmed
        string owner_name
        string owner_email
    }
    PARTICIPANTS {
        uuid id PK
        string name
        string email
        boolean is_confirmed
        uuid trip_id FK
    }
    ACTIVITIES {
        uuid id PK
        string title
        timestamp occurs_at
        uuid trip_id FK
    }
    LINKS {
        uuid id PK
        string title
        string url
        uuid trip_id FK
    }
    TRIP_EVENTS {
        uuid id PK
        string title
        string description
        string location
        timestamp starts_at
        timestamp ends_at
        uuid trip_id FK
    }
```

Migrations: `src/main/resources/db/migration/V1..V5__*.sql` (Flyway).

## Verification map

| Layer | How | Where |
|---|---|---|
| Unit-of-slice (HTTP) | MockMvc, 66 tests, 7 classes | `src/test/java/...` |
| Contract (black-box) | Postman collection, 29 requests, ordered, asserts statuses + captures ids | `src/test/postman/` |
| Contract (CI-friendly) | Executable curl mirror of the collection | `scripts/validate-api.sh` |
| Bugfix guards | `TripSliceRegressionTest` (update-startsAt, invite isolation), `LinkControllerTest` | `src/test/java/...` |

Run everything: `mvn test`, then `./scripts/validate-api.sh` against
`mvn spring-boot:run` (or `npx newman run src/test/postman/*.json -e ...local...`).
