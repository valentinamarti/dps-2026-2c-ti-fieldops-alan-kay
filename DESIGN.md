# Decisiones de diseño — Grupo Alan-Kay

## Modelo de dominio

`Expedition` es la entidad central: tiene objetivos, período, zonas de trabajo, responsables, permisos, restricciones, un itinerario (lista ordenada de actividades programadas) y un estado (borrador / revisada / aprobada / en ejecución / suspendida / finalizada). Se apoya en un catálogo de recursos (personas, vehículos, instrumentos, consumibles) y en un catálogo de zonas.

Cada actividad del itinerario referencia una `Activity` (con sus reglas propias según el tipo — duración, riesgo, recursos y personal requerido) más el horario real y los recursos asignados para esa expedición puntual. Antes de poder aprobarse, una expedición se corre contra un conjunto de reglas de validación (superposiciones, recursos faltantes, certificaciones, permisos, capacidad); las críticas bloquean la aprobación, las advertencias se pueden aceptar dejando una justificación registrada.

## Patrones y principios aplicados

### D1 — Tipos de actividad: composición en vez de herencia (Strategy)

- **Patrón / principio:** Strategy (Open/Closed).
- **Dónde:** `Activity` + interfaz `ActivityRules` (en `business.models.activities`, junto a quien la usa); implementaciones `SampleCollectionRules` y `DivingRules` en `business.rules`.
- **Por qué:** `Activity` es una única clase con los campos comunes obligatorios a todo tipo (nombre, ventana temporal, zona, dependencias, restricciones), y delega en `ActivityRules` todo lo que varía según el tipo: duración estimada, riesgo, recursos y personal requerido. El enunciado pide que cada tipo tenga sus propias reglas, y agregar un tipo nuevo debe significar agregar una clase, no modificar un `if`/`switch` existente.
- **Alternativas descartadas:** herencia (subclases de `Activity` por tipo) — mezclaría campos comunes con comportamiento variable en la misma jerarquía. `enum` + `switch` por tipo — antipatrón señalado en clase, viola Open/Closed.


### D2 — Validaciones como objetos con severidad (Policy) + orquestador

- **Patrón / principio:** Policy.
- **Dónde:** interfaz `ActivityRules`, sus implementaciones concretas, y `ValidationOrchestrator`.
- **Por qué:** cada regla (superposición de horarios, recursos faltantes, certificaciones ausentes, permisos faltantes, exceso de capacidad) es un objeto independiente que devuelve severidad (crítica / advertencia) además de si aplica. El orquestador corre todas las reglas y separa resultados en críticas y advertencias, que es exactamente lo que exige el enunciado para decidir si una expedición puede aprobarse.
- **Alternativas descartadas:** dos interfaces separadas (una para críticas, otra para advertencias) — obligaría a duplicar una regla si su severidad pudiera depender del contexto. Lista de validaciones sueltas sin severidad (boceto inicial) — no permite distinguir qué bloquea la aprobación de qué no.


### D3 — Builder para Expedición, con validación de negocio separada

- **Patrón / principio:** Builder + separación de responsabilidades (SRP).
- **Dónde:** `ExpeditionBuilder` + `Expedition`.
- **Por qué:** `Expedition` tiene muchas colecciones y campos opcionales (zonas, actividades, permisos, restricciones), lo que hace ilegible un constructor tradicional. El builder solo valida en `build()` que no falten los datos obligatorios; la validación de negocio pesada corre aparte con el `ValidationOrchestrator` (D2) antes de permitir el paso a "Approved" — así el builder no termina siendo responsable de todo.
- **Alternativas descartadas:** Builder + Director — más estructura de la que hace falta para este caso. Validar todas las reglas de negocio dentro de `build()` — mezclaría "está bien formado" con "es válido para operar", que tienen ciclos de vida distintos.


### D4 — Estimación como agregación, Informe como caso de uso que reutiliza

- **Patrón / principio:** evitar duplicación (DRY) reutilizando Strategy de D1.
- **Dónde:** `ExpeditionEstimator` e `ExpeditionReporter`.
- **Por qué:** la estimación de una expedición (riesgo, duración, consumo) itera sus actividades y delega en la `ActivityRules` de cada una — no repite lógica de cálculo. El informe reutiliza ese mismo servicio y le suma los datos de seguimiento (incidentes, observaciones, resultados) para armar un `ExpeditionReport`: un objeto de solo lectura, sin builder, porque no protege ningún invariante de negocio.
- **Alternativas descartadas:** calcular el resumen de riesgos del informe con lógica propia, independiente de `ExpeditionEstimator` — duplicaría reglas de cálculo. Builder para `ExpeditionReport` — no tiene invariantes que proteger, es una proyección de datos ya validados en otro lado.


### D5 — Actividad de catálogo vs. instancia puntual de una expedición

- **Patrón / principio:** separación entre entidad de catálogo y su instancia en un contexto puntual (reificación).
- **Dónde:** `Activity` (con sus datos generales y su `ActivityRules`) y el ítem de itinerario que la referencia dentro de una expedición concreta (horario real, recursos asignados).
- **Por qué:** `Activity` define datos y reglas reutilizables (tipo, duración estimada, riesgo, recursos requeridos) que no cambian entre expediciones. Lo que sí es específico de una expedición puntual es cuándo se hace realmente y qué recursos concretos se le asignaron — eso vive en una instancia aparte, no en la propia `Activity`.
- **Alternativas descartadas:** una sola clase `Activity` con todos los campos juntos (los generales y los específicos de la expedición) — generaría campos que a veces están completos (cuando está programada) y a veces no (cuando es solo de catálogo), con estados inconsistentes.


### D6 — El itinerario como la colección ordenada de actividades de una expedición

- **Patrón / principio:** encapsulamiento de colección.
- **Dónde:** `Expedition.itinerary`.
- **Por qué:** cada expedición tiene su propio itinerario, que es el conjunto ordenado de instancias de actividad (D5) programadas para esa expedición. Es a través del itinerario que se accede a las actividades de una expedición — no hay una lista de actividades suelta aparte. Esto mantiene el orden y las dependencias en un solo lugar, y es lo que recorren tanto las validaciones (D2) como la estimación (D4).
- **Alternativas descartadas:** que `Expedition` tenga una lista de actividades sin un itinerario que las agrupe — perdería el lugar natural para el orden, las dependencias entre actividades programadas y los datos propios de cada instancia.

### D7 — Dos contratos de recurso: por tiempo y por stock

- **Patrón / principio:** Interface Segregation (ISP).
- **Dónde:** `Resource` (padre, solo `id()`), `ReusableResource` (`Person`, `Vehicle`, `Instrument`) y `DepletableResource` (`Depletable`).
- **Por qué:** un recurso reutilizable se reserva por franjas horarias (`isAvailableDuring`, `reserve`) y uno consumible se agota (`hasStockFor`, `consume`). Son preguntas distintas que hacen clientes distintos, así que cada una tiene su interfaz chica. `Resource` es el tipo común para cuando alcanza con identificar el recurso (ej. los recursos asignados a un ítem del itinerario). No se usa con `instanceof` para decidir cómo tratar cada tipo: para eso están las interfaces hijas.
- **Alternativas descartadas:** una única interfaz `Resource` con los cuatro métodos, que obligaría a implementar métodos sin sentido (un combustible no se "reserva" por horario; una persona no tiene stock). Converger después, si la cátedra confirma que es un único concepto, rompe menos código que separar una interfaz ya unificada.


### D8 — Reificación de conceptos del dominio en value objects

- **Patrón / principio:** value objects.
- **Dónde:** `TimePeriod`, `Quantity` + `MeasurementUnit`, `Certification`, `Permit`.
- **Por qué:** cada uno encapsula su regla en un solo lugar. `TimePeriod` resuelve la superposición como intervalo semiabierto `[inicio, fin)`, así que dos franjas que solo se tocan en el borde no chocan. `Quantity` usa `BigDecimal`, no admite negativos y no permite operar con unidades distintas. `Certification` y `Permit` evitan comparar `String` sueltos. Son `record` inmutables con igualdad por valor.
- **Alternativas descartadas:** `LocalDateTime` sueltos, `double` + `String` de unidad y listas de `String` para certificaciones y permisos. Esto repartiría la lógica de solapamiento y de unidades en cada clase que la use.


### D9 — Disponibilidad temporal compartida por composición

- **Patrón / principio:** composición sobre herencia, sin duplicación (DRY).
- **Dónde:** `AvailabilityCalendar`, usado por `Person`, `Vehicle` e `Instrument`.
- **Por qué:** los tres recursos reutilizables comparten exactamente la misma regla de disponibilidad (no reservar franjas superpuestas). Esa regla vive en `AvailabilityCalendar` y cada recurso delega en él, así que cambiarla es tocar una sola clase.
- **Alternativas descartadas:** clase abstracta base (`AbstractReusableResource`) con la lógica de reservas. Ata la jerarquía a un único eje de variación y complica que un recurso futuro tenga otra forma de disponibilidad. Copiar la lógica en cada clase quedó descartado por duplicación.


### D10 — Certificaciones como capacidad aparte de la disponibilidad

- **Patrón / principio:** Interface Segregation (ISP) + composición, mismo criterio que D9.
- **Dónde:** interfaz `Certifiable` y value object `Certifications`, usados por `Person`, `Vehicle` e `Instrument`.
- **Por qué:** personas, vehículos e instrumentos pueden tener certificaciones. `Certifiable` permite que la validación de certificaciones faltantes pregunte `hasCertification` sin conocer la clase concreta ni hacer `instanceof`. La regla vive en `Certifications`, así que si una certificación gana reglas propias (ej. vencimiento) se cambia en un solo lugar.
- **Alternativas descartadas:** agregar `hasCertification` a `ReusableResource`, que mezclaría dos preguntas distintas (disponibilidad y habilitación) y obligaría a cualquier recurso reutilizable futuro a tener certificaciones. Repetir un `Set<Certification>` con su lógica en cada clase quedó descartado por duplicación.


### D11 — Los datos propios de cada tipo de actividad viven en su regla; requisitos como value objects

- **Patrón / principio:** Strategy (D1) + value objects (D8).
- **Dónde:** `ActivityRules`, `SampleCollectionRules(sampleCount)`, `DivingRules(maxDepthMeters)`, `StaffRequirement`, `RiskLevel`.
- **Por qué:**
  - De un tipo de actividad a otro no cambian solo las fórmulas, también los datos que usan (cantidad de muestras, profundidad). Cada implementación recibe sus datos en el constructor, así `Activity` tiene solo los campos comunes y los métodos de `ActivityRules` no necesitan parámetros.
  - Los requisitos de recursos se explican en D12.
  - `StaffRequirement` decide si un candidato califica a través de `Certifiable` (D10), sin conocer la clase concreta.
- **Alternativas descartadas:**
  - Pasar la `Activity` a cada método (`estimateDuration(Activity)`): hoy ninguna regla usa datos de la actividad, y además crea una dependencia circular entre `Activity` y sus reglas. Consecuencia: si una regla llega a necesitar un dato común (ej. la zona), hay que agregar el parámetro en todas las implementaciones.
  - Campos específicos de un tipo en `Activity` (ej. `depth` nullable): mezclaría datos de tipos distintos en una sola clase.


### D12 — Requisitos de recursos separados por forma de uso, en espejo con los recursos

- **Patrón / principio:** SRP + value objects, sobre el mismo eje que D7.
- **Dónde:** `ResourceCategory`, `ReusableRequirement(category, count)` y `DepletableRequirement(category, quantity)`; `ActivityRules.requiredEquipment()` y `requiredSupplies()`.
- **Por qué:**
  - Una regla de actividad describe **qué necesita** ("1 bote", "20 litros de combustible"), no qué instancia concreta usar. Elegir la instancia es trabajo de la asignación y de la replanificación.
  - Un recurso responde dos preguntas: **qué es** (`ResourceCategory`: bote, sonar, combustible) y **cómo se usa** (las interfaces de D7: se reserva o se gasta).
  - Los requisitos siguen esa misma división. Uno reutilizable se cuenta en unidades enteras y se cumple con recursos libres en el horario. Uno consumible se mide con `Quantity` y se cumple con stock. Son reglas distintas que cambian por motivos distintos, así que no es duplicación.
- **Alternativas descartadas:**
  - Un único `ResourceRequirement(tipo, Quantity)`: permite pedir "1.5 botes" y obliga a cada cliente a preguntar qué clase de requisito es (`if`/`instanceof` repetido en validación, asignación y estimación).
  - Requisitos que apuntan a instancias concretas (el bote `v-1`): atan la regla a un catálogo y dejan sin sentido la asignación y la replanificación.
  - Interfaz padre `Requirement`: hoy nadie necesita una lista mezclada. Se agrega si aparece ese caso.
- **Pendiente (nivel 3):** los recursos del catálogo tienen que declarar su `ResourceCategory` para poder compararse con los requisitos, y esa comparación tiene que vivir en un solo lugar.

<!-- Copiar el bloque por cada decisión nueva (D13, D14, ...) a medida que se cierren los niveles del plan.md -->


## Patrones que decidimos no aplicar

| Patrón | Por qué no                                                                                                                                                                                              | Consecuencias |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| Decorator | Para el caso de "advertencia aceptada con justificación" alcanza con un campo de justificación en el resultado de validación aceptado; no hace falta envolver el objeto.                                | Si más adelante se necesita apilar más comportamiento sobre una validación aceptada (ej. distintos tipos de justificación con reglas propias), habría que reconsiderarlo. |
| State (máquina de estados formal) | El estado de la expedición es por ahora un enum simple; las reglas de transición hacia "Approved" ya están resueltas por el `ValidationOrchestrator` (D2), sin necesitar una máquina de estados aparte. | Si las transiciones ganan reglas propias más allá de la aprobación (ej. condiciones para pasar a "Suspendida" o "Finalizada"), puede volverse difícil de seguir sin formalizar las transiciones. |
| Strategy de conversión entre sistemas de medida | Consideramos una posible extensión a futuro con más de un sistema de medida (ej. métrico e imperial: litros y galones, kilogramos y libras). Hoy hay un único sistema, así que `MeasurementUnit` es un enum simple y `Quantity` rechaza operar entre unidades distintas en vez de convertirlas. | Si se agrega otro sistema, habría que introducir una abstracción de conversión (ej. `UnitConverter`) que `Quantity` use para normalizar antes de sumar, restar o comparar. Como hoy toda la aritmética de unidades está encapsulada en `Quantity` (D8), el cambio queda en ese value object y no se propaga al resto del dominio. |

## Supuestos

- La disponibilidad de personas/vehículos/instrumentos se valida por franjas horarias dentro del período de la expedición (permitiendo participar de más de una expedición si los horarios no chocan), no por exclusividad total del recurso durante todo el período. **Pendiente de confirmar.**
- "Permisos" y "certificaciones" son conceptos distintos: permisos a nivel expedición/zona (autorización regulatoria), certificaciones a nivel recurso (habilitación puntual). Se modelan certificaciones también para vehículos e instrumentos, no solo personas. **Pendiente de confirmar**
- Los consumibles se modelan como un tipo de recurso aparte de personal/vehículos/instrumentos, con disponibilidad por stock (`DepletableResource`) en vez de por tiempo (`ReusableResource`). **Pendiente de confirmar.**
- Se asume que "disponibilidad" en el catálogo de recursos no es un único concepto: para recursos reutilizables es disponibilidad temporal, para consumibles es stock. **Pendiente de confirmar.**
- Los valores de `SampleCollectionRules` y `DivingRules` (tiempos, umbral de profundidad, cantidad de buzos y equipamiento) son ilustrativos: el enunciado no los define.
- Las restricciones de una actividad todavía no se modelan: ninguna regla las usa y no está definido qué forma tienen. Se agregan (como value object, no como `String`) cuando una validación las necesite.
 