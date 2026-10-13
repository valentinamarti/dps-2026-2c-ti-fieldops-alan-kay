# TPI — Field Ops

Backend for planning, validating, approving and following the execution of scientific field expeditions:
expeditions with zones, resources, permits and restrictions; an itinerary of activities and of sequential and
parallel blocks; validation, estimation and assignment; the approval lifecycle; tracking; and automatic
replanning after an incident.

**Group:** Alan-Kay
**Course:** DPS

## Team

- Matías Romanato
- Julieta Techenski
- Valentina Marti Reta
- Tobías Noceda
- Matías Sapino

## Requirements

- **JDK 25** — the build checks it with `maven-enforcer-plugin` and fails with any other version.
- **Maven 3.9+**

Check both before building; `mvn -v` must report Java 25 too, since Maven uses the JDK found through `JAVA_HOME`:

```bash
java -version
mvn -v
```

If Maven picks another JDK, point `JAVA_HOME` to JDK 25:

| System | Command |
| --- | --- |
| Linux | `export JAVA_HOME=/usr/lib/jvm/<jdk-25-dir>` |
| macOS | `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` |
| Windows (PowerShell) | `$env:JAVA_HOME = "C:\Program Files\Java\jdk-25"` |

Nothing else is needed: persistence is in memory, so there is no database to install or configure.

## Build and test

Run from the repository root. The commands are the same on Linux, macOS and Windows:

```bash
mvn test      # compiles and runs the tests (the JDK 25 check runs first, in validate)
mvn -B verify # full build, the same command as the CI
```

## Run

```bash
mvn -B install -DskipTests    # once, and after changing domain or app: puts the modules in your local Maven repository
mvn spring-boot:run -pl web
```

The application starts Tomcat on port 8080:

| URL | What it is |
| --- | --- |
| <http://localhost:8080/swagger-ui.html> | Swagger UI, where every endpoint can be tried |
| <http://localhost:8080/v3/api-docs> | The OpenAPI document springdoc generates from the controllers |

Without the `demo` profile the catalog and the zones start empty, so an expedition has to be created from
scratch: register the zones and the staff it references first, otherwise `POST /api/expeditions` answers `404`.
Stop the application with `Ctrl+C`.

To run the packaged jar instead:

```bash
mvn -B package -DskipTests
java -jar web/target/web-1.0-SNAPSHOT.jar
```

## Run the demonstration

The `demo` profile loads [`demo/catalog.json`](demo/catalog.json) and
[`demo/expedition.json`](demo/expedition.json) on startup, through the same use cases the REST adapter goes
through. [`demo.http`](demo.http) is the script that tells the story against the API from there, step by step
and commented (in Spanish, which is the language of the presentation).

```bash
mvn -B install -DskipTests
mvn spring-boot:run -pl web -Dspring-boot.run.profiles=demo
```

or, with the packaged jar:

```bash
mvn -B package -DskipTests
java -jar web/target/web-1.0-SNAPSHOT.jar --spring.profiles.active=demo
```

Then open `demo.http` in IntelliJ (Tools > HTTP Client, or the ▶ button next to each request) and run the
requests in order. VS Code works too, with the REST Client extension. Every step that needs an id takes it
from the previous answer, so nothing has to be copied by hand.

What the seed leaves loaded:

| | |
| --- | --- |
| Catalog | 8 people with their certifications, 2 vehicles, 9 instruments, 3 supplies with stock, and the spare drone marked unavailable while it is in maintenance |
| Zones | `z-coast` (Coastal reef) and `z-wetland` (Wetland basin), each with the permit it requires |
| Expedition | *Patagonia coastal survey*, in draft, with **10 activities of the 6 types**, a **parallel block inside a sequential one**, and **resources shared** between activities that do not overlap (the cooler and Lee, the drone and Alan, the truck and Nils) |

What the script adds and walks: the **night activity** of F1 (which is where the critical and the warning come
from), the approval that the critical blocks, the correction, the acceptance of the warning with its
justification, the approval, the execution, an incident, and the replanning of F3 with its audit trail.

The script is **not idempotent**: it leaves the expedition replaced and the night dive scheduled. To run it
again, **restart the application** — the profile loads the data from the files on every startup. The same walk
runs in the CI as `DemoScriptApiTest`, so if the script and the test drift apart the build says so.

The files are read off the classpath, so the same build works from `spring-boot:run`, from the jar and from
the tests. To point the profile at another scenario, with no code to change:

```bash
java -jar web/target/web-1.0-SNAPSHOT.jar --spring.profiles.active=demo \
  --fieldops.demo.catalog=file:/path/to/catalog.json \
  --fieldops.demo.expeditions=file:/path/to/expedition.json
```

## Structure

The project is a multi-module Maven build following a hexagonal architecture (see D35 and D36 in
[DESIGN.md](DESIGN.md)). The compiler enforces the dependency direction: `web` -> `app` -> `domain`.

| Module | Contains | Depends on |
| --- | --- | --- |
| `domain` | All of `business.*`: model, rules, validations, services, use cases (input ports) and `business.providers` (output ports). Publishes a `test-jar` with shared fixtures | No frameworks. The enforcer bans `org.springframework*`, direct or transitive |
| `app` | `adapters.driven`: driven adapters, today the in-memory repositories (`InMemoryExpeditionRepository`, `InMemoryZoneRepository`, `InMemoryReplanProposalRepository`) and the synchronous `InMemoryEventPublisher` | `domain` |
| `web` | Spring Boot application (`FieldOpsApplication`), the `@Configuration` classes that wire the use cases (composition root, D37) — `FieldOpsConfig` and `ReplanningConfig` (F3) — and `adapters.driving`: the REST adapter (`rest`) and the seed loader of the demonstration (`seed`, D50) | `domain`, `app` |

```
domain/src/main/java/ar/edu/itba/dps/fieldops/business
├── interfaces      # domain contracts: ActivityRules, ActivityType, ItineraryNode, ReplanReason, ReplanOptionGenerator, DomainEvent/EventSubscriber, resources, ValidationRule
├── models          # entities and value objects (activities, common, estimation, events, expeditions, replanning, report, resources, validation, zones)
├── providers       # output ports: ExpeditionRepository, ZoneRepository, ReplanProposalRepository, EventPublisher
├── rules           # ActivityRules implementations, one per activity type, the night decorator (NightOperationRules) and the ActivityTypeRegistry
├── validations     # ValidationRule implementations and the ApprovalPolicy
├── estimations     # ExpeditionEstimator
├── reports         # ExpeditionReporter
├── replanning      # ExpeditionReplanner, the replan reasons (Cancellation, Delay, ResourceWithdrawn, ResourceReplaced) and the option generators of F3
├── services        # ResourceReservations
├── usecases        # input ports: commands and operations by id (create, schedule, assign, transition, approve, track, finish, replan, register an incident, decide a replan proposal) and queries (get, list, validate, estimate, report, candidates, activity types, replan proposals)
└── exceptions      # domain exceptions
app/src/main/java/ar/edu/itba/dps/fieldops/adapters/driven     # in-memory repositories and InMemoryEventPublisher
web/src/main/java/ar/edu/itba/dps/fieldops                     # FieldOpsApplication, FieldOpsConfig, ReplanningConfig
web/src/main/java/.../adapters/driving/rest                    # controllers, DomainExceptionHandler, OpenApiConfig, and the dtos and mappers subpackages
web/src/main/java/.../adapters/driving/seed                    # DemoDataLoader and the @Profile("demo") that turns it on
demo/                                                          # the data of the demonstration, as JSON
demo.http                                                      # the script of the demonstration, against the API
*/src/test/java/...                                            # unit tests per module and end-to-end integration tests in web
DESIGN.md                                                      # design decisions (in Spanish)
```

## API contracts

48 endpoints, all under `/api`, generated into OpenAPI by springdoc:
<http://localhost:8080/swagger-ui.html>. Operations that are not CRUD are sub-resources in `POST` and not
verbs in the URL (`/approval`, `/suspension`, `/completion`), which is the convention D49 fixes.

### Expeditions

| Method and path | What it does |
| --- | --- |
| `POST /api/expeditions` | Creates a draft expedition, with its zones and responsibles referenced by id |
| `GET /api/expeditions` | Lists every expedition |
| `GET /api/expeditions/{id}` | One expedition, with its status and the replacement links of a replanning |

### Catalog of resources

| Method and path | What it does |
| --- | --- |
| `POST /api/staff` · `GET /api/staff` · `GET /api/staff/{id}` | Registers and queries people, with their certifications |
| `POST /api/staff/{id}/unavailabilities` | Records a period in which a person cannot be used |
| `POST /api/vehicles` · `GET /api/vehicles` · `GET /api/vehicles/{id}` | The equipment that has a license plate |
| `POST /api/vehicles/{id}/unavailabilities` | Records a period in which a vehicle cannot be used |
| `POST /api/instruments` · `GET /api/instruments` · `GET /api/instruments/{id}` | The equipment that does not |
| `POST /api/instruments/{id}/unavailabilities` | Records a period in which an instrument cannot be used |
| `GET /api/equipment` | Vehicles and instruments together, as two lists (D52) |
| `POST /api/supplies` · `GET /api/supplies` · `GET /api/supplies/{id}` | Registers and queries consumables, with the stock they have left |

A supply has stock and not a calendar, so `/api/supplies/{id}/unavailabilities` does not exist.

### Zones

| Method and path | What it does |
| --- | --- |
| `POST /api/zones` · `GET /api/zones` · `GET /api/zones/{id}` | Registers and queries the zones, with the permits each one requires |

### Itinerary

| Method and path | What it does |
| --- | --- |
| `GET /api/activity-types` | The six types of activity, with the parameters each one is asked for |
| `POST /api/expeditions/{id}/activities` | Schedules one activity of a type, with `atNight` for the night variant (F1) |
| `POST /api/expeditions/{id}/blocks` | Schedules a `sequential` or `parallel` block with its whole tree of children (F2) |
| `GET /api/expeditions/{id}/itinerary` | The itinerary as a tree of activities and blocks |

### Assignment

| Method and path | What it does |
| --- | --- |
| `POST /api/expeditions/{id}/activities/{activityId}/staff` | Assigns a person of the catalog |
| `POST /api/expeditions/{id}/activities/{activityId}/equipment` | Assigns a vehicle or an instrument |
| `POST /api/expeditions/{id}/activities/{activityId}/supplies` | Assigns a consumable |
| `GET /api/expeditions/{id}/activities/{activityId}/candidates` | Each requirement of the activity with the candidates that qualify for it |

### Validation and estimation

| Method and path | What it does |
| --- | --- |
| `GET /api/expeditions/{id}/validation` | Runs the approval rules without approving: criticals and warnings apart |
| `GET /api/expeditions/{id}/estimate` | Duration, overall risk and estimated consumption |

### Lifecycle and approval

| Method and path | What it does |
| --- | --- |
| `POST /api/expeditions/{id}/review` | Draft -> in review |
| `POST /api/expeditions/{id}/draft` | In review -> draft, to correct it |
| `POST /api/expeditions/{id}/approval` | Approves it, with the warnings a responsible accepts and their justification. A critical left open is a `422` that lists which ones |
| `POST /api/expeditions/{id}/execution` | Approved or suspended -> in execution |
| `POST /api/expeditions/{id}/suspension` | In execution -> suspended |
| `POST /api/expeditions/{id}/resumption` | Suspended -> in execution |
| `POST /api/expeditions/{id}/completion` | Closes the expedition; one with an activity in progress is a `409` |
| `GET /api/expeditions/{id}/report` | Operative summary, actual consumption, risks, incidents and accepted warnings |

### Tracking

| Method and path | What it does |
| --- | --- |
| `POST /api/expeditions/{id}/activities/{activityId}/start` | Records when the activity actually started |
| `POST /api/expeditions/{id}/activities/{activityId}/completion` | Closes it with its actual result, consuming the supplies it used |
| `POST /api/expeditions/{id}/activities/{activityId}/observations` | Records a note taken in the field |

### Incidents and replanning (F3)

| Method and path | What it does |
| --- | --- |
| `POST /api/expeditions/{id}/incidents` | Registers an incident and answers with the proposal it generated, with its options numbered |
| `GET /api/expeditions/{id}/replan-proposals` | The proposals of an expedition |
| `GET /api/replan-proposals/{proposalId}` | One proposal: the incident, the options and the decision. It is the audit record |
| `POST /api/replan-proposals/{proposalId}/acceptance` | Accepts one option and answers with the alternative expedition, as a draft |
| `POST /api/replan-proposals/{proposalId}/rejection` | Rejects the proposal; the expedition keeps the plan it had |

### Errors

Every error answers with the same shape, `ProblemDetail` (RFC 9457), and the mapping of domain exceptions to
status codes lives in the adapter and not in the domain (D49):

| Status | When |
| --- | --- |
| `400` | An invalid argument, a malformed schedule or tracking, an unknown activity type or a parameter that does not serve it |
| `404` | A resource, zone, expedition, scheduled activity, proposal, option or warning that does not exist |
| `409` | An illegal status transition, an expedition that is no longer editable, an activity in progress, a resource that is not available, a repeated id, a proposal that was already decided |
| `422` | A critical that blocks the approval (the body carries the list in `criticals`), a warning left unaccepted, stock that is not enough, someone who is not a responsible of the expedition |

The `422` of the approval carries both: the `detail` enumerates the criticals in text and the `criticals`
property brings them structured, with the same shape `GET /api/expeditions/{id}/validation` gives them.

## Tests

`mvn -B verify` runs 787 tests: 641 in `domain`, 20 in `app` and 126 in `web`.

| Kind | Where |
| --- | --- |
| Unit | Model, rules, validations and use cases in `domain`, with Mockito only where there is an output port |
| Parameterized | `DomainExceptionHandlerTest` walks the whole table of exception to status code, and fails if an exception of `business.exceptions` has no row |
| Repository | `app`: the in-memory adapters against the contract of each port (`InMemory*RepositoryTest`) |
| API | `web`: the full context with the real in-memory adapters and no mocks, one happy path per capability plus the acceptance criteria of F1, F2 and F3 |
| Demonstration | `DemoDataLoaderTest` (the files of `demo/` load and leave the expected scenario) and `DemoScriptApiTest` (the same walk as `demo.http`, end to end) |

Persistence is in memory, which the repository tests cover against the contract of each port. Real
persistence is registered as technical debt of this delivery in D51.

## Continuous integration

`.github/workflows/ci.yml` runs `mvn -B verify` with JDK 25 (Temurin) on every pull request and on every push
to `main`.

Branch protection for `main` is configured by hand on GitHub (Settings > Branches), because it does not live
in the code:

- Require a pull request before merging (no direct pushes to `main`).
- Require the `verify` status check (job of the `CI` workflow) to pass. The check shows up in the list after
  the workflow has run once.

Work happens on one branch per phase with small pull requests.

## Out of scope of this delivery

- **Frontend.** The group decided it is out of the scope of phase 5, which delivers the API, Swagger UI and
  the script. Swagger UI is not a replacement for a frontend.
- **Real persistence.** In memory, behind the output ports, with the repository tests as the contract (D51).
- Security, deployment and anything the statement explicitly does not ask for (microservices, cloud).

## Design decisions

See [DESIGN.md](DESIGN.md), in Spanish: D1 to D34 come from the first delivery and D35 to D59 from this one,
plus one section per new functionality (F1, F2 and F3) with the classes added and modified, the refactorings
and the technical debt that was left unresolved on purpose.
