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
- Maven

### Scope

Compilable Java project containing the domain models, contracts (interfaces), services and use cases, along with their tests.
It does not include a REST API, real persistence, frontend, security or deployment.

## Structure

```
src/main/java/ar/edu/itba/dps/fieldops
├── business          # business rules (policies): never depends on providers
│   ├── models        # entities and value objects
│   ├── exceptions    # domain exceptions
│   └── providers     # interfaces the business needs from the outside world
├── providers         # implementations of those interfaces (details)
└── Main.java         # composition root: instantiates and wires everything
src/test/java/...     # unit and integration tests, mirroring the main tree
DESIGN.md             # design decisions (in Spanish)
```

## Build and test

```bash
mvn test      # compiles and runs the tests
mvn verify    # full build (includes the JDK 25 check)
```

## Design decisions

See [DESIGN.md](DESIGN.md).
