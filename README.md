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

What exists today:

```
src/main/java/ar/edu/itba/dps/fieldops
└── business            # business rules (policies): never depends on providers
    ├── models          # entities and value objects
    │   ├── activities  # Activity, its rules contract and its requirements
    │   ├── common      # TimePeriod, Quantity, MeasurementUnit
    │   ├── resources   # people, vehicles, instruments, consumables
    │   └── zones       # zones and permits
    ├── rules           # ActivityRules implementations, one per activity type
    └── exceptions      # domain exceptions
src/test/java/...       # unit tests, mirroring the main tree
DESIGN.md               # design decisions (in Spanish)
```

Planned for when the project grows past a pure domain module — none of this exists yet:

```
business/providers      # interfaces the business needs from the outside world
providers               # implementations of those interfaces (details)
Main.java               # composition root: instantiates and wires everything
```

## Build and test

```bash
mvn test      # compiles and runs the tests (the JDK 25 check runs first, in validate)
mvn verify    # full build
```

## Design decisions

See [DESIGN.md](DESIGN.md).
