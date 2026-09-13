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
src/main/java/...   # domain: models, services, use cases, interfaces
src/test/java/...   # unit and integration tests
DESIGN.md           # design decisions (in Spanish)
```

## Build and test

```bash
mvn test      # compiles and runs the tests
mvn verify    # full build (includes the JDK 25 check)
```

## Design decisions

See [DESIGN.md](DESIGN.md).
