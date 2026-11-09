# Entrega 3 — Decisiones acordadas (02/11/2026)

Estado: decisiones tomadas antes de implementar, para validar con el equipo. Son la base de
`docs/entrega-3-plan.md` y de las entradas nuevas de `DESIGN.md` (D70 en adelante).

Consigna: `docs/fieldops-entrega-3.md`. Entrega: 09/11/2026.

## Punto de partida

Ya hay un evento, `IncidentRegistered`, publicado por `RegisterIncident` detrás del puerto `EventPublisher` y
entregado síncrono a `ProposeReplan` dentro de la misma transacción (D45, D68). La auditoría está en D48: la
propuesta guarda el incidente y la decisión, la expedición guarda la bitácora de incidentes y la original queda
intacta. La persistencia es PostgreSQL con JPA y una transacción por caso de uso (D67, D68). D45 dejó anotado
como deuda que, con persistencia real, la entrega tenía que pasar a un outbox.

## Arquitectura de eventos

| Tema | Decisión | Notas |
|---|---|---|
| Mecanismo asíncrono | **Outbox en PostgreSQL** | El evento se guarda en la misma transacción que el cambio y un despachador lo entrega después. No se pierde si la app se cae entre el commit y la entrega. Cierra la deuda de D45. Descartados: async en proceso (pierde eventos ante una caída) y un broker externo (infraestructura y tiempo que no hay). |
| Eventos de dominio | **Los tres de la consigna:** `ExpeditionApproved`, `IncidentRegistered` y `ActivityCompleted` | Son los hechos con más interesados. No se suman eventos sin un consumidor que los use. |
| Nombre del evento de incidente | **Se mantiene `IncidentRegistered`** | Es el `IncidentReported` de la consigna. Ya está en D45, el código y los tests; en `DESIGN.md` se aclara la equivalencia. |
| Quién genera los eventos | **La entidad los acumula** | La regla de cuándo ocurre cada hecho vive en el modelo: `Expedition` registra el evento al aprobar, al terminar una actividad o al registrar un incidente. El caso de uso guarda y después le pasa los eventos al puerto `EventPublisher`, sin saber cuáles son. El adaptador solo los guarda y los entrega. Revisa D45, que lo descartó porque no había `save`. |
| Qué lleva cada evento | **`EventId`, `occurredAt`, id de la expedición y los datos mínimos del hecho** | Un `record` inmutable en `business.models.events`, con value objects del dominio. `EventId` es un value object del dominio: identifica el hecho, que ocurrió una sola vez. `occurredAt` sale del hecho y no de un `now()`: el fin de la actividad, el momento del incidente y, para la aprobación, un `approvedAt` nuevo en el comando de aprobar. La conversión a JSON vive en el adaptador del outbox. Sin versión de esquema. |
| Publicación desacoplada | **`RegisterIncident` devuelve el incidente, no la propuesta** | La consigna pide que el código que produce el hecho no conozca a sus consumidores. Hoy `RegisterIncident` busca la propuesta que armó `ProposeReplan` y falla si no existe. Pasa a devolver el incidente (`POST /incidents` sigue respondiendo 201, con el incidente). Como la propuesta sigue siendo síncrona, ya existe al volver, y el cliente la consulta en `GET /api/expeditions/{id}/replan-proposals`. El frontend no cambia, porque no usaba la respuesta; `demo.http` toma el id de la propuesta del paso que las lista. Revisa D45. Descartado: mantenerlo como acoplamiento aceptado. |

## Consumidores: qué es asíncrono y qué es síncrono

| Tema | Decisión | Notas |
|---|---|---|
| Consumidores asíncronos | **Notificaciones** y **avance del informe** | Notificaciones: ante cada uno de los tres eventos, un aviso para cada responsable de la expedición, guardado en la base (puerto propio con adaptador JPA) y consultable por API y frontend, sin mail ni servicio externo. Avance del informe: `ActivityCompleted` actualiza una proyección del avance de la expedición que muestra el informe. Así, `ActivityCompleted` tiene dos interesados, y eso justifica el evento ("varias partes a la vez", dice la consigna). |
| Dónde viven los consumidores | **En el dominio** | Las reglas de los consumidores (a quién se avisa, qué se proyecta) son de negocio y viven en `business`. El despachador, en el adaptador, solo entrega. La consigna pide que la lógica de negocio no se duplique en la capa nueva. |
| Propuesta de replanificación | **Sigue síncrona** (`ProposeReplan`, D45) | El responsable necesita las opciones en el momento del incidente. Es el punto que se justifica en la presentación como "llamada síncrona a propósito". Si `ProposeReplan` falla, se deshace también el incidente (D68). Descartado: pasarla a asíncrona, que cambiaba la API, el frontend, `demo.http` y los tests de F3. |
| Convivencia síncrono / asíncrono | **Dos interfaces de suscriptor en el dominio** | `EventSubscriber` sigue siendo el síncrono, dentro de la transacción, y se suma una interfaz para los asíncronos. El publicador entrega a los síncronos y guarda el evento en el outbox; el despachador entrega a los asíncronos. El tipo del suscriptor dice qué garantía tiene. Descartados: todo por el outbox (contradice que la propuesta siga síncrona) y que lo decida la configuración (el dominio no sabría qué garantía tiene cada consumidor). |

## Registro de eventos y auditoría

| Tema | Decisión | Notas |
|---|---|---|
| Qué se registra | **Todos los eventos emitidos**, tengan o no consumidores asíncronos | La consigna dice "los eventos emitidos quedan registrados". El registro no se borra. |
| Tablas | **Una tabla de eventos (el registro) y una de entregas por consumidor** | La de eventos es inmutable. La de entregas tiene una fila por evento y consumidor asíncrono, con su estado (pendiente, entregado, fallido), los intentos y el último error. Hace falta porque un mismo evento puede entregarse bien a un consumidor y fallar en otro. Revisa la idea inicial de una sola tabla. |
| Consulta | **`GET /api/expeditions/{id}/events` + sección "Eventos" en la página de la expedición** | La línea de tiempo de la expedición, con el estado de entrega de cada consumidor. La lectura pasa por un puerto de salida del dominio y un caso de uso de consulta, así el endpoint no depende de JPA. |
| Integración con la auditoría | **El informe de la expedición incluye la línea de tiempo de eventos** | El informe ya reúne los incidentes y las advertencias aceptadas (D48). Sumarle los eventos los integra a la auditoría existente en vez de dejarlos aparte. Descartados: que alcance con el endpoint y la sección, y que solo la propuesta muestre el evento que la originó. |

## Garantías y fallas

| Tema | Decisión | Notas |
|---|---|---|
| Entrega | **Al-menos-una-vez** | Es la consecuencia del outbox: el despachador puede intentar entregar dos veces el mismo evento. |
| Duplicados | **Los evita el despachador**, con la tabla de entregas | No vuelve a entregar un par evento-consumidor ya entregado. El consumidor y la marca de la entrega corren en la misma transacción, porque los dos escriben en la misma base, así que una caída en el medio deshace las dos cosas y no duplica el efecto. Ningún consumidor tiene que acordarse de deduplicar. Descartados: deduplicar en cada consumidor y en los dos lugares. |
| Orden | **Por expedición y por consumidor** | Los eventos de una misma expedición le llegan a cada consumidor en el orden en que ocurrieron. Entre expediciones no se garantiza orden. Descartado: orden global estricto, porque un evento que falla frenaría a todos. |
| Orden ante una falla | **Los posteriores esperan** | Mientras un evento se reintenta para un consumidor, los siguientes de esa expedición esperan para ese consumidor hasta que el evento se entregue o quede `FAILED`. Solo se traba esa expedición y ese consumidor, y por poco tiempo. Descartado: saltear al que falla, que rompe el orden. |
| Falla de un consumidor asíncrono | **Reintentos con espera creciente y un límite** | Al agotarse, la entrega queda `FAILED` con su error, visible en el registro. La operación original no se deshace. Descartados: reintento sin límite y sin reintentos. |
| Falla del consumidor síncrono | **Se deshace la operación** | Si `ProposeReplan` falla, se deshace también el incidente (D68). Es la diferencia de garantía entre las dos interfaces de suscriptor. |
| Despachador | **`@Scheduled` cada N segundos, en `web`** | Lee las entregas pendientes. El intervalo va en `application.yml`. En los tests se invoca a mano. Descartado: entregar al commit con el `@Scheduled` de respaldo. |

## Alcance funcional

| Tema | Decisión | Notas |
|---|---|---|
| Estado del seguimiento en el itinerario | **Se incluye** | Hoy la API registra inicio, fin, resultado y observaciones pero no los devuelve. Se agregan a la respuesta del itinerario, con un badge en el frontend y los botones de seguimiento según el estado. Lo necesita la demo de `ActivityCompleted`. Se mantiene chico: la consigna pide no sumar funcionalidades ajenas a la propuesta de valor. |
| Frontend | **Secciones "Eventos" y "Notificaciones" en la página de la expedición** | Consume solo la API, como D69. |

## Tests y demo

| Tema | Decisión | Notas |
|---|---|---|
| Cómo probar lo asíncrono | **El despachador se invoca a mano, más un test de punta a punta con Awaitility** | Los tests a mano son deterministas y sin esperas. El de Awaitility prueba el `@Scheduled` real. |
| Qué se prueba | Lo de la lista de abajo | Cubre lo que pide la consigna, incluido "qué ocurre si el consumidor falla", para los dos tipos de consumidor. |
| Falla en la demo | **Se muestra con los tests y se explica con el registro** | No se agrega código que exista solo para la demo (criterio de D50). |
| Guion | **Se extiende `demo.http`** | Se suman los pasos de eventos: aprobar, ver el evento y las notificaciones, terminar una actividad, ver el avance en el informe y la línea de tiempo. `DemoScriptApiTest` lo sigue en el CI, como en la Entrega 2. |

Qué se prueba:

- Que cada operación emite su evento.
- El efecto de cada consumidor asíncrono.
- Que un consumidor asíncrono que falla reintenta, queda `FAILED`, no deshace la operación y no frena al otro consumidor.
- Que los eventos posteriores de esa expedición esperan.
- Que un evento entregado dos veces no duplica el efecto.
- Que si la transacción se deshace, el evento no queda registrado.
- Que si el consumidor síncrono falla, se deshace la operación.

## Supuestos y consecuencias

- **La aplicación corre en una sola instancia.** Con dos instancias, dos despachadores podrían tomar la misma entrega. Queda como supuesto para las preguntas de la presentación.
- **Agregar un tipo de evento no puede obligar a tocar un `switch`** en el adaptador que pasa los eventos a JSON. Tiene que seguir siendo "agregar una clase".
- **`DESIGN.md` registra los patrones no aplicados con sus consecuencias:** broker de mensajería, Event Sourcing (ya está en la tabla), CQRS y Saga. También registra **el costo del cambio**, qué clases existentes se tocaron. La consigna pide presentar "qué costo tuvo y qué valor agrega".

## Para la presentación

La consigna pide mostrar el flujo completo de un evento y discutir sus consecuencias. Con estas decisiones:

- **Flujo:**
  1. La entidad registra el hecho.
  2. El caso de uso guarda y publica.
  3. En la misma transacción, el publicador entrega a los síncronos y registra el evento con una entrega pendiente por cada consumidor asíncrono.
  4. El despachador entrega a notificaciones y al informe.
  5. El registro y el informe muestran cómo quedó cada entrega.
- **Síncrono vs. eventos:** la propuesta de replanificación es síncrona porque el responsable decide en el momento, y si falla se deshace el incidente. Notificaciones y avance son asíncronos porque pueden llegar después y no deben frenar ni deshacer la operación.
- **Consistencia eventual:** el aviso y el avance aparecen unos segundos después de aprobar o terminar.
- **Orden:** se garantiza por expedición y consumidor. Ante una falla, los siguientes esperan.
- **Reintentos:** hay un límite, y después la entrega queda `FAILED` y visible, sin frenar al otro consumidor.
- **Duplicados:** son posibles con al-menos-una-vez, y el despachador los evita, porque el efecto y la marca de la entrega se confirman juntos.

## Pendiente de definir

- Reparto de tareas entre los cinco integrantes.
- Qué decisiones se presentan en el video y quién habla de cada una (`docs/entrega-3-presentacion.md`).
- Valores concretos: intervalo del despachador, cantidad de reintentos y espera entre ellos.

Puntos a resolver al implementar:

- Quién genera el `EventId` (la entidad con un `UUID` o un proveedor de ids, como `ExpeditionIdProvider`) y cómo se toman los eventos pendientes de la entidad después del `save`.
- La forma del comando de aprobar con `approvedAt`, que cambia `ApproveExpeditionPort`, la API y `demo.http`. La alternativa es un puerto de reloj en el dominio, que no cambia la API.
- Qué guarda exactamente la proyección del avance del informe, sin duplicar la lógica de `ExpeditionReporter`. Por ejemplo, que la proyección sea la línea de tiempo de lo terminado y el informe la lea, en vez de volver a calcular el consumo.

## Fase 5 (09/11/2026)

| Tema | Decisión | Notas |
|---|---|---|
| Perfiles | **`prod` y `demo`, con `prod` como perfil por defecto** | Sin perfil, la aplicación va a PostgreSQL (`spring.profiles.default: prod`). Si no hubiera default, H2 en el classpath haría que un arranque sin perfil levantara una base embebida vacía sin avisar. |
| Base de `prod` | **PostgreSQL de Compose, vacía al principio y persistente** | Solo corren las migraciones, sin seed. Los cambios quedan en el volumen `fieldops-data`, entre reinicios de la aplicación y del contenedor. |
| Base de `demo` | **H2 en memoria, con el seed de siempre y cambios efímeros** | Corre sin Docker. Al arrancar carga `demo/catalog.json` y `demo/expedition.json`. Lo que se cambia se pierde al reiniciar, y el próximo arranque vuelve al seed. Usa las mismas migraciones que `prod`, en modo PostgreSQL y con `NON_KEYWORDS=VALUE`, porque `value` es una palabra reservada de H2. |
| Qué prueba cada base | **`DemoScriptApiTest` corre en H2 y el resto en PostgreSQL** | La CI recorre la demo sobre la base con la que se muestra. Las garantías del outbox se siguen probando en PostgreSQL con Testcontainers. |
| Cómo espera la demo al despachador | **Nota de "ESPERAR" en `demo.http`** (decisión 5.3) | Va en cada consulta asíncrona que sigue a una operación. La espera sirve para explicar la consistencia eventual. |

Consecuencia para la presentación: la demo no muestra que el outbox sobreviva a una caída, porque con H2 en memoria se pierde todo al reiniciar. Esa garantía es de `prod`.

## Cambios al implementar

Lo que este documento dice arriba y terminó distinto. Las decisiones están en `docs/entrega-3-plan.md`.

- **`POST /incidents` sigue respondiendo con la propuesta y su `Location`**, no con el incidente (decisión 1.1). `RegisterIncidentPort` sí devuelve el `Incident`, y es el controller el que busca la propuesta. `demo.http` y el frontend no cambiaron por esto.
- **`approvedAt` lo pone el controller con `LocalDateTime.now()`** (decisión 1.2). El cuerpo de la aprobación y `demo.http` no cambiaron.
- **La demo corre en H2, sin `docker compose up`** (ver "Fase 5"). Revisa la idea inicial de mostrarla sobre PostgreSQL.
- **La presentación es un video de YouTube sin listar, de 8 a 12 minutos, con los cinco hablando de una parte distinta.** Reemplaza la presentación en clase de un integrante elegido al azar. Lo informó Tobías el 09/11/2026.
