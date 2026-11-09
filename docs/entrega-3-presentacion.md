# Entrega 3 — Presentación en video

Estado: propuesta para debatir con el equipo. Lo que se acuerde pasa a `docs/entrega-3-plan.md` (decisiones 5.2 y 5.4).

## Formato

Tobías informó este formato, que actualiza lo que dice `docs/fieldops-entrega-3.md` sobre la presentación oral:

> "un video de youtube (unlisted) con una presentación de entre 8 y 12 minutos donde se muestren todos los integrantes del equipo hablando de al menos una parte diferente del sistema cada uno. La estética y organización queda a libre ideación y diseño de cada uno. Puede ser simplemente una llamada de meet grabada y presentada correctamente o puede grabarse y editar video si alguien tiene ese conocimiento."

**Propuesta:** una Meet grabada, sin edición, compartiendo pantalla, en la que cada integrante habla de al menos una decisión. Se sube a YouTube como video sin listar.

Falta confirmar la fecha límite del video.

## Qué se debate

De las 10 decisiones de abajo se eligen 5 o 6, y quién presenta cada una. La fase es una pista de quién la conoce mejor, no una asignación.

| # | Decisión | Parte del sistema | Fase | ¿Entra? | Quién |
| --- | --- | --- | --- | --- | --- |
| 1 | Hexagonal con módulos Maven | Arquitectura | Entrega 2 | | |
| 2 | Tipos de actividad por composición, la nocturna como Decorator | Dominio | Entregas 1 y 2 | | |
| 3 | El itinerario es un árbol | Dominio | Entrega 2 | | |
| 4 | Validaciones como objetos y aprobación con advertencias aceptadas | Dominio | Entrega 1 | | |
| 5 | Replanificar crea una alternativa y deja la original intacta | Replanificación | Entrega 2 | | |
| 6 | Entidades JPA propias y una transacción por caso de uso | Persistencia | Entrega 2 | | |
| 7 | La expedición acumula sus eventos y se publican al guardarla | Eventos | Entrega 3, Fase 1 | | |
| 8 | Outbox en PostgreSQL | Eventos | Entrega 3, Fase 2 | | |
| 9 | Propuesta síncrona, avisos y avance asíncronos | Eventos | Entrega 3, Fases 1 y 4 | | |
| 10 | Garantías de la entrega asíncrona | Eventos | Entrega 3, Fase 3 | | |

**Recomendación inicial:** una de dominio (la 2 o la 3), la 1, la 5, la 8, la 9 y la 10. La 8, la 9 y la 10 cubren lo que la consigna pide discutir sobre eventos. La 7 es la primera suplente, porque explica quién emite el evento, y se puede resumir dentro de la 8 si no entra.

## Las 10 candidatas

### 1. Hexagonal con módulos Maven (D35, D36)

- **Problema:** una arquitectura hexagonal solo sirve si la dependencia hacia adentro no se puede romper por descuido. En un solo módulo, cualquier clase puede importar cualquier otra.
- **Decisión:** tres módulos, `domain`, `app` y `web`, con las dependencias apuntando al dominio. El enforcer prohíbe Spring en `domain`, también si llega de forma transitiva.
- **Alternativas descartadas:** un solo módulo con paquetes y un test de arquitectura, porque la regla se verifica recién al correr los tests y se puede debilitar. Clean Architecture, porque sus anillos no se distinguen del hexágono en un dominio de este tamaño.
- **Consecuencia:** hay tres `pom` que mantener y cada cambio entre capas cruza un módulo. A cambio, si `domain` importa una clase de `app` o de `web`, no compila. ArchUnit queda para las reglas finas adentro de cada módulo.

### 2. Tipos de actividad por composición y la nocturna como Decorator (D1, D42, D44)

- **Problema:** cada tipo de actividad tiene sus propias reglas de duración, riesgo y recursos, y la variante nocturna se aplica a cualquier tipo.
- **Decisión:** `Activity` delega lo que varía en `ActivityRules` (Strategy). `NightOperationRules` envuelve cualquier regla y le suma iluminación, certificación, permiso de zona y un nivel de riesgo (Decorator). Un registro crea cada tipo por su nombre.
- **Alternativas descartadas:** herencia, un `enum` con `switch`, una clase nocturna por tipo y un booleano `night` con condicionales.
- **Consecuencia:** agregar un tipo o una variante es agregar una clase. Las validaciones, el estimador y el informe no preguntan si una actividad es nocturna. Son seis tipos, y otra variante se puede apilar sobre la nocturna.

### 3. El itinerario es un árbol (D39, D41)

- **Problema:** las actividades se agrupan en bloques secuenciales o paralelos, anidables, que tienen que validarse, estimarse e informarse como cualquier actividad.
- **Decisión:** Composite. `ItineraryNode` responde igual para una actividad y para un bloque. `SequentialBlock` y `ParallelBlock` combinan a sus hijos de forma distinta. Dos ramas paralelas no pueden compartir un recurso reutilizable aunque sus horarios no se pisen.
- **Alternativas descartadas:** un solo bloque con un `enum` de modo, el bloque como subtipo de `Activity` y una lista plana con un id de grupo.
- **Consecuencia:** las validaciones, las reservas, el seguimiento y el informe siguen viendo las hojas, y no cambiaron. Saber si dos actividades corren en paralelo recorre el árbol por cada par, algo que con este tamaño no pesa.

### 4. Validaciones como objetos y aprobación con advertencias aceptadas (D2, D22, D31)

- **Problema:** una expedición con errores críticos no puede aprobarse, y las advertencias se pueden aceptar dejando la justificación. Una primera versión permitía aprobar pasando un validador vacío.
- **Decisión:** cada regla es un objeto que devuelve hallazgos con su severidad. `Expedition.approve` corre siempre las siete reglas obligatorias, y quien llama solo puede sumar reglas. Cada advertencia la acepta un responsable, con justificación.
- **Alternativas descartadas:** todos los chequeos en un método de `Expedition`, recibir el validador desde afuera y reservar los recursos al asignarlos.
- **Consecuencia:** una regla nueva es una clase nueva. No hay forma de aprobar salteando el mínimo, se llame al caso de uso o a la entidad.

### 5. Replanificar crea una alternativa y deja la original intacta (D25, D33, D47, D48)

- **Problema:** ante un incidente hay que proponer otro plan, y el plan original, el incidente y la decisión tienen que quedar auditados.
- **Decisión:** aceptar una opción crea otra expedición en borrador, que pasa por revisión y aprobación. La original queda suspendida y, al aprobarse la alternativa, reemplazada. La propuesta guarda el incidente, las opciones y quién decidió qué.
- **Alternativas descartadas:** modificar la original en el lugar, reprogramar de forma automática y copiar el itinerario adentro de la propuesta.
- **Consecuencia:** la auditoría no necesita un mecanismo aparte, porque la original sigue consultable. La alternativa hereda la bitácora de incidentes, y aceptar exige cerrar antes las actividades en curso.

### 6. Entidades JPA propias y una transacción por caso de uso (D65, D67, D68)

- **Problema:** hacía falta persistencia real sin que el dominio supiera que existe una base.
- **Decisión:** el dominio expone snapshots y el adaptador los traduce a sus propias entidades. Un proxy abre una transacción por caso de uso, y `@Version` detecta escrituras concurrentes, que la API responde con 409.
- **Alternativas descartadas:** anotar el dominio con JPA, poner `@Transactional` en los casos de uso, guardar la expedición como un documento JSON y usar H2 para los tests.
- **Consecuencia:** `domain` sigue sin frameworks. Un campo nuevo también hay que sumarlo al snapshot, y los tests de `app` necesitan Docker. La demo usa H2 (D82), así que ese es el único recorrido que no corre sobre PostgreSQL.

### 7. La expedición acumula sus eventos y se publican al guardarla (D70, D73)

- **Problema:** quien produce el hecho no tiene que conocer a los consumidores, y un caso de uso nuevo no se puede olvidar de publicar.
- **Decisión:** `Expedition` registra el evento al final de cada operación. `PublishingExpeditionRepository`, un Decorator del repositorio, lo publica al guardar. `RegisterIncident` pasó de tres dependencias a una.
- **Alternativas descartadas:** publicar desde cada caso de uso, publicar desde el repositorio JPA y usar `@DomainEvents` de Spring Data.
- **Consecuencia:** una operación que falla no deja eventos. El costo es que, leyendo un caso de uso, no se ve que publique algo.

### 8. Outbox en PostgreSQL (D74)

- **Problema:** si el evento no se guarda junto con el cambio que lo produjo, una caída entre el commit y la entrega lo pierde sin dejar rastro.
- **Decisión:** el evento y una entrega pendiente por consumidor se guardan en la misma transacción que el cambio, en dos tablas: `domain_event` y `event_delivery`. El orden lo da `sequence`, no `occurredAt`.
- **Alternativas descartadas:** async en proceso, que pierde eventos ante una caída. Un broker, que suma infraestructura y no da la atomicidad entre el cambio y el evento. Publicar después del commit y usar una sola tabla.
- **Consecuencia:** todo hecho queda registrado aunque nadie lo escuche, y el registro se integra a la auditoría. Con H2 en memoria, la demo no muestra esta durabilidad: es una garantía de `prod`.

### 9. Propuesta síncrona, avisos y avance asíncronos (D45, D72, D79, D80)

- **Problema:** un mismo hecho tiene interesados con necesidades distintas. El responsable necesita las opciones en el momento, y un aviso puede llegar después.
- **Decisión:** dos interfaces de suscriptor. `EventSubscriber` corre adentro de la transacción: si `ProposeReplan` falla, se deshace el incidente. `AsyncEventSubscriber` lo entrega el despachador: si `NotifyResponsibles` o `TrackProgress` fallan, la operación no se deshace.
- **Alternativas descartadas:** todo asíncrono, porque la propuesta tiene que existir cuando vuelve el registro del incidente. Una sola interfaz con un indicador, y que la garantía la decida la configuración.
- **Consecuencia:** consistencia eventual: avisos y avance aparecen unos segundos después. El tipo del suscriptor dice qué garantía tiene, a costa de repetir dos firmas.

### 10. Garantías de la entrega asíncrona (D77, D78)

- **Problema:** con eventos hay que responder qué pasa con el orden, los reintentos y los duplicados.
- **Decisión:** un `@Scheduled` cada 2 s. Entrega al menos una vez, con el efecto y la marca en la misma transacción. Orden por expedición y por consumidor. 5 intentos con esperas de 1, 2, 4 y 8 s, y después `FAILED`, visible en el registro.
- **Alternativas descartadas:** deduplicar en cada consumidor, orden global estricto y saltear al evento que falla.
- **Consecuencia:** mientras un evento se reintenta, los siguientes de esa expedición esperan para ese consumidor, unos 25 s en el peor caso. Un efecto fuera de la base, como un mail, sí se podría duplicar. Hoy ningún consumidor lo tiene. Una sola instancia es un supuesto.

## Guion propuesto

Unos 10:30, con margen dentro de los 8 a 12 minutos.

| Tramo | Tiempo |
| --- | --- |
| Dominio: problema, actores y reglas centrales. La audiencia no lo conoce | 1:30 |
| 6 decisiones, 1:15 cada una (con 5 decisiones, 1:30 cada una) | 7:30 |
| Demo corta: aprobar, ver el evento, su entrega y el aviso | 1:30 |

**Reparto:** con 5 decisiones, cada integrante presenta una, y dos de ellos suman el dominio y la demo. Con 6, uno presenta dos decisiones. Cada persona habla de una parte distinta.

## Qué tiene que quedar cubierto

Sale de la consigna, y conviene revisarlo al cerrar la lista:

- El flujo completo de un evento: quién lo emite, quién lo consume y qué garantías tiene la entrega (7, 8 y 9).
- Dónde se eligieron eventos y dónde llamadas síncronas, y por qué (9).
- Consistencia eventual, orden, reintentos y duplicados (9 y 10).
- Qué costo tuvo y qué valor agrega (sección "Propuesta de valor" de `DESIGN.md`).
- Los cinco integrantes en pantalla, y entre 8 y 12 minutos.

## Recomendaciones

Son un punto de partida para la reunión. El equipo decide.

- **Elegir 5 decisiones: 1, 5, 8, 9 y 10.** Con 5, cada integrante presenta una y el reparto queda parejo, con 1:30 por decisión.
- **Si se eligen 6, sumar la 2.** Muestra la extensibilidad del dominio con un ejemplo que después aparece en la demo: el buceo nocturno. Se prefiere a la 3, y el tramo de dominio puede mencionar los bloques del itinerario.
- **Mantener la 8, la 9 y la 10 en cualquier combinación.** Cubren lo que la consigna pide discutir sobre eventos.
- **Dejar la 7 como primera suplente.** Si no entra, se resume dentro de la 8 para explicar quién emite el evento.
- **Decir en la 8 que la demo no muestra la durabilidad del outbox.** La demo corre en H2 en memoria, y esa garantía es de `prod`.
- **Repartir según quién conoce cada parte:**

| Tramo | Quién lo conoce mejor |
| --- | --- |
| 8. Outbox en PostgreSQL | Responsable de la Fase 2 |
| 9. Propuesta síncrona, avisos y avance asíncronos | Responsable de la Fase 1 o de la Fase 4 |
| 10. Garantías de la entrega asíncrona | Responsable de la Fase 3 |
| 1 o 5, y el dominio | Responsable de la Fase 1 o de la Fase 4, el que no presente la 9 |
| 1 o 5, y la demo | Responsable de la Fase 5 |

- **Confirmar la fecha límite del video antes de fijar cuándo se graba.**
- **Cronometrar cada tramo en una pasada previa.** La duración es un requisito, y los tiempos del guion dejan 1:30 de margen hasta los 12 minutos.
