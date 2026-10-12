# TPI — Field Ops

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

### Scope

Compilable Java project containing the domain models, contracts (interfaces), services and use cases, along with their tests, and a Spring Boot application that wires them.
It does not include a REST API (there are no endpoints yet), real persistence, frontend, security or deployment.

## Structure

The project is a multi-module Maven build following a hexagonal architecture (see D35 and D36 in [DESIGN.md](DESIGN.md)). The compiler enforces the dependency direction: `web` -> `app` -> `domain`.

| Module | Contains | Depends on |
| --- | --- | --- |
| `domain` | All of `business.*`: model, rules, validations, services, use cases (input ports) and `business.providers` (output ports). Publishes a `test-jar` with shared fixtures | No frameworks. The enforcer bans `org.springframework*`, direct or transitive |
| `app` | `adapters.driven`: driven adapters, today the in-memory repositories (`InMemoryExpeditionRepository`, `InMemoryZoneRepository`, `InMemoryReplanProposalRepository`) and the synchronous `InMemoryEventPublisher` | `domain` |
| `web` | Spring Boot application (`FieldOpsApplication`) and the `@Configuration` classes that wire the use cases (composition root, D37): `FieldOpsConfig` and `ReplanningConfig` (F3) | `domain`, `app` |

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
*/src/test/java/...                                            # unit tests per module and end-to-end integration tests in web
DESIGN.md                                                      # design decisions (in Spanish)
```

## Build and test

Run from the repository root. The commands are the same on Linux, macOS and Windows:

```bash
mvn test      # compiles and runs the tests (the JDK 25 check runs first, in validate)
mvn -B verify # full build, same command as the CI
```

## Run

```bash
mvn -B install -DskipTests    # once, and after changing domain or app: puts the modules in your local Maven repository
mvn spring-boot:run -pl web
```

The application starts Tomcat on port 8080. It exposes no endpoints yet, so you will only see the startup log. Stop it with `Ctrl+C`.

To run the packaged jar instead:

```bash
mvn -B package -DskipTests
java -jar web/target/web-1.0-SNAPSHOT.jar
```

## Continuous integration

`.github/workflows/ci.yml` runs `mvn -B verify` with JDK 25 (Temurin) on every pull request and on every push to `main`.

Branch protection for `main` is configured by hand on GitHub (Settings > Branches), because it does not live in the code:

- Require a pull request before merging (no direct pushes to `main`).
- Require the `verify` status check (job of the `CI` workflow) to pass. The check shows up in the list after the workflow has run once.

Work happens on one branch per phase with small pull requests.

## Design decisions

See [DESIGN.md](DESIGN.md).
