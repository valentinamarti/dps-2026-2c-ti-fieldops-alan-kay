# Decisiones de diseño — Grupo Alan-Kay

## Modelo de dominio

`Expedition` es la entidad central: tiene objetivos, período, zonas de trabajo, responsables, permisos, restricciones, un itinerario (lista ordenada de actividades programadas) y un estado (borrador / revisada / aprobada / en ejecución / suspendida / finalizada). Se apoya en un catálogo de recursos (personas, vehículos, instrumentos, consumibles) y en un catálogo de zonas.

Cada actividad del itinerario referencia una `Activity` (con sus reglas propias según el tipo — duración, riesgo, recursos y personal requerido) más el horario real y los recursos asignados para esa expedición puntual. Antes de poder aprobarse, una expedición se corre contra un conjunto de reglas de validación (superposiciones, recursos faltantes, certificaciones, permisos, capacidad); las críticas bloquean la aprobación, las advertencias se pueden aceptar dejando una justificación registrada.

## Patrones y principios aplicados

### D1 — Tipos de actividad: composición en vez de herencia (Strategy)

- **Patrón / principio:** Strategy (Open/Closed).
- **Dónde:** `Activity` (en `business.models.activities`) + interfaz `ActivityRules` (en `business.interfaces.activities`); implementaciones `SampleCollectionRules` y `DivingRules` en `business.rules`.
- **Por qué:** `Activity` es una única clase con los campos comunes obligatorios a todo tipo (nombre, ventana temporal, zona, dependencias, restricciones), y delega en `ActivityRules` todo lo que varía según el tipo: duración estimada, riesgo, recursos y personal requerido. El enunciado pide que cada tipo tenga sus propias reglas, y agregar un tipo nuevo debe significar agregar una clase, no modificar un `if`/`switch` existente.
- **Alternativas descartadas:** herencia (subclases de `Activity` por tipo) — mezclaría campos comunes con comportamiento variable en la misma jerarquía. `enum` + `switch` por tipo — antipatrón señalado en clase, viola Open/Closed.


### D2 — Validaciones como objetos con severidad (Policy) + orquestador

- **Patrón / principio:** Policy.
- **Dónde:** interfaz `ValidationRule`, sus implementaciones concretas, y `ValidationOrchestrator`.
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
- **Dónde:** `business.interfaces.resources`: `Resource` (padre, solo `getId()`), `ReusableResource` (`Person`, `Vehicle`, `Instrument`) y `DepletableResource` (`Depletable`). `Vehicle` e `Instrument` llegan a `ReusableResource` a través de `Equipment` (D17). Las implementaciones viven en `business.models.resources`.
- **Por qué:** un recurso reutilizable se reserva por franjas horarias (`isAvailableDuring`, `reserve`) y uno consumible se agota (`hasStockFor`, `consume`). Son preguntas distintas que hacen clientes distintos, así que cada una tiene su interfaz chica. `Resource` es el tipo común para cuando alcanza con identificar el recurso (ej. los recursos asignados a un ítem del itinerario). No se usa con `instanceof` para decidir cómo tratar cada tipo: para eso están las interfaces hijas.
- **Alternativas descartadas:** una única interfaz `Resource` con los cuatro métodos, que obligaría a implementar métodos sin sentido (un combustible no se "reserva" por horario; una persona no tiene stock). Converger después, si la cátedra confirma que es un único concepto, rompe menos código que separar una interfaz ya unificada.


### D8 — Reificación de conceptos del dominio en value objects

- **Patrón / principio:** value objects.
- **Dónde:** `TimePeriod`, `Quantity` + `MeasurementUnit`, `Certification`, `Permit`.
- **Por qué:** cada uno encapsula su regla en un solo lugar. `TimePeriod` resuelve la superposición como intervalo semiabierto `[inicio, fin)`, así que dos franjas que solo se tocan en el borde no chocan. `Quantity` usa `BigDecimal`, no admite negativos y no permite operar con unidades distintas. `Certification` y `Permit` evitan comparar `String` sueltos. Son `record` inmutables con igualdad por valor.
- **Detalle de implementación:** `Quantity` normaliza el monto con `stripTrailingZeros()` en su constructor compacto, porque `BigDecimal.equals` compara también la escala (`20` no sería igual a `20.0`) y eso rompería la igualdad por valor que da el `record`. Sin esa normalización, dos cantidades que representan lo mismo no serían intercambiables como claves ni en asserts. La contracara es que `stripTrailingZeros()` escribe `100` como `1E+2`, así que `Quantity` define su propio `toString()` con `toPlainString()`: los mensajes de error dicen `100 LITERS` y no `1E+2 LITERS`.
- **Alternativas descartadas:** `LocalDateTime` sueltos, `double` + `String` de unidad y listas de `String` para certificaciones y permisos. Esto repartiría la lógica de solapamiento y de unidades en cada clase que la use. Comparar con `compareTo` en vez de normalizar la escala: obligaría a redefinir `equals`/`hashCode` a mano y perdería la ventaja de usar `record`.


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
- **Resuelto en D17 y D18:** los recursos del catálogo declaran su `ResourceCategory` a través de `Categorized`, y la comparación contra los requisitos vive en los propios requisitos.

### D13 — La zona es dueña de la comparación de permisos

- **Patrón / principio:** Information Expert (la lógica vive donde están los datos).
- **Dónde:** `Zone.missingPermits(grantedPermits)` y `Zone.isAccessibleWith(grantedPermits)`.
- **Por qué:** la `Zone` es la que sabe qué permisos exige, así que es la que compara contra los que tiene la expedición. La validación de permisos faltantes (D2) no necesita leer `requiredPermits` ni saber cómo se comparan: le pregunta a la zona. `missingPermits` devuelve **cuáles** faltan y no solo un booleano, porque el resultado de la validación tiene que poder decir qué permiso falta; `isAccessibleWith` es el atajo para cuando eso no interesa.
- **Alternativas descartadas:** exponer `requiredPermits` con un getter y comparar afuera — repartiría la comparación entre la validación, la asignación y el informe. Devolver solo `boolean` — obligaría a recalcular la diferencia en el lugar donde se arma el mensaje de error.


### D14 — Las preguntas devuelven `boolean`, los comandos fallan con excepción

- **Patrón / principio:** Command-Query Separation + fail fast.
- **Dónde:** `ReusableResource.isAvailableDuring` / `reserve`, `DepletableResource.hasStockFor` / `consume`; `ResourceUnavailableException` e `InsufficientStockException` en `business.exceptions`.
- **Por qué:** cada contrato de recurso (D7) tiene una consulta y un comando. La consulta responde `boolean` porque es exactamente lo que las validaciones (D2) necesitan para decidir sin romper nada: corren sobre una expedición en borrador y tienen que poder juntar **todos** los problemas, no frenarse en el primero. El comando, en cambio, no tiene forma válida de "fallar a medias": si `reserve` o `consume` se ejecutan sobre un recurso que no da, es un error de programación (alguien no consultó antes), y devolver `false` lo dejaría pasar silenciosamente. Por eso son excepciones *unchecked*: no son casos de negocio esperados que el llamador deba manejar, son invariantes rotos. `Depletable.consume` verifica antes de modificar, así que el stock queda intacto cuando falla.
- **Límite conocido:** `hasStockFor` sí lanza `IllegalArgumentException` si la unidad no coincide, porque `Quantity` no opera entre unidades distintas (D8). No es una excepción a la regla de arriba: "no alcanza el stock" es una respuesta válida (`false`), pero "¿me alcanzan 20 litros para 1 kilogramo?" es una pregunta mal formada, no un problema de la expedición. La validación de recursos faltantes (D2) tiene que comparar contra requisitos de la misma unidad; si llega a necesitar tolerar el desajuste, va a hacer falta un `UnitConverter` (ver la tabla de patrones no aplicados).
- **Alternativas descartadas:** comandos que devuelven `boolean` — el `if` de chequeo es fácil de omitir y el error aparece mucho después. Excepciones *checked* — obligarían a un `try/catch` en cada llamador que ya consultó y sabe que va a andar. Devolver `Optional`/`Result` — suma ceremonia sin agregar información: acá no hay más de un motivo de falla por operación.


### D15 — Entidades por identidad (clases), value objects por valor (`record`)

- **Patrón / principio:** distinción entidad / value object.
- **Dónde:** clases con identidad: `Person`, `Vehicle`, `Instrument`, `Depletable`, `Zone`, `Activity`. `record` con igualdad por valor: `TimePeriod`, `Quantity`, `Certification`, `Certifications`, `Permit`, `ResourceCategory`, `ReusableRequirement`, `DepletableRequirement`, `StaffRequirement`.
- **Por qué:**
  - Las entidades tienen `id` pero **no** redefinen `equals`/`hashCode`: se comparan por identidad de instancia. Dos `Vehicle` con la misma patente son dos vehículos distintos del catálogo, y cada uno tiene su propio `AvailabilityCalendar` (D9) — igualarlos por `id` haría que reservar uno pareciera reservar al otro.
  - De ahí se apoya `Activity.dependsOn`: una dependencia apunta a *esa* actividad concreta, no a "cualquier actividad que se llame igual".
  - Los value objects son `record`: inmutables, sin identidad propia, intercambiables si valen lo mismo (D8).
- **Convención de accessors:** los `record` exponen sus componentes con el nombre del campo (`name()`, `amount()`), que es lo que genera el lenguaje. Las entidades exponen `getX()` generado por Lombok `@Getter`, para no escribir boilerplate a mano. Son dos estilos porque son dos categorías de objeto distintas, no un descuido.
- **`@Getter` va por campo, nunca sobre la clase:** anotar la clase expone *todo* el estado interno, y varias entidades tienen colaboradores que no deben salir. Queda privado lo que ya tiene un método que responde la pregunta: `requiredPermits` en `Zone` (se pregunta con `missingPermits`, D13), `calendar` en `Person`/`Vehicle`/`Instrument` (se pregunta con `isAvailableDuring`, D9), `certifications` en los mismos (se pregunta con `hasCertification`, D10) y `rules` en `Activity` (se delega con `estimatedDuration` y compañía, D1). Exponerlos dejaría que un cliente se saltee la abstracción: si `Activity` devolviera su `ActivityRules`, el Strategy de D1 pasaría a ser público y cualquiera podría puentear la delegación.
- **Caso aparte — `Depletable.getStock()`:** este sí se expone. `stock` es un campo mutable (se reasigna al consumir) pero el `Quantity` que devuelve es inmutable (D8), así que entrega una foto del estado actual y no una referencia con la que romper invariantes. El informe de expedición (D4) necesita leer el consumo real, y no hay forma de obtenerlo preguntando `hasStockFor`.
- **Nota de build:** desde JDK 23 `javac` no corre los annotation processors que encuentra en el classpath. Lombok tiene que declararse explícitamente en el `<annotationProcessorPaths>` del `maven-compiler-plugin`; si falta, `@Getter` no genera nada y el proyecto no compila. El plugin ya activa el procesamiento por su cuenta al ver esa lista, así que no hace falta agregar `<proc>full</proc>`.
- **Alternativas descartadas:** `equals`/`hashCode` por `id` en las entidades — dos instancias con el mismo `id` pero distinto estado (distintas reservas, distinto stock) se tratarían como la misma. Hacer `record` todas las clases — los recursos son mutables por definición (se reservan, se consumen) y `Activity` referencia otras `Activity`, donde la identidad es justamente lo que importa. El caso menos obvio es `Zone`, que es inmutable y técnicamente podría ser un `record`: se deja como entidad porque pertenece a un catálogo y las expediciones la referencian por identidad — dos zonas con el mismo nombre son dos zonas distintas, no la misma.


### D16 — Guards de argumentos compartidos, en un solo lugar

- **Patrón / principio:** fail fast + DRY.
- **Dónde:** `DomainArguments` (`requireText`, `requireSet`, `requireList`), usado por `Person`, `Vehicle`, `Instrument`, `Depletable`, `Zone`, `Activity`, `Certification`, `Certifications`, `Permit`, `ResourceCategory` y `StaffRequirement`.
- **Por qué:** antes cada clase validaba distinto. Cuatro rechazaban un `id` en blanco y cinco lo aceptaban, así que `new Person("", "Ana", Set.of())` construía una persona sin identidad que recién iba a romper mucho después. Y las colecciones nulas fallaban dentro de `Set.copyOf`, con el mensaje interno del JDK (`Cannot invoke "java.util.Collection.isEmpty()" because "coll" is null`) en vez de decir qué campo faltaba. Ahora la regla vive en una sola clase: un texto obligatorio no puede ser nulo ni estar en blanco, y una colección obligatoria no puede ser nula y siempre se copia a una versión inmutable.
- **Sobre los tipos de excepción:** un texto mal formado es `IllegalArgumentException` (el argumento llegó, pero no sirve); una colaboración ausente es `NullPointerException` con el nombre del campo, que es la semántica de `Objects.requireNonNull` y la que ya usaban `Activity` y `Depletable` para sus colaboradores. Se mantuvo esa división en vez de unificar todo en una sola, para no cambiar el comportamiento ya testeado.
- **Alternativas descartadas:**
  - Repetir el guard privado que tenía `Activity` en cada clase: el mismo bloque duplicado en nueve lugares, y la garantía de que la próxima clase se olvide de alguna rama.
  - Reificar el identificador en un value object (`ResourceId`), que es lo que hace D8 con `Certification` y `Permit`: es el camino más OO y queda abierto, pero hoy un `id` no tiene ninguna regla propia más allá de "no está vacío" — sería un tipo nuevo por cada entidad sin comportamiento que justifique el costo de tocar todas las firmas. Se reconsidera si los ids ganan reglas (formato, unicidad dentro del catálogo).
- **Consecuencia:** `DomainArguments` es una clase de métodos estáticos, que no es orientada a objetos. Se acepta porque es exactamente el rol de `Objects.requireNonNull` en la biblioteca estándar: una precondición, no una responsabilidad del dominio. Si crece más allá de guards de argumentos, es señal de que hay un concepto sin reificar.

### D17 — La categoría como capacidad; el requisito decide si un recurso lo cumple

- **Patrón / principio:** Interface Segregation (ISP) + Information Expert, mismo criterio que D10 y D13.
- **Dónde:** interfaz `Categorized` (`belongsTo`); `Equipment extends ReusableResource, Categorized` (implementada por `Vehicle` e `Instrument`); `DepletableResource extends Resource, Categorized` (`Depletable`). La comparación vive en `ReusableRequirement.accepts` y `DepletableRequirement.isCoveredBy`.
- **Por qué:**
  - Cierra el pendiente de D12: para saber si un recurso del catálogo cumple un requisito, el recurso tiene que declarar qué es.
  - El requisito es el que conoce la categoría y la cantidad pedidas, así que es el que decide si un candidato lo cumple. El recurso solo responde `belongsTo`, sin exponer su categoría. Validación, asignación y replanificación le preguntan al requisito, y la regla de "cumple" queda en un solo lugar.
  - `Person` no implementa `Categorized`: el personal se pide por certificaciones (`StaffRequirement`, D10 y D11). Darle una categoría sería agregar un campo que nadie consulta.
  - `Equipment` existe porque el catálogo y la asignación necesitan un tipo que sea a la vez "se reserva por horario" y "se pide por categoría". Sin él, el catálogo tendría una lista de `Vehicle` y otra de `Instrument`, y sumar un tipo de equipo nuevo (ej. un dron) obligaría a modificarlo (Open/Closed).
  - `DepletableResource` extiende `Categorized` directamente porque todo consumible se pide por categoría: una interfaz de rol aparte no tendría un cliente distinto que la justifique.
- **Alternativas descartadas:**
  - Exponer `getCategory()` y comparar afuera: la comparación se repetiría en el catálogo, la validación y la asignación.
  - Poner la categoría en `Resource`: obligaría a `Person` a tener una.
  - Resolver el matching dentro del catálogo: el catálogo tendría que conocer la regla de cada tipo de requisito; si esa regla cambia (ej. categorías equivalentes), habría que tocar el catálogo y no el requisito.
- **Consecuencia:** si un requisito de consumible y un recurso de su misma categoría usan unidades distintas, `isCoveredBy` falla con excepción. Es el límite ya documentado en D14: indica un catálogo mal cargado, no un faltante de la expedición.


### D18 — Catálogo de recursos: un recurso por id, consultas por requisito

- **Patrón / principio:** encapsulamiento de colección + invariante de unicidad.
- **Dónde:** `ResourceCatalog` (`addStaff`, `addEquipment`, `addSupply`, `availableStaffFor`, `availableEquipmentFor`, `suppliesCovering`) y `DuplicateResourceException`.
- **Por qué:**
  - El enunciado nombra los "recursos duplicados" como uno de los problemas a resolver. El catálogo rechaza un segundo recurso con un `id` ya registrado, con un único espacio de ids para todos los tipos: una asignación o un informe que nombra un `id` nunca es ambiguo.
  - Es lo que hace seguro comparar entidades por instancia (D15): con una sola instancia por `id`, la identidad de instancia coincide con la identidad de negocio y no puede haber dos calendarios para el mismo recurso real.
  - Las consultas devuelven **candidatos**, no asignaciones: el catálogo responde "quién puede" (cumple el requisito y está libre, o tiene stock); "quién va" es trabajo de la asignación. Si cambia el criterio para elegir entre candidatos, el catálogo no cambia.
  - Mismo criterio que D14: registrar es un comando y falla con excepción; consultar devuelve una lista, posiblemente vacía.
- **Alternativas descartadas:**
  - Detectar duplicados recién en la validación de la expedición: llega tarde, el mismo recurso real ya pudo reservarse en dos instancias distintas.
  - Un catálogo por tipo de recurso: repetiría la regla de unicidad y permitiría repetir un `id` entre tipos.
  - Exponer las listas con getters y filtrar afuera: cada cliente reimplementaría "cumple el requisito y está libre".
- **Consecuencia:** es una clase concreta en memoria. Si más adelante hay persistencia, el contrato se extrae a `business/providers` con estas mismas consultas y esta clase pasa a ser un detalle.

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
- `Activity.dependsOn` responde solo por las dependencias **directas**, no por las transitivas: hoy nadie recorre el grafo. No hace falta detectar ciclos porque son imposibles por construcción — `dependencies` es `final`, se copia con `List.copyOf` en el constructor y no hay setter, así que una actividad solo puede depender de otras que ya existían cuando se la creó. El orden y la transitividad se resuelven en el itinerario (D6), que es donde se va a validar que una actividad no arranque antes de que terminen sus dependencias.
 