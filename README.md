# java-journey-muse-lab 🍋

Trip-planner API rebuilt **from zero** as a **modular monolith with vertical
slices**, ported from `java-journey-lemon-lab` with the REST contract preserved
and 4 latent bugs fixed. Spring Boot 4.1.1 · Java 26 · H2 + Flyway.

## Quickstart

Prerequisites: JDK 26 (pinned in `.java-version` for jenv/sdkman/asdf;
the build targets Java 26 bytecode), Maven 3.x.

```bash
mvn test                    # 66 tests, 7 classes — must be green
mvn spring-boot:run         # API on http://localhost:8080
./scripts/validate-api.sh   # 29-request black-box validation (curl + jq)
```

Postman: import `src/test/postman/java-journey-muse-lab.postman_collection.json`
with `src/test/postman/local.postman_environment.json`, or run headless:

```bash
npx newman run src/test/postman/java-journey-muse-lab.postman_collection.json \
  -e src/test/postman/local.postman_environment.json
```

## Layout (vertical slices)

```
src/main/java/com/muse/journey/
├── JourneyApplication.java
├── shared/                     # kernel: ApiError, GlobalExceptionHandler
└── modules/
    ├── trips/                  # TripController, TripService, TripRepository, DTOs
    ├── participants/           # ParticipantController/Service/Repository, DTOs
    ├── activities/             # ActivityController/Service/Repository, DTOs
    ├── links/                  # LinkController/Service/Repository, DTOs
    └── events/                 # TripEventController/Service/Repository, DTOs
src/test/java/com/muse/journey/ # Mirror of the slices + regression tests
src/test/postman/               # Collection + local environment
scripts/validate-api.sh         # Executable curl mirror of the collection
docs/adr/000*.md                # ADRs with mermaid diagrams + bugfix log
docs/architecture.md            # C4, ownership table, ER diagram, verification map
```

Slice rule: cross-slice access only via the other slice's **service public
API** (e.g. trips invites through `ParticipantService`, never the repository).

## Endpoints

| Method | Path | Slice |
|---|---|---|
| POST / GET | `/trips` | trips |
| GET | `/trips/{id}`, `/trips/{id}/overview` | trips |
| PUT | `/trips/{id}` | trips |
| PATCH | `/trips/{id}/confirmation` | trips |
| POST | `/trips/{id}/invite` | trips → participants |
| GET | `/trips/{id}/participants` | trips → participants |
| GET / POST | `/participants` | participants |
| GET / PUT / DELETE | `/participants/{id}` | participants |
| POST | `/participants/{id}/confirm` | participants |
| POST / GET | `/trips/{id}/activities` | activities |
| GET / PUT / DELETE | `/trips/{id}/activities/{activityId}` | activities |
| POST / GET | `/trips/{id}/links` | links |
| GET / PUT / DELETE | `/trips/{id}/links/{linkId}` | links |
| POST / GET | `/trips/{id}/events` | events |
| GET / PUT / DELETE | `/trips/{id}/events/{eventId}` | events |

## What changed vs the original

See [ADR 0001](docs/adr/0001-modular-monolith-vertical-slices.md): god
controller split into 4 slice controllers + `TripService`, `updateTrip` now
sets `startsAt` (was `endsAt` twice), invite returns only the invited rows
(was `findAll()`), create-trip returns DTOs instead of JPA entities, and the
links CRUD gained 9 tests + Postman coverage.

See [ADR 0002](docs/adr/0002-trip-events-slice.md): new `events` slice owning
the `TripEvent` aggregate (`/trips/{id}/events` CRUD, Flyway V5), following
the same vertical-slice rules — 20 MockMvc tests + Postman folder +
`validate-api.sh` coverage.
