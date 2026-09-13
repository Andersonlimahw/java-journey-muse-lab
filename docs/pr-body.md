# docs: ADR-0001 modular monolith + architecture artifacts

Documents the refactor ported from `java-journey-lemon-lab` (see
`docs/adr/0001-modular-monolith-vertical-slices.md`): god `TripController`
split into 4 vertical slices + shared kernel, REST contract frozen, 4 bugs
fixed. Code is on `main`; this PR adds the decision record, the C4/ER
diagrams and the verification map. Rendered Mermaid below.

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
    end
    T -->|public API only| P
    A --> T
    L --> T
    T & P & A & L -.->|errors| K
```

Slice rule: cross-slice access only via the other slice's service public API
(`TripController → ParticipantService`, never `→ ParticipantRepository`).

## Create-trip flow (most coupled use case)

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant TC as trips::TripController
    participant TS as trips::TripService
    participant PS as participants::ParticipantService
    participant DB as H2 (Flyway V1–V4)
    C->>TC: POST /trips {destination, dates, emails_to_invite}
    TC->>TS: createTrip(payload)
    TS->>DB: INSERT trips
    TC->>PS: registerParticipantsToEvent(emails, trip)
    PS->>DB: INSERT participants (batch)
    PS-->>TC: List<Participant> (saved rows only)
    TC-->>C: 200 {uuid, participants[]}
```

## Data model

```mermaid
erDiagram
    TRIPS ||--o{ PARTICIPANTS : "has (trip_id, cascade)"
    TRIPS ||--o{ ACTIVITIES : "has (trip_id, cascade)"
    TRIPS ||--o{ LINKS : "has (trip_id, cascade)"
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
```

## Bug fixes (ported with guards)

| # | Original | Fix | Guard |
|---|---|---|---|
| 1 | `TripController.update` set `endsAt` twice, `startsAt` never | `TripService.updateTrip` sets both | `TripSliceRegressionTest` + Postman asserts `startsAt` |
| 2 | invite returned `findAll()` (cross-trip leak) | returns `saveAll()` rows only | regression test + Postman `participants.length == 1` |
| 3 | `TripCreateResponse` exposed JPA entities | `ParticipantData` DTOs, same JSON shape | MockMvc + Postman read `uuid` |
| 4 | links CRUD had zero tests | `LinkControllerTest` (9 tests) + Postman folder | `mvn test` |

## Verification

- `mvn test` → **45/45 green** (6 classes, MockMvc over the real HTTP layer)
- `src/test/postman/java-journey-muse-lab.postman_collection.json` (23 requests,
  ordered, captures ids) + `local.postman_environment.json`
- `./scripts/validate-api.sh [baseUrl]` — executable curl mirror of the collection
- `npx newman run src/test/postman/java-journey-muse-lab.postman_collection.json -e src/test/postman/local.postman_environment.json`

Full context: `docs/architecture.md` (C4 L1/L2, ownership table, verification map).
