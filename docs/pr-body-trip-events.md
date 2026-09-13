# feat: TripEvents slice (`/trips/{id}/events` CRUD)

New `events` vertical slice owning the `TripEvent` aggregate end to end,
following the ADR-0001 slice rules: `title` (required), `description` +
`location` (optional), `starts_at` / `ends_at` (required, `ends_at >=
starts_at`), trip-scoped CRUD with 404s for unknown trips and cross-trip
access. No existing path, verb or status changed — contract growth only.
Full rationale: `docs/adr/0002-trip-events-slice.md`.

## Module boundaries

```mermaid
flowchart LR
    subgraph app["java-journey-muse-lab (single deployable)"]
        direction TB
        K["shared kernel<br/>ApiError, GlobalExceptionHandler"]
        T["slice: trips<br/>TripController, TripService, TripRepository"]
        P["slice: participants<br/>ParticipantController, ParticipantService, ParticipantRepository"]
        A["slice: activities<br/>ActivityController, ActivityService, ActivityRepository"]
        L["slice: links<br/>LinkController, LinkService, LinkRepository"]
        E["slice: events 🆕<br/>TripEventController, TripEventService, TripEventRepository"]
    end
    T -->|public API only| P
    A --> T
    L --> T
    E --> T
    T & P & A & L & E -.->|errors| K
```

Slice rule: cross-slice access only via the other slice's service public API.
The events slice touches `trips` only to resolve the parent trip (404 if
missing), exactly like the activities slice.

## Register-event flow

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant EC as events::TripEventController
    participant ES as events::TripEventService
    participant TR as trips::TripRepository
    participant DB as H2 (Flyway V5)
    C->>EC: POST /trips/{id}/events {title, starts_at, ends_at, ...}
    EC->>TR: findById(id) → 404 if missing
    EC->>ES: validatePayload(payload) → 400 if invalid
    ES->>DB: INSERT trip_events
    ES-->>EC: TripEventResponse(eventId)
    EC-->>C: 200 {eventId}
```

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

## Endpoints (new)

| Method | Path |
|---|---|
| POST / GET | `/trips/{id}/events` |
| GET / PUT / DELETE | `/trips/{id}/events/{eventId}` |

Validation (manual `TripEventService.validatePayload`, same style as
activities): blank title → 400, missing/malformed dates → 400, `ends_at`
before `starts_at` → 400.

## Verification

- `mvn test` → **65/65 green** (7 classes: +20 `TripEventControllerTest`)
- `src/test/postman/java-journey-muse-lab.postman_collection.json` (29
  requests: +6 `events` folder, captures `eventId`) + `local.postman_environment.json`
- `./scripts/validate-api.sh [baseUrl]` — extended with the events slice
  (create/list/get/update/delete + gone-check) and an `ends_at < starts_at`
  400 negative case
- `npx newman run src/test/postman/java-journey-muse-lab.postman_collection.json -e src/test/postman/local.postman_environment.json`

Full context: `docs/architecture.md` (C4 L1/L2, ownership table, ER,
verification map), `docs/adr/0002-trip-events-slice.md` (alternatives,
naming, consequences).
