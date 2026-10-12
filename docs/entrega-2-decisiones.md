# Entrega 2 — Decisiones acordadas (11/10/2026)

Estado: decisiones tomadas antes de implementar. Sirven de base para las entradas nuevas de `DESIGN.md` (D35 en adelante).

## Arquitectura e infraestructura

| Tema | Decisión | Notas |
|---|---|---|
| Arquitectura | **Hexagonal** | `business` pasa a ser el hexágono; `business.providers` pasan a ser puertos de salida (driven); los casos de uso son los puertos de entrada (driving). Adaptadores en `adapters/driving` (REST) y `adapters/driven` (persistencia). |
| Módulos Maven | **Multi-módulo** (domain / app / web) | El compilador impide que el dominio importe Spring. Hay que mover paquetes y tests. |
| API REST | **Incluida en el alcance de la entrega** | La consigna la exige como requisito no funcional y entregable (código de la aplicación y contratos de la API en el README). Implica sumar Spring Boot y el módulo `web`, con los adaptadores driving. |
| Persistencia | **Postergada: se sigue con in-memory** | Se decide después de la clase de Spring Data. Queda pendiente: base (H2 + JPA era la opción recomendada), mapeo (entidades JPA separadas + mappers era la recomendada) y tests de repositorio contra persistencia real. |
| Frontend | **Pendiente de definir** | La consigna pide lo mínimo indispensable. La opción recomendada era Thymeleaf; no está confirmada. |
| CI | **GitHub Actions + branch protection** | Workflow con JDK 25 que corre `mvn verify` en cada PR, y regla en GitHub que exige que pase para mergear. Documentar la regla en el README, porque no queda en el código. No depende de Spring, se puede hacer ya. |

## Funcionalidades nuevas

| Funcionalidad | Decisión |
|---|---|
| F1 — Nocturna | **Decorator sobre `ActivityRules`** (`NightOperationRules`): envuelve las reglas de cualquier actividad base y suma certificación nocturna para todo el personal, permiso especial por zona, iluminación y factor de riesgo. Sin condicionales por tipo. |
| F2 — Bloques | **Composite con interfaz común** entre actividad (hoja) y bloque (compuesto). Duración: suma en secuencial, máximo en paralelo. La validación detecta conflictos de recursos entre actividades paralelas del mismo bloque. Implica refactorizar `Itinerary`, `ItineraryItem` y `ResourceOverlapValidation`. |
| F3 — Replanificación automática | **Evento de dominio `IncidentRegistered` + `EventPublisher` (puerto del dominio), entrega síncrona en memoria**, así al volver el registro del incidente la propuesta ya existe. **Se generan varias propuestas** (reprogramación, reemplazo de recursos, cancelación parcial) y el responsable elige o rechaza. Auditoría: plan original, incidente y decisión consultables. |

Punto a resolver al implementar F3: D33 descartó elegir automáticamente el reemplazo de un recurso por falta de criterio en el enunciado. Al generar varias propuestas hay que definir qué candidatos ofrece el catálogo y documentarlo en `DESIGN.md`.

## Demo

Se hacen las tres piezas, que se complementan. No se crean clases de Java solo para la demo: los datos viven en archivos y el guion se ejecuta contra la API.

| Pieza | Qué es | Para qué sirve | Depende de |
|---|---|---|---|
| **1. Seed como datos** | Archivos (por ejemplo `demo/catalog.json` y `demo/expedition.json`) y **un único cargador genérico** que los lee al arrancar con el perfil `demo` y los traduce a objetos de dominio con los builders existentes. | Carga el catálogo y los datos de partida de la demo de forma repetible. Es lo que deja lista la base para el guion. Cuando haya persistencia real se reemplaza por `data.sql` o una migración sin tocar el dominio. | Nada nuevo: se puede escribir y probar ya, sin Spring. |
| **2. Script de requests contra la API REST** | Un `demo.http` (IntelliJ / VS Code) o un `demo.sh` con curl, que cuenta la historia paso a paso. | Es el guion repetible de la demo. Recorre: crear la expedición, intentar aprobar y ver el error crítico, aceptar la advertencia, aprobar, ejecutar, registrar el incidente y elegir una propuesta. | La API REST, que es parte del alcance. |
| **3. Swagger / OpenAPI (springdoc)** | Documentación generada de la API, con interfaz para probar los endpoints. | Cubre el entregable "contratos de la API" del README y permite recorrer la demo con clicks. No reemplaza al script, porque no es repetible por sí solo. | La API REST, que es parte del alcance. |

Relación entre las tres: el seed (1) deja cargado el catálogo, el script (2) ejecuta la historia contra la API, y Swagger (3) documenta los contratos y sirve para explorar.

Requisitos de la consigna para la demostración: 10 o más actividades, seis tipos (hoy hay dos: buceo y recolección de muestras; faltan cuatro, más la nocturna), un bloque compuesto, recursos compartidos, un error crítico, una advertencia aceptable y una replanificación originada en un incidente. Los tipos de actividad nuevos son reglas de dominio que la consigna exige, no clases de demo.

## Pendiente de definir

- Persistencia real (después de la clase de Spring Data) y frontend.
- Fecha real de entrega: el calendario de la clase 8 marca el martes 13 como "Entrega 2 TPI" y la clase de Rest API es el 20/10. Conviene confirmarla con el docente.
- Formato del script de la demo: `demo.http` o `demo.sh`.
- Cuáles son los cuatro tipos de actividad que faltan para la demo.
- Reparto de tareas entre los cinco integrantes.
